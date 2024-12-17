package com.mobilinkd.bleconfig

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.concurrent.schedule
import kotlin.math.min


@SuppressLint("MissingPermission")

/*
 * There are three main states for the BLE service once initialized:
 *
 *  CLOSED -- gatt is null
 *  CONNECTED -- gatt is connected
 *  DISCONNECTED -- gatt is not null, but disconnected
 *
 * The gatt client can be connected and disconnected numerous times in a session. Reconnecting
 * a client is fast, using cached information. This may be desirable for some applications where,
 * for example, the connection should only be maintained while the app is running.
 *
 * If that is not needed, calling open()/close() is most efficient.
 *
 * Open when bonded, trusted ->
 *  Open, connect, service discovery, enable notification, set mtu, done.
 *  DISCONNECTED, CONNECTING, DISCOVERED, CONNECTED.
 *
 * Open when bonded ->
 *  Open, connect, service discovery, disconnect, automatic pairing (trust), connect, service discovery, enable notification, set mtu, done.
 *
 * Open when not bonded, bonding allowed, trust allowed ->
 *  Open, create bond, connect, service discovery, disconnect, automatic pairing (trust), connect, service discovery, enable notification, set mtu, done.
 *
 * Open when not bonded, bonding allowed, trust disallowed ->
 *  Open, create bond, connect, service discovery, disconnect, automatic pairing (trust), connect, service discovery, enable notification, set mtu, done.
 */

// A service that interacts with the BLE device via the Android BLE API.
class BluetoothLEService : Service() {
    private var device: BluetoothDevice? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var mtu = 20
    private var gatt: BluetoothGatt? = null
    private var retryCount = 0
    private val _inputStream = BLEInputStream()
    private val _outputStream = BLEOutputStream()
    private var receiveThread = ReceiveThread()
    private var connectionState = ConnectionState.CLOSED
    private var shutdownTimer: Timer? = null
    private lateinit var serviceHandler: Handler
    private lateinit var broadcastManager: LocalBroadcastManager

    val inputStream get(): InputStream = _inputStream
    val outputStream get(): OutputStream = _outputStream

    private inner class BLEInputStream: InputStream() {
        private var buffer = ByteArray(0)
        private val bytesAvailable = Semaphore(0)

        fun receive(data: ByteArray) {
            synchronized(buffer) {
                buffer += data
                bytesAvailable.release(data.size)
            }
        }

        override fun read(): Int {
            try {
                bytesAvailable.acquire(1)
                bytesAvailable.acquire(1)
                synchronized(buffer) {
                    val result = buffer.first().toInt()
                    buffer = buffer.drop(1).toByteArray()
                    return result
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "read() interrupted")
                return -1
            }
        }

        override fun read(dest: ByteArray, offset: Int, length: Int): Int {
            try {
                bytesAvailable.acquire(1)
                synchronized(buffer) {
                    val size = min(length, buffer.size)
                    // Expect that we have at lease size - 1 permits available.
                    if (bytesAvailable.tryAcquire(size - 1)) {
                        System.arraycopy(buffer, 0, dest, offset, size)
                        buffer = buffer.drop(size).toByteArray()
                        return size
                    } else {
                        // We have one...
                        Log.e(TAG, "invalid number of semaphore permits")
                        val head = buffer.first()
                        buffer = buffer.drop(1).toByteArray()
                        dest[offset] = head
                        return 1
                    }
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "read() interrupted")
                return -1
            }
        }

        fun clear() {
            buffer = ByteArray(0)
            bytesAvailable.drainPermits()
        }
    }

    private inner class BLEOutputStream: OutputStream() {
        private var buffer = ByteArray(0)
        private var awaitingCallback = Semaphore(1)

        override fun write(b: Int) {
            synchronized(buffer) {
                buffer += byteArrayOf(b.toByte())
            }
        }

        override fun write(src: ByteArray, offset: Int, length: Int) {
            val data = src.slice(IntRange(offset, offset + length - 1))
            synchronized(buffer) {
                buffer += data
            }
        }

        override fun flush() {
            Log.d(TAG, "Flushed.")
            if (awaitingCallback.tryAcquire()) sendMore()
        }

        private fun send(): Int {
            synchronized(buffer) {
                if (buffer.isNotEmpty()) {
                    val chunk = buffer.take(mtu).toByteArray()
                    buffer = buffer.drop(mtu).toByteArray()
                    this@BluetoothLEService.send(chunk)
                    return chunk.size
                } else {
                    return 0
                }
            }
        }

        fun sendMore() {
            if (send() == 0) awaitingCallback.release()
        }

        fun clear() {
            buffer = ByteArray(0)
            awaitingCallback.drainPermits()
            awaitingCallback.release()
        }
    }

    private inner class ReceiveThread: Thread("M17 KISS HT BLE Receive Thread") {
        var running = false

        private fun sendData(data: ByteArray) {
            val intent = Intent(BLE_EVENT)
            intent.putExtra(BLE_EVENT_ACTION, DATA_RECEIVED)
            intent.putExtra(BLE_EVENT_DATA, data)
            broadcastManager.sendBroadcast(intent)
        }

        override fun run() {
            running = true
            Log.d(TAG, "ReceiveThread.run()")

            while (running) {
                try {
                    val buffer = ByteArray(512)
                    while (running) {
                        val size = _inputStream.read(buffer, 0, buffer.size)
                        sendData(buffer.take(size).toByteArray())
                        Log.d(TAG, "received $size bytes")
                    }
                } catch (e : Exception) {
                    Log.d(TAG, "ReceiveThread.run exception")
                }
            }
            Log.d(TAG, "BLEReceiveThread.terminate()")
        }

        fun shutdown() {
            Log.d(TAG, "ReceiveThread.shutdown()")
            running = false
            interrupt()
        }
    }

    fun onConnected(gatt: BluetoothGatt) {
        synchronized(connectionState) {
            connectionState = ConnectionState.CONNECTED
        }
        val intent = Intent(BLE_EVENT)
        intent.putExtra(BLE_EVENT_ACTION, GATT_CONNECTED)
        intent.putExtra(BLE_EVENT_DEVICE, gatt.device)
        broadcastManager.sendBroadcast(intent)
    }

    private fun setDisconnectedState() {
        synchronized(connectionState) {
            connectionState = ConnectionState.DISCONNECTED
        }
    }

    fun onDisconnected(gatt: BluetoothGatt) {
        if (D) Log.d(TAG,"onDisconnected: device = ${gatt.device.name}")
        setDisconnectedState()
        val intent = Intent(BLE_EVENT)
        intent.putExtra(BLE_EVENT_ACTION, GATT_DISCONNECTED)
        broadcastManager.sendBroadcast(intent)

    }

    fun connect(device: BluetoothDevice, paired: Boolean = false): Boolean {
        if (D) Log.d(TAG, "connect(device = ${device.address}), state = $connectionState")
        synchronized(connectionState) {
            when (connectionState) {
                ConnectionState.CONNECTED -> {
                    if (D) Log.d(TAG, "connect(): already connected")
                    return false
                }
                ConnectionState.DISCONNECTING -> {
                    if (D) Log.d(TAG, "connect(): disconnect requested")
                    gatt?.close()
                    connectionState = ConnectionState.DISCONNECTED
                    return false
                }
                else -> {
                    this.device = device
                    connectionState = ConnectionState.CONNECTING
                    return connectInternal(device, paired)
                }
            }
        }
    }

    private fun refresh(gatt: BluetoothGatt): Boolean {
        var isRefreshed = false
        try {
            val refresh = gatt.javaClass.getMethod("refresh")
            isRefreshed = (refresh.invoke(gatt) as Boolean)
            Log.i(TAG,"Gatt cache refresh successful for ${gatt.device.address}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred while refreshing device: $e")
        }

        return isRefreshed
    }

    fun connectionFailed(msg: String) {
        // It is the responsibility of the message recipient to close the connection.
        setDisconnectedState()
        val intent = Intent(BLE_EVENT)
        intent.putExtra(BLE_EVENT_ACTION, GATT_CONNECTION_FAILED)
        intent.putExtra(BLE_EVENT_MESSAGE, msg)
        broadcastManager.sendBroadcast(intent)
    }

    fun discoveryFailed(msg: String) {
        // It is the responsibility of the message recipient to close the connection.
        setDisconnectedState()
        val intent = Intent(BLE_EVENT)
        intent.putExtra(BLE_EVENT_ACTION, GATT_DISCOVERY_FAILED)
        intent.putExtra(BLE_EVENT_MESSAGE, msg)
        broadcastManager.sendBroadcast(intent)
    }

    // Various callback methods defined by the BLE API.
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
        ) {
            if (D) Log.d(TAG, "onConnectionStateChange() - status = $status, state = $newState, bondState = ${gatt.device.bondState}")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to ${gatt.device.name}.")
                    this@BluetoothLEService.gatt = gatt
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    when (status) {
                        BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION -> {
                            if ((connectionState == ConnectionState.CONNECTING) || (connectionState == ConnectionState.RETRYING)) {
                                Log.w(TAG, "Discovery failed. connectionState = $connectionState")
                                if (retryCount > 0) {
                                    synchronized(connectionState) {
                                        connectionState = ConnectionState.RETRYING
                                    }
                                    gatt.close()
                                    connect(device!!, true)
                                    retryCount = 0
                                    return
                                }
                                // This happens with dual-mode devices occasionally. The cache for the
                                // device must be cleared or discovery will never succeed.
                                // Probably should abort and tell user to enable/disable Bluetooth.
                                discoveryFailed("Discovery failed")
                                return
                            }
                            this@BluetoothLEService.gatt = null
                            if (retryCount > 0) {
                                Log.w(TAG, "Authorization Error, retrying ${device!!.address}")
                                synchronized(connectionState) {
                                    connectionState = ConnectionState.RETRYING
                                }
//                                gatt.close()
//                                connect(device!!)
                                gatt.connect()
                                retryCount = 0
                            } else {
                                Log.e(TAG, "Authorization failed")
                                connectionFailed("Authorization error")
                            }
                        }
                        147 -> { // GATT_CONNECTION_TIMEOUT -- New in API level 35
                            connectionFailed("Connection timed out")
                        }
                        else -> {
                            Log.i(TAG, "GATT disconnected from ${gatt.device.name}")
                            onDisconnected(gatt)
                        }
                    }
                }
            }
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (D) Log.d(TAG, "onServicesDiscovered(): status = $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(TNC_SERVICE_UUID)
                if (service == null) {
                    Log.w(TAG, "KISS TNC Service not found")
                    discoveryFailed("KISS TNC Service not found")
                } else {
                    Log.d(TAG, "KISS TNC Service found")

                    synchronized(connectionState) {
                        connectionState = ConnectionState.DISCOVERED
                    }

                    val rxCharacteristic = service.getCharacteristic(TNC_SERVICE_RX_UUID)
                    val txCharacteristic = service.getCharacteristic(TNC_SERVICE_TX_UUID)
                    val descriptor = rxCharacteristic.getDescriptor(CONFIG_DESCRIPTOR_UUID)

                    this@BluetoothLEService.rxCharacteristic = rxCharacteristic
                    this@BluetoothLEService.txCharacteristic = txCharacteristic

                    txCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    if (!gatt.setCharacteristicNotification(rxCharacteristic, true)) {
                        Log.e(TAG, "Could not enable notification")
                        return
                    }

                    if (D) Log.d(TAG, "Writing descriptor to enable notification")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(
                            descriptor,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        )
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (D) Log.i(TAG, "Notification enabled")
                gatt.requestMtu(517) // This requires API Level 21
            } else {
                // Should result in a disconnect...
                Log.w(TAG,"Descriptor write failed, status = $status")
                connectionFailed("Descriptor write failed")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (D) Log.i(TAG, "MTU changed to $mtu")
            this@BluetoothLEService.mtu = mtu - 3
            if (rxCharacteristic != null) {
                onConnected(gatt)
            } else {
                Log.w(TAG, "onMtuChanged() when discovery not yet complete.")
            }
        }

        @TargetApi(Build.VERSION_CODES.S_V2)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return // handled below

            if (D) Log.d(TAG, "onCharacteristicChanged()")
            _inputStream.receive(characteristic.value)
        }

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (D) Log.d(TAG, "onCharacteristicChanged()")
            _inputStream.receive(value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w("onCharacteristicWrite", "Failed write, retrying: $status")
            } else {
                if (D) Log.d(TAG, "onCharacteristicWrite: GATT_SUCCESS")
                _outputStream.sendMore()
            }
        }
    }

    private fun send(data: ByteArray) : Boolean
    {
        if (D) Log.d(TAG, "send(${data.toHexString()})")

        return if (txCharacteristic != null && gatt!= null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                txCharacteristic
                val result = gatt!!.writeCharacteristic(
                    txCharacteristic!!,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
                if (result != BluetoothStatusCodes.SUCCESS) {
                    Log.e(TAG, "write failed; result = $result")
                    close()
                    false
                } else {
                    true
                }
            } else {
                txCharacteristic!!.value = data
                val result = gatt!!.writeCharacteristic(txCharacteristic!!)
                if (!result) {
                    Log.e(TAG, "writeCharacteristic() failed")
                }
                result
            }
        } else {
            Log.w(TAG, "write called while not connected")
            false
        }
    }

    fun write(data: ByteArray) {
        _outputStream.write(data)
        _outputStream.flush()
    }

    private fun connectInternal(device: BluetoothDevice, doRefresh: Boolean = false): Boolean {
        synchronized(connectionState) {
            // May have called close() before this delayed function is called.
            if (connectionState == ConnectionState.CONNECTING) {
                if (D) Log.d(TAG, "connectInternal() continuing.")
                val gatt = device.connectGatt(
                    applicationContext,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
                if (doRefresh) refresh(gatt) // Needed to consistently discover BLE services.
                this@BluetoothLEService.gatt = gatt
                return true
            } else {
                Log.i(TAG, "connect_internal() cancelled.")
                connectionState = ConnectionState.DISCONNECTED
                return false
            }
        }
    }

    fun reopen(): Boolean {
        if (D) Log.d(TAG, "reopen(): device = ${device?.address}")

        if (device?.bondState != BluetoothDevice.BOND_BONDED) {
            Log.e(TAG, "reopen(): device is no longer bonded")
            close()
            connectionFailed("TNC is no longer paired")
            return false
        }

        synchronized(connectionState) {
            if (connectionState != ConnectionState.DISCONNECTED) {
                Log.e(TAG, "Cannot reopen when not disconnected")
                Log.e(TAG, "Cannot reopen when not disconnected")
                return false
            }

            gatt?.let {
                connectionState = ConnectionState.CONNECTING
                retryCount = 1
                _outputStream.clear()
                _inputStream.clear()
                return it.connect()
            }
        }

        Log.e(TAG, "Cannot reopen when gatt is null")
        return false
    }

    fun open(device: BluetoothDevice): Boolean {
        if (D) Log.d(TAG, "open(device = ${device.address})")

        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            Log.e(TAG, "open(): device is not bonded")
            close()
            connectionFailed("TNC is not paired")
            return false
        }

        when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> {
                if (D) Log.d(TAG, "${device.address} is a BT classic device")
            }
            BluetoothDevice.DEVICE_TYPE_LE -> {
                if (D) Log.d(TAG, "${device.address} is a BLE device")
            }
            BluetoothDevice.DEVICE_TYPE_DUAL -> {
                if (D) Log.d(TAG, "${device.address} is a dual-mode device")
            }
            BluetoothDevice.DEVICE_TYPE_UNKNOWN -> {
                if (D) Log.d(TAG, "${device.address} is an unknown device type")
            }
        }

        synchronized(connectionState) {
            when (connectionState) {
                ConnectionState.CLOSED -> {
                    connectionState = ConnectionState.CONNECTING
                    retryCount = 1
                    _outputStream.clear()
                    _inputStream.clear()
                    this.device = device
                    return connectInternal(device)
                }
                ConnectionState.DISCONNECTED -> {
                    if (gatt == null) {
                        Log.e(TAG, "open(): gatt is null but state is DISCONNECTED, not CLOSED")
                        ConnectionState.CLOSED
                        return false
                    } else if (device.address != this.device?.address) {
                        Log.e(TAG, "open(): attempting to connect to different device when DISCONNECTED, not CLOSED")
                        return false
                    }
                    return reopen()
                }
                else -> {
                    if (D) Log.d(TAG, "${device.address} is already connecting...") else {}
                }
            }
        }
        return false
    }

    fun disconnect(): Boolean {
        if (D) Log.d(TAG, "disconnect()")
        synchronized(connectionState) {
            if (connectionState == ConnectionState.CONNECTED) {
                return gatt?.let {
                    it.disconnect()
                    true
                } ?: false
            } else {
                return false
            }
        }
    }

    fun close(gatt: BluetoothGatt? = this.gatt) {
        if (D) Log.d(TAG, "close()")
        synchronized(connectionState) {
            gatt?.close()
            this.gatt = null
            device = null
            connectionState = ConnectionState.CLOSED
        }
        val intent = Intent(BLE_EVENT)
        intent.putExtra(BLE_EVENT_ACTION, GATT_CLOSED)
        broadcastManager.sendBroadcast(intent)
    }

    fun isDisconnected() : Boolean {
        if (D) Log.d(TAG, "connectionState = $connectionState")
        return ConnectionState.DISCONNECTED == connectionState
    }

    fun isClosed() : Boolean {
        if (D) Log.d(TAG, "connectionState = $connectionState")
        return ConnectionState.CLOSED == connectionState
    }

    fun isConnected() : Boolean {
        return connectionState == ConnectionState.CONNECTED
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLEService
            get() = this@BluetoothLEService
    }

    override fun onCreate() {
        if (D) Log.d(TAG, "onCreate()")
        super.onCreate()
        receiveThread.start()
        serviceHandler = Handler(Looper.myLooper()!!)
        broadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (D) Log.d(TAG, "onStartCommand(intent: ${intent?.action}, flags: $flags, startId: $startId)")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "onBind()")
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind()")
        if (gatt != null) {
            shutdownTimer = Timer("shutdownTimer", false)
            shutdownTimer?.schedule(10000) { stopSelf() }
        } else {
            stopSelf()
        }
        return false
    }

    override fun onDestroy() {
        if (D) Log.d(TAG, "onDestroy()")
        super.onDestroy()
        gatt?.close()
        gatt = null
        device = null
        receiveThread.shutdown()
        receiveThread.join(10)
    }

    override fun stopService(name: Intent?): Boolean {
        Log.i(TAG, "stopService: ${name?.action}")
        gatt?.close()
        gatt = null
        device = null
        connectionState = ConnectionState.DISCONNECTED
        receiveThread.shutdown()
        receiveThread.join(10)
        return super.stopService(name)
    }

    private val mBinder: IBinder = LocalBinder()

    fun initialize() {
        if (D) Log.d(TAG, "initialize()")
         shutdownTimer?.cancel()
         shutdownTimer = null
         gatt?.let {
            // Let the app know about the active connection.
            onConnected(it)
        }
    }

    companion object {
        private const val D = false
        private val TAG = BluetoothLEService::class.java.simpleName

        fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

        private val TNC_SERVICE_UUID = UUID.fromString("00000001-ba2a-46c9-ae49-01b0961f68bb")
        private val TNC_SERVICE_TX_UUID = UUID.fromString("00000002-ba2a-46c9-ae49-01b0961f68bb")
        private val TNC_SERVICE_RX_UUID = UUID.fromString("00000003-ba2a-46c9-ae49-01b0961f68bb")
        private val CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val DATA_RECEIVED = 1
        const val GATT_CONNECTED = 2
        const val GATT_DISCONNECTED = 3
        const val GATT_CONNECTION_FAILED = 4
        const val GATT_DISCOVERY_FAILED = 5
        const val GATT_CLOSED = 6

        enum class ConnectionState { CLOSED, DISCONNECTED, CONNECTING, RETRYING, DISCOVERED, CONNECTED, DISCONNECTING }

        const val BLE_EVENT = "BLE_EVENT"
        const val BLE_EVENT_ACTION = "ACTION"
        const val BLE_EVENT_DEVICE = "DEVICE"
        const val BLE_EVENT_MESSAGE = "MESSAGE"
        const val BLE_EVENT_DATA = "DATA"

        fun getServiceUUID() : UUID {
            return TNC_SERVICE_UUID
        }
    }
}
