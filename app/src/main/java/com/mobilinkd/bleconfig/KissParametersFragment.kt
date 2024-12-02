package com.mobilinkd.bleconfig

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mobilinkd.bleconfig.databinding.KissParametersFragmentBinding

class KissParametersFragment : Fragment(R.layout.kiss_parameters_fragment) {

    lateinit var binding: KissParametersFragmentBinding
    private var tncProtocolDecoder: TncProtocolDecoder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = KissParametersFragmentBinding.inflate(inflater, container, false)
        binding.kissTxDelayChooser.setOnValueChangedListener { onTxDelayChanged() }
        binding.kissPersistenceChooser.setOnValueChangedListener { onPersistenceChanged() }
        binding.kissSlotTimeChooser.setOnValueChangedListener { onSlotTimeChanged() }
        binding.kissDuplexSwitch.setOnClickListener { onDuplexChanged() }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            it.setFragmentDescription(R.string.kiss_fragment_label)
            tncProtocolDecoder = it.tncProtocolDecoder
        }
    }

    private fun onTxDelayChanged() {
        if (D) Log.d(TAG, "onTxDelayChanged() = ${binding.kissTxDelayChooser.value}")
    }

    private fun onPersistenceChanged() {
        if (D) Log.d(TAG, "onPersistenceChanged() = ${binding.kissPersistenceChooser.value}")
    }

    private fun onSlotTimeChanged() {
        if (D) Log.d(TAG, "onSlotTimeChanged() = ${binding.kissSlotTimeChooser.value}")
    }

    private fun onDuplexChanged() {
        if (D) Log.d(TAG, "onDuplexChanged() = ${binding.kissDuplexSwitch.isChecked}")
    }

    companion object {
        private val TAG = KissParametersFragment::class.java.name
        private const val D = true
    }
}