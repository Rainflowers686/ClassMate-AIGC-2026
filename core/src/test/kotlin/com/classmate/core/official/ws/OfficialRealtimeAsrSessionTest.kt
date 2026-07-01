package com.classmate.core.official.ws

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialRealtimeAsrSessionTest {

    /** A fake transport that records frames and lets the test drive the listener callbacks. */
    private class FakeTransport(private val openable: Boolean = true) : OfficialWsTransport {
        val textFrames = mutableListOf<String>()
        val binaryFrames = mutableListOf<ByteArray>()
        var closed = false
        var listener: OfficialWsListener? = null

        override fun open(url: String, headers: Map<String, String>, listener: OfficialWsListener): OfficialWsConnection? {
            if (!openable) return null
            this.listener = listener
            return object : OfficialWsConnection {
                override fun sendText(text: String): Boolean { textFrames += text; return true }
                override fun sendBinary(bytes: ByteArray): Boolean { binaryFrames += bytes; return true }
                override fun close() { closed = true }
            }
        }
    }

    private val ready = OfficialWsConfig(baseUrl = "ws://api-ai.vivo.com.cn/asr/v2", appKey = "k", userId = "u")

    @Test
    fun unconfiguredReturnsFalseAndReportsSafeFallback() {
        val errors = mutableListOf<String>()
        val started = OfficialRealtimeAsrSession(FakeTransport()).start(
            OfficialWsConfig(), OfficialAsrWsProtocol.AsrEngine.REALTIME_SHORT, "r", 1L, {}, {}, { errors += it },
        )
        assertFalse(started)
        assertTrue(errors.single().contains("录音会保留"))
    }

    @Test
    fun transportAbsentFallsBackSafely() {
        val errors = mutableListOf<String>()
        val started = OfficialRealtimeAsrSession(FakeTransport(openable = false)).start(
            ready, OfficialAsrWsProtocol.AsrEngine.REALTIME_SHORT, "r", 1L, {}, {}, { errors += it },
        )
        assertFalse(started)
        assertTrue(errors.single().contains("录音会保留"))
    }

    @Test
    fun openSendsStartFrameStreamsPcmAndEndsOnStop() {
        val transport = FakeTransport()
        val session = OfficialRealtimeAsrSession(transport)
        val partials = mutableListOf<String>()
        val finals = mutableListOf<String>()
        val started = session.start(ready, OfficialAsrWsProtocol.AsrEngine.REALTIME_SHORT, "req-1", 1L, { partials += it }, { finals += it }, {})
        assertTrue(started)

        transport.listener!!.onOpen()
        assertTrue("start frame sent on open", transport.textFrames.single().contains("\"type\":\"started\""))

        session.feedPcm(ByteArray(1280) { 1 })
        assertEquals(1, transport.binaryFrames.size)

        transport.listener!!.onText("""{"action":"result","type":"asr","data":{"text":"你好","is_last":false},"code":0}""")
        transport.listener!!.onText("""{"action":"result","type":"asr","data":{"text":"你好世界","is_last":true},"code":0}""")
        assertEquals(listOf("你好"), partials)
        assertEquals(listOf("你好世界"), finals)

        session.stop()
        // The last binary frame is the end marker, and the connection is closed.
        assertEquals("--end--", String(transport.binaryFrames.last()))
        assertTrue(transport.closed)
    }

    @Test
    fun cancelClosesWithoutEndMarker() {
        val transport = FakeTransport()
        val session = OfficialRealtimeAsrSession(transport)
        session.start(ready, OfficialAsrWsProtocol.AsrEngine.REALTIME_SHORT, "r", 1L, {}, {}, {})
        transport.listener!!.onOpen()
        session.cancel()
        assertTrue(transport.closed)
        assertFalse("no end marker on cancel", transport.binaryFrames.any { String(it) == "--end--" })
    }
}
