package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository

/**
 * Use case для удаления устройства из локальной БД.
 */
class RemoveDeviceUseCase(
    private val deviceRegistryRepository: DeviceRegistryRepository
) {
    suspend operator fun invoke(deviceId: DeviceId): AppResult<Unit> {
        return deviceRegistryRepository.removeDevice(deviceId)
    }
}
