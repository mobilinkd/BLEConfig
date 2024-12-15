package com.mobilinkd.bleconfig

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mobilinkd.bleconfig.databinding.ReceiveAudioFragmentBinding
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.round

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ReceiveAudioFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ReceiveAudioFragment : Fragment() {
    lateinit var binding: ReceiveAudioFragmentBinding
    private val tncViewModel: TncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ReceiveAudioFragmentBinding.inflate(inflater, container, false)
        binding.inputGainSlider.addOnChangeListener { slider, value, fromUser -> onInputGainChanged(value, fromUser) }
        binding.inputTwistSlider.addOnChangeListener { slider, value, fromUser -> onInputTwistChanged(value, fromUser) }
        binding.receiveAudioLevel.valueTo = 8f

        val audioLevelObserver = Observer<Int> { setAudioLevel(it) }
        val inputGainObserver = Observer<Int> { binding.inputGainSlider.value = it.toFloat() }
        val inputTwistObserver = Observer<Int> { binding.inputTwistSlider?.value = it.toFloat() }

        tncViewModel.tncInputLevel.observe(viewLifecycleOwner, audioLevelObserver)
        tncViewModel.tncInputGain.observe(viewLifecycleOwner, inputGainObserver)
        tncViewModel.tncInputTwist.observe(viewLifecycleOwner, inputTwistObserver)

        tncViewModel.tncBatteryLevel.value?.let { setAudioLevel(it) }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            if (it.device == null) {
                findNavController().popBackStack(R.id.ConnectingFragment, false)
            }

            it.setFragmentDescription(R.string.receive_audio_fragment_label)
            it.setAlpha(0.1f)
            it.tncInterface.startReceiveAudio()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (D) Log.d(TAG, "onConfigurationChanged()")
        parentFragmentManager.beginTransaction().detach(this).commitAllowingStateLoss()
        super.onConfigurationChanged(newConfig)
        parentFragmentManager.beginTransaction().attach(this).commitAllowingStateLoss()
    }

    override fun onPause() {
        super.onPause()
        (activity as MainActivity).tncInterface.stopReceiveAudio()
    }

    private fun setAudioLevel(level: Int) {
        var v = log2((level shr 8).toFloat() + 1)
        if (D) Log.d(TAG, "setAudioLeve(), level = $level -> $v")
        if (v < 4.0) binding.receiveAudioLevel.trackActiveTintList = requireContext().getColorStateList(R.color.level_bad)
        else if (v < 7.0) binding.receiveAudioLevel.trackActiveTintList = requireContext().getColorStateList(R.color.level_ok)
        else binding.receiveAudioLevel.trackActiveTintList = requireContext().getColorStateList(R.color.level_good)

        if (v > 8f) v = 8f
        binding.receiveAudioLevel.value = v
    }

    private fun onInputGainChanged(value: Float, fromUser: Boolean) {
        if (D) Log.d(TAG, "onInputGainChanged() = $value")
        if (fromUser) {
            (activity as MainActivity).tncInterface.setInputGain(value.toInt())
            binding.receiveAudioLevel.trackActiveTintList = requireContext().getColorStateList(R.color.gray_600)
        }
    }

    private fun onInputTwistChanged(value: Float, fromUser: Boolean) {
        if (D) Log.d(TAG, "onInputTwistChanged() = $value")
        if (fromUser) {
            (activity as MainActivity).tncInterface.setInputTwist(value.toInt())
        }
    }

    companion object {
        private val TAG = ReceiveAudioFragment::class.java.simpleName
        private const val D = true
    }
}