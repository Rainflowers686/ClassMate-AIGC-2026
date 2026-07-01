package com.classmate.app.state

import com.classmate.app.asr.AsrState
import com.classmate.app.l3.ClassroomAudioRecorder
import com.classmate.app.l3.InputFileKind
import com.classmate.app.l3.L3RecordingStatus
import com.classmate.app.l3.RecordingArtifactResult
import com.classmate.app.l3.RecordingFileManager
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.transcript.TranscriptSourceType
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The inline "record + live transcribe" flow (P0-1). AUDIO evidence means a real audio file; transcript
 * evidence means real recognized text; neither is ever fabricated, and cancel keeps nothing.
 */
class RecordingTranscriptionFlowTest {

    private class FakeRecorder(private val stopSucceeds: Boolean) : ClassroomAudioRecorder {
        override fun start(sessionId: String) = RecordingArtifactResult(true, "$sessionId.m4a", "录音已开始。")
        override fun stop() =
            if (stopSucceeds) RecordingArtifactResult(true, "rec.m4a", "已保存。", fileSizeBytes = 4096L)
            else RecordingArtifactResult(false, "rec.m4a", "文件为空。")
        override fun cancel() = RecordingArtifactResult(true, "rec.m4a", "已取消。")
    }

    private fun vm(stopSucceeds: Boolean = true) = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-rt").resolve("config.local.json").toFile()),
        classroomAudioRecorder = FakeRecorder(stopSucceeds),
    )

    private class FileRecorder(private val dir: java.io.File) : ClassroomAudioRecorder {
        override fun start(sessionId: String) = RecordingArtifactResult(true, "$sessionId.m4a", "录音已开始。")
        override fun stop(): RecordingArtifactResult {
            val file = java.io.File(dir, "saved.m4a")
            file.parentFile?.mkdirs()
            file.writeBytes(ByteArray(16) { 1 })
            return RecordingArtifactResult(true, file.name, "已保存。", fileSizeBytes = file.length())
        }
        override fun cancel() = RecordingArtifactResult(true, "saved.m4a", "已取消。")
    }

    @Test
    fun startBeginsRecordingAndListening() {
        val viewModel = vm()
        val state = viewModel.startRecordingWithTranscription(asrAvailable = true, permissionGranted = true, now = 1_000L)
        assertEquals(AsrState.LISTENING, state)
        assertEquals(L3RecordingStatus.RECORDING, viewModel.ui.currentRecording?.status)
        assertEquals(AsrState.LISTENING, viewModel.ui.asrSession.state)
    }

    @Test
    fun partialThenFinalAppearInState() {
        val viewModel = vm()
        viewModel.startRecordingWithTranscription(true, true, 1_000L)
        viewModel.asrOnPartial("电磁感")
        assertEquals("电磁感", viewModel.ui.asrSession.partialText)
        viewModel.asrOnFinal("电磁感应定律说明磁通量变化产生感应电动势", confidence = 0.9)
        assertEquals(1, viewModel.ui.asrSession.segments.size)
        assertEquals("", viewModel.ui.asrSession.partialText)
    }

    @Test
    fun stopWithTranscriptSavesBothAudioAndTranscriptEvidence() {
        val viewModel = vm(stopSucceeds = true)
        viewModel.startRecordingWithTranscription(true, true, 1_000L)
        viewModel.asrOnFinal("磁通量变化产生感应电动势", confidence = null)
        viewModel.stopRecordingWithTranscription(now = 5_000L)
        assertTrue("AUDIO evidence saved", viewModel.ui.inputArtifacts.any { it.kind == InputFileKind.AUDIO })
        val transcript = viewModel.ui.transcripts.firstOrNull()
        assertNotNull("transcript evidence saved", transcript)
        assertEquals(TranscriptSourceType.LIVE_ASR, transcript!!.sourceType)
    }

    @Test
    fun stopWithoutTranscriptSavesOnlyAudio() {
        val viewModel = vm(stopSucceeds = true)
        viewModel.startRecordingWithTranscription(true, true, 1_000L)
        viewModel.stopRecordingWithTranscription(now = 5_000L)
        assertTrue(viewModel.ui.inputArtifacts.any { it.kind == InputFileKind.AUDIO })
        assertTrue("no fabricated transcript", viewModel.ui.transcripts.none { t -> t.segments.any { it.text.isNotBlank() } })
    }

    @Test
    fun cancelSavesNeitherAudioNorTranscript() {
        val viewModel = vm()
        viewModel.startRecordingWithTranscription(true, true, 1_000L)
        viewModel.asrOnFinal("这段会被丢弃", confidence = null)
        viewModel.cancelRecordingWithTranscription()
        assertNull(viewModel.ui.currentRecording)
        assertFalse(viewModel.ui.inputArtifacts.any { it.kind == InputFileKind.AUDIO })
        assertTrue(viewModel.ui.asrSession.segments.isEmpty())
        assertTrue(viewModel.ui.transcripts.none { t -> t.segments.any { it.text.isNotBlank() } })
    }

    @Test
    fun systemBackWhileRecordingPromptsInsteadOfExiting() {
        val viewModel = vm()
        viewModel.startRecordingWithTranscription(true, true, 1_000L)
        assertTrue(viewModel.canHandleSystemBack)
        assertTrue("back is handled in-app", viewModel.handleSystemBack())
        assertTrue("a confirm prompt is shown", viewModel.ui.showRecordingBackPrompt)
        assertEquals("did not navigate away", Screen.HOME, viewModel.currentScreen)

        // Choosing cancel from the prompt drops the recording with no evidence.
        viewModel.cancelRecordingFromBackPrompt()
        assertFalse(viewModel.ui.showRecordingBackPrompt)
        assertNull(viewModel.ui.currentRecording)
        assertFalse(viewModel.ui.inputArtifacts.any { it.kind == InputFileKind.AUDIO })
    }

    @Test
    fun unavailableRecognizerStillRecordsWithHonestState() {
        val viewModel = vm()
        val state = viewModel.startRecordingWithTranscription(asrAvailable = false, permissionGranted = true, now = 1_000L)
        assertEquals(AsrState.UNSUPPORTED, state)
        // The recording proceeds even when the recognizer is unavailable — no dead end.
        assertEquals(L3RecordingStatus.RECORDING, viewModel.ui.currentRecording?.status)
    }

    @Test
    fun savedRecordingCanInvokeOfficialAsrPathWithoutSystemRecognizer() {
        val dir = Files.createTempDirectory("cm-rt-files").toFile()
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-rt-config").resolve("config.local.json").toFile()),
            classroomAudioRecorder = FileRecorder(dir),
            recordingFileManager = RecordingFileManager(dir),
        )
        viewModel.startClassroomRecording(now = 1_000L)
        viewModel.stopClassroomRecording(now = 5_000L)
        val record = viewModel.ui.recordingRecords.single()

        assertEquals(L3RecordingStatus.SAVED, record.status)
        assertTrue(java.io.File(dir, record.artifactFileName ?: "").exists())
        assertTrue("saved audio can be read for official ASR action", viewModel.savedRecordingAudioAvailable(record.id))
        assertTrue("no fake transcript is generated", viewModel.ui.transcripts.isEmpty())
    }
}
