package com.classmate.core.official.ws

import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialTtsWsSessionTest {

    private class FakeTransport(private val openable: Boolean = true) : OfficialWsTransport {
        val textFrames = mutableListOf<String>()
        var closed = false
        var listener: OfficialWsListener? = null
        override fun open(url: String, headers: Map<String, String>, listener: OfficialWsListener): OfficialWsConnection? {
            if (!openable) return null
            this.listener = listener
            return object : OfficialWsConnection {
                override fun sendText(text: String): Boolean { textFrames += text; return true }
                override fun sendBinary(bytes: ByteArray): Boolean = true
                override fun close() { closed = true }
            }
        }
    }

    private val ready = OfficialWsConfig(baseUrl = OfficialTtsWsProtocol.DEFAULT_URL, appKey = "k", userId = "u")
    private val params = OfficialTtsWsProtocol.TtsParams(userId = "u", requestId = "r", systemTimeMs = 1L)
    private val request = OfficialTtsWsProtocol.TtsRequest(text = "你好世界", reqId = 1L)

    private fun audioFrame(pcm: ByteArray, status: Int): String =
        """{"error_code":0,"data":{"status":$status,"audio":"${Base64.getEncoder().encodeToString(pcm)}"}}"""

    @Test
    fun unconfiguredFallsBackSafely() {
        val errors = mutableListOf<String>()
        val ok = OfficialTtsWsSession(FakeTransport()).synthesize(OfficialWsConfig(), params, request, {}, { errors += it })
        assertFalse(ok)
        assertTrue(errors.single().contains("系统 TTS"))
    }

    @Test
    fun transportAbsentFallsBackSafely() {
        val errors = mutableListOf<String>()
        val ok = OfficialTtsWsSession(FakeTransport(openable = false)).synthesize(ready, params, request, {}, { errors += it })
        assertFalse(ok)
        assertTrue(errors.single().contains("系统 TTS"))
    }

    @Test
    fun openSendsRequestAndConcatenatesPcmUntilStatusTwo() {
        val transport = FakeTransport()
        val session = OfficialTtsWsSession(transport)
        var result: ByteArray? = null
        val ok = session.synthesize(ready, params, request, { result = it }, {})
        assertTrue(ok)

        transport.listener!!.onOpen()
        assertTrue("request JSON sent on open", transport.textFrames.single().contains("\"auf\":\"audio/L16;rate=24000\""))

        transport.listener!!.onText("""{"error_code":0,"error_msg":"success"}""") // handshake, no audio
        transport.listener!!.onText(audioFrame(byteArrayOf(1, 2), status = 1))
        transport.listener!!.onText(audioFrame(byteArrayOf(3, 4), status = 2))

        assertArrayEquals(byteArrayOf(1, 2, 3, 4), result)
        assertTrue("session closes after final slice", transport.closed)
    }

    @Test
    fun errorFrameReportsSafeFallback() {
        val transport = FakeTransport()
        val errors = mutableListOf<String>()
        OfficialTtsWsSession(transport).synthesize(ready, params, request, {}, { errors += it })
        transport.listener!!.onOpen()
        transport.listener!!.onText("""{"error_code":40010,"error_msg":"bad"}""")
        assertTrue(errors.single().contains("系统 TTS"))
    }
}
