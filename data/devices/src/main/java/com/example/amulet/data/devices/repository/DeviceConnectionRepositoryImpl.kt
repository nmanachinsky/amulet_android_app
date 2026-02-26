package com.example.amulet.data.devices.repository

import com.example.amulet.data.devices.datasource.ble.DevicesBleDataSource
import com.example.amulet.data.devices.datasource.local.DevicesLocalDataSource
import com.example.amulet.data.devices.mapper.BleMapper
import com.example.amulet.core.database.entity.DeviceStatus
import com.example.amulet.shared.core.AppError
import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.BleConnectionState
import com.example.amulet.shared.domain.devices.model.DeviceLiveStatus
import com.example.amulet.shared.domain.devices.model.ScannedAmulet
import com.example.amulet.shared.domain.devices.repository.DeviceConnectionRepository
import com.example.amulet.shared.domain.user.model.UserId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceConnectionRepositoryImpl @Inject constructor(
    private val bleDataSource: DevicesBleDataSource,
    private val localDataSource: DevicesLocalDataSource,
    private val bleMapper: BleMapper
) : DeviceConnectionRepository {

    override fun scanForDevices(timeoutMs: Long): Flow<List<ScannedAmulet>> {
        return bleDataSource.scanForDevices(timeoutMs).map { scannedDevices ->
            scannedDevices.map { bleMapper.mapScannedDevice(it) }
        }
    }

    override fun connectToDevice(userId: UserId, bleAddress: String): Flow<BleConnectionState> {
        return kotlinx.coroutines.flow.flow {
            emit(BleConnectionState.Connecting)

            val result = bleDataSource.connect(bleAddress)
            val error = result.component2()
            if (error != null) {
                emit(BleConnectionState.Failed(error))
                return@flow
            }

            val entity = localDataSource.getDeviceByBleAddress(bleAddress, userId.value)
            if (entity != null) {
                val updated = entity.copy(
                    status = DeviceStatus.ONLINE,
                    lastConnectedAt = System.currentTimeMillis(),
                )
                localDataSource.upsertDevice(updated)
            }
            emit(BleConnectionState.Connected)
        }
    }

    override suspend fun disconnectFromDevice(): AppResult<Unit> {
        return bleDataSource.disconnect()
    }

    override fun observeConnectionState(): Flow<BleConnectionState> {
        return bleDataSource.observeConnectionState().map { state ->
            bleMapper.mapConnectionState(state)
        }
    }

    override fun observeConnectedDeviceStatus(): Flow<DeviceLiveStatus?> {
        return bleDataSource.observeDeviceStatus().map { status ->
            status?.let { bleMapper.mapDeviceStatus(it) }
        }
    }
}
