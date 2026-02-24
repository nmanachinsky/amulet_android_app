package com.example.amulet.core.ble

import com.example.amulet.core.ble.model.DeviceStatus
import com.example.amulet.shared.domain.devices.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DeviceStateManager {
    val batteryLevel: StateFlow<Int>
    val deviceStatus: StateFlow<DeviceStatus?>
    val protocolVersion: StateFlow<String?>
    fun observeNotifications(type: NotificationType? = null): Flow<String>
}
