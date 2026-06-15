package com.classmate.core.capture

import com.classmate.core.transcript.TranscriptSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAsr(val result: CaptureResult<AsrTranscriptResult>, override val isConfigured: Boolean = true) : SpeechToTextProvider {
    override fun transcribeLongAudio(audioBytes: ByteArray, fileName: String, audioFormat: String, onProgress: (Int) -> Unit) = result
}
private class FakeOcr(val result: CaptureResult<OcrResult>, override val isConfigured: Boolean = true) : OcrProvider {
    override fun recognize(imageBytes: ByteArray) = result
}

class CaptureUseCaseTest {

    // ---- A. transcript / ASR draft flow ----------------------------------------------------------

    @Test fun asrSuccessProducesEditableTranscriptDraft() {
        val asr = FakeAsr(CaptureResult.Success(AsrTranscriptResult(listOf(AsrSegment("感应电动势由磁通量变化率决定", 0, 2000, 1)))))
        val draft = (CreateTranscriptDraftUseCase(asr).fromAudio("a".toByteArray(), "l.mp3", "auto", "电磁感应") as CaptureResult.Success).value
        assertEquals(TranscriptSourceType.AUDIO_TRANSCRIPT, draft.sourceType)
        assertEquals(1, draft.segments.size)
        assertEquals("感应电动势由磁通量变化率决定", draft.segments[0].text)
    }

    @Test fun asrConfigMissingFallsBackNotCrash() {
        val asr = FakeAsr(CaptureResult.fail(CaptureError.ConfigMissing), isConfigured = false)
        val r = CreateTranscriptDraftUseCase(asr).fromAudio("a".toByteArray(), "l.mp3", "auto", "t")
        assertTrue(r is CaptureResult.Failure)
        // Manual paste still works without any provider.
        val pasted = CreateTranscriptDraftUseCase(asr).fromPastedText("第一句\n第二句", "手动课堂")
        assertEquals(2, pasted.segments.size)
        assertEquals(TranscriptSourceType.PASTED_TRANSCRIPT, pasted.sourceType)
    }

    @Test fun unconfirmedDraftYieldsNoAnalysisPayloadUntilConfirmed() {
        val asr = FakeAsr(CaptureResult.Success(AsrTranscriptResult(listOf(AsrSegment("内容", 0, 1000, 1)))))
        val draft = (CreateTranscriptDraftUseCase(asr).fromAudio("a".toByteArray(), "l.mp3", "auto", "课") as CaptureResult.Success).value
        // A draft is NOT a ClassroomCaptureResult — only confirm produces the analysis-ready payload.
        val confirmed = ConfirmTranscriptDraftUseCase().confirm(draft, "电磁感应")
        assertTrue(confirmed is ClassroomCaptureResult)
        assertEquals("电磁感应", confirmed!!.courseTitle)
        assertEquals("内容", confirmed.courseText)
    }

    @Test fun confirmEmptyDraftReturnsNullSoNothingBlankReachesAnalysis() {
        val empty = CreateTranscriptDraftUseCase(FakeAsr(CaptureResult.fail(CaptureError.Unknown))).fromPastedText("   \n  ", "空")
        assertNull(ConfirmTranscriptDraftUseCase().confirm(empty))
    }

    // ---- B. OCR + on-device dual-track image draft -----------------------------------------------

    @Test fun ocrSuccessMergesBlocksAndKeepsOnDeviceDraft() {
        val ocr = FakeOcr(CaptureResult.Success(OcrResult(listOf(OcrTextBlock("公式一"), OcrTextBlock("公式二")))))
        val draft = CreateImageStudyDraftUseCase(ocr).create("img".toByteArray(), "拍照学习输入", onDeviceDraftText = "端侧草稿")
        assertTrue(draft.hasOcr)
        assertEquals("公式一\n公式二", draft.ocrNormalizedText)
        assertTrue(draft.hasOnDeviceDraft) // both tracks coexist; neither replaces the other
        assertNull(draft.ocrError)
    }

    @Test fun ocrFailureLeavesOnDeviceDraftEditable() {
        val ocr = FakeOcr(CaptureResult.fail(CaptureError.ConfigMissing), isConfigured = false)
        val draft = CreateImageStudyDraftUseCase(ocr).create("img".toByteArray(), "图片学习输入", onDeviceDraftText = "端侧蓝心草稿")
        assertFalse(draft.hasOcr)
        assertEquals(CaptureError.ConfigMissing, draft.ocrError)
        // The on-device draft is still the editable starting text — never crashes, never blocks.
        assertEquals("端侧蓝心草稿", draft.initialEditableText())
    }

    @Test fun confirmImageDraftUsesEditedTextAndRejectsBlank() {
        val draft = ImageStudyDraft(id = "1", origin = "拍照学习输入", onDeviceDraftText = "草稿")
        assertNull(ConfirmImageStudyDraftUseCase().confirm(draft, "   ", "课"))
        val confirmed = ConfirmImageStudyDraftUseCase().confirm(draft, "用户编辑后的课堂文本", "电磁感应")
        assertEquals("用户编辑后的课堂文本", confirmed!!.courseText)
    }

    // ---- C. evidence retrieval foundation --------------------------------------------------------

    @Test fun retrievalReturnsTopEvidenceByOverlap() {
        val segments = listOf(
            EvidenceSegmentInput("s1", "楞次定律：感应电流方向总是阻碍磁通量的变化"),
            EvidenceSegmentInput("s2", "今天天气很好我们去操场"),
            EvidenceSegmentInput("s3", "感应电动势由磁通量变化率决定"),
        )
        val top = RetrieveCourseEvidenceUseCase().retrieve("磁通量变化", segments, topN = 2)
        assertEquals(2, top.size)
        assertTrue("expected an electromagnetics segment first", top[0].segmentId == "s1" || top[0].segmentId == "s3")
        assertEquals(0, top[0].rank)
    }

    @Test fun retrievalEmptyWhenNoOverlapOrNoSegments() {
        val segments = listOf(EvidenceSegmentInput("s1", "完全无关的内容"))
        assertTrue(RetrieveCourseEvidenceUseCase().retrieve("量子纠缠超导体", segments).isEmpty())
        assertTrue(RetrieveCourseEvidenceUseCase().retrieve("任何问题", emptyList()).isEmpty())
    }

    @Test fun retrievalWorksWhenRewriteAndSimilarityProvidersMissing() {
        // null providers (and ConfigMissing ones) must not break local retrieval.
        val uc = RetrieveCourseEvidenceUseCase(
            similarityProvider = VivoTextSimilarityProvider(),
            queryRewriteProvider = VivoQueryRewriteProvider(),
        )
        val segments = listOf(EvidenceSegmentInput("s1", "感应电动势由磁通量变化率决定"))
        val top = uc.retrieve("磁通量", segments)
        assertEquals(1, top.size)
        assertEquals("s1", top[0].segmentId)
    }
}
