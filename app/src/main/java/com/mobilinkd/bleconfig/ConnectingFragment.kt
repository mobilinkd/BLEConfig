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

/**
 * Fragment for connecting or reconnecting to a device.
 *
 * Possibilities on Resume
 *
 *  - Arrive via SelectDeviceFragment
 *    - Try to connect to specified device
 *  - Arrive via MainMenuFragment
 *    - Connected -- Close and navigate to SelectDeviceFragment.
 *    - Disconnected -- Reconnect
 *  - Arrive via ConnectingFragment
 *    - Resumed while pairing (wasStopped = false) -- Continue until callback received.
 *    - Stopped (wasStopped = true) -- Close on stop and navigate to SelectDeviceFragment on resume.
 *
 */
class ConnectingFragment : Fragment(R.layout.connecting_fragment) {
    lateinit var binding: ConnectingFragmentBinding
    private var device: BluetoothDevice? = null
    private var source = R.id.SelectDeviceFragment
    private var isConnected = false
    private var isConnecting = false
    private var wasStopped = false

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
            source = it.getInt(ARG_SOURCE)
        }
        if (D) Log.d(TAG, "onViewCreated(device = ${device?.address}, source = ${source}), isConnected = ${isConnected}")
    }

    private val connectionCallback = object: MainActivity.ConnectionCallback {
        override fun onConnect() {
            if (D) Log.d(TAG, "onDeviceConnected()")
            isConnected = true
            isConnecting = false
            wasStopped = false
            arguments?.putInt(ARG_SOURCE, R.id.MainMenuFragment)
            if (this@ConnectingFragment.isAdded) findNavController().navigate(R.id.action_ConnectingFragment_to_MainMenuFragment)
        }

        override fun onFailure( msg: String) {
            if (isConnecting) {
                isConnected = false
                isConnecting = false
                wasStopped = false
                arguments?.putInt(ARG_SOURCE, R.id.SelectDeviceFragment)
                if (this@ConnectingFragment.isAdded) findNavController().popBackStack(
                    R.id.SelectDeviceFragment,
                    false)
            }
        }

        override fun onDisconnect() {
            if (isConnecting) {
                isConnected = false
                isConnecting = false
                wasStopped = false
                arguments?.putInt(ARG_SOURCE, R.id.SelectDeviceFragment)
                if (this@ConnectingFragment.isAdded) findNavController().popBackStack(
                    R.id.SelectDeviceFragment,
                    false
                )
            }
        }
    }

    private fun setSource(id: Int) {
        arguments?.putInt(ARG_SOURCE, id)
        source = id
    }

    override fun onResume() {
        if (D) Log.d(TAG, "onResume()")
        super.onResume()

        (activity as MainActivity).setAlpha(0.1f)
        (activity as MainActivity).setFragmentDescription(R.string.connecting_fragment_label)
        when (source) {
            R.id.SelectDeviceFragment -> {
                Log.i(TAG, "Resume from SelectDeviceFragment")
                Log.i(TAG, "Connecting to ${device!!.address}")
                isConnecting = (activity as MainActivity).connect(device!!, connectionCallback)
                if (!isConnecting) {
                    findNavController().popBackStack(R.id.SelectDeviceFragment, false)
                }
                setSource(R.id.ConnectingFragment)
                return
            }
            R.id.ConnectingFragment -> {
                Log.i(TAG, "Resume from ConnectingFragment")
                if (wasStopped) {
                    wasStopped = false
                    setSource(R.id.SelectDeviceFragment)
                    findNavController().popBackStack(R.id.SelectDeviceFragment, false)
                }
                return
            }
            R.id.MainMenuFragment -> {
                Log.i(TAG, "Resume from MainMenuFragment")
                if ((activity as MainActivity).device != null) {
                    // Connected; disconnect and select new device.
                    (activity as MainActivity).tncInterface.saveIfChanged()
                    (activity as MainActivity).disconnect()
                    setSource(R.id.SelectDeviceFragment)
                    findNavController().popBackStack(R.id.SelectDeviceFragment, false)
                } else {
                    // Disconnected; reconnect or connect to last device.
                    isConnecting = (activity as MainActivity).reconnect(connectionCallback)
                    if (!isConnecting) {
                        isConnecting = (activity as MainActivity).connect(device!!, connectionCallback)
                        if (!isConnecting) {
                            Log.w(TAG, "Connect failed")
                            setSource(R.id.SelectDeviceFragment)
                            findNavController().popBackStack(R.id.SelectDeviceFragment, false)
                        }
                    }
                }
            }
            else -> {
                Log.e(TAG, "Unexpected source = $source")
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
        wasStopped = (source == R.id.ConnectingFragment)
    }

    private fun onDeviceConnected() {
        if (D) Log.d(TAG, "onDeviceConnected()")
        if (this.isAdded) findNavController().navigate(R.id.action_ConnectingFragment_to_MainMenuFragment)
    }

    companion object {
        private val TAG = ConnectingFragment::class.java.simpleName
        private const val D = true

        const val ARG_DEVICE = "device"
        const val ARG_SOURCE = "source"
    }
}