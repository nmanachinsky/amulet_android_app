package com.example.amulet.data.devices.datasource.ble

import com.example.amulet.core.ble.AmuletDevice
import com.example.amulet.core.ble.model.BleResult
import com.example.amulet.core.ble.model.ConnectionState
import com.example.amulet.core.ble.model.DeviceStatus
import com.example.amulet.core.ble.model.AnimationPlan
import com.example.amulet.core.ble.model.UploadProgress
import com.example.amulet.core.ble.scanner.BleScanner
import com.example.amulet.core.ble.scanner.ScannedDevice
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.core.ble.model.AmuletCommand
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Реализация источника данных для работы с устройствами через BLE.
 */
class DevicesBleDataSourceImpl @Inject constructor(
    private val bleDevice: AmuletDevice,
    private val bleScanner: BleScanner
) : DevicesBleDataSource {
    
    override fun scanForDevices(
        timeoutMs: Long
    ): Flow<List<ScannedDevice>> {
        Logger.d("scanForDevices: timeoutMs=${'$'}timeoutMs", tag = TAG)
        return bleScanner.scanForAmulets(timeoutMs)
    }
    
    override suspend fun connect(
        deviceAddress: String,
        autoReconnect: Boolean
    ): AppResult<Unit> {
        return try {
            Logger.d("DevicesBleDataSourceImpl.connect: start deviceAddress=${'$'}deviceAddress autoReconnect=${'$'}autoReconnect", tag = TAG)
            bleDevice.connect(deviceAddress, autoReconnect)
            try {
                Logger.d("DevicesBleDataSourceImpl.connect: performing soft handshake after connect (with small delay)", tag = TAG)
                delay(2000)
                bleDevice.getProtocolVersion()
            } catch (e: Exception) {
                Logger.d("DevicesBleDataSourceImpl.connect: soft handshake failed, ignoring: ${'$'}e", tag = TAG)
            }
            Logger.d("DevicesBleDataSourceImpl.connect: success deviceAddress=${'$'}deviceAddress", tag = TAG)
            Ok(Unit)
        } catch (e: Exception) {
            Logger.e("DevicesBleDataSourceImpl.connect: exception for ${'$'}deviceAddress: ${'$'}e", tag = TAG)
            Err(mapBleException(e))
        }
    }
    
    override suspend fun disconnect(): AppResult<Unit> {
        return try {
            bleDevice.disconnect()
            Ok(Unit)
        } catch (e: Exception) {
            Err(mapBleException(e))
        }
    }
    
    override fun observeConnectionState(): Flow<ConnectionState> {
        return bleDevice.connectionState
    }
    
    override fun observeBatteryLevel(): Flow<Int> {
        return bleDevice.batteryLevel.onEach { level ->
            Logger.d("observeBatteryLevel: level=$level", tag = TAG)
        }
    }
    
    override fun observeDeviceStatus(): Flow<DeviceStatus?> {
        return bleDevice.deviceStatus.onEach { status ->
            Logger.d("observeDeviceStatus: from bleDevice status=$status", tag = TAG)
        }
    }
    
    override suspend fun getProtocolVersion(): AppResult<String?> {
        return try {
            val version = bleDevice.getProtocolVersion()
            Ok(version)
        } catch (e: Exception) {
            Err(mapBleException(e))
        }
    }

    override fun observeNotifications(type: NotificationType?): Flow<String> {
        return bleDevice.observeNotifications(type)
    }

    override fun uploadAnimation(plan: AnimationPlan): Flow<UploadProgress> {
        Logger.d(
            "uploadAnimation: planId=${plan.id} payloadBytes=${plan.payload.size} duration=${plan.totalDurationMs}",
            tag = TAG
        )
        return bleDevice.uploadAnimation(plan)
            .onEach { progress ->
                Logger.d("uploadAnimation: progress=$progress", tag = TAG)
            }
    }
    
    /**
     * Маппинг BLE исключений в типизированные ошибки AppError.
     */
    override suspend fun sendCommand(command: AmuletCommand): AppResult<Unit> {
        return try {
            Logger.d("DevicesBleDataSourceImpl.sendCommand: command=$command", tag = TAG)
            when (val result = bleDevice.sendCommand(command)) {
                is BleResult.Success -> Ok(Unit)
                is BleResult.Error -> {
                    val error: AppError = if (result.code == "TIMEOUT") {
                        AppError.BleError.CommandTimeout(result.message)
                    } else {
                        AppError.BleError.WriteFailed
                    }
                    Err(error)
                }
            }
        } catch (e: Exception) {
            Logger.e("DevicesBleDataSourceImpl.sendCommand: exception: $e", tag = TAG)
            Err(mapBleException(e))
        }
    }

    private fun mapBleException(e: Exception): AppError {
        return when {
            e.message?.contains("not found", ignoreCase = true) == true ->
                AppError.BleError.DeviceNotFound
            
            e.message?.contains("connection", ignoreCase = true) == true ->
                AppError.BleError.ConnectionFailed
            
            e.message?.contains("disconnected", ignoreCase = true) == true ->
                AppError.BleError.DeviceDisconnected
            
            e.message?.contains("timeout", ignoreCase = true) == true ->
                AppError.BleError.CommandTimeout(e.message ?: "Unknown command")
            
            else -> AppError.Unknown
        }
    }
    
    companion object {
        private const val TAG = "DevicesBleDataSource"
    }
}
