package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.practices.model.PracticeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

        // Ожидаем начала воспроизведения (детали BLE-протокола скрыты в репозитории)
        deviceControlRepository.awaitPlaybackStarted(practiceId, timeoutMs)
    }

    companion object {
        private const val DEFAULT_PLAY_TIMEOUT_MS = 5_000L
    }
}
