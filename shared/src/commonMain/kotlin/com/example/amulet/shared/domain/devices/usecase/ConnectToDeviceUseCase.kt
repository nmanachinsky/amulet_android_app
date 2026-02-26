package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.devices.model.BleConnectionState
import com.example.amulet.shared.domain.devices.repository.DeviceConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class ConnectToDeviceUseCase(
    private val deviceConnectionRepository: DeviceConnectionRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    suspend operator fun invoke(bleAddress: String): Flow<BleConnectionState> {
        val userId = observeCurrentUserIdUseCase().first() ?: return flowOf(BleConnectionState.Failed(AppError.Unauthorized))
        return deviceConnectionRepository.connectToDevice(userId, bleAddress)
    }
}
