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
                Logger.d("BleGattClient: write failed",  TAG)
                request.result.complete(false)
            } finally {
                isWriting = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun performWrite(request: WriteRequest): Boolean {
        val gatt = bluetoothGatt ?: run {
            Logger.d("BleGattClient: performWrite failed - GATT is null", TAG)
            return false
        }
        val characteristic = findCharacteristicByUuid(request.uuid) ?: run {
            Logger.d("BleGattClient: performWrite failed - characteristic ${request.uuid} not found", TAG)
            return false
        }

        Logger.d("BleGattClient: performWrite uuid=${request.uuid} dataSize=${request.data.size} responseRequired=${request.responseRequired}", TAG)

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
            Logger.d("BleGattClient: writeCharacteristic returned false (not started)", TAG)
            currentWriteResult = null
            return false
        }

        Logger.d("BleGattClient: writeCharacteristic started, awaiting result...", TAG)

        return try {
            val status = withTimeoutOrNull(GattConstants.COMMAND_TIMEOUT_MS) {
                resultDeferred.await()
            } ?: run {
                Logger.d("BleGattClient: write timeout after ${GattConstants.COMMAND_TIMEOUT_MS}ms", TAG)
                return false
            }

            val success = status == BluetoothGatt.GATT_SUCCESS
            Logger.d("BleGattClient: write completed success=$success status=$status", TAG)
            success
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
        Logger.d("BleGattClient: connect() called address=$address autoReconnect=$autoReconnect", TAG)

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Logger.d("BleGattClient: Bluetooth adapter not available or disabled", TAG)
            throw IllegalStateException("Bluetooth adapter is not available or disabled")
        }

        Logger.d("BleGattClient: Bluetooth adapter available, starting connection", TAG)

        this.currentDeviceAddress = address
        this.autoReconnect = autoReconnect
        _connectionState.value = ConnectionState.Connecting
        Logger.d("BleGattClient: Connection state changed to Connecting", TAG)

        return suspendCancellableCoroutine { continuation ->
            try {
                val device = bluetoothAdapter.getRemoteDevice(address)
                Logger.d("BleGattClient: Got remote device for address=$address", TAG)
                bluetoothGatt?.close()
                bluetoothGatt = device.connectGatt(
                    context,
                    autoReconnect,
                    createGattCallback(continuation),
                    BluetoothDevice.TRANSPORT_LE
                )
                Logger.d(
                    "BleGattClient: connectGatt() called with autoConnect=$autoReconnect",
                    TAG
                )
                continuation.invokeOnCancellation {
                    Logger.d("BleGattClient: Connection cancelled, closing GATT", TAG)
                    bluetoothGatt?.close()
                }
            } catch (e: Exception) {
                Logger.d("BleGattClient: Exception during connectGatt",  TAG)
                _connectionState.value = ConnectionState.Failed(e)
                continuation.resumeWithException(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        Logger.d("BleGattClient: disconnect() called", TAG)
        autoReconnect = false
        bluetoothGatt?.disconnect()
        Logger.d("BleGattClient: GATT disconnect() called", TAG)
        bluetoothGatt?.close()
        Logger.d("BleGattClient: GATT close() called", TAG)
        bluetoothGatt = null
        _connectionState.value = ConnectionState.Disconnected
        Logger.d("BleGattClient: Connection state changed to Disconnected", TAG)
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
        Logger.d("BleGattClient: readCharacteristic() called uuid=$uuid", TAG)
        val gatt = bluetoothGatt ?: run {
            Logger.d("BleGattClient: readCharacteristic failed - GATT is null", TAG)
            return null
        }
        val characteristic = findCharacteristicByUuid(uuid) ?: run {
            Logger.d("BleGattClient: readCharacteristic failed - characteristic $uuid not found", TAG)
            return null
        }

        val resultDeferred = CompletableDeferred<ByteArray?>()
        currentReadResult = uuid to resultDeferred

        val started = withContext(Dispatchers.Main) {
            gatt.readCharacteristic(characteristic)
        }

        if (!started) {
            Logger.d("BleGattClient: readCharacteristic returned false (not started)", TAG)
            currentReadResult = null
            return null
        }

        Logger.d("BleGattClient: readCharacteristic started, awaiting result...", TAG)

        return try {
            withTimeoutOrNull(GattConstants.COMMAND_TIMEOUT_MS) {
                resultDeferred.await()
            }?.also {
                Logger.d("BleGattClient: read completed dataSize=${it.size}", TAG)
            } ?: run {
                Logger.d("BleGattClient: read timeout after ${GattConstants.COMMAND_TIMEOUT_MS}ms", TAG)
                null
            }
        } finally {
            currentReadResult = null
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun discoverServices() {
        Logger.d("BleGattClient: discoverServices() called", TAG)
        val gatt = bluetoothGatt ?: throw IllegalStateException("Not connected")
        
        val started = gatt.discoverServices()
        if (!started) {
            Logger.d("BleGattClient: discoverServices() failed to start", TAG)
            throw IllegalStateException("Service discovery failed to start")
        }
        Logger.d("BleGattClient: discoverServices() started, awaiting completion...", TAG)

        val state = withTimeout(GattConstants.DISCOVERY_TIMEOUT_MS) {
            _connectionState.first { it is ConnectionState.ServicesDiscovered || it is ConnectionState.Failed }
        }
        
        when (state) {
            is ConnectionState.ServicesDiscovered -> {
                Logger.d("BleGattClient: discoverServices() completed with state=ServicesDiscovered", TAG)
            }
            is ConnectionState.Failed -> {
                Logger.d("BleGattClient: discoverServices() failed: ${state.cause}", TAG)
                throw IllegalStateException("Service discovery failed", state.cause)
            }
            else -> {
                Logger.d("BleGattClient: discoverServices() unexpected state=$state", TAG)
                throw IllegalStateException("Unexpected connection state after discovery: $state")
            }
        }
    }

    override fun cleanup() {
        Logger.d("BleGattClient: cleanup() called", TAG)
        pendingWrites.close()
        bluetoothGatt?.close()
        scope.cancel()
        Logger.d("BleGattClient: cleanup() completed", TAG)
    }

    @SuppressLint("MissingPermission")
    private fun createGattCallback(continuation: CancellableContinuation<Unit>?) = object : BluetoothGattCallback() {
        private var connectContinuationCompleted = false

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Logger.d("BleGattClient: onConnectionStateChange status=$status newState=$newState", TAG)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Logger.d("BleGattClient: STATE_CONNECTED, requesting MTU", TAG)
                    _connectionState.value = ConnectionState.Connected
                    _events.tryEmit(GattEvent.Connected)
                    if (!connectContinuationCompleted && continuation != null) {
                        connectContinuationCompleted = true
                        continuation.resume(Unit)
                    }
                    gatt.requestMtu(GattConstants.PREFERRED_MTU)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Logger.d("BleGattClient: STATE_DISCONNECTED, cleaning up resources", TAG)
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
            Logger.d("BleGattClient: onMtuChanged mtu=$mtu status=$status servicesCount=${gatt.services.size}", TAG)
            _events.tryEmit(GattEvent.MtuChanged(mtu))
            // Fallback: если сервисы уже в кэше - используем их
            if (gatt.services.isNotEmpty()) {
                Logger.d("BleGattClient: Services already cached (${gatt.services.size}), setting up notifications", TAG)
                setupNotifications(gatt)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Logger.d("BleGattClient: onServicesDiscovered status=$status", TAG)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d("BleGattClient: Services discovered successfully, setting up notifications", TAG)
                setupNotifications(gatt)
            } else {
                Logger.d("BleGattClient: Service discovery failed with status=$status", TAG)
                _connectionState.value = ConnectionState.Failed(Exception("Service discovery failed"))
            }
        }

        private fun setupNotifications(gatt: BluetoothGatt) {
            Logger.d("BleGattClient: setupNotifications() started", TAG)
            pendingNotificationDescriptors = 0
            notificationDescriptorQueue.clear()

            fun enqueueCccd(characteristic: BluetoothGattCharacteristic?) {
                if (characteristic == null) return
                val descriptor = characteristic.getDescriptor(GattConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
                if (descriptor != null) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    notificationDescriptorQueue.add(descriptor)
                    Logger.d("BleGattClient: Enqueued CCCD for characteristic=${characteristic.uuid}", TAG)
                } else {
                    Logger.d("BleGattClient: No CCCD descriptor for characteristic=${characteristic.uuid}", TAG)
                }
            }

            val uartService = gatt.getService(GattConstants.NORDIC_UART_SERVICE_UUID)
            Logger.d("BleGattClient: UART service=${if (uartService != null) "found" else "missing"}", TAG)
            val rxChar = uartService?.getCharacteristic(GattConstants.NORDIC_UART_RX_CHARACTERISTIC_UUID)
            rxChar?.let { 
                gatt.setCharacteristicNotification(it, true)
                enqueueCccd(it) 
            }

            val batteryService = gatt.getService(GattConstants.BATTERY_SERVICE_UUID)
            Logger.d("BleGattClient: Battery service=${if (batteryService != null) "found" else "missing"}", TAG)
            val batteryChar = batteryService?.getCharacteristic(GattConstants.BATTERY_LEVEL_CHARACTERISTIC_UUID)
            batteryChar?.let { 
                gatt.setCharacteristicNotification(it, true)
                enqueueCccd(it) 
            }

            val amuletService = gatt.getService(GattConstants.AMULET_DEVICE_SERVICE_UUID)
            Logger.d("BleGattClient: Amulet service=${if (amuletService != null) "found" else "missing"}", TAG)
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
            Logger.d("BleGattClient: Total notification descriptors to write: $pendingNotificationDescriptors", TAG)

            if (pendingNotificationDescriptors == 0) {
                Logger.d("BleGattClient: No descriptors to write, transitioning to ServicesDiscovered", TAG)
                _connectionState.value = ConnectionState.ServicesDiscovered
            } else {
                writeNextNotificationDescriptor(gatt)
            }
        }

        private fun writeNextNotificationDescriptor(gatt: BluetoothGatt) {
            val next = notificationDescriptorQueue.peek() ?: run {
                Logger.d("BleGattClient: No more descriptors to write", TAG)
                return
            }
            Logger.d("BleGattClient: Writing notification descriptor for characteristic=${next.characteristic.uuid}", TAG)
            val started = gatt.writeDescriptor(next)
            if (!started) {
                Logger.d("BleGattClient: writeDescriptor failed to start, clearing queue", TAG)
                notificationDescriptorQueue.clear()
                pendingNotificationDescriptors = 0
                _connectionState.value = ConnectionState.ServicesDiscovered
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Logger.d("BleGattClient: onDescriptorWrite characteristic=${descriptor.characteristic.uuid} status=$status", TAG)
            if (pendingNotificationDescriptors > 0) {
                pendingNotificationDescriptors--
            }
            notificationDescriptorQueue.poll()
            if (notificationDescriptorQueue.isEmpty()) {
                Logger.d("BleGattClient: All notification descriptors written, transitioning to ServicesDiscovered", TAG)
                _connectionState.value = ConnectionState.ServicesDiscovered
            } else {
                Logger.d("BleGattClient: Remaining descriptors to write: ${notificationDescriptorQueue.size}", TAG)
                writeNextNotificationDescriptor(gatt)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Logger.d("BleGattClient: onCharacteristicChanged uuid=${characteristic.uuid} dataSize=${characteristic.value.size}", TAG)
            _events.tryEmit(GattEvent.CharacteristicChanged(characteristic.uuid, characteristic.value))
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Logger.d("BleGattClient: onCharacteristicRead uuid=${characteristic.uuid} status=$status dataSize=${characteristic.value?.size ?: 0}", TAG)
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
            Logger.d("BleGattClient: onCharacteristicWrite uuid=${characteristic.uuid} status=$status", TAG)
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
