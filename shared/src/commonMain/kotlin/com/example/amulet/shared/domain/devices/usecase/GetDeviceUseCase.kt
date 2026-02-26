package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository

/**
 * UseCase для получения устройства по ID.
 */
class GetDeviceUseCase(
    private val deviceRegistryRepository: DeviceRegistryRepository
) {
    suspend operator fun invoke(deviceId: DeviceId): AppResult<Device> {
        return deviceRegistryRepository.getDevice(deviceId)
    }
}
