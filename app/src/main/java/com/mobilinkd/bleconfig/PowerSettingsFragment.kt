package com.mobilinkd.bleconfig

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import com.mobilinkd.bleconfig.KissParametersFragment.Companion
import com.mobilinkd.bleconfig.databinding.PowerSettingsFragmentBinding

class PowerSettingsFragment : Fragment(R.layout.power_settings_fragment) {

    lateinit var binding: PowerSettingsFragmentBinding
    private val tncViewModel: TncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PowerSettingsFragmentBinding.inflate(inflater, container, false)
        binding.usbPowerOnSwitch.setOnClickListener { onUsbPowerOnChanged() }
        binding.usbPowerOffSwitch.setOnClickListener { onUsbPowerOffChanged() }

        val batteryLevelObserver = Observer<Int> { setPowerLevel(it) }
        val usbPowerOnObserver = Observer<Int> { binding.usbPowerOnSwitch.isChecked = it != 0 }
        val usbPowerOffObserver = Observer<Int> { binding.usbPowerOffSwitch.isChecked = it != 0 }

        tncViewModel.tncBatteryLevel.observe(viewLifecycleOwner, batteryLevelObserver)
        tncViewModel.tncUsbPowerOn.observe(viewLifecycleOwner, usbPowerOnObserver)
        tncViewModel.tncUsbPowerOff.observe(viewLifecycleOwner, usbPowerOffObserver)

        tncViewModel.tncBatteryLevel.value?.let { setPowerLevel(it) }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            if (it.device == null) {
                findNavController().popBackStack(R.id.ConnectingFragment, false)
            }

            it.setFragmentDescription(R.string.power_fragment_label)
            it.setAlpha(0.1f)
            it.tncInterface.getBatteryLevel()
        }
    }

    private fun setPowerLevel(mV: Int) {
        if (D) Log.d(TAG, "battery level = ${mV}mV")
        var level = mV.toFloat()

        if (level < 3200.0) level = 3200f
        else if (level > 4200.0) level = 4200f

        if (level < 3400f) binding.batteryLevel.trackActiveTintList = requireContext().getColorStateList(R.color.level_bad)
        else if (level < 3700f) binding.batteryLevel.trackActiveTintList = requireContext().getColorStateList(R.color.level_ok)
        else binding.batteryLevel.trackActiveTintList = requireContext().getColorStateList(R.color.level_good)

        binding.batteryLevel.value = level
        binding.batteryLevelMvLabel.text = getString(R.string.battery_level_mv_label, mV)
    }

    private fun onUsbPowerOnChanged() {
        if (D) Log.d(TAG, "onUsbPowerOnChanged() = ${binding.usbPowerOnSwitch.isChecked}")
        (activity as MainActivity).tncInterface.setUsbPowerOn(binding.usbPowerOnSwitch.isChecked)
    }

    private fun onUsbPowerOffChanged() {
        if (D) Log.d(TAG, "onUsbPowerOffChanged() = ${binding.usbPowerOffSwitch.isChecked}")
        (activity as MainActivity).tncInterface.setUsbPowerOff(binding.usbPowerOffSwitch.isChecked)
    }

    companion object {
        private val TAG = PowerSettingsFragment::class.java.simpleName
        private const val D = true
    }
}