package com.yourapp.myvu.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

interface LlmProvider {
    suspend fun chat(messages: List<ChatMessage>): String
    val name: String
}

data class ChatMessage(
    val role: String,
    val content: String
)

class ClaudeProvider(
    private val apiKey: String,
    private val model: String = "claude-3-haiku-20240307",
    private val baseUrl: String = "https://api.anthropic.com"
) : LlmProvider {

    override val name = "Claude"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            messages.forEach { msg ->
                val msgObj = JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                }
                messagesArray.put(msgObj)
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("max_tokens", 1024)
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url("$baseUrl/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IOException("Empty response")

            if (!response.isSuccessful) {
                throw IOException("API error ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)
            val content = json.getJSONArray("content")
            content.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            Log.e("ClaudeProvider", "Error: ${e.message}", e)
            "Erro ao conectar com Claude: ${e.message}"
        }
    }
}

class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    private val baseUrl: String = "https://api.openai.com"
) : LlmProvider {

    override val name = "OpenAI"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            messages.forEach { msg ->
                val msgObj = JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                }
                messagesArray.put(msgObj)
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("max_tokens", 1024)
            }

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IOException("Empty response")

            if (!response.isSuccessful) {
                throw IOException("API error ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            Log.e("OpenAiProvider", "Error: ${e.message}", e)
            "Erro ao conectar com OpenAI: ${e.message}"
        }
    }
}

class GroqProvider(
    private val apiKey: String,
    private val model: String = "llama3-8b-8192"
) : LlmProvider {

    override val name = "Groq"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            messages.forEach { msg ->
                val msgObj = JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                }
                messagesArray.put(msgObj)
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("max_tokens", 1024)
            }

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IOException("Empty response")

            if (!response.isSuccessful) {
                throw IOException("API error ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            Log.e("GroqProvider", "Error: ${e.message}", e)
            "Erro ao conectar com Groq: ${e.message}"
        }
    }
}

class LocalOpenAiProvider(
    private val apiKey: String = "lm-studio",
    private val model: String = "local-model",
    private val baseUrl: String = "http://10.0.2.2:1234"
) : LlmProvider {

    override val name = "Local"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            messages.forEach { msg ->
                val msgObj = JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                }
                messagesArray.put(msgObj)
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("max_tokens", 1024)
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url("$baseUrl/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IOException("Empty response")

            if (!response.isSuccessful) {
                throw IOException("API error ${response.code}: $responseBody")
            }

            val json = JSONObject(responseBody)
            json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            Log.e("LocalProvider", "Error: ${e.message}", e)
            "Erro ao conectar com LLM local: ${e.message}"
        }
    }
}
