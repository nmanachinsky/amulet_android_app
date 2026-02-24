package com.example.amulet.core.ble.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.example.amulet.core.ble.internal.GattConstants
import com.example.amulet.core.ble.model.ConnectionState
import com.example.amulet.shared.core.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class BleGattClientImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BleGattClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentDeviceAddress: String? = null
    private var autoReconnect: Boolean = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<GattEvent>(extraBufferCapacity = 64)
    override val events: Flow<GattEvent> = _events.asSharedFlow()

    private val pendingWrites = Channel<WriteRequest>(Channel.BUFFERED)
    private var isWriting = false

    @Volatile
    private var currentWriteResult: Pair<UUID, CompletableDeferred<Int>>? = null

    @Volatile
    private var currentReadResult: Pair<UUID, CompletableDeferred<ByteArray?>>? = null

    private var pendingNotificationDescriptors: Int = 0
    private val notificationDescriptorQueue = ConcurrentLinkedQueue<BluetoothGattDescriptor>()

    init {
        scope.launch { processWriteQueue() }
    }

    private suspend fun processWriteQueue() {
        for (request in pendingWrites) {
            if (isWriting) {
                request.result.complete(false)
                continue
            }
            isWriting = true
            try {
                val success = performWrite(request)
                request.result.complete(success)
            } catch (e: Exception) {
                Logger.e("BleGattClient: write failed", e, TAG)
                request.result.complete(false)
            } finally {
                isWriting = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun performWrite(request: WriteRequest): Boolean {
        val gatt = bluetoothGatt ?: return false
        val characteristic = findCharacteristicByUuid(request.uuid) ?: return false

        val resultDeferred = CompletableDeferred<Int>()
        currentWriteResult = request.uuid to resultDeferred

        val started = withContext(Dispatchers.Main) {
            characteristic.value = request.data
            characteristic.writeType = if (request.responseRequired) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            gatt.writeCharacteristic(characteristic)
        }

        if (!started) {
            currentWriteResult = null
            return false
        }

        return try {
            val status = withTimeoutOrNull(GattConstants.COMMAND_TIMEOUT_MS) {
                resultDeferred.await()
            } ?: return false

            status == BluetoothGatt.GATT_SUCCESS
        } finally {
            currentWriteResult = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun findCharacteristicByUuid(uuid: UUID): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null
        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                if (characteristic.uuid == uuid) return characteristic
            }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String, autoReconnect: Boolean) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            throw IllegalStateException("Bluetooth adapter is not available or disabled")
        }

        this.currentDeviceAddress = address
        this.autoReconnect = autoReconnect
        _connectionState.value = ConnectionState.Connecting

        return suspendCancellableCoroutine { continuation ->
            try {
                val device = bluetoothAdapter.getRemoteDevice(address)
                bluetoothGatt?.close()
                bluetoothGatt = device.connectGatt(
                    context,
                    autoReconnect,
                    createGattCallback(continuation),
                    BluetoothDevice.TRANSPORT_LE
                )
                continuation.invokeOnCancellation {
                    bluetoothGatt?.close()
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Failed(e)
                continuation.resumeWithException(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        autoReconnect = false
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun writeCharacteristic(
        uuid: UUID,
        data: ByteArray,
        responseRequired: Boolean
    ): Boolean {
        val result = CompletableDeferred<Boolean>()
        pendingWrites.send(WriteRequest(uuid, data, responseRequired, result))
        return result.await()
    }

    @SuppressLint("MissingPermission")
    override suspend fun readCharacteristic(uuid: UUID): ByteArray? {
        val gatt = bluetoothGatt ?: return null
        val characteristic = findCharacteristicByUuid(uuid) ?: return null

        val resultDeferred = CompletableDeferred<ByteArray?>()
        currentReadResult = uuid to resultDeferred

        val started = withContext(Dispatchers.Main) {
            gatt.readCharacteristic(characteristic)
        }

        if (!started) {
            currentReadResult = null
            return null
        }

        return try {
            withTimeoutOrNull(GattConstants.COMMAND_TIMEOUT_MS) {
                resultDeferred.await()
            }
        } finally {
            currentReadResult = null
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun discoverServices() {
        val gatt = bluetoothGatt ?: throw IllegalStateException("Not connected")
        
        val started = gatt.discoverServices()
        if (!started) {
            throw IllegalStateException("Service discovery failed to start")
        }

        withTimeout(GattConstants.DISCOVERY_TIMEOUT_MS) {
            _connectionState.first { it is ConnectionState.ServicesDiscovered || it is ConnectionState.Failed }
        }
    }

    override fun cleanup() {
        pendingWrites.close()
        bluetoothGatt?.close()
        scope.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun createGattCallback(continuation: CancellableContinuation<Unit>?) = object : BluetoothGattCallback() {
        private var connectContinuationCompleted = false

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.Connected
                    _events.tryEmit(GattEvent.Connected)
                    if (!connectContinuationCompleted && continuation != null) {
                        connectContinuationCompleted = true
                        continuation.resume(Unit)
                    }
                    gatt.requestMtu(GattConstants.PREFERRED_MTU)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.Disconnected
                    _events.tryEmit(GattEvent.Disconnected)
                    // Очистка состояний для предотвращения фантомных блокировок
                    pendingNotificationDescriptors = 0
                    notificationDescriptorQueue.clear()
                    isWriting = false

                    currentWriteResult?.second?.complete(BluetoothGatt.GATT_FAILURE)
                    currentWriteResult = null
                    currentReadResult?.second?.complete(null)
                    currentReadResult = null

                    while (true) {
                        val request = pendingWrites.tryReceive().getOrNull() ?: break
                        request.result.complete(false)
                    }
                    if (!connectContinuationCompleted && continuation != null) {
                        connectContinuationCompleted = true
                        continuation.resumeWithException(Exception("Disconnected while connecting"))
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            _events.tryEmit(GattEvent.MtuChanged(mtu))
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setupNotifications(gatt)
            } else {
                _connectionState.value = ConnectionState.Failed(Exception("Service discovery failed"))
            }
        }

        private fun setupNotifications(gatt: BluetoothGatt) {
            pendingNotificationDescriptors = 0
            notificationDescriptorQueue.clear()

            fun enqueueCccd(characteristic: BluetoothGattCharacteristic?) {
                if (characteristic == null) return
                val descriptor = characteristic.getDescriptor(GattConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    notificationDescriptorQueue.add(descriptor)
                }
            }

            val uartService = gatt.getService(GattConstants.NORDIC_UART_SERVICE_UUID)
            val rxChar = uartService?.getCharacteristic(GattConstants.NORDIC_UART_RX_CHARACTERISTIC_UUID)
            rxChar?.let { 
                gatt.setCharacteristicNotification(it, true)
                enqueueCccd(it) 
            }

            val batteryService = gatt.getService(GattConstants.BATTERY_SERVICE_UUID)
            val batteryChar = batteryService?.getCharacteristic(GattConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID)
            batteryChar?.let { 
                gatt.setCharacteristicNotification(it, true)
                enqueueCccd(it) 
            }

            val amuletService = gatt.getService(GattConstants.AMULET_DEVICE_SERVICE_UUID)
            val statusChar = amuletService?.getCharacteristic(GattConstants.AMULET_DEVICE_STATUS_CHARACTERISTIC_UUID)
            statusChar?.let { 
                gatt.setCharacteristicNotification(it, true)
                enqueueCccd(it) 
            }

            val animChar = amuletService?.getCharacteristic(GattConstants.AMULET_ANIMATION_CHARACTERISTIC_UUID)
            animChar?.let { 
                gatt.setCharacteristicNotification(it, true)
                enqueueCccd(it) 
            }

            pendingNotificationDescriptors = notificationDescriptorQueue.size

            if (pendingNotificationDescriptors == 0) {
                _connectionState.value = ConnectionState.ServicesDiscovered
            } else {
                writeNextNotificationDescriptor(gatt)
            }
        }

        private fun writeNextNotificationDescriptor(gatt: BluetoothGatt) {
            val next = notificationDescriptorQueue.peek() ?: return
            val started = gatt.writeDescriptor(next)
            if (!started) {
                notificationDescriptorQueue.clear()
                pendingNotificationDescriptors = 0
                _connectionState.value = ConnectionState.ServicesDiscovered
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (pendingNotificationDescriptors > 0) {
                pendingNotificationDescriptors--
            }
            notificationDescriptorQueue.poll()
            if (notificationDescriptorQueue.isEmpty()) {
                _connectionState.value = ConnectionState.ServicesDiscovered
            } else {
                writeNextNotificationDescriptor(gatt)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            _events.tryEmit(GattEvent.CharacteristicChanged(characteristic.uuid, characteristic.value))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _events.tryEmit(GattEvent.CharacteristicRead(characteristic.uuid, characteristic.value))
            }

            val pending = currentReadResult
            if (pending != null && pending.first == characteristic.uuid) {
                pending.second.complete(
                    if (status == BluetoothGatt.GATT_SUCCESS) characteristic.value else null
                )
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            _events.tryEmit(GattEvent.CharacteristicWrite(characteristic.uuid, status))

            val pending = currentWriteResult
            if (pending != null && pending.first == characteristic.uuid) {
                pending.second.complete(status)
            }
        }
    }

    private data class WriteRequest(
        val uuid: UUID,
        val data: ByteArray,
        val responseRequired: Boolean,
        val result: CompletableDeferred<Boolean>
    )

    companion object {
        private const val TAG = "BleGattClient"
    }
}
