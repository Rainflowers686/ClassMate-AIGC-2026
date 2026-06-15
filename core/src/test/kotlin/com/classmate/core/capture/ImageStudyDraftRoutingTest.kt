package com.classmate.core.capture

import com.classmate.core.ai.AiExecutionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeOcrProvider(val result: CaptureResult<OcrResult>, override val isConfigured: Boolean = true) : OcrProvider {
    override fun recognize(imageBytes: ByteArray) = result
}

/** ImageStudyDraft routed through Cloud(OCR) → On-device(multimodal draft) → Manual; dual-track preserved. */
class ImageStudyDraftRoutingTest {

    @Test fun ocrSuccessIsCloudSourceAndKeepsBothTracks() {
        val uc = RoutedImageStudyDraftUseCase(FakeOcrProvider(CaptureResult.Success(OcrResult(listOf(OcrTextBlock("公式 ε=−dΦ/dt"))))))
        val r = uc.create("img".toByteArray(), "拍照学习输入", onDeviceDraftText = "端侧蓝心草稿")
        assertEquals(AiExecutionSource.CLOUD, r.source)
        val draft = r.value!!
        assertEquals("公式 ε=−dΦ/dt", draft.ocrNormalizedText) // OCR track
        assertEquals("端侧蓝心草稿", draft.onDeviceDraftText)   // on-device track coexists (no replacement)
        assertTrue(r.decision.userConfirmationRequired)
    }

    @Test fun ocrConfigMissingFallsToOnDeviceDraft() {
        val uc = RoutedImageStudyDraftUseCase(FakeOcrProvider(CaptureResult.fail(CaptureError.ConfigMissing), isConfigured = false))
        val r = uc.create("img".toByteArray(), "图片学习输入", onDeviceDraftText = "端侧蓝心草稿")
        assertEquals(AiExecutionSource.ON_DEVICE, r.source)
        assertEquals(CaptureError.ConfigMissing, r.value!!.ocrError)
        assertEquals("端侧蓝心草稿", r.value!!.initialEditableText())
        assertTrue(r.decision.attempted.contains(AiExecutionSource.CLOUD))
        assertTrue(r.decision.attempted.contains(AiExecutionSource.ON_DEVICE))
    }

    @Test fun ocrFailureAndNoOnDeviceFallsToManualEditableDraft() {
        val uc = RoutedImageStudyDraftUseCase(FakeOcrProvider(CaptureResult.fail(CaptureError.ServiceUnavailable)))
        val r = uc.create("img".toByteArray(), "图片学习输入", onDeviceDraftText = "")
        assertEquals(AiExecutionSource.MANUAL, r.source)
        assertTrue(r.value != null) // always an editable draft, never a crash
        assertTrue(r.decision.userConfirmationRequired)
    }
}
