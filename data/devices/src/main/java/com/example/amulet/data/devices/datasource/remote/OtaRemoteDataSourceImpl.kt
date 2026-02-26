package com.example.amulet.data.devices.datasource.remote

import com.example.amulet.core.network.FileDownloader
import com.example.amulet.core.network.NetworkExceptionMapper
import com.example.amulet.core.network.dto.ota.FirmwareInfoDto
import com.example.amulet.core.network.dto.ota.FirmwareReportRequestDto
import com.example.amulet.core.network.safeApiCall
import com.example.amulet.core.network.service.OtaApiService
import com.example.amulet.shared.core.AppResult
import javax.inject.Inject

/**
 * Реализация источника удаленных данных для OTA обновлений.
 */
class OtaRemoteDataSourceImpl @Inject constructor(
    private val apiService: OtaApiService,
    private val fileDownloader: FileDownloader,
    private val exceptionMapper: NetworkExceptionMapper
) : OtaRemoteDataSource {
    
    override suspend fun checkFirmwareUpdate(
        hardware: Int,
        currentFirmware: String
    ): AppResult<FirmwareInfoDto?> = safeApiCall(exceptionMapper) {
        apiService.getLatestFirmware(hardware, currentFirmware)
    }
    
    override suspend fun downloadFirmware(url: String): AppResult<ByteArray> {
        return fileDownloader.download(url)
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
