package com.mobilinkd.bleconfig

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mobilinkd.bleconfig.databinding.TncInformationFragmentBinding


class TncInformationFragment : Fragment(R.layout.tnc_information_fragment) {

    lateinit var binding: TncInformationFragmentBinding
    private val tncViewModel: TncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TncInformationFragmentBinding.inflate(inflater, container, false)

        binding.hardwareVersion.text = tncViewModel.tncHardwareVersion.value
        binding.firmwareVersion.text = tncViewModel.tncFirmwareVersion.value
        binding.macAddress.text = tncViewModel.tncMacAddress.value
        binding.serialNumber.text = tncViewModel.tncSerialNumber.value
        binding.dateTime.text = tncViewModel.tncDateTime.value

        val hwVersionObserver = Observer<String> { binding.hardwareVersion.text = it }
        val fwVersionObserver = Observer<String> { binding.firmwareVersion.text = it }
        val macAddressObserver = Observer<String> { binding.macAddress.text = it }
        val serialNumberObserver = Observer<String> { binding.serialNumber.text = it }
        val dateTimeObserver = Observer<String> { binding.dateTime.text = it }

        tncViewModel.tncHardwareVersion.observe(viewLifecycleOwner, hwVersionObserver)
        tncViewModel.tncFirmwareVersion.observe(viewLifecycleOwner, fwVersionObserver)
        tncViewModel.tncMacAddress.observe(viewLifecycleOwner, macAddressObserver)
        tncViewModel.tncSerialNumber.observe(viewLifecycleOwner, serialNumberObserver)
        tncViewModel.tncDateTime.observe(viewLifecycleOwner, dateTimeObserver)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            if (it.device == null) {
                findNavController().popBackStack(R.id.ConnectingFragment, false)
            }

            it.setFragmentDescription(R.string.info_fragment_label)
            it.setAlpha(0.1f)
            it.tncInterface.setDateTime()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (D) Log.d(TAG, "onConfigurationChanged()")
        parentFragmentManager.beginTransaction().detach(this).commitAllowingStateLoss()
        super.onConfigurationChanged(newConfig)
        parentFragmentManager.beginTransaction().attach(this).commitAllowingStateLoss()
    }

    companion object {
        private val TAG = TncInformationFragment::class.java.simpleName
        private const val D = true
    }
}