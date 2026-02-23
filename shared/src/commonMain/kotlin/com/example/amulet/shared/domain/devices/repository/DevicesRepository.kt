package com.example.amulet.shared.domain.devices.repository

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.BleConnectionState
import com.example.amulet.shared.domain.devices.model.Device
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.model.DeviceLiveStatus
import com.example.amulet.shared.domain.devices.model.ScannedAmulet
import com.example.amulet.shared.domain.devices.model.DeviceAnimationPlan
import com.example.amulet.shared.domain.devices.model.AmuletCommand
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для управления устройствами амулета.
 * Работает только локально: БД + BLE подключения.
 * 
 * Все BLE-специфичные детали (MAC адреса, сканирование, GATT) скрыты внутри.
 */
interface DevicesRepository {
    
    /**
     * Наблюдать за списком всех добавленных устройств.
     * Источник истины - локальная БД.
     */
    fun observeDevices(userId: UserId): Flow<List<Device>>
    
    /**
     * Получить устройство по ID.
     */
    suspend fun getDevice(deviceId: DeviceId): AppResult<Device>
    
    /**
     * Получить последнее подключенное устройство текущего пользователя.
     */
    suspend fun getLastConnectedDevice(userId: UserId): Device?
    
    /**
     * Добавить новое устройство в локальную БД.
     * 
     * @param userId ID пользователя
     * @param bleAddress BLE MAC адрес устройства
     * @param name Имя устройства
     * @param hardwareVersion Версия железа
     * @return Добавленное устройство
     */
    suspend fun addDevice(
        userId: UserId,
        bleAddress: String,
        name: String,
        hardwareVersion: Int
    ): AppResult<Device>
    
    /**
     * Удалить устройство из локальной БД.
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
    
    /**
     * Применить яркость устройства на физическом амулете через BLE.
     */
    suspend fun applyBrightnessToDevice(
        deviceId: DeviceId,
        brightness: Double
    ): AppResult<Unit>
    
    /**
     * Применить силу вибрации устройства на физическом амулете через BLE.
     */
    suspend fun applyHapticsToDevice(
        deviceId: DeviceId,
        haptics: Double
    ): AppResult<Unit>
    
    // ========== BLE сканирование и подключение ==========
    
    /**
     * Сканировать доступные BLE устройства в реальном времени.
     * Возвращает поток списков всех найденных устройств.
     * 
     * @param timeoutMs Таймаут сканирования
     * @return Flow со списками найденных устройств
     */
    fun scanForDevices(
        timeoutMs: Long = 30_000L
    ): Flow<List<ScannedAmulet>>
    
    /**
     * Подключиться к устройству по BLE адресу.
     * 
     * @param userId ID пользователя
     * @param bleAddress MAC адрес устройства
     * @return Flow с состоянием подключения
     */
    fun connectToDevice(
        userId: UserId,
        bleAddress: String
    ): Flow<BleConnectionState>
    
    /**
     * Отключиться от текущего подключенного устройства.
     */
    suspend fun disconnectFromDevice(): AppResult<Unit>
    
    /**
     * Наблюдать за состоянием BLE подключения.
     */
    fun observeConnectionState(): Flow<BleConnectionState>
    
    /**
     * Наблюдать за статусом подключенного устройства (батарея, прошивка и т.д.).
     */
    fun observeConnectedDeviceStatus(): Flow<DeviceLiveStatus?>

    /**
     * Загрузить бинарный таймлайн (DeviceAnimationPlan) на устройство и вернуть поток прогресса (0..100).
     */
    fun uploadTimelinePlan(
        plan: DeviceAnimationPlan,
        hardwareVersion: Int
    ): Flow<Int>

    /**
     * Отправить одиночную команду на текущее подключенное устройство.
     */
    suspend fun sendCommand(command: AmuletCommand): AppResult<Unit>

    /**
     * Подписаться на сырые BLE уведомления (NOTIFY:...).
     */
    fun observeNotifications(type: NotificationType? = null): Flow<String>
}
