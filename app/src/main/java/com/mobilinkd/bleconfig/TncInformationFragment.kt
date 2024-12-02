package com.mobilinkd.bleconfig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mobilinkd.bleconfig.databinding.TncInformationFragmentBinding

class TncInformationFragment : Fragment(R.layout.tnc_information_fragment) {

    lateinit var binding: TncInformationFragmentBinding
    private var tncProtocolDecoder: TncProtocolDecoder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TncInformationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.let {
            it.setFragmentDescription(R.string.info_fragment_label)
            tncProtocolDecoder = it.tncProtocolDecoder
        }
    }

    companion object {
        private val TAG = TncInformationFragment::class.java.name
        private const val D = true
    }
}