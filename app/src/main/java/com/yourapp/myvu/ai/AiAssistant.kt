package com.yourapp.myvu.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.*
import java.util.*

interface SttProvider {
    fun startListening()
    fun stopListening()
    fun setOnResultListener(listener: (String) -> Unit)
    fun setOnPartialResultListener(listener: (String) -> Unit)
    fun destroy()
}

interface TtsProvider {
    fun speak(text: String, utteranceId: String)
    fun stop()
    fun setOnDoneListener(listener: (String) -> Unit)
    fun destroy()
}

class AndroidSttProvider(
    private val context: Context
) : SttProvider {

    private var speechRecognizer: SpeechRecognizer? = null
    private var onResultListener: ((String) -> Unit)? = null
    private var onPartialResultListener: ((String) -> Unit)? = null
    private var isListening = false

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("AndroidStt", "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d("AndroidStt", "Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            Log.d("AndroidStt", "Speech ended")
        }

        override fun onError(error: Int) {
            Log.e("AndroidStt", "Error: $error")
            isListening = false
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotEmpty()) {
                onResultListener?.invoke(text)
            }
            isListening = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotEmpty()) {
                onPartialResultListener?.invoke(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun startListening() {
        if (isListening) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)
        isListening = true
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    override fun setOnResultListener(listener: (String) -> Unit) {
        onResultListener = listener
    }

    override fun setOnPartialResultListener(listener: (String) -> Unit) {
        onPartialResultListener = listener
    }

    override fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

class AndroidTtsProvider(
    private val context: Context
) : TtsProvider {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var onDoneListener: ((String) -> Unit)? = null
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.language = Locale("pt", "BR")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        utteranceId?.let { onDoneListener?.invoke(it) }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {}
                })
                pendingText?.let { speak(it, "pending") }
            }
        }
    }

    override fun speak(text: String, utteranceId: String) {
        if (isReady) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            tts?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
        } else {
            pendingText = text
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun setOnDoneListener(listener: (String) -> Unit) {
        onDoneListener = listener
    }

    override fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

class MyvuAiAssistant(
    private val context: Context,
    private val llmProvider: LlmProvider,
    private val onGlassesText: (String) -> Unit,
    private val onGlassesSpeak: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val sttProvider = AndroidSttProvider(context)
    private val ttsProvider = AndroidTtsProvider(context)
    private val conversationHistory = mutableListOf<ChatMessage>()
    private val systemPrompt = """
        Você é um assistente de voz integrado a óculos de realidade aumentada Meizu MYVU.
        Responda de forma curta e direta, ideal para ser exibida em uma tela pequena.
        Máximo de 2-3 frases por resposta.
        Responda em português brasileiro.
    """.trimIndent()

    private var isProcessing = false
    private var isListening = false
    private var continuousMode = false

    fun start() {
        sttProvider.setOnResultListener { text ->
            scope.launch {
                processUserInput(text)
            }
        }

        sttProvider.setOnPartialResultListener { partial ->
            onGlassesText.invoke("Ouvindo: $partial")
        }

        ttsProvider.setOnDoneListener { _ ->
            if (continuousMode && !isListening) {
                startListening()
            }
        }
    }

    fun startListening() {
        if (!isProcessing) {
            isListening = true
            sttProvider.startListening()
            onGlassesText.invoke("Ouvindo...")
        }
    }

    fun stopListening() {
        sttProvider.stopListening()
        isListening = false
    }

    fun toggleContinuousMode() {
        continuousMode = !continuousMode
        val status = if (continuousMode) "ativado" else "desativado"
        onGlassesText.invoke("Modo contínuo $status")
    }

    fun sendTextMessage(text: String) {
        scope.launch {
            processUserInput(text)
        }
    }

    private suspend fun processUserInput(input: String) {
        if (isProcessing) return
        isProcessing = true

        try {
            conversationHistory.add(ChatMessage("user", input))

            if (conversationHistory.size > 20) {
                val systemMsg = conversationHistory.removeAt(0)
                conversationHistory.removeAt(0)
                conversationHistory.add(0, systemMsg)
            }

            val messages = mutableListOf(
                ChatMessage("system", systemPrompt)
            ).plus(conversationHistory.takeLast(10))

            onGlassesText.invoke("Processando...")

            val response = llmProvider.chat(messages)

            conversationHistory.add(ChatMessage("assistant", response))

            onGlassesText.invoke(response)

            ttsProvider.speak(response, "assistant_response")

        } catch (e: Exception) {
            Log.e("AiAssistant", "Error: ${e.message}", e)
            onGlassesText.invoke("Erro: ${e.message}")
        } finally {
            isProcessing = false
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
        onGlassesText.invoke("Histórico limpo")
    }

    fun destroy() {
        scope.cancel()
        sttProvider.destroy()
        ttsProvider.destroy()
    }
}
