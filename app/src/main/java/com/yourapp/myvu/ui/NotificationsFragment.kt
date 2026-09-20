package com.yourapp.myvu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.yourapp.myvu.MainActivity
import com.yourapp.myvu.databinding.FragmentNotificationsBinding
import com.yourapp.myvu.service.MyvuService

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSendNotification.setOnClickListener {
            val title = binding.etNotificationTitle.text.toString()
            val body = binding.etNotificationBody.text.toString()

            if (title.isNotEmpty() && body.isNotEmpty()) {
                MyvuService.sendNotification(requireContext(), title, body)
                Toast.makeText(context, "Notificação enviada!", Toast.LENGTH_SHORT).show()
                binding.etNotificationTitle.text?.clear()
                binding.etNotificationBody.text?.clear()
            } else {
                Toast.makeText(context, "Preencha título e mensagem", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnTestNotification.setOnClickListener {
            MyvuService.sendNotification(
                requireContext(),
                "Teste",
                "Esta é uma notificação de teste dos óculos MYVU"
            )
            Toast.makeText(context, "Notificação de teste enviada", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearHistory.setOnClickListener {
            binding.tvNotificationHistory.text = "Nenhuma notificação enviada"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
