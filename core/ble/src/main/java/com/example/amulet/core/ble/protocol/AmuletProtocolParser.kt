package com.example.amulet.core.ble.protocol

import com.example.amulet.core.ble.internal.FlowControlManager
import com.example.amulet.core.ble.internal.GattConstants
import com.example.amulet.core.ble.model.BleResult
import com.example.amulet.core.ble.model.DeviceReadyState
import com.example.amulet.core.ble.model.DeviceStatus
import com.example.amulet.core.ble.transport.GattEvent
import com.example.amulet.shared.core.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicReference

@Singleton
class AmuletProtocolParser @Inject constructor(
    private val flowControlManager: FlowControlManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _commandResponses = MutableSharedFlow<CommandResponse>(extraBufferCapacity = 16)
    val commandResponses: Flow<CommandResponse> = _commandResponses.asSharedFlow()

    private val _notifications = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val notifications: Flow<String> = _notifications.asSharedFlow()

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _deviceStatus = MutableStateFlow<DeviceStatus?>(null)
    val deviceStatus: StateFlow<DeviceStatus?> = _deviceStatus.asStateFlow()

    private val pendingCommandName = AtomicReference<String?>(null)

    private val _protocolVersion = MutableStateFlow<String?>(null)
    val protocolVersion: StateFlow<String?> = _protocolVersion.asStateFlow()

    fun processGattEvent(event: GattEvent) {
        when (event) {
            is GattEvent.CharacteristicChanged -> handleCharacteristicChanged(event)
            is GattEvent.CharacteristicRead -> handleCharacteristicRead(event)
            else -> {}
        }
    }

    private fun handleCharacteristicChanged(event: GattEvent.CharacteristicChanged) {
        val message = event.data.toString(Charsets.UTF_8)
        Logger.d("AmuletProtocolParser: received '$message'", TAG)

        when (event.uuid) {
            GattConstants.NORDIC_UART_RX_CHARACTERISTIC_UUID,
            GattConstants.AMULET_ANIMATION_CHARACTERISTIC_UUID -> {
                parseMessage(message)
            }
            GattConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID -> {
                val level = event.data.getOrNull(0)?.toInt()?.and(0xFF) ?: 0
                _batteryLevel.value = level
                updateDeviceStatus(batteryLevel = level)
            }
            GattConstants.AMULET_DEVICE_STATUS_CHARACTERISTIC_UUID -> {
                parseDeviceStatus(event.data.toString(Charsets.UTF_8))
            }
        }
    }

    private fun handleCharacteristicRead(event: GattEvent.CharacteristicRead) {
        when (event.uuid) {
            GattConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID -> {
                val level = event.data.getOrNull(0)?.toInt()?.and(0xFF) ?: 0
                _batteryLevel.value = level
                updateDeviceStatus(batteryLevel = level)
            }
            GattConstants.AMULET_DEVICE_STATUS_CHARACTERISTIC_UUID -> {
                parseDeviceStatus(event.data.toString(Charsets.UTF_8))
            }
        }
    }

    private fun parseMessage(message: String) {
        scope.launch {
            when {
                message.startsWith("STATE:") -> {
                    flowControlManager.handleDeviceState(message)
                }
                message.startsWith("OK:") -> {
                    val commandName = message.removePrefix("OK:").substringBefore(":")
                    val expected = pendingCommandName.get()
                    if (expected == commandName) {
                        pendingCommandName.compareAndSet(expected, null)
                        _commandResponses.emit(CommandResponse.Success(commandName, message))
                    } else if (expected == null) {
                        Logger.d("AmuletProtocolParser: OK with no pending command, commandName=$commandName", TAG)
                    }

                    if (commandName == "GET_PROTOCOL_VERSION") {
                        val suffix = message.substringAfter("OK:GET_PROTOCOL_VERSION:", missingDelimiterValue = "")
                        if (suffix.isNotEmpty()) {
                            _protocolVersion.value = suffix
                        }
                    }
                }
                message.startsWith("ERROR:") -> {
                    val parts = message.split(":", limit = 4)
                    val code = parts.getOrNull(1) ?: "UNKNOWN_ERROR"
                    val errorMessage = parts.getOrNull(2) ?: "Device reported error"
                    val expected = pendingCommandName.getAndSet(null)
                    _commandResponses.emit(CommandResponse.Error(commandName = expected ?: "", code, errorMessage))
                }
                message.startsWith("NOTIFY:PROTOCOL_VERSION:") -> {
                    _protocolVersion.value = message.substringAfter("NOTIFY:PROTOCOL_VERSION:")
                }
                message.startsWith("v") && message.getOrNull(1)?.isDigit() == true -> {
                    _protocolVersion.value = message
                }
            }
            _notifications.emit(message)
        }
    }

    private fun parseDeviceStatus(statusData: String) {
        try {
            val raw = statusData.trim()
            if (raw.equals("READY", ignoreCase = true)) {
                updateDeviceStatus(isOnline = true)
                return
            }

            val parts = raw.split(";").mapNotNull { token ->
                val idx = token.indexOf(":")
                if (idx > 0 && idx != token.lastIndex) {
                    token.substring(0, idx) to token.substring(idx + 1)
                } else null
            }.toMap()

            if (parts.isNotEmpty()) {
                val status = DeviceStatus(
                    firmwareVersion = parts["FIRMWARE"] ?: "",
                    hardwareVersion = parts["HARDWARE"]?.toIntOrNull() ?: 0,
                    batteryLevel = parts["BATTERY"]?.toIntOrNull() ?: _batteryLevel.value,
                    isCharging = parts["CHARGING"] == "true",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis()
                )
                _deviceStatus.value = status
            }
        } catch (e: Exception) {
            Logger.e("AmuletProtocolParser: parseDeviceStatus failed", e, TAG)
        }
    }

    private fun updateDeviceStatus(batteryLevel: Int? = null, isOnline: Boolean? = null) {
        val current = _deviceStatus.value
        _deviceStatus.value = current?.copy(
            batteryLevel = batteryLevel ?: current.batteryLevel,
            isOnline = isOnline ?: current.isOnline,
            lastSeen = System.currentTimeMillis()
        ) ?: DeviceStatus(
            firmwareVersion = "",
            hardwareVersion = 0,
            batteryLevel = batteryLevel ?: 0,
            isCharging = false,
            isOnline = isOnline ?: true,
            lastSeen = System.currentTimeMillis()
        )
    }

    suspend fun waitForCommandResponse(expectedCommand: String, timeoutMs: Long = GattConstants.COMMAND_TIMEOUT_MS): BleResult {
        pendingCommandName.set(expectedCommand)
        return try {
            withTimeout(timeoutMs) {
                commandResponses.first { it.commandName == expectedCommand }.toBleResult()
            }
        } catch (e: Exception) {
            pendingCommandName.compareAndSet(expectedCommand, null)
            BleResult.Error("TIMEOUT", e.message ?: "Timeout waiting for response")
        } finally {
            pendingCommandName.compareAndSet(expectedCommand, null)
        }
    }

    fun reset() {
        pendingCommandName.set(null)
        _protocolVersion.value = null
    }

    companion object {
        private const val TAG = "AmuletProtocolParser"
    }
}

sealed interface CommandResponse {
    val commandName: String
    data class Success(override val commandName: String, val rawMessage: String) : CommandResponse
    data class Error(override val commandName: String, val code: String, val message: String) : CommandResponse
}

private fun CommandResponse.toBleResult(): BleResult = when (this) {
    is CommandResponse.Success -> BleResult.Success
    is CommandResponse.Error -> BleResult.Error(code, message)
}
