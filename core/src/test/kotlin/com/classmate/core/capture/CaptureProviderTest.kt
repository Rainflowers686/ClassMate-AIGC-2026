package com.classmate.core.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A scripted transport: routes by URL path substring; records nothing sensitive. */
private class FakeCaptureTransport(
    private val byPath: Map<String, CaptureHttpResponse>,
    private val default: CaptureHttpResponse = CaptureHttpResponse(200, "{}"),
) : CaptureTransport {
    val calls = mutableListOf<String>()
    private fun route(url: String): CaptureHttpResponse {
        calls += url
        return byPath.entries.firstOrNull { url.contains(it.key) }?.value ?: default
    }
    override fun postForm(url: String, headers: Map<String, String>, form: Map<String, String>, timeoutMs: Long) = route(url)
    override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long) = route(url)
    override fun postMultipart(url: String, headers: Map<String, String>, fileField: String, fileBytes: ByteArray, fileName: String, timeoutMs: Long) = route(url)
}

private fun configured() = CaptureProviderConfig(appId = "demoid", appKey = "demokey")

class CaptureProviderTest {

    // ---- error mapping ---------------------------------------------------------------------------

    @Test fun httpStatusMapsToCaptureError() {
        assertEquals(CaptureError.AuthFailed, CaptureError.fromHttpStatus(401))
        assertEquals(CaptureError.AuthFailed, CaptureError.fromHttpStatus(403))
        assertEquals(CaptureError.QuotaExceeded, CaptureError.fromHttpStatus(429))
        assertEquals(CaptureError.ServiceUnavailable, CaptureError.fromHttpStatus(500))
        assertEquals(CaptureError.AuthFailed, CaptureError.fromHttpStatus(400, """{"message":"invalid api-key"}"""))
    }

    @Test fun asrBusinessCodesMap() {
        assertEquals(CaptureError.AudioTooLong, CaptureError.fromAsrBusinessCode(10002))
        assertEquals(CaptureError.InvalidAudio, CaptureError.fromAsrBusinessCode(10101))
        assertEquals(CaptureError.ServiceUnavailable, CaptureError.fromAsrBusinessCode(10402))
    }

    @Test fun ocrErrorCodesMap() {
        assertEquals(CaptureError.ParseFailed, CaptureError.fromOcrErrorCode(1))
        assertEquals(CaptureError.UnsupportedFormat, CaptureError.fromOcrErrorCode(2))
    }

    @Test fun everyErrorHasGentleMessage() {
        CaptureError.values().forEach { e ->
            val msg = e.userMessageZh()
            assertTrue("blank message for $e", msg.isNotBlank())
            // Gentle / honest: never blames, never over-claims.
            listOf("已完成实时", "自动听课", "替代听脑", "崩溃", "失败！").forEach { bad ->
                assertTrue("message for $e must not contain $bad", !msg.contains(bad))
            }
        }
    }

    // ---- ASR: ConfigMissing never crashes --------------------------------------------------------

    @Test fun asrConfigMissingReturnsFailureNotCrash() {
        val provider = VivoAsrProvider() // ABSENT config + NotConfigured transport
        val r = provider.transcribeLongAudio("audio".toByteArray(), "a.mp3", "auto")
        assertTrue(r is CaptureResult.Failure)
        assertEquals(CaptureError.ConfigMissing, (r as CaptureResult.Failure).failure.error)
    }

    // ---- ASR: full task-flow with a scripted transport -------------------------------------------

    @Test fun asrLongAudioFlowProducesSegments() {
        val transport = FakeCaptureTransport(
            mapOf(
                "/lasr/create" to CaptureHttpResponse(200, """{"code":0,"data":{"audio_id":"a1"}}"""),
                "/lasr/upload" to CaptureHttpResponse(200, """{"code":0,"data":{"slices":1,"total":1}}"""),
                "/lasr/run" to CaptureHttpResponse(200, """{"code":0,"data":{"task_id":"t1"}}"""),
                "/lasr/progress" to CaptureHttpResponse(200, """{"code":0,"data":{"progress":100}}"""),
                "/lasr/result" to CaptureHttpResponse(200, """{"code":0,"data":{"result":[{"onebest":"楞次定律","bg":0,"ed":2190,"speaker":1}]}}"""),
            ),
        )
        val provider = VivoAsrProvider(config = configured(), transport = transport, pollSleepMs = 0, sleeper = {})
        val r = provider.transcribeLongAudio("audiobytes".toByteArray(), "lecture.mp3", "auto")
        assertTrue(r is CaptureResult.Success)
        val result = (r as CaptureResult.Success).value
        assertEquals(1, result.segments.size)
        assertEquals("楞次定律", result.segments[0].text)
        assertEquals(0L, result.segments[0].startMs)
        assertTrue(transport.calls.any { it.contains("/lasr/create") } && transport.calls.any { it.contains("/lasr/result") })
    }

    @Test fun asrBusinessErrorMapsThroughResult() {
        val provider = VivoAsrProvider(config = configured())
        val r = provider.parseAsrResult(CaptureHttpResponse(200, """{"code":10402,"data":{}}"""))
        assertTrue(r is CaptureResult.Failure)
        assertEquals(CaptureError.ServiceUnavailable, (r as CaptureResult.Failure).failure.error)
        assertEquals("10402", r.failure.vendorCode)
    }

    // ---- OCR -------------------------------------------------------------------------------------

    @Test fun ocrConfigMissingReturnsFailure() {
        val r = VivoOcrProvider().recognize("img".toByteArray())
        assertEquals(CaptureError.ConfigMissing, (r as CaptureResult.Failure).failure.error)
    }

    @Test fun ocrParsesBlocksAndOrdersByPosition() {
        val provider = VivoOcrProvider(config = configured())
        // Two blocks; the lower-on-page one is listed first to prove top→bottom ordering.
        val body = """{"error_code":0,"result":{"OCR":[
            {"words":"第二行","location":{"top_left":{"x":10.0,"y":200.0}}},
            {"words":"第一行","location":{"top_left":{"x":10.0,"y":50.0}}}
        ],"angle":0}}"""
        val r = provider.parseOcr(CaptureHttpResponse(200, body))
        assertTrue(r is CaptureResult.Success)
        assertEquals("第一行\n第二行", (r as CaptureResult.Success).value.normalizedText())
    }

    @Test fun ocrImageErrorMapsToUnsupportedFormat() {
        val provider = VivoOcrProvider(config = configured())
        val r = provider.parseOcr(CaptureHttpResponse(200, """{"error_code":2,"error_msg":"image error"}"""))
        assertEquals(CaptureError.UnsupportedFormat, (r as CaptureResult.Failure).failure.error)
    }

    // ---- reserved retrieval providers: ConfigMissing ---------------------------------------------

    @Test fun reservedRetrievalProvidersAreConfigMissing() {
        assertEquals(CaptureError.ConfigMissing, (VivoTextSimilarityProvider().similarity("q", listOf("a")) as CaptureResult.Failure).failure.error)
        assertEquals(CaptureError.ConfigMissing, (VivoQueryRewriteProvider().rewrite("q") as CaptureResult.Failure).failure.error)
        assertEquals(CaptureError.ConfigMissing, (VivoEmbeddingProvider().embed(listOf("a")) as CaptureResult.Failure).failure.error)
        // Empty candidates short-circuit to success without touching the network.
        assertTrue(VivoTextSimilarityProvider(config = configured()).similarity("q", emptyList()) is CaptureResult.Success)
    }

    @Test fun asrResultEmptyIsParseFailed() {
        val provider = VivoAsrProvider(config = configured())
        val r = provider.parseAsrResult(CaptureHttpResponse(200, """{"code":0,"data":{"result":[]}}"""))
        assertNull((r as? CaptureResult.Success)?.value)
        assertEquals(CaptureError.ParseFailed, (r as CaptureResult.Failure).failure.error)
    }
}
