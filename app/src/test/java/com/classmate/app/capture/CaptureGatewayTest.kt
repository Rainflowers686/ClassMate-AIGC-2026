package com.classmate.app.capture

import com.classmate.app.platform.CaptureConfigLoader
import com.classmate.core.capture.CaptureError
import com.classmate.core.capture.CaptureHttpResponse
import com.classmate.core.capture.CaptureResult
import com.classmate.core.capture.CaptureTransport
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A transport that fails the test if it is ever hit — proves the ConfigMissing path never goes to network. */
private object NeverCalledTransport : CaptureTransport {
    private fun fail(): Nothing = throw AssertionError("transport must not be called when unconfigured")
    override fun postForm(url: String, headers: Map<String, String>, form: Map<String, String>, timeoutMs: Long) = fail()
    override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long) = fail()
    override fun postMultipart(url: String, headers: Map<String, String>, fileField: String, fileBytes: ByteArray, fileName: String, timeoutMs: Long): CaptureHttpResponse = fail()
}

/** Gateway smoke with NO credentials (loader points at a missing file): everything degrades to ConfigMissing. */
class CaptureGatewayTest {

    private fun unconfiguredGateway() = CaptureGateway(
        configLoader = CaptureConfigLoader(File("does-not-exist-gateway-test.json")),
        transport = NeverCalledTransport,
    )

    @Test fun ocrUnconfiguredReturnsConfigMissingWithoutNetwork() {
        val gw = unconfiguredGateway()
        assertEquals(false, gw.isOcrConfigured)
        val r = gw.recognizeImage("img".toByteArray())
        assertEquals(CaptureError.ConfigMissing, (r as CaptureResult.Failure).failure.error)
    }

    @Test fun asrUnconfiguredReturnsConfigMissingWithoutNetwork() {
        val gw = unconfiguredGateway()
        val r = gw.transcribeAudio("audio".toByteArray(), "a.mp3", "auto", "课")
        assertEquals(CaptureError.ConfigMissing, (r as CaptureResult.Failure).failure.error)
    }

    @Test fun manualPasteAndImageDraftStayUsableWhenUnconfigured() {
        val gw = unconfiguredGateway()
        // Manual transcript paste needs no credentials.
        val draft = gw.draftFromPastedText("第一句\n第二句", "手动课堂")
        assertEquals(2, draft.segments.size)
        // Image draft: OCR ConfigMissing, but the on-device draft stays editable.
        val image = gw.createImageStudyDraft("img".toByteArray(), "图片学习输入", onDeviceDraftText = "端侧蓝心草稿")
        assertEquals(CaptureError.ConfigMissing, image.ocrError)
        assertEquals("端侧蓝心草稿", image.initialEditableText())
        // Confirm is still the only path that yields an analysis-ready result.
        val confirmed = gw.confirmImageDraft(image, "用户编辑后的文本", "电磁感应")
        assertTrue(confirmed != null)
        assertEquals("用户编辑后的文本", confirmed!!.courseText)
    }
}
