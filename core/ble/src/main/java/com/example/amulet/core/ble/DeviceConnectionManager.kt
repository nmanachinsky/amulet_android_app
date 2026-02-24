package com.example.amulet.core.ble

import com.example.amulet.core.ble.model.ConnectionState
import kotlinx.coroutines.flow.StateFlow

interface DeviceConnectionManager {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(deviceAddress: String, autoReconnect: Boolean = true)
    suspend fun disconnect()
}
