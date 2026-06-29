package com.classmate.core.official.ws

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Official TTS WebSocket protocol (docId 1735) — URL/headers/request/response/WAV/segmentation, grounded in
 * the official接口文档. Never leaks the AppKey.
 */
class OfficialTtsWsProtocolTest {

    private val params = OfficialTtsWsProtocol.TtsParams(userId = "u32", requestId = "req-1", systemTimeMs = 1_700_000_000_000L)

    @Test
    fun urlHasTtsPathAndRequiredParams() {
        val url = OfficialTtsWsProtocol.buildUrl(OfficialTtsWsProtocol.DEFAULT_URL, params)
        assertTrue(url.startsWith("wss://api-ai.vivo.com.cn/tts?"))
        listOf("engineid=long_audio_synthesis_screen", "system_time=1700000000000", "user_id=u32", "model=", "product=", "package=", "client_version=", "system_version=", "sdk_version=", "android_version=", "requestId=req-1")
            .forEach { assertTrue("url missing $it", url.contains(it)) }
    }

    @Test
    fun headersCarryBearerAndGatewaySignatureButRedactKey() {
        val headers = OfficialTtsWsProtocol.authHeaders("SECRET_KEY")
        assertEquals("Bearer SECRET_KEY", headers["Authorization"])
        assertEquals("developers-aigc", headers[OfficialTtsWsProtocol.SIGNATURE_HEADER])
        val redacted = OfficialTtsWsProtocol.redactAuthValue(headers["Authorization"]!!)
        assertEquals("Bearer ***", redacted)
        assertFalse(redacted.contains("SECRET_KEY"))
    }

    @Test
    fun requestJsonBase64EncodesTextAndUsesL16_24k() {
        val json = OfficialTtsWsProtocol.buildRequestJson(OfficialTtsWsProtocol.TtsRequest(text = "你好", reqId = 123L))
        assertTrue(json.contains("\"auf\":\"audio/L16;rate=24000\""))
        assertTrue(json.contains("\"encoding\":\"utf8\""))
        assertTrue(json.contains("\"reqId\":123"))
        val expectedB64 = Base64.getEncoder().encodeToString("你好".toByteArray(StandardCharsets.UTF_8))
        assertTrue("text must be base64(utf8)", json.contains("\"text\":\"$expectedB64\""))
    }

    @Test
    fun responseParserHandlesStatusAudioErrorAndStop() {
        // Handshake / status-only frame (error_code 0, no data).
        assertTrue(OfficialTtsWsProtocol.parseChunk("""{"error_code":0,"error_msg":"success","sid":"x"}""") is OfficialTtsWsProtocol.TtsChunk.Status)

        // Audio slice (status 1, not last).
        val pcm = byteArrayOf(1, 2, 3, 4)
        val b64 = Base64.getEncoder().encodeToString(pcm)
        val mid = OfficialTtsWsProtocol.parseChunk("""{"error_code":0,"data":{"status":1,"audio":"$b64"}}""")
        assertTrue(mid is OfficialTtsWsProtocol.TtsChunk.Audio && !(mid as OfficialTtsWsProtocol.TtsChunk.Audio).isLast)
        assertTrue((mid as OfficialTtsWsProtocol.TtsChunk.Audio).pcm.contentEquals(pcm))

        // Final slice (status 2).
        val last = OfficialTtsWsProtocol.parseChunk("""{"error_code":0,"data":{"status":2,"audio":"$b64"}}""")
        assertTrue(last is OfficialTtsWsProtocol.TtsChunk.Audio && (last as OfficialTtsWsProtocol.TtsChunk.Audio).isLast)

        // Error frame.
        val err = OfficialTtsWsProtocol.parseChunk("""{"error_code":40010,"error_msg":"bad"}""")
        assertTrue(err is OfficialTtsWsProtocol.TtsChunk.Error)
        assertTrue((err as OfficialTtsWsProtocol.TtsChunk.Error).safeMessage.contains("系统 TTS"))

        assertTrue(OfficialTtsWsProtocol.parseChunk("not json") is OfficialTtsWsProtocol.TtsChunk.Ignored)
    }

    @Test
    fun longTextSegmentsRespect2048BytesBeforeBase64() {
        val short = OfficialTtsWsProtocol.segmentText("短文本")
        assertEquals(listOf("短文本"), short)
        // 2000 Chinese chars = 6000 UTF-8 bytes -> must split into >= 3 segments, none over 2048 bytes.
        val long = "字".repeat(2000)
        val segments = OfficialTtsWsProtocol.segmentText(long)
        assertTrue(segments.size >= 3)
        segments.forEach { assertTrue(it.toByteArray(StandardCharsets.UTF_8).size <= 2048) }
        assertEquals(long, segments.joinToString(""))
    }

    @Test
    fun wavWriterCreatesValidRiffWaveHeader() {
        val pcm = ByteArray(8) { it.toByte() }
        val wav = PcmWavWriter.toWav(pcm)
        assertEquals("RIFF", String(wav.copyOfRange(0, 4), Charsets.US_ASCII))
        assertEquals("WAVE", String(wav.copyOfRange(8, 12), Charsets.US_ASCII))
        assertEquals("fmt ", String(wav.copyOfRange(12, 16), Charsets.US_ASCII))
        assertEquals("data", String(wav.copyOfRange(36, 40), Charsets.US_ASCII))
        assertEquals(44 + pcm.size, wav.size)
        // sampleRate (offset 24, little-endian) = 24000.
        val sr = (wav[24].toInt() and 0xFF) or ((wav[25].toInt() and 0xFF) shl 8) or ((wav[26].toInt() and 0xFF) shl 16) or ((wav[27].toInt() and 0xFF) shl 24)
        assertEquals(24000, sr)
    }
}
