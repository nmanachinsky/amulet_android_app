package com.example.amulet.data.devices.repository

import com.example.amulet.data.devices.datasource.ble.DevicesBleDataSource
import com.example.amulet.data.devices.datasource.local.DevicesLocalDataSource
import com.example.amulet.data.devices.mapper.BleMapper
import com.example.amulet.data.devices.mapper.toDevice
import com.example.amulet.data.devices.mapper.toDeviceEntity
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.core.logging.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.onFailure
import com.example.amulet.shared.domain.devices.model.BleConnectionState
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.model.DeviceLiveStatus
import com.example.amulet.shared.domain.devices.model.DeviceSettings
import com.example.amulet.shared.domain.devices.model.ScannedAmulet
import com.example.amulet.shared.domain.devices.model.AmuletCommand
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.shared.domain.devices.model.DeviceAnimationPlan
import com.example.amulet.shared.domain.devices.repository.DevicesRepository
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet.core.ble.model.AnimationPlan
import com.example.amulet.core.ble.model.UploadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория устройств.
 * Работает только локально: БД + BLE.
 * Устройства привязаны к текущему пользователю.
 */
@Singleton
class DevicesRepositoryImpl @Inject constructor(
    private val localDataSource: DevicesLocalDataSource,
    private val bleDataSource: DevicesBleDataSource,
    private val bleMapper: BleMapper
) : DevicesRepository {
    
    override fun observeDevices(userId: UserId): Flow<List<Device>> {
        return localDataSource.observeDevicesByOwner(userId.value)
            .map { entities -> entities.map { it.toDevice() } }
    }
    
    override suspend fun getDevice(deviceId: DeviceId): AppResult<Device> {
        return try {
            val entity = localDataSource.getDeviceById(deviceId.value)
                ?: return Err(AppError.NotFound)
            Ok(entity.toDevice())
        } catch (e: Exception) {
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun getLastConnectedDevice(userId: UserId): Device? {
        return try {
            localDataSource.getLastConnectedDeviceByOwner(userId.value)?.toDevice()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun addDevice(
        userId: UserId,
        bleAddress: String,
        name: String,
        hardwareVersion: Int
    ): AppResult<Device> {
        return try {
            val existing = localDataSource.getDeviceByBleAddress(bleAddress, userId.value)
            if (existing != null) {
                return Err(AppError.Validation(mapOf("bleAddress" to "Device already added")))
            }
            
            val device = Device(
                id = DeviceId(UUID.randomUUID().toString()),
                ownerId = userId.value,
                bleAddress = bleAddress,
                hardwareVersion = hardwareVersion,
                firmwareVersion = "unknown",
                name = name,
                batteryLevel = null,
                status = com.example.amulet.shared.domain.devices.model.DeviceStatus.OFFLINE,
                addedAt = System.currentTimeMillis(),
                lastConnectedAt = System.currentTimeMillis(),
                settings = DeviceSettings()
            )
            
            localDataSource.upsertDevice(device.toDeviceEntity())
            Ok(device)
        } catch (e: Exception) {
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun removeDevice(deviceId: DeviceId): AppResult<Unit> {
        return try {
            localDataSource.deleteDeviceById(deviceId.value)
            Ok(Unit)
        } catch (e: Exception) {
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun updateDeviceSettings(
        deviceId: DeviceId,
        name: String?,
        brightness: Double?,
        haptics: Double?,
        gestures: Map<String, String>?
    ): AppResult<Device> {
        return try {
            val entity = localDataSource.getDeviceById(deviceId.value)
                ?: return Err(AppError.NotFound)
            
            val device = entity.toDevice()
            val updatedSettings = device.settings.copy(
                brightness = brightness ?: device.settings.brightness,
                haptics = haptics ?: device.settings.haptics,
                gestures = gestures ?: device.settings.gestures
            )
            
            val updatedDevice = device.copy(
                name = name ?: device.name,
                settings = updatedSettings
            )
            
            localDataSource.upsertDevice(updatedDevice.toDeviceEntity())
            Ok(updatedDevice)
        } catch (e: Exception) {
            Err(AppError.DatabaseError)
        }
    }
    
    override suspend fun applyBrightnessToDevice(
        deviceId: DeviceId,
        brightness: Double
    ): AppResult<Unit> {
        return try {
            val entity = localDataSource.getDeviceById(deviceId.value)
                ?: return Err(AppError.NotFound)
            val level = (brightness * 255.0).roundToInt().coerceIn(0, 255)
            val command = AmuletCommand.Custom(
                command = "SET_BRIGHTNESS",
                parameters = listOf(level.toString())
            )
            bleDataSource.sendCommand(command)
        } catch (e: Exception) {
            Err(AppError.Unknown)
        }
    }
    
    override suspend fun applyHapticsToDevice(
        deviceId: DeviceId,
        haptics: Double
    ): AppResult<Unit> {
        return try {
            val entity = localDataSource.getDeviceById(deviceId.value)
                ?: return Err(AppError.NotFound)
            val strength = (haptics * 255.0).roundToInt().coerceIn(0, 255)
            val command = AmuletCommand.Custom(
                command = "SET_VIB_STRENGTH",
                parameters = listOf(strength.toString())
            )
            bleDataSource.sendCommand(command)
        } catch (e: Exception) {
            Err(AppError.Unknown)
        }
    }
    
    override fun scanForDevices(timeoutMs: Long): Flow<List<ScannedAmulet>> {
        return bleDataSource.scanForDevices(timeoutMs).map { scannedDevices ->
            scannedDevices.map { bleMapper.mapScannedDevice(it) }
        }
    }
    
    override fun connectToDevice(userId: UserId, bleAddress: String): Flow<BleConnectionState> {
        return kotlinx.coroutines.flow.flow {
            Logger.d("connectToDevice: start for $bleAddress", tag = TAG)
            emit(BleConnectionState.Connecting)
            
            val result = bleDataSource.connect(bleAddress)
            val error = result.component2()
            if (error != null) {
                Logger.e("connectToDevice: connect failed for $bleAddress with error=$error", tag = TAG)
                emit(BleConnectionState.Failed(error))
                return@flow
            }
            try {
                val entity = localDataSource.getDeviceByBleAddress(bleAddress, userId.value)
                if (entity != null) {
                    val updated = entity.copy(
                        status = com.example.amulet.core.database.entity.DeviceStatus.ONLINE,
                        lastConnectedAt = System.currentTimeMillis(),
                    )
                    localDataSource.upsertDevice(updated)
                }
            } catch (e: Exception) {
                Logger.e("connectToDevice: failed to update lastConnectedAt for $bleAddress: $e", tag = TAG)
            }
            Logger.d("connectToDevice: connect succeeded for $bleAddress", tag = TAG)
            emit(BleConnectionState.Connected)
        }
    }
    
    override suspend fun disconnectFromDevice(): AppResult<Unit> {
        return bleDataSource.disconnect()
    }
    
    override fun observeConnectionState(): Flow<BleConnectionState> {
        return bleDataSource.observeConnectionState().map { state ->
            bleMapper.mapConnectionState(state)
        }
    }
    
    override fun observeConnectedDeviceStatus(): Flow<DeviceLiveStatus?> {
        return bleDataSource.observeDeviceStatus().map { status ->
            Logger.d("observeConnectedDeviceStatus: raw bleStatus=$status", tag = TAG)
            status?.let {
                val mapped = bleMapper.mapDeviceStatus(it)
                Logger.d("observeConnectedDeviceStatus: mapped liveStatus=$mapped", tag = TAG)
                mapped
            }
        }
    }

    override fun uploadTimelinePlan(
        plan: DeviceAnimationPlan,
        hardwareVersion: Int
    ): Flow<Int> {
        Logger.d(
            "uploadTimelinePlan: start hardwareVersion=$hardwareVersion segments=${plan.segments.size} duration=${plan.totalDurationMs}",
            tag = TAG
        )

        // Конкатенация сегментов в единый бинарный payload для BLE.
        val totalSize = plan.segments.sumOf { it.size }
        val payload = ByteArray(totalSize)
        var offset = 0
        plan.segments.forEach { segmentBytes ->
            segmentBytes.copyInto(
                destination = payload,
                destinationOffset = offset
            )
            offset += segmentBytes.size
        }

        Logger.d(
            "uploadTimelinePlan: payloadSize=${payload.size} payloadSizeMod21=${payload.size % 21}",
            tag = TAG
        )

        if (plan.id.contains("_seg_")) {
            Logger.d(
                "uploadTimelinePlan: SEG_DEBUG id=${plan.id} segments=${plan.segments.size} payloadSize=${payload.size}",
                tag = TAG
            )
        }

        val blePlan = AnimationPlan(
            id = plan.id,
            payload = payload,
            totalDurationMs = plan.totalDurationMs,
            hardwareVersion = hardwareVersion,
            isPreview = plan.isPreview
        )

        return bleDataSource.uploadAnimation(blePlan)
            .map { progress ->
                Logger.d("uploadTimelinePlan: progress=$progress", tag = TAG)
                if (progress.state is UploadState.Failed) {
                    val cause = (progress.state as UploadState.Failed).cause
                    throw cause ?: IllegalStateException("Animation upload failed for planId=${plan.id}")
                }
                progress.percent
            }
    }

    override suspend fun sendCommand(command: AmuletCommand): AppResult<Unit> {
        return bleDataSource.sendCommand(command)
    }

    override fun observeNotifications(type: NotificationType?): Flow<String> {
        return bleDataSource.observeNotifications(type)
    }
    
    companion object {
        private const val TAG = "DevicesRepository"
    }
}
