package com.mobilinkd.bleconfig

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mobilinkd.bleconfig.databinding.ConnectingFragmentBinding

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
const val ARG_DEVICE = "device"

class ConnectingFragment : Fragment(R.layout.connecting_fragment) {
    lateinit var binding: ConnectingFragmentBinding
    private var device: BluetoothDevice? = null
    private var isConnected = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ConnectingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            device = it.getParcelable(ARG_DEVICE) as BluetoothDevice?
        }
        if (D) Log.d(TAG, "onViewCreated(device = ${device?.address})")
    }

    val connectionCallback = object: MainActivity.ConnectionCallback {
        override fun onConnect() {
            if (D) Log.d(TAG, "onDeviceConnected()")
            if (this@ConnectingFragment.isAdded) findNavController().navigate(R.id.action_ConnectingFragment_to_MainMenuFragment)
        }

        override fun onFailure( msg: String) {
            isConnected = false
            if (this@ConnectingFragment.isAdded) findNavController().popBackStack(R.id.SelectDeviceFragment, false)
        }

        override fun onDisconnect() {
            isConnected = false
            if (this@ConnectingFragment.isAdded) findNavController().popBackStack(R.id.SelectDeviceFragment, false)
        }
    }

    override fun onResume() {
        if (D) Log.d(TAG, "onResume()")
        super.onResume()
        (activity as MainActivity).setAlpha(0.1f)
        (activity as MainActivity).setFragmentDescription(R.string.connecting_fragment_label)
        if (device != null) {
            if (!isConnected) {
                Log.i(TAG, "Connecting to ${device!!.address}")
                isConnected = (activity as MainActivity).connect(device!!, connectionCallback)
                if (!isConnected) {
                    findNavController().popBackStack(R.id.SelectDeviceFragment, false)
                }
            } else {
                Log.i(TAG, "Already connected to ${device!!.address}?!?")
            }
        }  else {
            if (!isConnected) {
                Log.i(TAG, "Reconnecting")
                isConnected = (activity as MainActivity).reconnect(connectionCallback)
                if (!isConnected) {
                    findNavController().popBackStack(R.id.SelectDeviceFragment, false)
                }
            } else {
                Log.i(TAG, "Already connected?!?")
                findNavController().popBackStack(R.id.SelectDeviceFragment, false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (D) Log.d(TAG, "onPause()")
    }

    override fun onStop() {
        if (D) Log.d(TAG, "onStop()")
        super.onStop()
        device = null
    }

    private fun onDeviceConnected() {
        if (D) Log.d(TAG, "onDeviceConnected()")
        if (this.isAdded) findNavController().navigate(R.id.action_ConnectingFragment_to_MainMenuFragment)
    }

    companion object {
        private val TAG = ConnectingFragment::class.java.name
        private const val D = true
    }
}