package com.mobilinkd.bleconfig

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mobilinkd.bleconfig.databinding.ReceiveAudioFragmentBinding

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

    private var audioInputGainMinimum: Int? = null
    private var audioInputGainMaximum: Int? = null
    private var audioInputTwistMinimum: Int? = null
    private var audioInputTwistMaximum: Int? = null

    private var audioInputGain: Int? = null
    private var audioInputTwist: Int? = null

    private var lastInputGainUpdateTime: Long = 0
    private var lastInputTwistUpdateTime: Long = 0
    private var tncProtocolDecoder: TncProtocolDecoder? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ReceiveAudioFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            it.setFragmentDescription(R.string.receive_audio_fragment_label)
            tncProtocolDecoder = it.tncProtocolDecoder
        }
    }

    companion object {
        private val TAG = ReceiveAudioFragment::class.java.name
        private const val D = true
    }
}