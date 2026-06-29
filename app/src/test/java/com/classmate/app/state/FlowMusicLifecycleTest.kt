package com.classmate.app.state

import com.classmate.app.audio.FlowAudioController
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import com.classmate.app.ui.flow.AmbientSoundCatalog
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P1-2: background music is owned by the ViewModel, not the Flow Composable. Leaving the page never stops
 * it; only an explicit pause/stop or the VM being cleared does. A fake controller records the real calls.
 */
class FlowMusicLifecycleTest {

    private class FakeFlowAudio : FlowAudioController {
        var playing = false
        var released = false
        var lastVolume = -1f
        override fun play(sound: com.classmate.app.ui.flow.AmbientSound, volume: Float) { playing = true; lastVolume = volume }
        override fun pause() { playing = false }
        override fun stop() { playing = false }
        override fun setVolume(volume: Float) { lastVolume = volume }
        override fun release() { released = true; playing = false }
    }

    private fun vm(audio: FlowAudioController): AppViewModel = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-flow").resolve("config.local.json").toFile()),
        historyStore = InMemoryHistoryStore(emptyList()),
        exportStore = InMemoryExportStore(),
        flowAudioController = audio,
    )

    private val rain = AmbientSoundCatalog.all.first()

    @Test
    fun startingMusicThenLeavingThePageKeepsItPlaying() {
        val audio = FakeFlowAudio()
        val viewModel = vm(audio)
        viewModel.flowMusicPlay(rain)
        assertTrue("controller is playing", audio.playing)
        assertEquals(FlowMusicStatus.PLAYING, viewModel.ui.flowMusic.status)

        // Simulate leaving the Flow page: the screen recomposes/disposes, but nothing calls stop/release.
        // The VM state (and the controller) are untouched -> still playing.
        assertTrue("music survives leaving the page", viewModel.ui.flowMusic.playing)
        assertTrue(audio.playing)
    }

    @Test
    fun userPauseAndStopChangeState() {
        val audio = FakeFlowAudio()
        val viewModel = vm(audio)
        viewModel.flowMusicPlay(rain)
        viewModel.flowMusicPause()
        assertEquals(FlowMusicStatus.PAUSED, viewModel.ui.flowMusic.status)
        assertFalse(audio.playing)
        viewModel.flowMusicPlay(rain)
        viewModel.flowMusicStop()
        assertEquals(FlowMusicStatus.STOPPED, viewModel.ui.flowMusic.status)
        assertFalse(audio.playing)
    }

    @Test
    fun volumeChangePropagatesToController() {
        val audio = FakeFlowAudio()
        val viewModel = vm(audio)
        viewModel.flowMusicSetVolume(0.8f)
        assertEquals(0.8f, audio.lastVolume, 0.001f)
        assertEquals(0.8f, viewModel.ui.flowMusic.volume, 0.001f)
    }
}
