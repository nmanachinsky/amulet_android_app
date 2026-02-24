package com.example.amulet.data.devices.datasource.ble

import com.example.amulet.core.ble.model.FirmwareInfo
import com.example.amulet.core.ble.model.OtaProgress
import com.example.amulet.shared.core.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Источник данных для OTA обновлений через BLE.
 * Инкапсулирует работу с AmuletDevice для OTA операций.
 * Загрузка прошивки происходит на стороне Data слоя.
 */
interface OtaBleDataSource {
    
    /**
     * Запустить OTA обновление прошивки через BLE.
     * Использует команды START_OTA -> OTA_CHUNK -> OTA_COMMIT с Flow Control.
     * Вызывающий должен предоставить уже загруженные данные прошивки.
     *
     * @param firmwareData Данные прошивки (загруженные из сети)
     * @param firmwareInfo Информация о прошивке
     * @return Flow с прогрессом обновления
     */
    fun startBleOtaUpdate(firmwareData: ByteArray, firmwareInfo: FirmwareInfo): Flow<OtaProgress>
    
    /**
     * Запустить OTA обновление через Wi-Fi.
     * Требует предварительной настройки Wi-Fi через SetWifiCred команду.
     *
     * @param ssid SSID Wi-Fi сети
     * @param password Пароль Wi-Fi сети
     * @param firmwareInfo Информация о прошивке
     * @return Flow с прогрессом обновления
     */
    fun startWifiOtaUpdate(
        ssid: String,
        password: String,
        firmwareInfo: FirmwareInfo
    ): Flow<OtaProgress>
}
