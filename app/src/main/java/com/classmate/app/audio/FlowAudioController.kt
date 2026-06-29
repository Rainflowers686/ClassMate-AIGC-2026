package com.classmate.app.audio

import android.content.Context
import com.classmate.app.ui.flow.AmbientSound
import com.classmate.app.ui.flow.AmbientSoundPlayer

/**
 * P1-2: App/ViewModel-level owner of the Flow background-music player. The playback lifecycle lives ABOVE
 * the Flow page Composable, so leaving the page does NOT stop the music — only the user (pause / stop) or
 * the ViewModel (onCleared) does. On any audio error the player is safely released, never left dangling.
 */
interface FlowAudioController {
    fun play(sound: AmbientSound, volume: Float)
    fun pause()
    fun stop()
    fun setVolume(volume: Float)
    fun release()
}

/** Default used in unit tests and the VM's no-arg path — records nothing, touches no Android MediaPlayer. */
object NoOpFlowAudioController : FlowAudioController {
    override fun play(sound: AmbientSound, volume: Float) {}
    override fun pause() {}
    override fun stop() {}
    override fun setVolume(volume: Float) {}
    override fun release() {}
}

/** Production controller backed by [AmbientSoundPlayer] on the application context (no Activity leak). */
class AndroidFlowAudioController(context: Context) : FlowAudioController {
    private val player = AmbientSoundPlayer(context.applicationContext)

    override fun play(sound: AmbientSound, volume: Float) {
        try {
            player.play(sound, volume)
        } catch (_: Exception) {
            // Audio error -> stop safely instead of leaving a half-initialised player around.
            player.release()
        }
    }

    override fun pause() { runCatching { player.pause() } }
    override fun stop() { runCatching { player.stop() } }
    override fun setVolume(volume: Float) { runCatching { player.setVolume(volume) } }
    override fun release() { runCatching { player.release() } }
}
