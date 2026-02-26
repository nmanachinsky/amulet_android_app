package com.example.amulet.core.ble.internal

import com.example.amulet.core.ble.DeviceCommandSender
import com.example.amulet.core.ble.internal.GattConstants
import com.example.amulet.core.ble.model.BleResult
import com.example.amulet.core.ble.model.DeviceReadyState
import com.example.amulet.core.ble.model.toCommandString
import com.example.amulet.core.ble.protocol.AmuletProtocolParser
import com.example.amulet.core.ble.transport.BleGattClient
import com.example.amulet.shared.core.logging.Logger
import com.example.amulet.core.ble.model.AmuletCommand
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCommandSenderImpl @Inject constructor(
    private val gattClient: BleGattClient,
    private val protocolParser: AmuletProtocolParser,
    flowControlManager: FlowControlManager,
    private val retryPolicy: RetryPolicy
) : DeviceCommandSender {

    private val commandMutex = Mutex()

    override val deviceReadyState: StateFlow<DeviceReadyState> = flowControlManager.readyState

    override suspend fun sendCommand(command: AmuletCommand): BleResult {
        return commandMutex.withLock {
            val commandString = command.toCommandString()
            Logger.d("DeviceCommandSender: sending $commandString", TAG)
            
            val result = retryPolicy.executeWithRetry(
                operation = { sendCommandInternal(command) }
            )
            
            Logger.d("DeviceCommandSender: result=$result for $commandString", TAG)
            result
        }
    }

    private suspend fun sendCommandInternal(command: AmuletCommand): BleResult {
        val commandString = command.toCommandString()
        val commandName = commandString.substringBefore(":")

        val characteristicUuid = when {
            commandString.startsWith("START_OTA") ||
            commandString.startsWith("OTA_CHUNK") ||
            commandString.startsWith("OTA_COMMIT") -> GattConstants.AMULET_OTA_CHARACTERISTIC_UUID
            commandString.startsWith("BEGIN_PLAN") ||
            commandString.startsWith("ADD_COMMAND") ||
            commandString.startsWith("ADD_SEGMENTS") ||
            commandString.startsWith("COMMIT_PLAN") ||
            commandString.startsWith("ROLLBACK_PLAN") -> GattConstants.AMULET_ANIMATION_CHARACTERISTIC_UUID
            else -> GattConstants.NORDIC_UART_TX_CHARACTERISTIC_UUID
        }

        val forceWriteWithResponse = commandName == "SET_BRIGHTNESS" || commandName == "SET_VIB_STRENGTH"

        return coroutineScope {
            val responseDeferred = async {
                protocolParser.waitForCommandResponse(commandName)
            }

            val success = gattClient.writeCharacteristic(
                characteristicUuid,
                commandString.toByteArray(Charsets.UTF_8),
                responseRequired = forceWriteWithResponse
            )

            if (!success) {
                responseDeferred.cancel()
                return@coroutineScope BleResult.Error("WRITE_FAILED", "Failed to write command")
            }

            responseDeferred.await()
        }
    }

    companion object {
        private const val TAG = "DeviceCommandSender"
    }
}
