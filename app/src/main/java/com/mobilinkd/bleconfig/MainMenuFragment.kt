package com.mobilinkd.bleconfig

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.mobilinkd.bleconfig.databinding.MainMenuFragmentBinding

class MainMenuFragment : Fragment() {

    private var _binding: MainMenuFragmentBinding? = null
    private var device: BluetoothDevice? = null
    private lateinit var broadcastManager: LocalBroadcastManager

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val bleBroadcastReceiver = object: BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = it.getIntExtra(BluetoothLEService.BLE_EVENT_ACTION, 0)
                when (action) {
                    BluetoothLEService.GATT_CONNECTED -> {
                        device =
                            it.getParcelableExtra(BluetoothLEService.BLE_EVENT_DEVICE) as BluetoothDevice?
                        device?.let { dev -> (activity as MainActivity).setFragmentDescription(dev.name) }
                        return
                    }
                    BluetoothLEService.GATT_DISCONNECTED -> {
                        Log.i(TAG, "GATT disconnected")
                        device = null
                        if (!findNavController().navigateUp()) {
                            activity?.finish()
                        }
                    }
                    else -> {
                        if (D) Log.d(TAG, "bleBroadcastReceiver: action = $action ignored")
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        broadcastManager = LocalBroadcastManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainMenuFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.receiveAudioButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_ReceiveAudioFragment)
        }

        binding.transmitAudioButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_transmitAudioFragment)
        }

        binding.powerSettingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_powerSettingsFragment)
        }

        binding.kissParametersButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_kissParametersFragment)
        }

        binding.modemConfigurationButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_modemConfigurationFragment)
        }

        binding.tncInformationButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_tncInformationFragment)
        }
    }

    override fun onStart() {
        super.onStart()
        if (D) Log.d(TAG, "onStart()")
        broadcastManager.registerReceiver(bleBroadcastReceiver,
            IntentFilter(BluetoothLEService.BLE_EVENT)
        )
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if (D) Log.d(TAG, "onResume()")
        (activity as MainActivity?)?.let {
            if (it.device == null) {
                findNavController().popBackStack(R.id.ConnectingFragment, false)
            } else {
                it.device?.let {device ->
                    (activity as MainActivity).setFragmentDescription(device.name)
                }
            }
            it.setAlpha(0.1f)
            it.tncInterface.getAllValues()
        }
    }

    override fun onPause() {
        super.onPause()
        if (D) Log.d(TAG, "onPause()")
    }

    override fun onStop() {
        super.onStop()
        if (D) Log.d(TAG, "onStop()")
        broadcastManager.unregisterReceiver(bleBroadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (D) Log.d(TAG, "onConfigurationChanged()")
        parentFragmentManager.beginTransaction().detach(this).commitAllowingStateLoss()
        super.onConfigurationChanged(newConfig)
        parentFragmentManager.beginTransaction().attach(this).commitAllowingStateLoss()
    }

    companion object {
        private val TAG = MainMenuFragment::class.java.simpleName
        private const val D = true
    }
}