package com.mobilinkd.bleconfig

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mobilinkd.bleconfig.databinding.ModemConfigurationFragmentBinding

class ModemConfigurationFragment : Fragment(R.layout.modem_configuration_fragment) {

    lateinit var binding: ModemConfigurationFragmentBinding
    private val tncViewModel: TncViewModel by activityViewModels()
    private lateinit var modemList: MutableList<String>
    private lateinit var modemSpinnerAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ModemConfigurationFragmentBinding.inflate(inflater, container, false)

        modemList = mutableListOf(getString(R.string.modem_1200_afsk))
        modemSpinnerAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, modemList)
        modemSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.modemTypeSpinner.adapter = modemSpinnerAdapter

        binding.modemTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (D) Log.d(TAG, "onNothingSelected")
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val modemName = modemList[position]
                val modemType = getModemNumber(modemName)
                if (D) Log.d(TAG, "onItemSelected = $position, $modemName, $modemType")
                (activity as MainActivity).tncInterface.setModemType(modemType)
            }
        }
        binding.passAllSwitch.setOnClickListener { onPassAllChanged() }
        binding.receivePolaritySwitch.setOnClickListener { onReceivePolarityChanged() }
        binding.transmitPolaritySwitch.setOnClickListener { onTransmitPolarityChanged() }

        val supportedModemTypesObserver = Observer<ByteArray> { updateSupportedModemTypes(it) }
        val modemTypeObserver = Observer<Int> { updateModemType(it) }
        val passAllSwitchObserver = Observer<Int> { binding.passAllSwitch.isChecked = it != 0 }
        val receivePolaritySwitchObserver = Observer<Int> { binding.receivePolaritySwitch.isChecked = it != 0 }
        val transmitPolaritySwitchObserver = Observer<Int> { binding.transmitPolaritySwitch.isChecked = it != 0 }

        tncViewModel.tncSupportedModemTypes.observe(viewLifecycleOwner, supportedModemTypesObserver)
        tncViewModel.tncModemType.observe(viewLifecycleOwner, modemTypeObserver)
        tncViewModel.tncPassall.observe(viewLifecycleOwner, passAllSwitchObserver)
        tncViewModel.tncRxPolarity.observe(viewLifecycleOwner, receivePolaritySwitchObserver)
        tncViewModel.tncTxPolarity.observe(viewLifecycleOwner, transmitPolaritySwitchObserver)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            if (it.device == null) {
                findNavController().popBackStack(R.id.ConnectingFragment, false)
            }

            it.setFragmentDescription(R.string.modem_fragment_label)
            it.setAlpha(0.1f)
        }
    }

    private fun updateSupportedModemTypes(modems: ByteArray) {
        if (D) Log.d(TAG, "updateSupportedModemTypes = ${modems.toHexString()}")
        modemList.clear()
        modems.forEach {
            MODEM_TYPES[it.toInt()]?.let { modemType ->
                val modemName = getString(modemType)
                if (D) Log.d(TAG, "adding modem type '$modemName'")
                modemList.add(modemName)
            }
        }
        modemSpinnerAdapter.notifyDataSetChanged()
    }

    private fun updateModemType(modem: Int) {
        if (D) Log.d(TAG, "updateModemType = $modem")
        val modemName = MODEM_TYPES[modem]
        modemName?.let {
            modemList.forEachIndexed { index, s ->
                if (s == getString(modemName))
                    binding.modemTypeSpinner.setSelection(index)
            }
        }
    }

    private fun getModemNumber(modem: String): Int {
        return MODEM_TYPES.entries.find { modem == getString(it.value) }?.key ?: 0
    }

    private fun onPassAllChanged() {
        if (D) Log.d(TAG, "onPassAllChanged")
        (activity as MainActivity).tncInterface.setPassAll(binding.passAllSwitch.isChecked)
    }

    private fun onReceivePolarityChanged() {
        if (D) Log.d(TAG, "onReceivePolarityChanged")
        (activity as MainActivity).tncInterface.setReceivePolarity(binding.receivePolaritySwitch.isChecked)
    }

    private fun onTransmitPolarityChanged() {
        if (D) Log.d(TAG, "onTransmitPolarityChanged")
        (activity as MainActivity).tncInterface.setTransmitPolarity(binding.transmitPolaritySwitch.isChecked)
    }

    companion object {
        private val TAG = ModemConfigurationFragment::class.java.simpleName
        private const val D = true

        fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

        private val MODEM_TYPES = hashMapOf(
            1 to R.string.modem_1200_afsk,
            2 to R.string.modem_300_afsk,
            3 to R.string.modem_9600_fsk,
            5 to R.string.modem_M17
        )
    }
}
