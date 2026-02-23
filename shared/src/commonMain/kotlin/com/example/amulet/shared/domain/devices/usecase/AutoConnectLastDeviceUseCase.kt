package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.devices.model.BleConnectionState
import com.example.amulet.shared.domain.devices.repository.DevicesRepository
import kotlinx.coroutines.flow.first

class AutoConnectLastDeviceUseCase(
    private val devicesRepository: DevicesRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke() {
        Logger.d("AutoConnect: start", tag = TAG)
        
        val result = getCurrentUserIdUseCase()
        if (!result.isOk) {
            Logger.d("AutoConnect: no userId, result=$result", tag = TAG)
            return
        }
        val userId = result.component1()!!
        
        val lastUsed = try {
            devicesRepository.getLastConnectedDevice(userId)
        } catch (e: Exception) {
            Logger.e("AutoConnect: failed to get last connected device: $e", tag = TAG)
            null
        }

        if (lastUsed == null) {
            Logger.d("AutoConnect: no last connected device found", tag = TAG)
            return
        }

        Logger.d("AutoConnect: last device id=${lastUsed.id.value} ble=${lastUsed.bleAddress}", tag = TAG)

        try {
            val flow = devicesRepository.connectToDevice(userId, lastUsed.bleAddress)
            flow.first { state ->
                when (state) {
                    is BleConnectionState.Connected -> {
                        Logger.d("AutoConnect: connected to ${lastUsed.bleAddress}", tag = TAG)
                        true
                    }
                    is BleConnectionState.Failed -> {
                        Logger.e("AutoConnect: failed to connect to ${lastUsed.bleAddress} error=${state.error}", tag = TAG)
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Logger.e("AutoConnect: exception during connect to ${lastUsed.bleAddress}: $e", tag = TAG)
        }
    }

    private companion object {
        private const val TAG = "AutoConnectLastDevice"
    }
}
