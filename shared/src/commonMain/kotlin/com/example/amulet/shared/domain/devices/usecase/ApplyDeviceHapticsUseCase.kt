package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository

/**
 * UseCase для применения силы вибрации устройства на физическом амулете.
 */
class ApplyDeviceHapticsUseCase(
    private val deviceControlRepository: DeviceControlRepository
) {
    suspend operator fun invoke(
        deviceId: DeviceId,
        haptics: Double
    ): AppResult<Unit> {
        return deviceControlRepository.setHaptics(deviceId, haptics)
    }
}
