@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.amulet.shared.domain.dashboard.usecase

import com.example.amulet.shared.domain.hugs.ObserveHugsForUserUseCase
import com.example.amulet.shared.domain.hugs.model.Hug
import com.example.amulet.shared.domain.practices.MoodRepository
import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.practices.model.MoodEntry
import com.example.amulet.shared.domain.practices.model.MoodKind
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.user.usecase.ObserveCurrentUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Агрегированная статистика для дашборда.
 * Отдельная доменная модель, не завязанная на UI.
 */
data class DashboardDailyStats(
    val practiceMinutes: Int,
    val hugsCount: Int,
    val calmLevel: Int, // 0-100
)

class GetDashboardDailyStatsUseCase(
    private val practicesRepository: PracticesRepository,
    private val moodRepository: MoodRepository,
    private val observeHugsForUserUseCase: ObserveHugsForUserUseCase,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase,
) {

    operator fun invoke(): Flow<DashboardDailyStats> {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date

        return observeCurrentUserIdUseCase().flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(DashboardDailyStats(0, 0, DEFAULT_CALM_LEVEL))
            }

            val sessionsFlow = practicesRepository.getSessionsHistoryStream(userId, null)
            val moodsFlow = moodRepository.getMoodHistoryStream(userId)
            val hugsFlow = observeHugsForUserUseCase(userId)

            combine(sessionsFlow, moodsFlow, hugsFlow) { sessions, moods, hugs ->
                val practiceMinutes = calculateTodayPracticeMinutes(sessions, today, timeZone)
                val hugsCount = calculateTodayHugsCount(hugs, today, timeZone)
                val calmLevel = calculateCalmLevel(moods, today, timeZone)

                DashboardDailyStats(
                    practiceMinutes = practiceMinutes,
                    hugsCount = hugsCount,
                    calmLevel = calmLevel,
                )
            }
        }
    }

    private fun calculateTodayPracticeMinutes(
        sessions: List<PracticeSession>,
        today: kotlinx.datetime.LocalDate,
        timeZone: TimeZone,
    ): Int {
        return sessions
            .filter { it.completed && isSameDay(it.startedAt, today, timeZone) }
            .sumOf { session ->
                val seconds = session.actualDurationSec ?: session.durationSec ?: 0
                (seconds / 60).coerceAtLeast(0)
            }
    }

    private fun calculateTodayHugsCount(
        hugs: List<Hug>,
        today: kotlinx.datetime.LocalDate,
        timeZone: TimeZone,
    ): Int {
        return hugs.count { hug ->
            hug.createdAt.toLocalDateTime(timeZone).date == today
        }
    }

    private fun calculateCalmLevel(
        moods: List<MoodEntry>,
        today: kotlinx.datetime.LocalDate,
        timeZone: TimeZone,
    ): Int {
        if (moods.isEmpty()) return DEFAULT_CALM_LEVEL

        val todayMood = moods
            .filter { isSameDay(it.createdAt, today, timeZone) }
            .maxByOrNull { it.createdAt }

        val latest = todayMood ?: moods.maxByOrNull { it.createdAt }
        return moodToCalmLevel(latest?.mood)
    }

    private fun isSameDay(
        epochMillis: Long,
        date: kotlinx.datetime.LocalDate,
        timeZone: TimeZone,
    ): Boolean {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        return instant.toLocalDateTime(timeZone).date == date
    }

    private fun moodToCalmLevel(mood: MoodKind?): Int = when (mood) {
        MoodKind.RELAX, MoodKind.SLEEP -> 85
        MoodKind.HAPPY -> 90
        MoodKind.FOCUS -> 75
        MoodKind.NEUTRAL -> 65
        MoodKind.TIRED -> 55
        MoodKind.SAD -> 45
        MoodKind.NERVOUS, MoodKind.ANGRY -> 35
        null -> DEFAULT_CALM_LEVEL
    }

    private companion object {
        private const val DEFAULT_CALM_LEVEL: Int = 60
    }
}
