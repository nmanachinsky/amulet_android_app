package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.practices.model.PracticeId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HasPracticeScriptOnDeviceUseCase(
    private val deviceControlRepository: DeviceControlRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    /**
     * Проверяет, существует ли скрипт практики на текущем подключенном устройстве.
     */
    suspend operator fun invoke(practiceId: PracticeId): Boolean = withContext(dispatcher) {
        try {
            val result = deviceControlRepository.hasPracticeScript(practiceId)
            result.isOk
        } catch (_: Exception) {
            false
        }
    }
}
