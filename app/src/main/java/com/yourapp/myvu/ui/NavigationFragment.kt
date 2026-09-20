package com.yourapp.myvu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.yourapp.myvu.MainActivity
import com.yourapp.myvu.databinding.FragmentNavigationBinding
import com.yourapp.myvu.service.MyvuService

class NavigationFragment : Fragment() {

    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnStartNavigation.setOnClickListener {
            val destination = binding.etDestination.text.toString()
            if (destination.isNotEmpty()) {
                (activity as? MainActivity)?.getService()?.let { service ->
                    MyvuService.startNavigation(requireContext(), destination)
                    Toast.makeText(context, "Navegação iniciada para: $destination", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Digite um destino", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopNavigation.setOnClickListener {
            (activity as? MainActivity)?.getService()?.let { service ->
                MyvuService.stopNavigation(requireContext())
                Toast.makeText(context, "Navegação parada", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnNavigateHome.setOnClickListener {
            MyvuService.startNavigation(requireContext(), "Casa")
        }

        binding.btnNavigateWork.setOnClickListener {
            MyvuService.startNavigation(requireContext(), "Trabalho")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
