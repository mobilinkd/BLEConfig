package com.mobilinkd.bleconfig

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.mobilinkd.bleconfig.databinding.KissParametersFragmentBinding
import kotlinx.coroutines.launch

class KissParametersFragment : Fragment(R.layout.kiss_parameters_fragment) {

    lateinit var binding: KissParametersFragmentBinding
    private val tncViewModel: TncViewModel by activityViewModels()

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

        val txDelayObserver = Observer<Int> { binding.kissTxDelayChooser.value = it }
        val persistenceObserver = Observer<Int> { binding.kissPersistenceChooser.value = it }
        val slotTimeObserver = Observer<Int> { binding.kissSlotTimeChooser.value = it }
        val duplexObserver = Observer<Int> { binding.kissDuplexSwitch.isChecked = (it != 0) }

        tncViewModel.tncTxDelay.observe(viewLifecycleOwner, txDelayObserver)
        tncViewModel.tncPersistence.observe(viewLifecycleOwner, persistenceObserver)
        tncViewModel.tncSlotTime.observe(viewLifecycleOwner, slotTimeObserver)
        tncViewModel.tncDuplex.observe(viewLifecycleOwner, duplexObserver)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            if (it.device == null) {
                findNavController().popBackStack(R.id.ConnectingFragment, false)
            }

            it.setFragmentDescription(R.string.kiss_fragment_label)
            it.setAlpha(0.1f)
        }
    }

    private fun onTxDelayChanged() {
        if (D) Log.d(TAG, "onTxDelayChanged() = ${binding.kissTxDelayChooser.value}")
        (activity as MainActivity).tncInterface.setTxDelay(binding.kissTxDelayChooser.value)
    }

    private fun onPersistenceChanged() {
        if (D) Log.d(TAG, "onPersistenceChanged() = ${binding.kissPersistenceChooser.value}")
        (activity as MainActivity).tncInterface.setPersistence(binding.kissPersistenceChooser.value)
    }

    private fun onSlotTimeChanged() {
        if (D) Log.d(TAG, "onSlotTimeChanged() = ${binding.kissSlotTimeChooser.value}")
        (activity as MainActivity).tncInterface.setSlotTime(binding.kissSlotTimeChooser.value)
    }

    private fun onDuplexChanged() {
        if (D) Log.d(TAG, "onDuplexChanged() = ${binding.kissDuplexSwitch.isChecked}")
        (activity as MainActivity).tncInterface.setDuplex(binding.kissDuplexSwitch.isChecked)
    }

    companion object {
        private val TAG = KissParametersFragment::class.java.simpleName
        private const val D = true
    }
}