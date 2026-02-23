package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.devices.model.BleConnectionState
import com.example.amulet.shared.domain.devices.repository.DevicesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class ConnectToDeviceUseCase(
    private val devicesRepository: DevicesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    suspend operator fun invoke(bleAddress: String): Flow<BleConnectionState> {
        val userId = observeCurrentUserIdUseCase().first() ?: return flowOf(BleConnectionState.Failed(AppError.Unauthorized))
        return devicesRepository.connectToDevice(userId, bleAddress)
    }
}
