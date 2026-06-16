package com.classmate.app.ui.flow

import android.content.Context
import android.media.MediaPlayer

class AmbientSoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null
    private var currentSoundId: String? = null
    private var currentVolume: Float = 0.45f

    fun play(sound: AmbientSound, volume: Float = currentVolume) {
        val safeVolume = volume.coerceIn(0f, 1f)
        currentVolume = safeVolume
        if (currentSoundId != sound.id || player == null) {
            release()
            player = MediaPlayer.create(appContext, sound.rawResId)?.apply {
                isLooping = true
                setVolume(safeVolume, safeVolume)
            }
            currentSoundId = sound.id
        }
        player?.let {
            it.setVolume(safeVolume, safeVolume)
            if (!it.isPlaying) it.start()
        }
    }

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun stop() {
        player?.takeIf { it.isPlaying }?.pause()
        player?.seekTo(0)
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        player?.setVolume(currentVolume, currentVolume)
    }

    fun release() {
        player?.release()
        player = null
        currentSoundId = null
    }
}
