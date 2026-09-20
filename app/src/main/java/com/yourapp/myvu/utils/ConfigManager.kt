package com.yourapp.myvu.utils

import android.content.Context
import android.content.SharedPreferences

data class AppConfig(
    var llmProvider: String = "claude",
    var llmApiKey: String = "",
    var llmModel: String = "",
    var googleMapsKey: String = "",
    var homeAssistantUrl: String = "",
    var homeAssistantToken: String = "",
    var ttsLanguage: String = "pt-BR",
    var translationSource: String = "en",
    var translationTarget: String = "pt",
    var stepGoal: Int = 10000,
    var heartRateHigh: Float = 100f,
    var heartRateLow: Float = 50f,
    var teleprompterSpeed: Long = 5000L,
    var autoConnect: Boolean = true,
    var mirrorNotifications: Boolean = true,
    var mirrorMusic: Boolean = false,
    var healthMonitoring: Boolean = false
)

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("myvu_config", Context.MODE_PRIVATE)

    fun saveConfig(config: AppConfig) {
        prefs.edit().apply {
            putString("llm_provider", config.llmProvider)
            putString("llm_api_key", config.llmApiKey)
            putString("llm_model", config.llmModel)
            putString("google_maps_key", config.googleMapsKey)
            putString("home_assistant_url", config.homeAssistantUrl)
            putString("home_assistant_token", config.homeAssistantToken)
            putString("tts_language", config.ttsLanguage)
            putString("translation_source", config.translationSource)
            putString("translation_target", config.translationTarget)
            putInt("step_goal", config.stepGoal)
            putFloat("heart_rate_high", config.heartRateHigh)
            putFloat("heart_rate_low", config.heartRateLow)
            putLong("teleprompter_speed", config.teleprompterSpeed)
            putBoolean("auto_connect", config.autoConnect)
            putBoolean("mirror_notifications", config.mirrorNotifications)
            putBoolean("mirror_music", config.mirrorMusic)
            putBoolean("health_monitoring", config.healthMonitoring)
            apply()
        }
    }

    fun loadConfig(): AppConfig {
        return AppConfig(
            llmProvider = prefs.getString("llm_provider", "claude") ?: "claude",
            llmApiKey = prefs.getString("llm_api_key", "") ?: "",
            llmModel = prefs.getString("llm_model", "") ?: "",
            googleMapsKey = prefs.getString("google_maps_key", "") ?: "",
            homeAssistantUrl = prefs.getString("home_assistant_url", "") ?: "",
            homeAssistantToken = prefs.getString("home_assistant_token", "") ?: "",
            ttsLanguage = prefs.getString("tts_language", "pt-BR") ?: "pt-BR",
            translationSource = prefs.getString("translation_source", "en") ?: "en",
            translationTarget = prefs.getString("translation_target", "pt") ?: "pt",
            stepGoal = prefs.getInt("step_goal", 10000),
            heartRateHigh = prefs.getFloat("heart_rate_high", 100f),
            heartRateLow = prefs.getFloat("heart_rate_low", 50f),
            teleprompterSpeed = prefs.getLong("teleprompter_speed", 5000L),
            autoConnect = prefs.getBoolean("auto_connect", true),
            mirrorNotifications = prefs.getBoolean("mirror_notifications", true),
            mirrorMusic = prefs.getBoolean("mirror_music", false),
            healthMonitoring = prefs.getBoolean("health_monitoring", false)
        )
    }

    fun clearConfig() {
        prefs.edit().clear().apply()
    }
}
