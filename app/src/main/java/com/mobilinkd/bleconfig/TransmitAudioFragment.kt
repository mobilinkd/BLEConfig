package com.mobilinkd.bleconfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mobilinkd.bleconfig.databinding.TransmitAudioFragmentBinding

class TransmitAudioFragment : Fragment(R.layout.transmit_audio_fragment) {
    lateinit var binding: TransmitAudioFragmentBinding
    private var tncProtocolDecoder: TncProtocolDecoder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TransmitAudioFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            it.setFragmentDescription(R.string.transmit_audio_fragment_label)
            tncProtocolDecoder = it.tncProtocolDecoder
        }
    }

    companion object {
        private val TAG = TransmitAudioFragment::class.java.name
        private const val D = true
    }
}