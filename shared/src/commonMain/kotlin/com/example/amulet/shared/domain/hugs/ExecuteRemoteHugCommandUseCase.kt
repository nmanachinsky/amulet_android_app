package com.example.amulet.shared.domain.hugs

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.hugs.model.HugId
import com.example.amulet.shared.domain.hugs.model.HugStatus
import com.example.amulet.shared.domain.hugs.model.RemoteHugCommand
import com.example.amulet.shared.domain.hugs.model.PairMemberSettings
import com.example.amulet.shared.domain.playback.DevicePlaybackEngine
import com.example.amulet.shared.domain.playback.PlayableMedia
import com.example.amulet.shared.domain.patterns.usecase.GetPatternByIdUseCase
import com.example.amulet.shared.domain.patterns.usecase.EnsurePatternLoadedUseCase
import com.example.amulet.shared.domain.practices.usecase.GetUserPreferencesStreamUseCase
import com.example.amulet.shared.core.logging.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ExecuteRemoteHugCommandUseCase(
    private val getPatternById: GetPatternByIdUseCase,
    private val playbackEngine: DevicePlaybackEngine,
    private val updateHugStatus: UpdateHugStatusUseCase,
    private val ensurePatternLoaded: EnsurePatternLoadedUseCase,
    private val pairsRepository: PairsRepository,
    private val getUserPreferencesStream: GetUserPreferencesStreamUseCase,
) {

    /**
     * Выполнить удалённую команду «объятия».
     *
     * @param command входящая команда из push/бекенда.
     * @param intensity относительная интенсивность паттерна (0.0..1.0).
     */
    suspend operator fun invoke(
        command: RemoteHugCommand,
        intensity: Double = 1.0,
    ): AppResult<Unit> {
        // Сначала проверяем глобальный DND и pair-level настройки получателя: mute / тихие часы.
        if (shouldSuppressPlayback(command)) {
            val hugId = command.hugId
            if (hugId != null) {
                updateHugStatus(hugId, HugStatus.DELIVERED)
            }
            return Ok(Unit)
        }

        val patternId = command.patternIdOverride
            ?: command.emotion?.patternId
            ?: return Err(AppError.NotFound)

        // 1. Пробуем взять паттерн локально.
        Logger.d(
            "ExecuteRemoteHugCommandUseCase: resolve pattern start hugId=${command.hugId?.value} patternId=${patternId.value}",
            tag = "ExecuteRemoteHugCommandUseCase"
        )
        var pattern = getPatternById(patternId).firstOrNull()
        if (pattern == null) {
            Logger.d(
                "ExecuteRemoteHugCommandUseCase: pattern not in local DB -> ensurePatternLoaded patternId=${patternId.value}",
                tag = "ExecuteRemoteHugCommandUseCase"
            )
            // 2. Пытаемся загрузить паттерн с сервера и сохранить локально.
            val ensured = ensurePatternLoaded(patternId)
            if (!ensured.isOk) {
                val error = ensured.component2()
                Logger.w(
                    "ExecuteRemoteHugCommandUseCase: ensurePatternLoaded failed patternId=${patternId.value} error=$error",
                    tag = "ExecuteRemoteHugCommandUseCase"
                )
                // Прокидываем ошибку загрузки паттерна наверх.
                return ensured
            }

            // 3. Повторно читаем из репозитория.
            pattern = getPatternById(patternId).filterNotNull().first()
        }

        Logger.d(
            "ExecuteRemoteHugCommandUseCase: pattern resolved patternId=${patternId.value} -> play",
            tag = "ExecuteRemoteHugCommandUseCase"
        )

        val playbackResult = playbackEngine.play(
            PlayableMedia.Preview(pattern.spec, intensity)
        )

        playbackResult.onSuccess {
            val hugId = command.hugId
            if (hugId != null) {
                // Помечаем объятие как доставленное на устройство.
                updateHugStatus(hugId, HugStatus.DELIVERED)
            }
        }

        return playbackResult
    }

    private suspend fun shouldSuppressPlayback(command: RemoteHugCommand): Boolean {
        // Глобальный DND для «объятий» отключает реакции амулета для всех пар.
        val globalPrefs = getUserPreferencesStream().first()
        if (globalPrefs.hugsDndEnabled) return true

        val pairId = command.pairId ?: return false
        val toUserId = command.toUserId ?: return false

        val pair = pairsRepository.observePair(pairId).firstOrNull() ?: return false
        val member = pair.members.firstOrNull { it.userId == toUserId } ?: return false
        val settings = member.settings

        if (settings.muted) return true

        return isInQuietHours(settings)
    }

    private fun isInQuietHours(settings: PairMemberSettings): Boolean {
        val start = settings.quietHoursStartMinutes
        val end = settings.quietHoursEndMinutes
        if (start == null || end == null) return false

        val nowLocal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMinutes = nowLocal.hour * 60 + nowLocal.minute

        return if (start <= end) {
            currentMinutes in start until end
        } else {
            // Интервал через полночь, например 22:00–07:00
            currentMinutes >= start || currentMinutes < end
        }
    }
}
