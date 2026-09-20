package com.yourapp.myvu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.yourapp.myvu.MainActivity
import com.yourapp.myvu.databinding.FragmentAssistantBinding
import com.yourapp.myvu.service.MyvuService

class AssistantFragment : Fragment() {

    private var _binding: FragmentAssistantBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSendQuestion.setOnClickListener {
            val question = binding.etQuestion.text.toString()
            if (question.isNotEmpty()) {
                Toast.makeText(context, "Enviando pergunta: $question", Toast.LENGTH_SHORT).show()
                binding.etQuestion.text?.clear()
            }
        }

        binding.btnToggleVoice.setOnClickListener {
            Toast.makeText(context, "Assistente de voz ativado/desativado", Toast.LENGTH_SHORT).show()
        }

        binding.btnTestTts.setOnClickListener {
            Toast.makeText(context, "Testando TTS nos óculos...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
