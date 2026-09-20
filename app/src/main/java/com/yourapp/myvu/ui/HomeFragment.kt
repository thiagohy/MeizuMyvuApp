package com.yourapp.myvu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yourapp.myvu.MainActivity
import com.yourapp.myvu.databinding.FragmentHomeBinding
import com.yourapp.myvu.service.MyvuService
import me.panny777.myvu.core.ConnectionState

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeConnectionState()
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            (activity as? MainActivity)?.getService()?.connect()
        }

        binding.btnDisconnect.setOnClickListener {
            (activity as? MainActivity)?.getService()?.disconnect()
        }

        binding.btnSyncTime.setOnClickListener {
            (activity as? MainActivity)?.getService()?.syncTime()
        }

        binding.btnTeleprompter.setOnClickListener {
            val text = binding.etTeleprompterText.text.toString()
            if (text.isNotEmpty()) {
                (activity as? MainActivity)?.getService()?.sendTeleprompterToGlasses(text)
            }
        }

        binding.btnBrightnessUp.setOnClickListener {
            (activity as? MainActivity)?.getService()?.let { service ->
                // Get current brightness and increment
                service.setBrightness(5) // Placeholder
            }
        }

        binding.btnBrightnessDown.setOnClickListener {
            (activity as? MainActivity)?.getService()?.let { service ->
                service.setBrightness(3) // Placeholder
            }
        }
    }

    private fun observeConnectionState() {
        (activity as? MainActivity)?.getService()?.let { service ->
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                service.connectionState.collect { state ->
                    updateConnectionUI(state)
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                service.deviceInfo.collect { info ->
                    info?.let {
                        binding.tvDeviceInfo.text = """
                            Bateria: ${it.battery}%
                            FW: ${it.firmwareVersion}
                        """.trimIndent()
                    }
                }
            }
        }
    }

    private fun updateConnectionUI(state: ConnectionState) {
        when (state) {
            ConnectionState.READY -> {
                binding.tvConnectionStatus.text = "Conectado"
                binding.tvConnectionStatus.setTextColor(0xFF4CAF50.toInt())
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
            }
            ConnectionState.CONNECTING -> {
                binding.tvConnectionStatus.text = "Conectando..."
                binding.tvConnectionStatus.setTextColor(0xFFFFC107.toInt())
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = false
            }
            ConnectionState.DISCONNECTED -> {
                binding.tvConnectionStatus.text = "Desconectado"
                binding.tvConnectionStatus.setTextColor(0xFFF44336.toInt())
                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = false
            }
            else -> {
                binding.tvConnectionStatus.text = state.toString()
                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
