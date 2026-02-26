package com.example.amulet.shared.domain.devices.repository

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.BleConnectionState
import com.example.amulet.shared.domain.devices.model.DeviceLiveStatus
import com.example.amulet.shared.domain.devices.model.ScannedAmulet
import com.example.amulet.shared.domain.user.model.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для управления BLE подключениями.
 * Отвечает за сканирование, подключение, отключение и наблюдение за состоянием подключения.
 */
interface DeviceConnectionRepository {
    
    /**
     * Сканировать доступные BLE устройства.
     */
    fun scanForDevices(timeoutMs: Long = 30_000L): Flow<List<ScannedAmulet>>
    
    /**
     * Подключиться к устройству.
     */
    fun connectToDevice(userId: UserId, bleAddress: String): Flow<BleConnectionState>
    
    /**
     * Отключиться от текущего устройства.
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
}
