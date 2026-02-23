package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.repository.DevicesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ObserveDevicesUseCase(
    private val devicesRepository: DevicesRepository,
    private val observeCurrentUserIdUseCase: ObserveCurrentUserIdUseCase
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Device>> =
        observeCurrentUserIdUseCase().flatMapLatest { userId ->
            userId?.let { devicesRepository.observeDevices(it) }
                ?: flowOf(emptyList())
        }
}
