package com.example.amulet.data.devices.repository

import com.example.amulet.core.ble.model.FirmwareInfo
import com.example.amulet.core.ble.model.OtaState
import com.example.amulet.data.devices.datasource.ble.OtaBleDataSource
import com.example.amulet.data.devices.datasource.local.DevicesLocalDataSource
import com.example.amulet.data.devices.datasource.local.OtaLocalDataSource
import com.example.amulet.data.devices.datasource.remote.OtaRemoteDataSource
import com.example.amulet.data.devices.mapper.OtaMapper
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.model.FirmwareUpdate
import com.example.amulet.shared.domain.devices.model.OtaUpdateProgress
import com.example.amulet.shared.domain.devices.model.OtaUpdateState
import com.example.amulet.shared.domain.devices.repository.OtaRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для управления OTA обновлениями.
 */
@Singleton
class OtaRepositoryImpl @Inject constructor(
    private val remoteDataSource: OtaRemoteDataSource,
    private val localDataSource: OtaLocalDataSource,
    private val bleDataSource: OtaBleDataSource,
    private val devicesLocalDataSource: DevicesLocalDataSource,
    private val otaMapper: OtaMapper
) : OtaRepository {
    
    override suspend fun checkFirmwareUpdate(deviceId: DeviceId): AppResult<FirmwareUpdate?> {
        // Получаем информацию об устройстве из БД
        val device = devicesLocalDataSource.getDeviceById(deviceId.value)
            ?: return Err(AppError.NotFound)
        
        val hardwareVersion = device.hardwareVersion
        val currentFirmware = device.firmwareVersion ?: "0.0.0"
        
        // Проверяем обновление на сервере
        return remoteDataSource.checkFirmwareUpdate(hardwareVersion, currentFirmware)
            .andThen { firmwareDto ->
                if (firmwareDto != null) {
                    // Кэшируем информацию о прошивке
                    val entity = otaMapper.toEntity(firmwareDto, hardwareVersion)
                    localDataSource.upsertFirmware(entity)
                    Ok(otaMapper.toDomain(firmwareDto))
                } else {
                    Ok(null)
                }
            }
    }
    
    override fun startBleOtaUpdate(
        deviceId: DeviceId,
        firmwareUpdate: FirmwareUpdate
    ): Flow<OtaUpdateProgress> {
        val firmwareInfo = FirmwareInfo(
            version = firmwareUpdate.version,
            url = firmwareUpdate.url,
            checksum = firmwareUpdate.checksum,
            size = firmwareUpdate.size,
            hardwareVersion = 0 // Будет определен из контекста устройства
        )
        
        return bleDataSource.startBleOtaUpdate(firmwareInfo).map { progress ->
            mapOtaProgress(progress.state, progress.percent, progress.sentBytes, progress.totalBytes)
        }
    }
    
    override fun startWifiOtaUpdate(
        deviceId: DeviceId,
        ssid: String,
        password: String,
        firmwareUpdate: FirmwareUpdate
    ): Flow<OtaUpdateProgress> {
        val firmwareInfo = FirmwareInfo(
            version = firmwareUpdate.version,
            url = firmwareUpdate.url,
            checksum = firmwareUpdate.checksum,
            size = firmwareUpdate.size,
            hardwareVersion = 0
        )
        
        return bleDataSource.startWifiOtaUpdate(ssid, password, firmwareInfo).map { progress ->
            mapOtaProgress(progress.state, progress.percent, progress.sentBytes, progress.totalBytes)
        }
    }
    
    override suspend fun reportFirmwareInstall(
        deviceId: DeviceId,
        fromVersion: String,
        toVersion: String,
        success: Boolean,
        errorMessage: String?
    ): AppResult<Unit> {
        val status = if (success) "success" else "failed"
        
        return remoteDataSource.reportFirmwareInstall(
            deviceId = deviceId.value,
            fromVersion = fromVersion,
            toVersion = toVersion,
            status = status,
            errorCode = if (!success) "UPDATE_FAILED" else null,
            errorMessage = errorMessage
        )
    }
    
    override suspend fun cancelOtaUpdate(): AppResult<Unit> {
        return Ok(Unit)
    }
    
    /**
     * Маппинг прогресса OTA из BLE в доменную модель.
     */
    private fun mapOtaProgress(
        state: OtaState,
        percent: Int,
        currentBytes: Long,
        totalBytes: Long
    ): OtaUpdateProgress {
        val domainState = when (state) {
            OtaState.Preparing -> OtaUpdateState.PREPARING
            OtaState.Transferring -> OtaUpdateState.TRANSFERRING
            OtaState.Verifying -> OtaUpdateState.VERIFYING
            OtaState.Installing -> OtaUpdateState.INSTALLING
            OtaState.Completed -> OtaUpdateState.COMPLETED
            is OtaState.Failed -> OtaUpdateState.FAILED
        }
        
        val errorMessage = if (state is OtaState.Failed) {
            state.cause?.message
        } else null
        
        return OtaUpdateProgress(
            state = domainState,
            percent = percent,
            currentBytes = currentBytes,
            totalBytes = totalBytes,
            error = errorMessage
        )
    }
}
