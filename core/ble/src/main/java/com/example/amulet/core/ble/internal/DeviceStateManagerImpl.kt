package com.example.amulet.core.ble.internal

import com.example.amulet.core.ble.DeviceStateManager
import com.example.amulet.core.ble.model.DeviceStatus
import com.example.amulet.core.ble.protocol.AmuletProtocolParser
import com.example.amulet.core.ble.transport.BleGattClient
import com.example.amulet.core.ble.transport.GattEvent
import com.example.amulet.shared.domain.devices.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceStateManagerImpl @Inject constructor(
    private val gattClient: BleGattClient,
    private val protocolParser: AmuletProtocolParser
) : DeviceStateManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            gattClient.events.collect { event ->
                protocolParser.processGattEvent(event)
            }
        }
    }

    override val batteryLevel: StateFlow<Int> = protocolParser.batteryLevel

    override val deviceStatus: StateFlow<DeviceStatus?> = protocolParser.deviceStatus

    override val protocolVersion: StateFlow<String?> = protocolParser.protocolVersion

    override fun observeNotifications(type: NotificationType?): Flow<String> {
        return if (type == null) {
            protocolParser.notifications
        } else {
            protocolParser.notifications.filter { message ->
                when (type) {
                    NotificationType.BATTERY -> message.startsWith("NOTIFY:BATTERY:")
                    NotificationType.STATUS -> message.startsWith("NOTIFY:STATUS:")
                    NotificationType.OTA -> message.startsWith("NOTIFY:OTA:")
                    NotificationType.WIFI_OTA -> message.startsWith("NOTIFY:WIFI_OTA:")
                    NotificationType.PATTERN -> message.startsWith("NOTIFY:PATTERN:")
                    NotificationType.ANIMATION -> message.startsWith("NOTIFY:ANIMATION:")
                    NotificationType.CUSTOM -> message.startsWith("NOTIFY:")
                }
            }
        }
    }
}
