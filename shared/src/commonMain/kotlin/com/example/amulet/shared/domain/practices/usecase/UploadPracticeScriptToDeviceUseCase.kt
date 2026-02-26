package com.example.amulet.shared.domain.practices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.practices.model.Practice
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadPracticeScriptToDeviceUseCase(
    private val deviceControlRepository: DeviceControlRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    suspend operator fun invoke(practice: Practice): AppResult<Unit> = withContext(dispatcher) {
        deviceControlRepository.uploadPracticeScript(practice)
    }
}
