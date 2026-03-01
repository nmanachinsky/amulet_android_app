package com.example.amulet.core.ble.internal

import com.example.amulet.core.ble.DeviceConnectionManager
import com.example.amulet.core.ble.model.ConnectionState
import com.example.amulet.core.ble.transport.BleGattClient
import com.example.amulet.shared.core.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceConnectionManagerImpl @Inject constructor(
    private val gattClient: BleGattClient,
    private val reconnectPolicy: ReconnectPolicy
) : DeviceConnectionManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var autoReconnect: Boolean = false
    private var reconnectJob: Job? = null

    override val connectionState: StateFlow<ConnectionState> = gattClient.connectionState

    override suspend fun connect(deviceAddress: String, autoReconnect: Boolean) {
        this.autoReconnect = autoReconnect
        Logger.d("DeviceConnectionManager: connect deviceAddress=$deviceAddress autoReconnect=$autoReconnect", TAG)
        
        try {
            withTimeout(GattConstants.CONNECTION_TIMEOUT_MS) {
                gattClient.connect(deviceAddress, autoReconnect)
            }
            gattClient.discoverServices()
        } catch (e: Exception) {
            Logger.e("DeviceConnectionManager: connect failed", e, TAG)
            try {
                gattClient.disconnect()
            } catch (disconnectError: Exception) {
                Logger.e("DeviceConnectionManager: disconnect after failure also failed", disconnectError, TAG)
            }
            if (autoReconnect) {
                reconnectJob?.cancel()
                reconnectJob = scope.launch {
                    reconnectPolicy.attemptReconnection(
                        onAttempt = { attempt ->
                            Logger.d("DeviceConnectionManager: reconnection attempt $attempt", TAG)
                        }
                    ) {
                        try {
                            gattClient.disconnect()
                        } catch (disconnectError: Exception) {
                            Logger.e(
                                "DeviceConnectionManager: disconnect before reconnection attempt failed",
                                disconnectError,
                                TAG
                            )
                        }
                        withTimeout(GattConstants.CONNECTION_TIMEOUT_MS) {
                            gattClient.connect(deviceAddress, autoReconnect = true)
                        }
                        gattClient.discoverServices()
                    }
                }
            }
            throw e
        }
    }

    override suspend fun disconnect() {
        autoReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        gattClient.disconnect()
    }

    companion object {
        private const val TAG = "DeviceConnectionManager"
    }
}
