package com.classmate.app.state

import com.classmate.app.capture.CaptureGatewayPort
import com.classmate.app.platform.CaptureConfigStatus
import com.classmate.core.ai.AiCapability
import com.classmate.core.ai.AiCapabilityResult
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ai.AiExecutionStatus
import com.classmate.core.ai.AiRouteDecision
import com.classmate.core.capture.CaptureError
import com.classmate.core.capture.CaptureResult
import com.classmate.core.capture.ClassroomCaptureResult
import com.classmate.core.capture.EvidenceCandidate
import com.classmate.core.capture.ImageStudyDraft
import com.classmate.core.capture.OcrResult
import com.classmate.core.model.CourseSession
import com.classmate.core.transcript.TranscriptDraft
import com.classmate.core.transcript.TranscriptSegmentDraft
import com.classmate.core.transcript.TranscriptSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureGatewayWiringTest {

    @Test
    fun routedImageDraftRequiresConfirmBeforeCourseTextChanges() {
        val gateway = FakeCaptureGateway()
        val viewModel = AppViewModel(captureGatewayProvider = { gateway })
        val routed = routedImageDraft(
            ImageStudyDraft(
                id = "img-1",
                origin = "photo",
                ocrNormalizedText = "Faraday law OCR text",
                onDeviceDraftText = "semantic draft",
            ),
            AiExecutionSource.CLOUD,
        )

        viewModel.beginImageDraft("photo")
        viewModel.applyImageStudyDraftResult(routed)

        assertEquals("Faraday law OCR text", viewModel.ui.imageDraftText)
        assertEquals("", viewModel.ui.courseText)
        assertEquals(AiExecutionSource.CLOUD, viewModel.ui.imageDraftSource)

        viewModel.confirmImageDraft()

        assertTrue(gateway.confirmImageCalled)
        assertEquals("Faraday law OCR text", viewModel.ui.courseText)
        assertFalse(viewModel.ui.imageDraftActive)
    }

    @Test
    fun imageDraftConfigMissingKeepsOnDeviceDraftEditable() {
        val viewModel = AppViewModel(captureGatewayProvider = { FakeCaptureGateway() })
        val draft = ImageStudyDraft(
            id = "img-2",
            origin = "image",
            ocrError = CaptureError.ConfigMissing,
            onDeviceDraftText = "on-device semantic draft",
        )

        viewModel.applyImageStudyDraftResult(routedImageDraft(draft, AiExecutionSource.ON_DEVICE))

        assertEquals("on-device semantic draft", viewModel.ui.imageDraftText)
        assertEquals(AiExecutionSource.ON_DEVICE, viewModel.ui.imageDraftSource)
        assertFalse(viewModel.ui.imageDraftManualMode)
    }

    @Test
    fun asrFailureExposesManualTranscriptFallback() {
        val viewModel = AppViewModel(captureGatewayProvider = { FakeCaptureGateway() })

        val ok = viewModel.applyAudioTranscriptResult(CaptureResult.fail(CaptureError.ConfigMissing), "lesson.wav")

        assertFalse(ok)
        assertFalse(viewModel.ui.audioCaptureRunning)
        assertTrue(viewModel.ui.audioCaptureMessage.orEmpty().contains("ASR"))
        assertTrue(viewModel.ui.aiProcessing.canContinueManual)
    }

    @Test
    fun captureReadinessComesFromInjectedGateway() {
        val viewModel = AppViewModel(captureGatewayProvider = {
            FakeCaptureGateway(status = CaptureConfigStatus(configured = true, hasAppId = true, hasAppKey = true))
        })

        assertEquals("已配置", viewModel.captureConfigStatus().labelZh())
    }

    @Test
    fun pastedTranscriptCreatesEditableDraftThroughGateway() {
        val gateway = FakeCaptureGateway()
        val viewModel = AppViewModel(captureGatewayProvider = { gateway })

        viewModel.updateTranscriptPaste("line one\nline two")

        assertTrue(viewModel.createManualTranscriptDraftFromPaste(now = 123L))
        assertTrue(gateway.manualTranscriptCalled)
        assertEquals(TranscriptSourceType.PASTED_TRANSCRIPT, viewModel.ui.transcriptDraft!!.sourceType)
        assertEquals(2, viewModel.ui.transcriptDraft!!.segments.size)
    }

    @Test
    fun aiProcessingStateCarriesSafeLabels() {
        val state = AiProcessingUiState(
            visible = true,
            title = "正在生成知识地图",
            steps = listOf("准备资料", "云端处理中", "端侧兜底", "整理结果"),
            activeStep = 2,
            source = "端侧",
            fallbackMessage = "云端不可用，已切换端侧。",
            canCancel = true,
            canRetry = true,
            canContinueManual = true,
        )

        val joined = listOf(state.title, state.steps.joinToString(), state.source, state.fallbackMessage).joinToString(" ")
        assertFalse(joined.contains("LOCAL_FALLBACK"))
        assertFalse(joined.contains("DeepSeek"))
        assertTrue(state.canContinueManual)
    }
}

private fun routedImageDraft(draft: ImageStudyDraft, source: AiExecutionSource): AiCapabilityResult<ImageStudyDraft> =
    AiCapabilityResult(
        value = draft,
        source = source,
        status = AiExecutionStatus.SUCCESS,
        decision = AiRouteDecision(
            capability = AiCapability.OCR_TEXT_EXTRACTION,
            preferred = AiExecutionSource.CLOUD,
            attempted = listOf(AiExecutionSource.CLOUD, source).distinct(),
            selected = source,
            reason = "test",
            userConfirmationRequired = true,
        ),
    )

private class FakeCaptureGateway(
    private val status: CaptureConfigStatus = CaptureConfigStatus(configured = false, hasAppId = false, hasAppKey = false),
) : CaptureGatewayPort {
    var confirmImageCalled = false
    var manualTranscriptCalled = false

    override fun refresh() = Unit
    override fun status(): CaptureConfigStatus = status
    override val isOcrConfigured: Boolean = false
    override val isAsrConfigured: Boolean = false
    override fun recognizeImage(imageBytes: ByteArray): CaptureResult<OcrResult> = CaptureResult.fail(CaptureError.ConfigMissing)
    override fun createImageStudyDraftRouted(imageBytes: ByteArray, origin: String, onDeviceDraftText: String): AiCapabilityResult<ImageStudyDraft> =
        routedImageDraft(ImageStudyDraft(id = "fake-image", origin = origin, onDeviceDraftText = onDeviceDraftText), AiExecutionSource.ON_DEVICE)
    override fun createImageStudyDraft(imageBytes: ByteArray, origin: String, onDeviceDraftText: String): ImageStudyDraft =
        ImageStudyDraft(id = "fake-image", origin = origin, onDeviceDraftText = onDeviceDraftText)
    override fun confirmImageDraft(draft: ImageStudyDraft, editedText: String, courseTitle: String): ClassroomCaptureResult? {
        confirmImageCalled = true
        if (editedText.isBlank()) return null
        return ClassroomCaptureResult(courseTitle, editedText.trim(), null, "image draft", 1)
    }
    override fun transcribeAudio(
        audioBytes: ByteArray,
        fileName: String,
        audioFormat: String,
        title: String,
        onProgress: (Int) -> Unit,
    ): CaptureResult<TranscriptDraft> = CaptureResult.fail(CaptureError.ConfigMissing)
    override fun draftFromPastedText(text: String, title: String): TranscriptDraft {
        manualTranscriptCalled = true
        return TranscriptDraft(
            id = "manual-draft",
            sourceType = TranscriptSourceType.PASTED_TRANSCRIPT,
            title = title,
            segments = text.lines().filter { it.isNotBlank() }.mapIndexed { index, line ->
                TranscriptSegmentDraft(id = "manual-$index", text = line)
            },
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
    override fun confirmTranscriptDraft(draft: TranscriptDraft, courseTitle: String): ClassroomCaptureResult? = null
    override fun retrieveEvidence(question: String, session: CourseSession, topN: Int): List<EvidenceCandidate> = emptyList()
}
