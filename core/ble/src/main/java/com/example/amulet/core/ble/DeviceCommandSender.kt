package com.example.amulet.core.ble

import com.example.amulet.core.ble.model.BleResult
import com.example.amulet.core.ble.model.DeviceReadyState
import com.example.amulet.shared.domain.devices.model.AmuletCommand
import kotlinx.coroutines.flow.StateFlow

interface DeviceCommandSender {
    val deviceReadyState: StateFlow<DeviceReadyState>
    suspend fun sendCommand(command: AmuletCommand): BleResult
}
