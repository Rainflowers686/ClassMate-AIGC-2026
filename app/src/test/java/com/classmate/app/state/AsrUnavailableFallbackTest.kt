package com.classmate.app.state

import com.classmate.app.asr.AsrState
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-2 / P0-5: when system speech recognition is unavailable (and official ASR is unconfigured), the
 * recording must still work, no fake transcript is fabricated, and the copy stays honest — never claims
 * transcription is happening or that official ASR is ready.
 */
class AsrUnavailableFallbackTest {

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-asr").resolve("config.local.json").toFile()),
        historyStore = InMemoryHistoryStore(emptyList()),
        exportStore = InMemoryExportStore(),
    )

    @Test
    fun systemAsrUnavailableStillStartsRecordingAndNeverClaimsTranscription() {
        val viewModel = vm()
        val state = viewModel.startRecordingWithTranscription(asrAvailable = false, permissionGranted = true)
        // UNSUPPORTED (not LISTENING) -> the UI shows "录音继续", never "正在听写".
        assertEquals(AsrState.UNSUPPORTED, state)
    }

    @Test
    fun stoppingWithoutTranscriptKeepsAudioMessageHonestAndFabricatesNoTranscript() {
        val viewModel = vm()
        viewModel.startRecordingWithTranscription(asrAvailable = false, permissionGranted = true)
        val transcriptsBefore = viewModel.ui.transcripts.size
        viewModel.stopRecordingWithTranscription()
        assertTrue(
            "honest 转写暂不可用 message",
            viewModel.ui.audioCaptureMessage?.contains("转写暂不可用") == true,
        )
        assertEquals("no fake transcript is fabricated", transcriptsBefore, viewModel.ui.transcripts.size)
    }

    @Test
    fun permissionMissingAlsoFallsBackInsteadOfListening() {
        val viewModel = vm()
        val state = viewModel.startRecordingWithTranscription(asrAvailable = true, permissionGranted = false)
        assertFalse("not listening without permission", state == AsrState.LISTENING)
    }
}
