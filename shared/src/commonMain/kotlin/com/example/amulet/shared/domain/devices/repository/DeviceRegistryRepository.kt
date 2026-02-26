package com.example.amulet.shared.domain.devices.repository

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.model.DeviceSettings
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для управления списком устройств (CRUD операции).
 * Отвечает за хранение и управление данными устройств в локальной БД.
 */
interface DeviceRegistryRepository {
    
    /**
     * Наблюдать за списком всех добавленных устройств пользователя.
     * Источник истины - локальная БД.
     */
    fun observeDevices(userId: UserId): Flow<List<Device>>
    
    /**
     * Получить устройство по ID.
     */
    suspend fun getDevice(deviceId: DeviceId): AppResult<Device>
    
    /**
     * Получить последнее подключенное устройство пользователя.
     */
    suspend fun getLastConnectedDevice(userId: UserId): Device?
    
    /**
     * Добавить новое устройство.
     */
    suspend fun addDevice(
        userId: UserId,
        bleAddress: String,
        name: String,
        hardwareVersion: Int
    ): AppResult<Device>
    
    /**
     * Удалить устройство.
     */
    suspend fun removeDevice(deviceId: DeviceId): AppResult<Unit>
    
    /**
     * Обновить настройки устройства.
     */
    suspend fun updateDeviceSettings(
        deviceId: DeviceId,
        name: String? = null,
        brightness: Double? = null,
        haptics: Double? = null,
        gestures: Map<String, String>? = null
    ): AppResult<Device>
}
