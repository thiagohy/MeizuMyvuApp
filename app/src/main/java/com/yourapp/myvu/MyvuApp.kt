package com.yourapp.myvu

import android.app.Application
import android.util.Log
import com.yourapp.myvu.utils.ConfigManager
import me.panny777.myvu.core.SdkLog

class MyvuApp : Application() {

    companion object {
        private const val TAG = "MyvuApp"
        lateinit var instance: MyvuApp
            private set
    }

    lateinit var configManager: ConfigManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        configManager = ConfigManager(this)
        configureSdk()
        Log.d(TAG, "Application created")
    }

    private fun configureSdk() {
        SdkLog.setLogger { level, message, error ->
            when (level) {
                SdkLog.Level.DEBUG -> Log.d("MyvuSDK", message)
                SdkLog.Level.INFO -> Log.i("MyvuSDK", message)
                SdkLog.Level.WARN -> Log.w("MyvuSDK", message)
                SdkLog.Level.ERROR -> Log.e("MyvuSDK", message, error)
            }
        }
    }
}
