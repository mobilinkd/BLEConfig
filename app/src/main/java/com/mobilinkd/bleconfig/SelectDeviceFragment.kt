package com.mobilinkd.bleconfig

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.mobilinkd.bleconfig.databinding.SelectDeviceFragmentBinding
import java.util.LinkedList
import java.util.UUID


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SelectDeviceFragment : Fragment(),  LeDeviceListAdapter.BluetoothLEDeviceListener {

    private val _blePermissionsOld = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @RequiresApi(Build.VERSION_CODES.S)
    private val _blePermissionsNew = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
    )

    private var _binding: SelectDeviceFragmentBinding? = null

    private lateinit var _context: Context
    private lateinit var bluetoothManager : BluetoothManager
    private lateinit var bluetoothAdapter : BluetoothAdapter
    private lateinit var bluetoothLeScanner : BluetoothLeScanner
    private lateinit var leDeviceListAdapter: LeDeviceListAdapter

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = SelectDeviceFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (D) Log.d(TAG, "onViewCreated(view = $view)")

        binding.deviceRecyclerView.layoutManager = LinearLayoutManager(_context)
        leDeviceListAdapter = LeDeviceListAdapter(_context)
        leDeviceListAdapter.setClickListener(this)
        binding.deviceRecyclerView.adapter = leDeviceListAdapter
        (binding.deviceRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

    override fun onStart() {
        super.onStart()
        if (D) Log.d(TAG, "onStart()")

        requestBlePermissions(_context)
        requestBluetoothEnable(_context)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (D) Log.d(TAG, "onAttach(context = $context)")
        _context = context
        bluetoothManager = _context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() ->  ${this@SelectDeviceFragment.activity?.intent?.action}")
        (activity as MainActivity).setAlpha(1.0f)
        (activity as MainActivity).setFragmentDescription(R.string.select_device_fragment_label)
        (activity as MainActivity).disconnect()
        scanForTNCs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val permissionResultLauncher =
        this@SelectDeviceFragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissions: Map<String, Boolean> ->
            // Handle Permission granted/rejected
            if (D) Log.d(TAG,"Handling permission request")
            for (permission in permissions) {
                if (!permission.value) {
                    Toast.makeText(_context, R.string.bt_perms_needed, Toast.LENGTH_SHORT).show()
                    this@SelectDeviceFragment.activity?.finish()
                }
            }
        }

    private fun requestBlePermissions(context: Context) {
        val permissionsToRequest: MutableList<String> = LinkedList()
        val blePermissions : Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) _blePermissionsNew else _blePermissionsOld

        for (permission in blePermissions) {
            val result = ContextCompat.checkSelfPermission(context, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            } else {
                if (D) Log.d(TAG, "Have permission $permission")
            }
        }

        if (permissionsToRequest.size > 0) {
            if (D) Log.d(TAG, "Requesting ${permissionsToRequest.size} BLE permissions")
            permissionResultLauncher.launch(permissionsToRequest.toTypedArray())
            return
        }

        if (D) Log.d(TAG, "Have BLE permissions")
    }

    private val bluetoothResultLauncher =
        this@SelectDeviceFragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (D) Log.d(TAG, "Bluetooth adapter enable")
            } else {
                // User did not enable Bluetooth or an error occurred
                if (D) Log.d(TAG, "Request to enable the Bluetooth adapter was denied")
                Toast.makeText(_context, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show()
                this@SelectDeviceFragment.activity?.finish()
            }
        }

    private fun requestBluetoothEnable(context: Context): Boolean {
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            if (D) Log.d(TAG, "Bluetooth adapter is not enabled")
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothResultLauncher.launch(intent)
            return false
        } else {
            return true
        }
    }

    private var scanning = false
    private val bleHandler: Handler = object : Handler(Looper.getMainLooper()) {}
    private val SCAN_PERIOD: Long = 10000

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (D) Log.d(TAG, "BLE device found: ${result.device.address}")
            super.onScanResult(callbackType, result)
            if (leDeviceListAdapter.size() == 0) {
                (activity as MainActivity).setAlpha(0.25f)
            }
            leDeviceListAdapter.addDevice(result)

        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        if (scanning) {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
            if (D) Log.d(TAG, "BLE scanning stopped")
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanForTNCs() {
        if (!scanning) { // Stops scanning after a pre-defined scan period.
            bleHandler.postDelayed({
                stopScanning()
            }, SCAN_PERIOD)
            scanning = true
            val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(TNC_SERVICE_UUID)).build()
            val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            bluetoothLeScanner.startScan(mutableListOf(scanFilter), scanSettings, leScanCallback)
            if (D) Log.d(TAG, "BLE scanning started")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onBluetoothLEDeviceSelected(device: BluetoothDevice) {
        stopScanning()
        findNavController().navigate(R.id.action_SelectDeviceFragment_to_ConnectingFragment, bundleOf(ARG_DEVICE to device))
    }

    companion object {
        private val TAG = SelectDeviceFragment::class.java.name
        private const val D = true
        private val TNC_SERVICE_UUID = UUID.fromString("00000001-ba2a-46c9-ae49-01b0961f68bb")
    }
}