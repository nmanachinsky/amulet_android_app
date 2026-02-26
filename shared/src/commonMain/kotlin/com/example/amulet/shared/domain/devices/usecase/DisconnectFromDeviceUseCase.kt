package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.repository.DeviceConnectionRepository

/**
 * UseCase для отключения от текущего устройства.
 */
class DisconnectFromDeviceUseCase(
    private val deviceConnectionRepository: DeviceConnectionRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return deviceConnectionRepository.disconnectFromDevice()
    }
}
