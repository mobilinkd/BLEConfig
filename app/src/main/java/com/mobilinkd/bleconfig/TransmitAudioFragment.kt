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
import com.mobilinkd.bleconfig.databinding.TransmitAudioFragmentBinding
import java.util.Locale

class TransmitAudioFragment : Fragment(R.layout.transmit_audio_fragment) {
    lateinit var binding: TransmitAudioFragmentBinding
    private val tncViewModel: TncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TransmitAudioFragmentBinding.inflate(inflater, container, false)
        binding.transmitGainSlider.isEnabled = false
        binding.transmitTwistSlider.isEnabled = false
        binding.transmitTwist.isEnabled = false
        binding.transmitGain.isEnabled = false

        binding.pttStyleSwitch.setOnCheckedStateChangeListener { _, _ -> onPttStyleSwitchClicked() }
        binding.transmitGainSlider.addOnChangeListener { _, value, fromUser -> onTransmitGainChanged(value, fromUser) }
        binding.transmitTwistSlider.addOnChangeListener { _, value, fromUser -> onTransmitTwistChanged(value, fromUser) }
        binding.testToneSwitch.setOnCheckedStateChangeListener { _, _ -> onTransmitClicked() }
        binding.transmitButton.setOnClickListener { onTransmitClicked() }

        val pttStyleObserver = Observer<Int> { setPttStyle(it) }
        val transmitGainObserver = Observer<Int> {
            binding.transmitGainSlider.value = it.toFloat()
            binding.transmitGain.text = String.format(Locale.getDefault(),"%d", it)
        }
        val transmitTwistMinObserver = Observer<Int> { binding.transmitTwistSlider.valueFrom = it.toFloat() }
        val transmitTwistMaxObserver = Observer<Int> { binding.transmitTwistSlider.valueTo = it.toFloat() }
        val transmitTwistObserver = Observer<Int> {
            binding.transmitTwistSlider.value = it.toFloat()
            binding.transmitTwist.text = String.format(Locale.getDefault(),"%d", it)
        }

        tncViewModel.tncPttStyle.observe(viewLifecycleOwner, pttStyleObserver)
        tncViewModel.tncTxGain.observe(viewLifecycleOwner, transmitGainObserver)
        tncViewModel.tncMinimumTxTwist.observe(viewLifecycleOwner, transmitTwistMinObserver)
        tncViewModel.tncMaximumTxTwist.observe(viewLifecycleOwner, transmitTwistMaxObserver)
        tncViewModel.tncTxTwist.observe(viewLifecycleOwner, transmitTwistObserver)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            if (it.device == null) {
                findNavController().popBackStack(R.id.ConnectingFragment, false)
            }

            it.setFragmentDescription(R.string.transmit_audio_fragment_label)
            it.setAlpha(0.1f)
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).tncInterface.sendPttOff()
        binding.transmitGainSlider.isEnabled = false
        binding.transmitTwistSlider.isEnabled = false
        binding.transmitTwist.isEnabled = false
        binding.transmitGain.isEnabled = false
    }

    private fun onPttStyleSwitchClicked() {
        if (D) Log.d(TAG, "onPttStyleSwitchClicked")
        val ptt = binding.pttStyleSwitch.checkedChipId
        when (ptt) {
            binding.simplexChip.id -> {
                (activity as MainActivity).tncInterface.setPttStyle(0)
            }
            binding.multiplexChip.id -> {
                (activity as MainActivity).tncInterface.setPttStyle(1)
            }
            else -> {
                throw IllegalStateException("Invalid PTT style state")
            }
        }
    }

    private fun onTransmitGainChanged(value: Float, fromUser: Boolean) {
        if (D) Log.d(TAG, "onTransmitGainChanged() = $value")
        if (fromUser) {
            (activity as MainActivity).tncInterface.setTransmitGain(value.toInt())
        }
        binding.transmitGain.text = String.format(Locale.getDefault(),"%d", value.toInt())
    }

    private fun onTransmitTwistChanged(value: Float, fromUser: Boolean) {
        if (D) Log.d(TAG, "onTransmitTwistChanged() = $value")
        if (fromUser) {
            (activity as MainActivity).tncInterface.setTransmitTwist(value.toInt())
        }
        binding.transmitTwist.text = String.format(Locale.getDefault(),"%d", value.toInt())
    }

    private fun onTransmitClicked() {
        if (D) Log.d(TAG, "onTransmitClicked")
        if (binding.transmitButton.isChecked) {
            binding.transmitGainSlider.isEnabled = true
            binding.transmitTwistSlider.isEnabled = true
            binding.transmitTwist.isEnabled = true
            binding.transmitGain.isEnabled = true
            val tone = binding.testToneSwitch.checkedChipId
            when (tone) {
                binding.markChip.id -> {
                    (activity as MainActivity).tncInterface.sendPttMark()
                }
                binding.spaceChip.id -> {
                    (activity as MainActivity).tncInterface.sendPttSpace()
                }
                binding.bothChip.id -> {
                    (activity as MainActivity).tncInterface.sendPttBoth()
                }
            }
        } else {
            (activity as MainActivity).tncInterface.sendPttOff()
            binding.transmitGainSlider.isEnabled = false
            binding.transmitTwistSlider.isEnabled = false
            binding.transmitTwist.isEnabled = false
            binding.transmitGain.isEnabled = false
        }
    }

    private fun setPttStyle(v: Int) {
        when (v) {
            0 -> {
                binding.simplexChip.isChecked = true
            }
            1 -> {
                binding.multiplexChip.isChecked = true
            }
            else -> {
                Log.e(TAG, "Unknown PTT style received")
            }
        }
    }

    companion object {
        private val TAG = TransmitAudioFragment::class.java.simpleName
        private const val D = true
    }
}