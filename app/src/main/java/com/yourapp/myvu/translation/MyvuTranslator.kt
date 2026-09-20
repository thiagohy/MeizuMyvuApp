package com.yourapp.myvu.translation

import android.content.Context
import com.yourapp.myvu.ai.LlmProvider
import com.yourapp.myvu.ai.ChatMessage
import kotlinx.coroutines.*

interface SttTranslationProvider {
    fun startListening(sourceLang: String, targetLang: String)
    fun stopListening()
    fun setOnTranslationListener(listener: (String) -> Unit)
    fun destroy()
}

class MyvuTranslator(
    private val context: Context,
    private val llmProvider: LlmProvider,
    private val onTranslation: (String) -> Unit,
    private val onOriginal: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isTranslating = false
    private var sourceLang = "en"
    private var targetLang = "pt"

    private val languageNames = mapOf(
        "pt" to "português",
        "en" to "inglês",
        "es" to "espanhol",
        "fr" to "francês",
        "de" to "alemão",
        "it" to "italiano",
        "ja" to "japonês",
        "ko" to "coreano",
        "zh" to "chinês",
        "ru" to "russo",
        "ar" to "árabe"
    )

    fun setLanguages(source: String, target: String) {
        sourceLang = source
        targetLang = target
        onTranslation("Idiomas: ${languageNames[source]} → ${languageNames[target]}")
    }

    suspend fun translateText(text: String): String = withContext(Dispatchers.IO) {
        try {
            val sourceName = languageNames[sourceLang] ?: sourceLang
            val targetName = languageNames[targetLang] ?: targetLang

            val prompt = """
                Traduza o texto abaixo de $sourceName para $targetName.
                Responda APENAS com a tradução, sem explicações adicionais.
                
                Texto: $text
            """.trimIndent()

            val messages = listOf(
                ChatMessage("system", "Você é um tradutor profissional. Responda apenas com a tradução."),
                ChatMessage("user", prompt)
            )

            llmProvider.chat(messages)
        } catch (e: Exception) {
            "Erro na tradução: ${e.message}"
        }
    }

    suspend fun translateConversation(text: String, context: String = ""): String {
        return try {
            val sourceName = languageNames[sourceLang] ?: sourceLang
            val targetName = languageNames[targetLang] ?: targetLang

            val prompt = buildString {
                append("Traduza de $sourceName para $targetName.\n")
                if (context.isNotEmpty()) {
                    append("Contexto: $context\n")
                }
                append("Texto: $text")
            }

            val messages = listOf(
                ChatMessage("system", "Você é um tradutor profissional para conversação. Seja natural e fluido."),
                ChatMessage("user", prompt)
            )

            llmProvider.chat(messages)
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    fun getSupportedLanguages(): List<Pair<String, String>> {
        return languageNames.map { (code, name) -> code to name }
    }

    fun destroy() {
        scope.cancel()
    }
}

class ConversationMode(
    private val context: Context,
    private val translator: MyvuTranslator,
    private val onDisplay: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isActive = false
    private var currentSpeaker = "A"

    fun start(sourceLang: String, targetLang: String) {
        isActive = true
        currentSpeaker = "A"
        translator.setLanguages(sourceLang, targetLang)
        onDisplay("Modo conversação iniciado")
        onDisplay("Falante A (${languageNames[sourceLang]}) → Falante B (${languageNames[targetLang]})")
    }

    fun processSpeech(text: String) {
        if (!isActive) return

        scope.launch {
            val translated = translator.translateConversation(text)
            onDisplay("Falante $currentSpeaker: $text")
            onDisplay("Tradução: $translated")

            currentSpeaker = if (currentSpeaker == "A") "B" else "A"
        }
    }

    fun stop() {
        isActive = false
        onDisplay("Modo conversação encerrado")
    }

    private val languageNames = mapOf(
        "pt" to "Português",
        "en" to "Inglês",
        "es" to "Espanhol",
        "fr" to "Francês",
        "de" to "Alemão",
        "it" to "Italiano",
        "ja" to "Japonês",
        "ko" to "Coreano",
        "zh" to "Chinês",
        "ru" to "Russo",
        "ar" to "Árabe"
    )
}

class DocumentTranslator(
    private val translator: MyvuTranslator,
    private val onProgress: (Int) -> Unit,
    private val onComplete: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun translateDocument(text: String, sourceLang: String, targetLang: String) {
        scope.launch {
            translator.setLanguages(sourceLang, targetLang)

            val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
            val translated = mutableListOf<String>()

            paragraphs.forEachIndexed { index, paragraph ->
                val result = translator.translateText(paragraph)
                translated.add(result)

                val progress = ((index + 1) * 100) / paragraphs.size
                onProgress(progress)
            }

            onComplete(translated.joinToString("\n\n"))
        }
    }
}
