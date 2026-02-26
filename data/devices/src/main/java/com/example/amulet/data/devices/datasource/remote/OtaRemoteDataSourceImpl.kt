package com.example.amulet.data.devices.datasource.remote

import com.example.amulet.core.network.NetworkExceptionMapper
import com.example.amulet.core.network.dto.ota.FirmwareInfoDto
import com.example.amulet.core.network.dto.ota.FirmwareReportRequestDto
import com.example.amulet.core.network.safeApiCall
import com.example.amulet.core.network.service.OtaApiService
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Реализация источника удаленных данных для OTA обновлений.
 */
class OtaRemoteDataSourceImpl @Inject constructor(
    private val apiService: OtaApiService,
    private val exceptionMapper: NetworkExceptionMapper
) : OtaRemoteDataSource {
    
    override suspend fun checkFirmwareUpdate(
        hardware: Int,
        currentFirmware: String
    ): AppResult<FirmwareInfoDto?> = safeApiCall(exceptionMapper) {
        apiService.getLatestFirmware(hardware, currentFirmware)
    }
    
    override suspend fun downloadFirmware(url: String): AppResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val data = connection.inputStream.readBytes()
                connection.disconnect()
                Ok(data)
            } else {
                connection.disconnect()
                Err(AppError.Network)
            }
        } catch (e: Exception) {
            Err(AppError.Network)
        }
    }
    
    override suspend fun reportFirmwareInstall(
        deviceId: String,
        fromVersion: String,
        toVersion: String,
        status: String,
        errorCode: String?,
        errorMessage: String?
    ): AppResult<Unit> = safeApiCall(exceptionMapper) {
        val request = FirmwareReportRequestDto(
            fromVersion = fromVersion,
            toVersion = toVersion,
            status = status,
            errorCode = errorCode,
            errorMessage = errorMessage
        )
        apiService.reportFirmwareInstall(deviceId, request)
        Unit
    }
}
