package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.domain.practices.PracticesRepository
import com.example.amulet.shared.domain.courses.CoursesRepository
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.practices.model.PracticeFilter
import com.example.amulet.shared.domain.practices.model.PracticeSession
import com.example.amulet.shared.domain.practices.model.PracticeSessionSource
import com.example.amulet.shared.domain.practices.model.ScheduledSession
import com.example.amulet.shared.domain.practices.model.ScheduledSessionStatus
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.isoDayNumber
import kotlin.time.ExperimentalTime
import kotlin.time.Clock

class GetScheduledSessionsForDateRangeUseCase(
    private val practicesRepository: PracticesRepository,
    private val coursesRepository: CoursesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalTime::class)
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<ScheduledSession>> {
        return observeCurrentUserIdUseCase().flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(emptyList())
            }
            combine(
                practicesRepository.getSchedulesStream(userId),
                practicesRepository.getPracticesStream(userId, PracticeFilter()),
                coursesRepository.getCoursesStream(),
                practicesRepository.getSessionsHistoryStream(userId, null)
            ) { schedules, practices, courses, sessionsHistory ->
                val practicesMap = practices.associateBy { it.id }
                val coursesMap = courses.associateBy { it.id }

                val skippedIds: Set<String> = sessionsHistory.mapNotNull { session: PracticeSession ->
                    when (val source = session.source) {
                        is PracticeSessionSource.ScheduleSkip -> source.scheduledId
                        else -> null
                    }
                }.toSet()

                val completedIds: Set<String> = sessionsHistory.mapNotNull { session: PracticeSession ->
                    when (val source = session.source) {
                        is PracticeSessionSource.FromSchedule -> source.scheduledId
                        else -> null
                    }
                }.toSet()
                val timeZone = TimeZone.currentSystemDefault()
                val now = Clock.System.now()
                val sessions = mutableListOf<ScheduledSession>()

                var currentDate = startDate
                while (currentDate <= endDate) {
                    val dayOfWeek = currentDate.dayOfWeek.isoDayNumber

                    val dailySchedules = schedules.filter { dayOfWeek in it.daysOfWeek }

                    for (schedule in dailySchedules) {
                        val (hour, minute) = schedule.timeOfDay.split(":").map { it.toInt() }
                        val scheduledDateTime = LocalDateTime(currentDate, LocalTime(hour, minute))
                        val scheduledInstant = scheduledDateTime.toInstant(timeZone)

                        val scheduledId = "${schedule.id}_${currentDate}"
                        if (scheduledId in skippedIds) {
                            currentDate = currentDate.plus(DatePeriod(days = 1))
                            continue
                        }

                        val status = when {
                            scheduledId in completedIds -> ScheduledSessionStatus.COMPLETED
                            scheduledInstant < now -> ScheduledSessionStatus.MISSED
                            else -> ScheduledSessionStatus.PLANNED
                        }

                        sessions.add(
                            ScheduledSession(
                                id = scheduledId,
                                practiceId = schedule.practiceId,
                                practiceTitle = practicesMap[schedule.practiceId]?.title ?: "Практика",
                                courseId = schedule.courseId,
                                scheduledTime = scheduledInstant.toEpochMilliseconds(),
                                status = status,
                                durationSec = practicesMap[schedule.practiceId]?.durationSec,
                                courseTitle = schedule.courseId?.let { coursesMap[it]?.title }
                            )
                        )
                    }
                    currentDate = currentDate.plus(DatePeriod(days = 1))
                }
                sessions.sortedBy { it.scheduledTime }
            }
        }
    }
}
