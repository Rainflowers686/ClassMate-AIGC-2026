package com.classmate.core.official.ws

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-1: the official WebSocket ASR protocol (built from the official Python demos) must produce correct
 * URLs / frames, parse partial/final/error events, and NEVER leak the AppKey.
 */
class OfficialAsrWsProtocolTest {

    private fun params(engine: OfficialAsrWsProtocol.AsrEngine) = OfficialAsrWsProtocol.AsrParams(
        engine = engine,
        userId = "user-123",
        requestId = "req-abc",
        systemTimeMs = 1_700_000_000_000L,
    )

    @Test
    fun buildsUrlWithEngineIdAndParams() {
        val url = OfficialAsrWsProtocol.buildUrl("ws://api-ai.vivo.com.cn/asr/v2", params(OfficialAsrWsProtocol.AsrEngine.REALTIME_SHORT))
        assertTrue(url.startsWith("ws://api-ai.vivo.com.cn/asr/v2?"))
        assertTrue(url.contains("engineid=shortasrinput"))
        assertTrue(url.contains("user_id=user-123"))
        assertTrue(url.contains("system_time=1700000000000"))
        assertTrue(url.contains("requestId=req-abc"))
        // dictation uses the long-listen engine id.
        val dict = OfficialAsrWsProtocol.buildUrl("ws://api-ai.vivo.com.cn/asr/v2", params(OfficialAsrWsProtocol.AsrEngine.DICTATION))
        assertTrue(dict.contains("engineid=longasrlisten"))
    }

    @Test
    fun authHeaderCarriesBearerButRedactionHidesKey() {
        val (name, value) = OfficialAsrWsProtocol.authHeader("SECRET_APP_KEY")
        assertEquals("Authorization", name)
        assertEquals("Bearer SECRET_APP_KEY", value)
        // Diagnostics rendering must never reveal the key.
        val redacted = OfficialAsrWsProtocol.redactAuthValue(value)
        assertEquals("Bearer ***", redacted)
        assertFalse(redacted.contains("SECRET_APP_KEY"))
    }

    @Test
    fun startFrameIsValidJsonWithPcmAndRequestId() {
        val frame = OfficialAsrWsProtocol.buildStartFrame(requestId = "req-abc")
        assertTrue(frame.contains("\"type\":\"started\""))
        assertTrue(frame.contains("\"request_id\":\"req-abc\""))
        assertTrue(frame.contains("\"audio_type\":\"pcm\""))
    }

    @Test
    fun endAndCloseMarkersMatchDemo() {
        assertEquals("--end--", String(OfficialAsrWsProtocol.END_MARKER))
        assertEquals("--close--", String(OfficialAsrWsProtocol.CLOSE_MARKER))
    }

    @Test
    fun parsesPartialFinalErrorAndVad() {
        val partial = OfficialAsrWsProtocol.parseEvent("""{"action":"result","type":"asr","data":{"text":"你好","is_last":false},"code":0}""")
        assertTrue(partial is OfficialAsrWsProtocol.AsrEvent.Partial && partial.text == "你好")

        val final = OfficialAsrWsProtocol.parseEvent("""{"action":"result","type":"asr","data":{"text":"你好世界","is_last":true},"code":0}""")
        assertTrue(final is OfficialAsrWsProtocol.AsrEvent.Final && final.text == "你好世界")

        val error = OfficialAsrWsProtocol.parseEvent("""{"action":"error","code":40001,"sid":"x"}""")
        assertTrue(error is OfficialAsrWsProtocol.AsrEvent.Error)
        // The error message is user-safe and does not echo a raw body.
        assertTrue((error as OfficialAsrWsProtocol.AsrEvent.Error).safeMessage.contains("系统实时转写"))

        assertTrue(OfficialAsrWsProtocol.parseEvent("""{"action":"vad","code":1}""") is OfficialAsrWsProtocol.AsrEvent.Vad)
        assertTrue(OfficialAsrWsProtocol.parseEvent("not json") is OfficialAsrWsProtocol.AsrEvent.Ignored)
    }

    @Test
    fun readinessPolicyIsHonest() {
        val none = OfficialWsConfig()
        assertEquals(OfficialWsReadiness.TRANSPORT_MISSING, OfficialWsReadinessPolicy.evaluate(none, transportPresent = false))
        assertEquals(OfficialWsReadiness.CONFIG_MISSING, OfficialWsReadinessPolicy.evaluate(none, transportPresent = true))
        val ready = OfficialWsConfig(baseUrl = "ws://x/asr/v2", appKey = "k", userId = "u")
        assertEquals(OfficialWsReadiness.READY, OfficialWsReadinessPolicy.evaluate(ready, transportPresent = true))
    }
}
