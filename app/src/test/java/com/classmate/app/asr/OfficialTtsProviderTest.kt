package com.classmate.app.asr

import com.classmate.core.official.ws.OfficialWsConfig
import com.classmate.core.official.ws.OfficialWsConnection
import com.classmate.core.official.ws.OfficialWsListener
import com.classmate.core.official.ws.OfficialWsTransport
import com.classmate.core.official.ws.OfficialTtsWsProtocol
import java.io.File
import java.nio.file.Files
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialTtsProviderTest {

    private val ready = OfficialWsConfig(baseUrl = OfficialTtsWsProtocol.DEFAULT_URL, appKey = "k", userId = "u")

    /** Transport that, once the session has sent its request, replies with one final audio frame. onOpen is
     *  fired on a separate thread so the session has assigned its connection first. */
    private class FakeTtsTransport(private val pcm: ByteArray, private val failOpen: Boolean = false) : OfficialWsTransport {
        override fun open(url: String, headers: Map<String, String>, listener: OfficialWsListener): OfficialWsConnection? {
            if (failOpen) return null
            val conn = object : OfficialWsConnection {
                override fun sendText(text: String): Boolean {
                    listener.onText("""{"error_code":0,"data":{"status":2,"audio":"${Base64.getEncoder().encodeToString(pcm)}"}}""")
                    return true
                }
                override fun sendBinary(bytes: ByteArray): Boolean = true
                override fun close() = Unit
            }
            Thread { Thread.sleep(5); listener.onOpen() }.start()
            return conn
        }
    }

    private fun tempDir(): File = Files.createTempDirectory("cm-tts").toFile()

    @Test
    fun synthesizesAudioIntoNonEmptyWavFile() {
        val pcm = ByteArray(64) { (it % 7).toByte() }
        val dir = tempDir()
        val provider = OfficialTtsProvider(ttsDir = dir, transport = FakeTtsTransport(pcm), timeoutMs = 3_000)
        val result = provider.synthesizeToFile(ready, "你好世界，这是官方语音合成测试。", "official.wav")
        assertTrue("official synthesis should succeed", result.success)
        val file = File(result.filePath)
        assertTrue(file.exists())
        assertTrue("WAV is non-empty (header + pcm)", file.length() > 44L)
        assertEquals("RIFF", String(file.readBytes().copyOfRange(0, 4), Charsets.US_ASCII))
    }

    @Test
    fun unconfiguredReturnsFailureNoFile() {
        val result = OfficialTtsProvider(ttsDir = tempDir(), transport = FakeTtsTransport(ByteArray(8)), timeoutMs = 1_000)
            .synthesizeToFile(OfficialWsConfig(), "text", "x.wav")
        assertFalse(result.success)
        assertEquals("", result.filePath)
    }

    @Test
    fun transportUnavailableReturnsFailure() {
        val result = OfficialTtsProvider(ttsDir = tempDir(), transport = FakeTtsTransport(ByteArray(8), failOpen = true), timeoutMs = 1_000)
            .synthesizeToFile(ready, "text", "x.wav")
        assertFalse(result.success)
    }
}
