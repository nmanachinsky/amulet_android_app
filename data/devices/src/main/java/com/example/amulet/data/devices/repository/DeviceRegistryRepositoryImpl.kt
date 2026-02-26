package com.example.amulet.data.devices.repository

import com.example.amulet.data.devices.datasource.local.DevicesLocalDataSource
import com.example.amulet.data.devices.mapper.toDevice
import com.example.amulet.data.devices.mapper.toDeviceEntity
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.model.DeviceSettings
import com.example.amulet.shared.domain.devices.model.DeviceStatus
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRegistryRepositoryImpl @Inject constructor(
    private val localDataSource: DevicesLocalDataSource
) : DeviceRegistryRepository {

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
            status = DeviceStatus.OFFLINE,
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
}
