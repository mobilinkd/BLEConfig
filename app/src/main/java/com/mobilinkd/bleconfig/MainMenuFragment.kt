package com.mobilinkd.bleconfig

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mobilinkd.bleconfig.databinding.MainMenuFragmentBinding

class MainMenuFragment : Fragment() {

    private var _binding: MainMenuFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = MainMenuFragmentBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.receiveAudioButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_ReceiveAudioFragment)
        }

        binding.transmitAudioButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_transmitAudioFragment)
        }

        binding.powerSettingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_powerSettingsFragment)
        }

        binding.kissParametersButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_kissParametersFragment)
        }

        binding.modemConfigurationButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_modemConfigurationFragment)
        }

        binding.tncInformationButton.setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_tncInformationFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        if (D) Log.d(TAG, "onResume()")
        (activity as MainActivity).setFragmentDescription(R.string.main_menu_fragment_label)
    }

    override fun onPause() {
        super.onPause()
        if (D) Log.d(TAG, "onPause()")
    }

    override fun onStop() {
        super.onStop()
        if (D) Log.d(TAG, "onStop()")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val TAG = MainMenuFragment::class.java.name
        private const val D = true
    }
}