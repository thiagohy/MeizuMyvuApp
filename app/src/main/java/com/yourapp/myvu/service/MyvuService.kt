package com.yourapp.myvu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yourapp.myvu.MainActivity
import com.yourapp.myvu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.panny777.myvu.core.MyvuClient
import me.panny777.myvu.core.ConnectionState
import me.panny777.myvu.core.DeviceInfo
import me.panny777.myvu.core.GlassesEvent
import me.panny777.myvu.nav.NavSession
import me.panny777.myvu.ai.AiSession

class MyvuService : Service() {

    companion object {
        private const val TAG = "MyvuService"
        private const val NOTIFICATION_CHANNEL_ID = "myvu_service_channel"
        private const val NOTIFICATION_ID = 1001

        private const val ACTION_CONNECT = "com.yourapp.myvu.ACTION_CONNECT"
        private const val ACTION_DISCONNECT = "com.yourapp.myvu.ACTION_DISCONNECT"
        private const val ACTION_SEND_NOTIFICATION = "com.yourapp.myvu.ACTION_SEND_NOTIFICATION"
        private const val ACTION_START_NAVIGATION = "com.yourapp.myvu.ACTION_START_NAVIGATION"
        private const val ACTION_STOP_NAVIGATION = "com.yourapp.myvu.ACTION_STOP_NAVIGATION"
        private const val ACTION_SEND_TELEPROMPTER = "com.yourapp.myvu.ACTION_SEND_TELEPROMPTER"

        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_BODY = "extra_body"
        private const val EXTRA_DESTINATION = "extra_destination"
        private const val EXTRA_TEXT = "extra_text"

        fun startConnect(context: Context) {
            val intent = Intent(context, MyvuService::class.java).apply {
                action = ACTION_CONNECT
            }
            context.startForegroundService(intent)
        }

        fun disconnect(context: Context) {
            val intent = Intent(context, MyvuService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }

        fun sendNotification(context: Context, title: String, body: String) {
            val intent = Intent(context, MyvuService::class.java).apply {
                action = ACTION_SEND_NOTIFICATION
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
            }
            context.startService(intent)
        }

        fun startNavigation(context: Context, destination: String) {
            val intent = Intent(context, MyvuService::class.java).apply {
                action = ACTION_START_NAVIGATION
                putExtra(EXTRA_DESTINATION, destination)
            }
            context.startService(intent)
        }

        fun stopNavigation(context: Context) {
            val intent = Intent(context, MyvuService::class.java).apply {
                action = ACTION_STOP_NAVIGATION
            }
            context.startService(intent)
        }

        fun sendTeleprompter(context: Context, text: String) {
            val intent = Intent(context, MyvuService::class.java).apply {
                action = ACTION_SEND_TELEPROMPTER
                putExtra(EXTRA_TEXT, text)
            }
            context.startService(intent)
        }
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var client: MyvuClient? = null
    private var navSession: NavSession? = null
    private var aiSession: AiSession? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): MyvuService = this@MyvuService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Meizu MYVU Conectando..."))
        initClient()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> connect()
            ACTION_DISCONNECT -> disconnect()
            ACTION_SEND_NOTIFICATION -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
                val body = intent.getStringExtra(EXTRA_BODY) ?: ""
                sendNotificationToGlasses(title, body)
            }
            ACTION_START_NAVIGATION -> {
                val destination = intent.getStringExtra(EXTRA_DESTINATION) ?: ""
                startNavigationTo(destination)
            }
            ACTION_STOP_NAVIGATION -> stopNavigationSession()
            ACTION_SEND_TELEPROMPTER -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
                sendTeleprompterToGlasses(text)
            }
        }
        return START_STICKY
    }

    private fun initClient() {
        client = MyvuClient(this)

        serviceScope.launch {
            client?.state?.collect { state ->
                _connectionState.value = state
                updateNotification(state)
                Log.d(TAG, "Connection state: $state")
            }
        }

        serviceScope.launch {
            client?.deviceInfo?.collect { info ->
                _deviceInfo.value = info
                Log.d(TAG, "Device info: $battery")
            }
        }

        serviceScope.launch {
            client?.events?.collect { event ->
                handleEvent(event)
            }
        }
    }

    fun connect() {
        serviceScope.launch {
            client?.connectAutoSearch()
        }
    }

    fun disconnect() {
        navSession?.stop()
        aiSession?.detach()
        client?.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun sendNotificationToGlasses(title: String, body: String) {
        client?.showNotification(title, body)
    }

    fun sendTeleprompterToGlasses(text: String, title: String = "Teleprompter") {
        client?.openTeleprompter(text, title)
    }

    fun startNavigationTo(destination: String) {
        // NavSession needs to be initialized with a location source
        // This is a simplified version - you'll need to provide a proper LocationSource
        Log.d(TAG, "Starting navigation to: $destination")
    }

    fun stopNavigationSession() {
        navSession?.stop()
    }

    fun setBrightness(level: Int) {
        client?.setBrightness(level)
    }

    fun setVolume(level: Int) {
        client?.setVolume(level)
    }

    fun syncTime() {
        client?.syncTime()
    }

    private fun handleEvent(event: GlassesEvent) {
        when (event) {
            is GlassesEvent.WeatherRequested -> {
                Log.d(TAG, "Weather requested by glasses")
            }
            is GlassesEvent.TrackpadEvent -> {
                Log.d(TAG, "Trackpad event: $event")
            }
            else -> {
                Log.d(TAG, "Event: $event")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Meizu MYVU Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a conexão com os óculos MYVU"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Meizu MYVU")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_myvu)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(state: ConnectionState) {
        val text = when (state) {
            ConnectionState.DISCONNECTED -> "Desconectado"
            ConnectionState.CONNECTING -> "Conectando..."
            ConnectionState.PAIRING -> "Pareando..."
            ConnectionState.CONNECTED -> "Conectado"
            ConnectionState.READY -> "Pronto"
            ConnectionState.ERROR -> "Erro na conexão"
            else -> "Status desconhecido"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        client?.close()
        serviceScope.cancel()
    }
}
