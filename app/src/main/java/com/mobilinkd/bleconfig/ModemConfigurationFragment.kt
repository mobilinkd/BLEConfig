package com.mobilinkd.bleconfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mobilinkd.bleconfig.databinding.ModemConfigurationFragmentBinding

class ModemConfigurationFragment : Fragment(R.layout.modem_configuration_fragment) {

    lateinit var binding: ModemConfigurationFragmentBinding
    private var tncProtocolDecoder: TncProtocolDecoder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ModemConfigurationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            it.setFragmentDescription(R.string.modem_fragment_label)
            tncProtocolDecoder = it.tncProtocolDecoder
        }
    }

    companion object {
        private val TAG = ModemConfigurationFragment::class.java.name
        private const val D = true
    }
}