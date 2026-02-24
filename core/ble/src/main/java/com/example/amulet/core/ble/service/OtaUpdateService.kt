package com.example.amulet.core.ble.service

import com.example.amulet.core.ble.DeviceCommandSender
import com.example.amulet.core.ble.internal.FlowControlManager
import com.example.amulet.core.ble.internal.GattConstants
import com.example.amulet.core.ble.model.BleResult
import com.example.amulet.core.ble.model.FirmwareInfo
import com.example.amulet.core.ble.model.OtaProgress
import com.example.amulet.core.ble.model.OtaState
import com.example.amulet.core.ble.model.toBase64
import com.example.amulet.core.ble.model.toCommandString
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.shared.domain.devices.model.AmuletCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface OtaUpdateService {
    fun startOtaUpdate(firmwareData: ByteArray, firmwareInfo: FirmwareInfo): Flow<OtaProgress>
    fun startWifiOtaUpdate(firmwareInfo: FirmwareInfo): Flow<OtaProgress>
}

class OtaUpdateServiceImpl @Inject constructor(
    private val commandSender: DeviceCommandSender,
    private val flowControlManager: FlowControlManager
) : OtaUpdateService {
    override fun startOtaUpdate(firmwareData: ByteArray, firmwareInfo: FirmwareInfo): Flow<OtaProgress> = flow {
        Logger.d("OtaUpdateService: version=${firmwareInfo.version} size=${firmwareData.size}", TAG)
        emit(OtaProgress(firmwareData.size.toLong(), 0, OtaState.Preparing))

        try {
            val startCommand = "START_OTA:${firmwareInfo.version}:${firmwareInfo.checksum}"
            commandSender.sendCommand(AmuletCommand.Custom(startCommand))
            flowControlManager.waitForReady()

            emit(OtaProgress(firmwareData.size.toLong(), 0, OtaState.Transferring))

            val chunks = chunkData(firmwareData, GattConstants.CHUNK_SIZE)
            chunks.forEachIndexed { index, chunk ->
                flowControlManager.waitForReady()

                val chunkCommand = "OTA_CHUNK:${index + 1}:${chunk.size}:${chunk.toBase64()}"
                commandSender.sendCommand(AmuletCommand.Custom(chunkCommand))

                val sentBytes = ((index + 1) * GattConstants.CHUNK_SIZE).toLong()
                    .coerceAtMost(firmwareInfo.size)
                emit(OtaProgress(firmwareData.size.toLong(), sentBytes, OtaState.Transferring))
            }

            emit(OtaProgress(firmwareData.size.toLong(), firmwareData.size.toLong(), OtaState.Verifying))
            commandSender.sendCommand(AmuletCommand.Custom("OTA_COMMIT"))

            emit(OtaProgress(firmwareData.size.toLong(), firmwareData.size.toLong(), OtaState.Installing))

            flowControlManager.waitForReady(60_000L)

            emit(OtaProgress(firmwareData.size.toLong(), firmwareData.size.toLong(), OtaState.Completed))

        } catch (e: Exception) {
            Logger.e("OtaUpdateService: failed", e, TAG)
            emit(OtaProgress(firmwareData.size.toLong(), 0, OtaState.Failed(e)))
        }
    }

    override fun startWifiOtaUpdate(firmwareInfo: FirmwareInfo): Flow<OtaProgress> = flow {
        Logger.d("OtaUpdateService: WiFi OTA version=${firmwareInfo.version}", TAG)
        emit(OtaProgress(firmwareInfo.size, 0, OtaState.Preparing))

        try {
            val command = AmuletCommand.WifiOtaStart(
                url = firmwareInfo.url,
                version = firmwareInfo.version,
                checksum = firmwareInfo.checksum
            )
            commandSender.sendCommand(command)
            emit(OtaProgress(firmwareInfo.size, 0, OtaState.Transferring))
        } catch (e: Exception) {
            Logger.e("OtaUpdateService: WiFi OTA failed", e, TAG)
            emit(OtaProgress(firmwareInfo.size, 0, OtaState.Failed(e)))
        }
    }

    private fun chunkData(data: ByteArray, size: Int): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(size, data.size - offset)
            result.add(data.copyOfRange(offset, offset + chunkSize))
            offset += chunkSize
        }
        return result
    }

    companion object {
        private const val TAG = "OtaUpdateService"
    }
}
