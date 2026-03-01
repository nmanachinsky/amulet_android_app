package com.example.amulet.shared.domain.devices.usecase

import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.devices.model.BleConnectionState
import com.example.amulet.shared.domain.devices.repository.DeviceConnectionRepository
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first


class AutoConnectLastDeviceUseCase(
    private val deviceRegistryRepository: DeviceRegistryRepository,
    private val deviceConnectionRepository: DeviceConnectionRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke() {
        Logger.d("AutoConnect: start", tag = TAG)
        
        val result = getUserIdWithRetry()
        if (result == null) {
            Logger.d("AutoConnect: no userId, result=$result", tag = TAG)
            return
        }
        
        val lastUsed = try {
            deviceRegistryRepository.getLastConnectedDevice(result)
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
            val flow = deviceConnectionRepository.connectToDevice(result, lastUsed.bleAddress)
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

    private suspend fun getUserIdWithRetry(): UserId? {
        var delayMs = INITIAL_RETRY_DELAY_MS
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            val result = getCurrentUserIdUseCase()
            if (result.isOk) {
                return result.component1()
            }
            Logger.d("AutoConnect: no userId, attempt=${attempt + 1}/$MAX_RETRY_ATTEMPTS, delaying ${delayMs}ms", tag = TAG)
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
        }
        return null
    }

    private companion object {
        private const val TAG = "AutoConnectLastDevice"
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val INITIAL_RETRY_DELAY_MS = 500L
        private const val MAX_RETRY_DELAY_MS = 8000L
    }
}
