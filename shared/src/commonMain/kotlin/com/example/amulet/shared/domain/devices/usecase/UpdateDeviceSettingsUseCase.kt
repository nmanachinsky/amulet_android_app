package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository

/**
 * UseCase для обновления настроек устройства.
 */
class UpdateDeviceSettingsUseCase(
    private val deviceRegistryRepository: DeviceRegistryRepository
) {
    suspend operator fun invoke(
        deviceId: DeviceId,
        name: String? = null,
        brightness: Double? = null,
        haptics: Double? = null,
        gestures: Map<String, String>? = null
    ): AppResult<Device> {
        return deviceRegistryRepository.updateDeviceSettings(
            deviceId = deviceId,
            name = name,
            brightness = brightness,
            haptics = haptics,
            gestures = gestures
        )
    }
}
