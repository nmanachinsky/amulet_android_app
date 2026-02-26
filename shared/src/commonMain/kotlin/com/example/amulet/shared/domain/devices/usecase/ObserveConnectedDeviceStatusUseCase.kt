package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.domain.devices.model.DeviceLiveStatus
import com.example.amulet.shared.domain.devices.repository.DeviceConnectionRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase для наблюдения за живым статусом подключенного устройства.
 */
class ObserveConnectedDeviceStatusUseCase(
    private val deviceConnectionRepository: DeviceConnectionRepository
) {
    operator fun invoke(): Flow<DeviceLiveStatus?> {
        return deviceConnectionRepository.observeConnectedDeviceStatus()
    }
}
