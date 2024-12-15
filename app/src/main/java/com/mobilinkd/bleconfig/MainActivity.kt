package com.mobilinkd.bleconfig

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.mobilinkd.bleconfig.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    val device get() = bleDevice

    private lateinit var binding: ActivityMainBinding
    private var bleService: BluetoothLEService? = null
    private var bleDevice: BluetoothDevice? = null
    val tncViewModel by viewModels<TncViewModel>()
    private lateinit var kissDecoder: KissDecoder
    private var _connectionCallback: ConnectionCallback? = null
    private var reconnectOnRestart = false
    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var gattServiceIntent: Intent
    lateinit var tncInterface: TncInterface

    interface ConnectionCallback {
        fun onConnect()
        fun onFailure(msg: String)
        fun onDisconnect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val applicationVersion: TextView = findViewById(R.id.application_version)
        applicationVersion.text = getString(R.string.version_name, BuildConfig.VERSION_NAME)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)
        setSupportActionBar(findViewById(R.id.toolbar))

        broadcastManager = LocalBroadcastManager.getInstance(this)
        gattServiceIntent = Intent(this, BluetoothLEService::class.java)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStart() {
        super.onStart()
        if (D) Log.d(TAG, "onStart()")
        broadcastManager.registerReceiver(bleBroadcastReceiver,IntentFilter(BluetoothLEService.BLE_EVENT))
        if (bleService == null) {
            Log.i(TAG, "Binding BLEService")
            bindService(gattServiceIntent, bleConnection, BIND_AUTO_CREATE)
        } else {
            if (D) Log.d(TAG, "BLEService already bound")
            if (bleService?.isConnected() == false) {
                bleDevice = null
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (D) Log.d(TAG, "onOptionsItemSelected(home)")
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> {
                if (D) Log.d(TAG, "onOptionsItemSelected(${item.itemId})")
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (D) Log.d(TAG, "onResume()")

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (bluetoothManager.adapter == null) {
            Toast.makeText(this@MainActivity, R.string.no_bt_adapter, Toast.LENGTH_LONG).show()
            finish()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this@MainActivity, R.string.bt_perms_needed, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (D) Log.d(TAG, "onPause()")
    }

    override fun onStop() {
        super.onStop()
        if (D) Log.d(TAG, "onStop()")
        bleService?.let {
            if (it.isConnected()) {
                tncInterface.saveIfChanged()
                Log.i(TAG, "disconnecting on stop")
                it.disconnect()
            }
        }
        broadcastManager.unregisterReceiver(bleBroadcastReceiver)
    }

    override fun onDestroy() {
        if (D) Log.d(TAG, "onDestroy()")
        super.onDestroy()
        if (!isChangingConfigurations) {
            bleService?.let {
                it.close()
                unbindService(bleConnection)
            }
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        if (reconnectOnRestart) {
            bleDevice?.let {
                savedInstanceState.putString(DEVICE_TAG, it.address)
            }
        }
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val address = savedInstanceState.getString(DEVICE_TAG)
        address?.let {
            // TODO: Reconnect?
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (D) Log.d(TAG, "onSupportNavigateUp()")
        Log.i(TAG, "Coming from ${findNavController(R.id.nav_host_fragment_content_main).currentBackStackEntry?.id}")
        return findNavController(R.id.nav_host_fragment_content_main).navigateUp()
                || super.onSupportNavigateUp()
    }

    private val bleConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "onServiceConnected: className -> " + className.className)
            Log.i(TAG, "binding to: " + className.shortClassName)
            val binder = service as BluetoothLEService.LocalBinder
            bleService = binder.service
            binder.service.initialize()
            tncInterface = TncInterface(binder.service.outputStream)
            kissDecoder = KissDecoder(tncViewModel)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.i(TAG, "onServiceDisconnected: className -> " + className.className)
            bleService = null
            bleDevice = null
        }
    }

    @SuppressLint("MissingPermission")
    fun setFragmentDescription(@StringRes description: Int) {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(description)
    }

    @SuppressLint("MissingPermission")
    fun setFragmentDescription(description: String) {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(description)
    }

    val connectionCallback = object: ConnectionCallback {
        override fun onConnect() {
            if (D) Log.d(TAG, "onDeviceConnected()")
        }

        override fun onFailure( msg: String) {
            findNavController(R.id.nav_host_fragment_content_main).popBackStack(R.id.SelectDeviceFragment, false)
        }

        override fun onDisconnect() {
            findNavController(R.id.nav_host_fragment_content_main).popBackStack(R.id.SelectDeviceFragment, false)
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, callback: ConnectionCallback): Boolean {
        if (D) Log.d(TAG, "connect(device: ${device.address})")
        _connectionCallback = callback
        bleService?.let {
            if (it.isClosed()) {
                return it.open(device)
            } else {
                Log.e(TAG, "Cannot open ${device.name}; not closed.")
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun reconnect(callback: ConnectionCallback): Boolean {
        if (D) Log.d(TAG, "reconnect()")
        _connectionCallback = callback
        bleService?.let {
            if (it.isDisconnected()) {
                return it.reopen()
            } else {
                Log.e(TAG, "Cannot reopen BLE connection; not disconnected.")
            }
        }
        return false
    }

    fun disconnect() {
        bleService?.disconnect()
    }

    fun close() {
        bleService?.close()
    }

    fun setAlpha(alpha: Float) {
        if (alpha < 0.0f || alpha > 1.0f) throw IllegalArgumentException()
        val image: ImageView = findViewById(R.id.imageView)
        image.alpha = alpha
        val applicationVersion: TextView = findViewById(R.id.application_version)
        applicationVersion.alpha = alpha
    }

    private val bleBroadcastReceiver = object: BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = it.getIntExtra(BluetoothLEService.BLE_EVENT_ACTION, 0)
                when (action) {
                    BluetoothLEService.GATT_CONNECTED -> {
                        bleDevice = it.getParcelableExtra(BluetoothLEService.BLE_EVENT_DEVICE) as BluetoothDevice?
                        _connectionCallback?.onConnect()
                        return
                    }
                    BluetoothLEService.GATT_DISCONNECTED -> {
                        Log.i(TAG, "GATT disconnected")
                        _connectionCallback?.onDisconnect()
                        bleDevice = null
                    }

                    BluetoothLEService.GATT_CLOSED -> {
                        Log.i(TAG, "GATT closed")
                        if (bleDevice != null) {
                            _connectionCallback?.onDisconnect()
                            bleDevice = null
                        }
                    }

                    BluetoothLEService.GATT_CONNECTION_FAILED-> {
                        // This occurs if the TNC is no longer available, either turned off or out of
                        // range when the connection was attempted.
                        Log.w(TAG, "BLE connection failed.")
                        close()
                        val message = it.getStringExtra(BluetoothLEService.BLE_EVENT_MESSAGE)
                        message?.let { msg ->
                            _connectionCallback?.onFailure(msg)
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    BluetoothLEService.GATT_DISCOVERY_FAILED-> {
                        // This occurs occasionally with some dual-mode devices. The services are cached
                        // and will not be found until Bluetooth is enabled/disabled by user
                        Log.w(TAG, "BLE discovery failed.")
                        close()
                        val builder = AlertDialog.Builder(this@MainActivity)
                        val message = it.getStringExtra(BluetoothLEService.BLE_EVENT_MESSAGE)
                        message?.let { msg ->
                            _connectionCallback?.onFailure(msg)

                            builder.setTitle("Connection Failed")
                            builder.setMessage("Service discovery has failed multiple times. You may need to disable and enable the Bluetooth adapter to be able to connect.")
                            builder.setPositiveButton("OK") { _, _ ->
                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                            }
                            builder.show()
                        }
                        if (!findNavController(R.id.nav_host_fragment_content_main).popBackStack(
                                R.id.SelectDeviceFragment,
                                false
                            )
                        ) {
                            Log.e(TAG, "popBackStack failed")
                            findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_global_SelectDeviceFragment)
                        }
                    }
                    BluetoothLEService.DATA_RECEIVED -> {
                        val data = it.getByteArrayExtra(BluetoothLEService.BLE_EVENT_DATA)
                        data?.let { kissDecoder.decode(it) }
                        return
                    }
                    else -> {
                        Log.e(TAG, "Unexpected BLE_EVENT, action = $action")
                        return
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val D = true
        private const val DEVICE_TAG = "MAC_ADDRESS"
    }
}