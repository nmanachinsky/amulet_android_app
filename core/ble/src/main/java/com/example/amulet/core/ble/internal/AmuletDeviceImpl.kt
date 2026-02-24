package com.example.amulet.core.ble.internal

import com.example.amulet.core.ble.AmuletDevice
import com.example.amulet.core.ble.DeviceConnectionManager
import com.example.amulet.core.ble.DeviceStateManager
import com.example.amulet.core.ble.DeviceCommandSender
import com.example.amulet.core.ble.model.AnimationPlan
import com.example.amulet.core.ble.model.BleResult
import com.example.amulet.core.ble.model.ConnectionState
import com.example.amulet.core.ble.model.DeviceReadyState
import com.example.amulet.core.ble.model.DeviceStatus
import com.example.amulet.core.ble.model.FirmwareInfo
import com.example.amulet.core.ble.model.OtaProgress
import com.example.amulet.core.ble.model.UploadProgress
import com.example.amulet.core.ble.service.AnimationUploadService
import com.example.amulet.core.ble.service.OtaUpdateService
import com.example.amulet.shared.domain.devices.model.AmuletCommand
import com.example.amulet.shared.domain.devices.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import com.example.amulet.core.ble.internal.GattConstants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmuletDeviceImpl @Inject constructor(
    private val connectionManager: DeviceConnectionManager,
    private val stateManager: DeviceStateManager,
    private val commandSender: DeviceCommandSender,
    private val otaService: OtaUpdateService,
    private val animationUploadService: AnimationUploadService
) : AmuletDevice {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    override val batteryLevel: StateFlow<Int> = stateManager.batteryLevel
    override val deviceStatus: StateFlow<DeviceStatus?> = stateManager.deviceStatus
    override val protocolVersion: StateFlow<String?> = stateManager.protocolVersion
    override val deviceReadyState: StateFlow<DeviceReadyState> = commandSender.deviceReadyState

    override suspend fun connect(deviceAddress: String, autoReconnect: Boolean) {
        connectionManager.connect(deviceAddress, autoReconnect)
    }

    override suspend fun disconnect() {
        connectionManager.disconnect()
    }

    override suspend fun sendCommand(command: AmuletCommand): BleResult {
        return commandSender.sendCommand(command)
    }

    override fun uploadAnimation(plan: AnimationPlan): Flow<UploadProgress> {
        return animationUploadService.uploadAnimation(plan)
    }

    override fun startOtaUpdate(firmwareData: ByteArray, firmwareInfo: FirmwareInfo): Flow<OtaProgress> {
        return otaService.startOtaUpdate(firmwareData, firmwareInfo)
    }

    override fun startWifiOtaUpdate(firmwareInfo: FirmwareInfo): Flow<OtaProgress> {
        return otaService.startWifiOtaUpdate(firmwareInfo)
    }

    override suspend fun getProtocolVersion(): String? {
        val previous = stateManager.protocolVersion.value

        return try {
            sendCommand(AmuletCommand.GetProtocolVersion)
            withTimeout(GattConstants.COMMAND_TIMEOUT_MS) {
                stateManager.protocolVersion.first { it != null && it != previous }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun observeNotifications(type: NotificationType?): Flow<String> {
        return stateManager.observeNotifications(type)
    }
}
