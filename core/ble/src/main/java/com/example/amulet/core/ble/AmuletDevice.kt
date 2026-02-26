package com.example.amulet.core.ble

import com.example.amulet.core.ble.model.AnimationPlan
import com.example.amulet.core.ble.model.BleResult
import com.example.amulet.core.ble.model.ConnectionState
import com.example.amulet.core.ble.model.DeviceReadyState
import com.example.amulet.core.ble.model.DeviceStatus
import com.example.amulet.core.ble.model.FirmwareInfo
import com.example.amulet.core.ble.model.OtaProgress
import com.example.amulet.core.ble.model.UploadProgress
import com.example.amulet.core.ble.model.AmuletCommand
import com.example.amulet.shared.domain.devices.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AmuletDevice : DeviceConnectionManager, DeviceStateManager, DeviceCommandSender {
    fun uploadAnimation(plan: AnimationPlan): Flow<UploadProgress>
    fun startOtaUpdate(firmwareData: ByteArray, firmwareInfo: FirmwareInfo): Flow<OtaProgress>
    fun startWifiOtaUpdate(firmwareInfo: FirmwareInfo): Flow<OtaProgress>
    suspend fun getProtocolVersion(): String?
}
