package com.yourapp.myvu.smart_home

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SmartDevice(
    val id: String,
    val name: String,
    val type: String,
    val state: Boolean,
    val room: String = ""
)

interface SmartHomeProvider {
    suspend fun getDevices(): List<SmartDevice>
    suspend fun controlDevice(deviceId: String, action: String, params: Map<String, Any> = emptyMap()): Boolean
    suspend fun getStatus(): String
}

class HomeAssistantProvider(
    private val baseUrl: String,
    private val token: String
) : SmartHomeProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun getDevices(): List<SmartDevice> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/states")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            if (!response.isSuccessful) return@withContext emptyList()

            val json = org.json.JSONArray(body)
            val devices = mutableListOf<SmartDevice>()

            for (i in 0 until json.length()) {
                val entity = json.getJSONObject(i)
                val entityId = entity.getString("entity_id")
                val state = entity.getString("state")
                val attributes = entity.optJSONObject("attributes") ?: JSONObject()

                val friendlyName = attributes.optString("friendly_name", entityId)
                val deviceClass = attributes.optString("device_class", "")

                val type = when {
                    entityId.startsWith("light.") -> "light"
                    entityId.startsWith("switch.") -> "switch"
                    entityId.startsWith("climate.") -> "climate"
                    entityId.startsWith("media_player.") -> "media_player"
                    entityId.startsWith("sensor.") -> "sensor"
                    else -> "unknown"
                }

                if (type != "unknown") {
                    devices.add(
                        SmartDevice(
                            id = entityId,
                            name = friendlyName,
                            type = type,
                            state = state == "on" || state == "playing",
                            room = deviceClass
                        )
                    )
                }
            }

            devices
        } catch (e: Exception) {
            Log.e("HomeAssistant", "Error: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun controlDevice(
        deviceId: String,
        action: String,
        params: Map<String, Any>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = when {
                deviceId.startsWith("light.") -> "light"
                deviceId.startsWith("switch.") -> "switch"
                deviceId.startsWith("climate.") -> "climate"
                deviceId.startsWith("media_player.") -> "media_player"
                else -> return@withContext false
            }

            val body = JSONObject().apply {
                put("entity_id", deviceId)
                params.forEach { (key, value) -> put(key, value) }
            }

            val request = Request.Builder()
                .url("$baseUrl/api/services/$service/$action")
                .addHeader("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("HomeAssistant", "Error: ${e.message}", e)
            false
        }
    }

    override suspend fun getStatus(): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext "Offline"

            val json = JSONObject(body)
            json.optString("message", "Unknown")
        } catch (e: Exception) {
            "Offline"
        }
    }
}

class TuyaProvider(
    private val accessId: String,
    private val accessSecret: String,
    private val endpoint: String = "https://openapi.tuyaeu.com"
) : SmartHomeProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun getDevices(): List<SmartDevice> = withContext(Dispatchers.IO) {
        // Tuya API implementation
        emptyList()
    }

    override suspend fun controlDevice(deviceId: String, action: String, params: Map<String, Any>): Boolean {
        return false
    }

    override suspend fun getStatus(): String = "Tuya (não implementado)"
}

class MyvuSmartHome(
    private val provider: SmartHomeProvider,
    private val onDeviceUpdate: (String) -> Unit,
    private val onAlert: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var monitoring = false
    private var devices = listOf<SmartDevice>()

    fun startMonitoring() {
        monitoring = true
        scope.launch {
            refreshDevices()
            startPeriodicCheck()
        }
    }

    fun stopMonitoring() {
        monitoring = false
    }

    private suspend fun refreshDevices() {
        devices = provider.getDevices()
        onDeviceUpdate("📱 ${devices.size} dispositivos encontrados")
    }

    private fun startPeriodicCheck() {
        scope.launch {
            while (monitoring) {
                delay(30000)
                if (monitoring) {
                    checkDevices()
                }
            }
        }
    }

    private suspend fun checkDevices() {
        val newDevices = provider.getDevices()
        val changed = newDevices.filter { new ->
            val old = devices.find { it.id == new.id }
            old == null || old.state != new.state
        }

        changed.forEach { device ->
            val state = if (device.state) "ligado" else "desligado"
            onDeviceUpdate("${device.name}: $state")
        }

        devices = newDevices
    }

    fun toggleLight(deviceId: String) {
        scope.launch {
            val device = devices.find { it.id == deviceId }
            if (device != null) {
                val action = if (device.state) "turn_off" else "turn_on"
                provider.controlDevice(deviceId, action)
                refreshDevices()
            }
        }
    }

    fun setTemperature(deviceId: String, temperature: Double) {
        scope.launch {
            provider.controlDevice(deviceId, "set_temperature", mapOf("temperature" to temperature))
            onDeviceUpdate("Temperatura ajustada para ${temperature}°C")
        }
    }

    fun getDevicesSummary(): String {
        val lights = devices.filter { it.type == "light" }
        val switches = devices.filter { it.type == "switch" }
        val climate = devices.filter { it.type == "climate" }

        return buildString {
            appendLine("🏠 Resumo da Casa:")
            appendLine("💡 Luzes: ${lights.count { it.state }}/${lights.size} acesas")
            appendLine("🔌 Interruptores: ${switches.count { it.state }}/${switches.size} ligados")
            if (climate.isNotEmpty()) {
                appendLine("🌡️ Clima: ${climate.size} dispositivos")
            }
        }
    }

    fun destroy() {
        scope.cancel()
    }
}
