package com.example.amulet.core.network.service

import com.example.amulet.core.network.dto.ota.FirmwareInfoDto
import com.example.amulet.core.network.dto.ota.FirmwareReportRequestDto
import com.example.amulet.core.network.dto.ota.OkResponseDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * API сервис для OTA обновлений прошивки.
 * 
 * Реализует эндпоинты OpenAPI:
 * - GET /ota/firmware/latest - проверка доступности обновлений
 * - GET /ota/firmware/download - скачивание прошивки
 * - POST /devices/{deviceId}/firmware/report - отчет об установке прошивки
 */
interface OtaApiService {
    
    /**
     * Проверка доступности обновления прошивки.
     * 
     * OpenAPI: GET /ota/firmware/latest
     * 
     * @param hardware Версия оборудования устройства
     * @param currentFirmware Текущая версия прошивки устройства
     * @return FirmwareInfoDto если обновление доступно (200),
     *         или null если обновление не требуется (204)
     */
    @GET("ota/firmware/latest")
    suspend fun getLatestFirmware(
        @Query("hardware") hardware: Int,
        @Query("currentFirmware") currentFirmware: String
    ): FirmwareInfoDto?
    
    /**
     * Скачивание прошивки по URL.
     * 
     * @param url Полный URL для скачивания прошивки
     * @return Body с данными прошивки
     */
    @Streaming
    @GET
    suspend fun downloadFirmware(@Url url: String): ResponseBody
    
    /**
     * Отправка отчета об установке прошивки.
     * 
     * OpenAPI: POST /devices/{deviceId}/firmware/report
     * 
     * @param deviceId ID устройства
     * @param request Данные отчета об установке
     * @return Подтверждение успешного приема отчета
     */
    @POST("devices/{deviceId}/firmware/report")
    suspend fun reportFirmwareInstall(
        @Path("deviceId") deviceId: String,
        @Body request: FirmwareReportRequestDto
    ): OkResponseDto
}
