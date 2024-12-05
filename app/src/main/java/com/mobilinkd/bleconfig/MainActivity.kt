package com.mobilinkd.bleconfig

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.mobilinkd.bleconfig.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    val tncProtocolDecoder get() = _tncProtocolDecoder

    private lateinit var binding: ActivityMainBinding
    private var bleService: BluetoothLEService? = null
    private var bleDevice: BluetoothDevice? = null
    private var _tncProtocolDecoder: TncProtocolDecoder? = null
    private var _connectionCallback: ConnectionCallback? = null
    private var reconnectOnResume = false

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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStart() {
        super.onStart()
        if (D) Log.d(TAG, "onStart()")
        if (bleService == null) {
            Log.i(TAG, "Binding BLEService")
            val gattServiceIntent = Intent(this, BluetoothLEService::class.java)
            bindService(gattServiceIntent, bleConnection, BIND_AUTO_CREATE)
            _tncProtocolDecoder = TncProtocolDecoder(this)
        } else {
            if (D) Log.d(TAG, "BLEService already bound")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
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

        if (reconnectOnResume) {
            findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_global_ConnectingFragment)
            reconnectOnResume = false
        }
    }

    override fun onPause() {
        super.onPause()
        if (D) Log.d(TAG, "onPause()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (D) Log.d(TAG, "onConfigurationChanged()")
    }

    override fun onStop() {
        super.onStop()
        if (D) Log.d(TAG, "onStop()")
        disconnect()
        if (bleDevice != null) reconnectOnResume = true
    }

    override fun onDestroy() {
        if (D) Log.d(TAG, "onDestroy()")
        super.onDestroy()
        bleService?.let {
            unbindService(bleConnection)
            bleService = null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (D) Log.d(TAG, "onSupportNavigateUp()")
        return findNavController(R.id.nav_host_fragment_content_main).navigateUp()
                || super.onSupportNavigateUp()
    }

    private val bleConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "onServiceConnected: className -> " + className.className)
            Log.i(TAG, "binding to: " + className.shortClassName)
            val binder = service as BluetoothLEService.LocalBinder
            binder.service.initialize(bleHandler)
            bleService = binder.service
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.i(TAG, "onServiceDisconnected: className -> " + className.className)
            bleService = null
        }
    }

    @SuppressLint("MissingPermission")
    fun setFragmentDescription(@StringRes description: Int) {
//        val view: TextView = findViewById(R.id.fragmentDescription)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(description)
//        view.text = getString(description)
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

    private val bleHandler: Handler = object : Handler(Looper.getMainLooper()) {
        @SuppressLint("MissingPermission")
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                BluetoothLEService.DATA_RECEIVED -> {
                    // Process data
                }
                BluetoothLEService.GATT_CONNECTED -> {
                    Log.i(TAG, "GATT connected")
                    bleDevice = msg.obj as BluetoothDevice
                    val deviceName: TextView = findViewById(R.id.deviceName)
                    deviceName.text = bleDevice?.name
                    val deviceGroup: View = findViewById(R.id.deviceGroup)
                    deviceGroup.visibility = View.VISIBLE
                    _connectionCallback?.onConnect()
                }

                BluetoothLEService.GATT_DISCONNECTED -> {
                    Log.i(TAG, "GATT disconnected")
                    val deviceGroup: View = findViewById(R.id.deviceGroup)
                    deviceGroup.visibility = View.GONE
                    _connectionCallback?.onDisconnect()
                }

                BluetoothLEService.GATT_CLOSED -> {
                    Log.i(TAG, "GATT closed")
                    val deviceGroup: View = findViewById(R.id.deviceGroup)
                    deviceGroup.visibility = View.GONE
                    _connectionCallback?.onDisconnect()
                }

                BluetoothLEService.GATT_CONNECTION_FAILED-> {
                    // This occurs if the TNC is no longer available, either turned off or out of
                    // range when the connection was attempted.
                    Log.w(TAG, "BLE connection failed.")
                    _connectionCallback?.onFailure(msg.obj as String)
                    Toast.makeText(this@MainActivity, msg.obj as String, Toast.LENGTH_SHORT).show()
                    bleService?.close()
                    if (!findNavController(R.id.nav_host_fragment_content_main).popBackStack(R.id.SelectDeviceFragment, false)) {
                        Log.e(TAG, "popBackStack failed")
                        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_global_SelectDeviceFragment)
                    }
                }

                BluetoothLEService.GATT_DISCOVERY_FAILED-> {
                    // This occurs occasionally with some dual-mode devices. The services are cached
                    // and will not be found until Bluetooth is enabled/disabled by user
                    Log.w(TAG, "BLE connection failed.")
                    bleService?.close()
                    val builder = AlertDialog.Builder(this@MainActivity)
                    val message = msg.obj as String
                    _connectionCallback?.onFailure(message)
                    builder.setTitle("Connection Failed")
                    builder.setMessage("Service discovery has failed multiple times. You may need to disable and enable the Bluetooth adapter to be able to connect.")
                    builder.setPositiveButton("OK") { _, _ ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                    builder.show()
                    if (!findNavController(R.id.nav_host_fragment_content_main).popBackStack(R.id.SelectDeviceFragment, false)) {
                        Log.e(TAG, "popBackStack failed")
                        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_global_SelectDeviceFragment)
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.name
        private const val D = true
    }
}