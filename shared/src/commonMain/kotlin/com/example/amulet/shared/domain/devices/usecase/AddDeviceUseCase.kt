package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository
import com.github.michaelbull.result.flatMap

class AddDeviceUseCase(
    private val deviceRegistryRepository: DeviceRegistryRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(
        bleAddress: String,
        name: String,
        hardwareVersion: Int
    ): AppResult<Device> = getCurrentUserIdUseCase().flatMap { userId ->
        deviceRegistryRepository.addDevice(userId, bleAddress, name, hardwareVersion)
    }
}
