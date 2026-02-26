package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository

/**
 * UseCase для применения яркости устройства на физическом амулете.
 */
class ApplyDeviceBrightnessUseCase(
    private val deviceControlRepository: DeviceControlRepository
) {
    suspend operator fun invoke(
        deviceId: DeviceId,
        brightness: Double
    ): AppResult<Unit> {
        return deviceControlRepository.setBrightness(deviceId, brightness)
    }
}
