package com.classmate.app.state

import com.classmate.app.asr.AsrState
import com.classmate.app.material.LessonMaterialAssembler
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.material.MaterialSourceType
import com.classmate.core.material.SpeakerLabel
import com.classmate.core.transcript.TranscriptSourceType
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AsrLiveFlowTest {

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-asr").resolve("config.local.json").toFile()),
    )

    @Test
    fun unsupportedDeviceDoesNotListen() {
        val viewModel = vm()
        val state = viewModel.asrBegin(available = false, permissionGranted = true)
        assertEquals(AsrState.UNSUPPORTED, state)
        assertTrue(viewModel.ui.asrSession.segments.isEmpty())
    }

    @Test
    fun deniedPermissionFallsBackWithoutListening() {
        val viewModel = vm()
        val state = viewModel.asrBegin(available = true, permissionGranted = false)
        assertEquals(AsrState.PERMISSION_REQUIRED, state)
        // Honest, actionable guidance: ask for the recording permission and offer a manual fallback.
        val toast = viewModel.ui.toast.orEmpty()
        assertTrue(toast.contains("录音权限"))
        assertTrue(toast.contains("手动"))
    }

    @Test
    fun finalResultUsesSelectedSpeakerAndCommitsAsLiveAsrTranscript() {
        val viewModel = vm()
        assertEquals(AsrState.LISTENING, viewModel.asrBegin(available = true, permissionGranted = true))
        viewModel.setLiveSpeaker(SpeakerLabel.TEACHER)
        viewModel.asrOnPartial("临时识别文本")
        assertTrue(viewModel.ui.asrSession.segments.isEmpty()) // partial not stored
        viewModel.asrOnFinal("法拉第电磁感应定律", confidence = 0.9)
        assertEquals(1, viewModel.ui.asrSession.segments.size)
        assertEquals(SpeakerLabel.TEACHER, viewModel.ui.asrSession.segments.first().speaker)

        viewModel.stopAsr()
        val transcript = viewModel.ui.transcripts.single()
        assertEquals(TranscriptSourceType.LIVE_ASR, transcript.sourceType)
        assertEquals(1, transcript.segments.size)
    }

    @Test
    fun asrTranscriptFlowsIntoMaterialBundleWithRealtimeMarker() {
        val viewModel = vm()
        viewModel.asrBegin(available = true, permissionGranted = true)
        viewModel.setLiveSpeaker(SpeakerLabel.TEACHER)
        viewModel.asrOnFinal("磁通量的定义", confidence = null)
        viewModel.stopAsr()

        val bundle = LessonMaterialAssembler.fromImportWithOcr(
            title = "电磁感应",
            text = "",
            importType = ImportSourceType.PASTE_TEXT,
            ocrImports = emptyList(),
            transcripts = viewModel.ui.transcripts,
        )
        assertEquals(MaterialSourceType.TRANSCRIPT, bundle.sources.single().type)
        assertTrue(bundle.plainText().contains("[实时转写"))
        assertTrue(bundle.plainText().contains("磁通量的定义"))
    }

    @Test
    fun canGenerateTimelineFromAsrOnlyContent() {
        val viewModel = vm()
        viewModel.asrBegin(available = true, permissionGranted = true)
        viewModel.asrOnFinal("一段实时转写内容", confidence = null)
        assertTrue(viewModel.canGenerateLiveTimeline())
    }

    @Test
    fun liveAndSettingsCopyMakeNoExaggeratedAsrClaims() {
        val files = listOf(
            "src/main/java/com/classmate/app/ui/screens/live/LiveCompanionScreen.kt",
            "src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt",
        ).map { path -> listOf(File(path), File("app/$path")).first { it.exists() }.readText() }
        val combined = files.joinToString("\n")
        listOf("声纹识别已支持", "声纹身份识别已支持", "底噪处理已支持", "人声增强已支持", "已接入真实 ASR").forEach {
            assertFalse("must not claim '$it'", combined.contains(it))
        }
        // honest disclaimers are present
        assertTrue(combined.contains("不保存原始音频"))
        assertTrue(combined.contains("不后台录音"))
    }
}
