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
        val entity = localDataSource.getDeviceById(deviceId.value)
            ?: return Err(AppError.NotFound)
        return Ok(entity.toDevice())
    }
    
    override suspend fun getLastConnectedDevice(userId: UserId): Device? {
        return localDataSource.getLastConnectedDeviceByOwner(userId.value)?.toDevice()
    }
    
    override suspend fun addDevice(
        userId: UserId,
        bleAddress: String,
        name: String,
        hardwareVersion: Int
    ): AppResult<Device> {
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
        return Ok(device)
    }
    
    override suspend fun removeDevice(deviceId: DeviceId): AppResult<Unit> {
        localDataSource.deleteDeviceById(deviceId.value)
        return Ok(Unit)
    }
    
    override suspend fun updateDeviceSettings(
        deviceId: DeviceId,
        name: String?,
        brightness: Double?,
        haptics: Double?,
        gestures: Map<String, String>?
    ): AppResult<Device> {
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
        return Ok(updatedDevice)
    }
    
    override suspend fun applyBrightnessToDevice(
        deviceId: DeviceId,
        brightness: Double
    ): AppResult<Unit> {
        return applySetting(deviceId, "SET_BRIGHTNESS", brightness)
    }

    override suspend fun applyHapticsToDevice(
        deviceId: DeviceId,
        haptics: Double
    ): AppResult<Unit> {
        return applySetting(deviceId, "SET_VIB_STRENGTH", haptics)
    }

    private suspend fun applySetting(
        deviceId: DeviceId,
        command: String,
        value: Double
    ): AppResult<Unit> {
        val exists = localDataSource.getDeviceById(deviceId.value) != null
        if (!exists) return Err(AppError.NotFound)
        val level = (value * 255.0).roundToInt().coerceIn(0, 255)
        return sendCommand(AmuletCommand.Custom(command, listOf(level.toString())))
    }
    
    override fun scanForDevices(timeoutMs: Long): Flow<List<ScannedAmulet>> {
        return bleDataSource.scanForDevices(timeoutMs).map { scannedDevices ->
            scannedDevices.map { bleMapper.mapScannedDevice(it) }
        }
    }
    
    override fun connectToDevice(userId: UserId, bleAddress: String): Flow<BleConnectionState> {
        return kotlinx.coroutines.flow.flow {
            emit(BleConnectionState.Connecting)

            val result = bleDataSource.connect(bleAddress)
            val error = result.component2()
            if (error != null) {
                emit(BleConnectionState.Failed(error))
                return@flow
            }

            val entity = localDataSource.getDeviceByBleAddress(bleAddress, userId.value)
            if (entity != null) {
                val updated = entity.copy(
                    status = com.example.amulet.core.database.entity.DeviceStatus.ONLINE,
                    lastConnectedAt = System.currentTimeMillis(),
                )
                localDataSource.upsertDevice(updated)
            }
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
            status?.let { bleMapper.mapDeviceStatus(it) }
        }
    }

    override fun uploadTimelinePlan(
        plan: DeviceAnimationPlan,
        hardwareVersion: Int
    ): Flow<Int> {
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

        val blePlan = AnimationPlan(
            id = plan.id,
            payload = payload,
            totalDurationMs = plan.totalDurationMs,
            hardwareVersion = hardwareVersion,
            isPreview = plan.isPreview
        )

        return bleDataSource.uploadAnimation(blePlan)
            .map { progress ->
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
}
