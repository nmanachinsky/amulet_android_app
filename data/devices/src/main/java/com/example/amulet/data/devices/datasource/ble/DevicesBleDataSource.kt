package com.example.amulet.data.devices.datasource.ble

import com.example.amulet.core.ble.model.ConnectionState
import com.example.amulet.core.ble.model.DeviceStatus
import com.example.amulet.core.ble.model.AnimationPlan
import com.example.amulet.core.ble.model.UploadProgress
import com.example.amulet.core.ble.model.AmuletCommand
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.core.ble.scanner.ScannedDevice
import com.example.amulet.shared.core.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Источник данных для работы с устройствами через BLE.
 * Инкапсулирует работу с AmuletDevice и BleScanner.
 */
interface DevicesBleDataSource {
    
    /**
     * Сканировать BLE устройства амулета.
     * Возвращает поток списков всех найденных устройств.
     *
     * @param timeoutMs Таймаут сканирования (0 = бесконечно)
     * @return Flow со списками найденных устройств
     */
    fun scanForDevices(
        timeoutMs: Long = 10_000L
    ): Flow<List<ScannedDevice>>
    
    /**
     * Подключиться к устройству по MAC адресу.
     *
     * @param deviceAddress MAC адрес устройства
     * @param autoReconnect Автоматически переподключаться при потере связи
     */
    suspend fun connect(
        deviceAddress: String,
        autoReconnect: Boolean = true
    ): AppResult<Unit>
    
    /**
     * Отключиться от устройства.
     */
    suspend fun disconnect(): AppResult<Unit>
    
    /**
     * Наблюдать за состоянием подключения.
     */
    fun observeConnectionState(): Flow<ConnectionState>
    
    /**
     * Наблюдать за уровнем батареи устройства.
     */
    fun observeBatteryLevel(): Flow<Int>
    
    /**
     * Наблюдать за статусом устройства (серийный номер, версия прошивки и т.д.).
     */
    fun observeDeviceStatus(): Flow<DeviceStatus?>
    
    /**
     * Получить версию протокола, поддерживаемую устройством.
     *
     * @return Версия протокола (например, "v1.0" или "v2.0")
     */
    suspend fun getProtocolVersion(): AppResult<String?>

    /**
     * Загрузить план анимации на устройство и наблюдать прогресс.
     */
    fun uploadAnimation(plan: AnimationPlan): Flow<UploadProgress>

    /**
     * Отправить произвольную команду на устройство.
     */
    suspend fun sendCommand(command: AmuletCommand): AppResult<Unit>

    /**
     * Подписаться на сырые BLE уведомления (NOTIFY:...).
     */
    fun observeNotifications(type: NotificationType? = null): Flow<String>
}
