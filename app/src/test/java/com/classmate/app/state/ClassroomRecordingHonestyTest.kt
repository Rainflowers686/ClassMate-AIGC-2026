package com.classmate.app.state

import com.classmate.app.l3.ClassroomAudioRecorder
import com.classmate.app.l3.InputFileKind
import com.classmate.app.l3.L3RecordingStatus
import com.classmate.app.l3.RecordingArtifactResult
import com.classmate.app.platform.ConfigRepository
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A recording that does not produce a real, non-empty file must never be reported as success and must
 * never fabricate AUDIO evidence — the honest path is a FAILED record + a manual-transcription hint.
 */
class ClassroomRecordingHonestyTest {

    private class FakeRecorder(private val stopSucceeds: Boolean) : ClassroomAudioRecorder {
        override fun start(sessionId: String) =
            RecordingArtifactResult(true, "$sessionId.m4a", "录音已开始。")

        override fun stop() =
            if (stopSucceeds) RecordingArtifactResult(true, "rec.m4a", "录音已保存。", fileSizeBytes = 2048L)
            else RecordingArtifactResult(false, "rec.m4a", "录音文件为空或保存失败。")

        override fun cancel() =
            RecordingArtifactResult(true, "rec.m4a", "录音已取消。")
    }

    private fun vm(stopSucceeds: Boolean) = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-rec").resolve("config.local.json").toFile()),
        classroomAudioRecorder = FakeRecorder(stopSucceeds),
    )

    @Test
    fun failedStopDoesNotFabricateAudioEvidence() {
        val viewModel = vm(stopSucceeds = false)
        viewModel.startClassroomRecording(now = 1_000L)
        viewModel.stopClassroomRecording(now = 5_000L)

        val record = viewModel.ui.recordingRecords.last()
        assertEquals(L3RecordingStatus.FAILED, record.status)
        assertFalse(
            "failed recording must not create an AUDIO artifact",
            viewModel.ui.inputArtifacts.any { it.kind == InputFileKind.AUDIO },
        )
    }

    @Test
    fun savedRecordingKeepsFileSizeAndCreatesAudioArtifact() {
        val viewModel = vm(stopSucceeds = true)
        viewModel.startClassroomRecording(now = 1_000L)
        viewModel.stopClassroomRecording(now = 5_000L)

        val record = viewModel.ui.recordingRecords.last()
        assertEquals(L3RecordingStatus.SAVED, record.status)
        assertEquals(2048L, record.fileSizeBytes)
        assertTrue(record.durationMs > 0L)
        assertTrue(
            "saved recording should create an AUDIO artifact",
            viewModel.ui.inputArtifacts.any { it.kind == InputFileKind.AUDIO },
        )
    }

    @Test
    fun cancelRecordingDoesNotCreateEvidenceOrRecord() {
        val viewModel = vm(stopSucceeds = true)
        viewModel.startClassroomRecording(now = 1_000L)
        viewModel.cancelClassroomRecording()

        assertEquals(null, viewModel.ui.currentRecording)
        assertTrue(viewModel.ui.recordingRecords.isEmpty())
        assertFalse(viewModel.ui.inputArtifacts.any { it.kind == InputFileKind.AUDIO })
    }
}
