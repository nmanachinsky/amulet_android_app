package com.example.amulet.core.ble.transport

import com.example.amulet.core.ble.model.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface BleGattClient {
    val connectionState: StateFlow<ConnectionState>
    val events: Flow<GattEvent>
    
    suspend fun connect(address: String, autoReconnect: Boolean)
    suspend fun disconnect()
    
    suspend fun writeCharacteristic(uuid: UUID, data: ByteArray, responseRequired: Boolean): Boolean
    suspend fun readCharacteristic(uuid: UUID): ByteArray?
    
    suspend fun discoverServices()
    
    fun cleanup()
}

sealed interface GattEvent {
    data object Connected : GattEvent
    data object Disconnected : GattEvent
    data class ServicesDiscovered(val services: List<DiscoveredService>) : GattEvent
    data class CharacteristicChanged(val uuid: UUID, val data: ByteArray) : GattEvent
    data class CharacteristicRead(val uuid: UUID, val data: ByteArray) : GattEvent
    data class CharacteristicWrite(val uuid: UUID, val status: Int) : GattEvent
    data class MtuChanged(val mtu: Int) : GattEvent
    data class Error(val message: String, val exception: Throwable? = null) : GattEvent
}

data class DiscoveredService(
    val uuid: UUID,
    val characteristics: List<DiscoveredCharacteristic>
)

data class DiscoveredCharacteristic(
    val uuid: UUID,
    val properties: Int,
    val descriptors: List<UUID>
)
