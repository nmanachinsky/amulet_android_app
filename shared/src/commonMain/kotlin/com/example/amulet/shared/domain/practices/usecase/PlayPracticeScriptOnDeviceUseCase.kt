package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.practices.model.PracticeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class PlayPracticeScriptOnDeviceUseCase(
    private val deviceControlRepository: DeviceControlRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    suspend operator fun invoke(
        practiceId: PracticeId,
        timeoutMs: Long = DEFAULT_PLAY_TIMEOUT_MS,
    ): AppResult<Unit> = withContext(dispatcher) {
        val commandResult = deviceControlRepository.playPracticeScript(practiceId)

        val error = commandResult.component2()
        if (error != null) {
            return@withContext commandResult
        }

        try {
            withTimeout(timeoutMs) {
                deviceControlRepository.observeNotifications(NotificationType.PATTERN)
                    .first { it.startsWith("NOTIFY:PATTERN:STARTED:$practiceId") }
            }
        } catch (_: Exception) {
            // Игнорируем таймаут/ошибки ожидания уведомления, оставляем исходный результат
        }

        commandResult
    }

    companion object {
        private const val DEFAULT_PLAY_TIMEOUT_MS = 5_000L
    }
}
