package com.yourapp.myvu.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log

data class MusicInfo(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0,
    val position: Long = 0
)

interface MusicController {
    fun play()
    fun pause()
    fun next()
    fun previous()
    fun getCurrentTrack(): MusicInfo
    fun setOnTrackChangeListener(listener: (MusicInfo) -> Unit)
    fun destroy()
}

class AndroidMusicController(
    private val context: Context
) : MusicController {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var currentTrack = MusicInfo()
    private var onTrackChangeListener: ((MusicInfo) -> Unit)? = null
    private var mediaButtonReceiver: BroadcastReceiver? = null

    private val mediaReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.android.music.playstatechanged" -> {
                    val isPlaying = intent.getIntExtra("playstate", 0) == 1
                    currentTrack = currentTrack.copy(isPlaying = isPlaying)
                    onTrackChangeListener?.invoke(currentTrack)
                }
                "com.android.music.metachanged" -> {
                    val title = intent.getStringExtra("track") ?: ""
                    val artist = intent.getStringExtra("artist") ?: ""
                    val album = intent.getStringExtra("album") ?: ""
                    currentTrack = MusicInfo(
                        title = title,
                        artist = artist,
                        album = album,
                        isPlaying = true
                    )
                    onTrackChangeListener?.invoke(currentTrack)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction("com.android.music.playstatechanged")
            addAction("com.android.music.metachanged")
        }
        context.registerReceiver(mediaReceiver, filter)
    }

    override fun play() {
        sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    override fun pause() {
        sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    override fun next() {
        sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    override fun previous() {
        sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    override fun getCurrentTrack(): MusicInfo = currentTrack

    override fun setOnTrackChangeListener(listener: (MusicInfo) -> Unit) {
        onTrackChangeListener = listener
    }

    private fun sendMediaButton(keyCode: Int) {
        val event = android.view.KeyEvent(
            android.view.KeyEvent.ACTION_DOWN,
            keyCode
        )
        audioManager.dispatchMediaKeyEvent(event)

        val upEvent = android.view.KeyEvent(
            android.view.KeyEvent.ACTION_UP,
            keyCode
        )
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    override fun destroy() {
        try {
            context.unregisterReceiver(mediaReceiver)
        } catch (e: Exception) {
            Log.e("MusicController", "Error unregistering receiver", e)
        }
    }
}

class MyvuMusicManager(
    private val context: Context,
    private val onTrackUpdate: (String) -> Unit,
    private val onControl: (String) -> Unit
) {
    private val controller = AndroidMusicController(context)
    private var isMirroring = false

    fun startMirror() {
        isMirroring = true
        controller.setOnTrackChangeListener { track ->
            updateGlassesDisplay(track)
        }
        onControl("Espelhamento de música iniciado")
    }

    fun stopMirror() {
        isMirroring = false
        onControl("Espelhamento de música encerrado")
    }

    private fun updateGlassesDisplay(track: MusicInfo) {
        if (!isMirroring) return

        val status = if (track.isPlaying) "▶️" else "⏸️"
        onTrackUpdate("$status ${track.title}")
        onTrackUpdate("🎤 ${track.artist}")
        onTrackUpdate("💿 ${track.album}")
    }

    fun playPause() {
        if (controller.getCurrentTrack().isPlaying) {
            controller.pause()
            onControl("⏸️ Pausado")
        } else {
            controller.play()
            onControl("▶️ Reproduzindo")
        }
    }

    fun nextTrack() {
        controller.next()
        onControl("⏭️ Próxima faixa")
    }

    fun previousTrack() {
        controller.previous()
        onControl("⏮️ Faixa anterior")
    }

    fun getCurrentTrackInfo(): String {
        val track = controller.getCurrentTrack()
        return if (track.title.isNotEmpty()) {
            "${track.title} - ${track.artist}"
        } else {
            "Nenhuma música tocando"
        }
    }

    fun destroy() {
        controller.destroy()
    }
}
