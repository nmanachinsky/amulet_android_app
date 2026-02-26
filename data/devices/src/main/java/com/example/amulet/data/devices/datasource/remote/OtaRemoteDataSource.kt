package com.example.amulet.data.devices.datasource.remote

import com.example.amulet.core.network.dto.ota.FirmwareInfoDto
import com.example.amulet.shared.core.AppResult

/**
 * Источник удаленных данных для OTA обновлений.
 * Инкапсулирует работу с OtaApiService и маппинг сетевых ошибок.
 */
interface OtaRemoteDataSource {
    
    /**
     * Проверить доступность обновления прошивки.
     *
     * @param hardware Версия оборудования устройства
     * @param currentFirmware Текущая версия прошивки
     * @return Информация о новой прошивке (или null, если обновление не требуется)
     */
    suspend fun checkFirmwareUpdate(
        hardware: Int,
        currentFirmware: String
    ): AppResult<FirmwareInfoDto?>
    
    /**
     * Скачать прошивку по URL.
     *
     * @param url URL для скачивания прошивки
     * @return ByteArray с данными прошивки
     */
    suspend fun downloadFirmware(url: String): AppResult<ByteArray>
    
    /**
     * Отправить отчет об установке прошивки.
     *
     * @param deviceId ID устройства
     * @param fromVersion Исходная версия прошивки
     * @param toVersion Целевая версия прошивки
     * @param status Статус установки (success|failed|cancelled)
     * @param errorCode Код ошибки (если status = failed)
     * @param errorMessage Сообщение об ошибке (если status = failed)
     */
    suspend fun reportFirmwareInstall(
        deviceId: String,
        fromVersion: String,
        toVersion: String,
        status: String,
        errorCode: String? = null,
        errorMessage: String? = null
    ): AppResult<Unit>
}
