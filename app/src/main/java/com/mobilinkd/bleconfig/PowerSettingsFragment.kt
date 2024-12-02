package com.mobilinkd.bleconfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mobilinkd.bleconfig.databinding.PowerSettingsFragmentBinding

class PowerSettingsFragment : Fragment(R.layout.power_settings_fragment) {

    lateinit var binding: PowerSettingsFragmentBinding
    private var tncProtocolDecoder: TncProtocolDecoder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PowerSettingsFragmentBinding.inflate(inflater, container, false)
        binding.batteryLevelMvLabel.text = getString(R.string.battery_level_mv_label, 4000)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            it.setFragmentDescription(R.string.power_fragment_label)
            tncProtocolDecoder = it.tncProtocolDecoder
        }
    }

    companion object {
        private val TAG = PowerSettingsFragment::class.java.name
        private const val D = true
    }
}