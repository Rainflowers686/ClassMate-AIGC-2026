package com.classmate.app.asr

import com.classmate.app.l3.TtsFileResult
import com.classmate.core.official.ws.OfficialTtsWsProtocol
import com.classmate.core.official.ws.OfficialTtsWsSession
import com.classmate.core.official.ws.OfficialWsConfig
import com.classmate.core.official.ws.OfficialWsTransport
import com.classmate.core.official.ws.PcmWavWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

/**
 * Blocking wrapper around the official TTS WebSocket: segments the text (<=2048 UTF-8 bytes), synthesizes
 * each segment over a WS session, concatenates the PCM, and writes a 24k/16bit/mono WAV into the app-private
 * TTS dir. Returns failure (never throws) on missing config / error / timeout so the caller falls back to the
 * system TextToSpeech. Call OFF the main thread — it waits on the WS callbacks via a latch.
 *
 * Never logs or returns the AppKey; the config's value is used only for the handshake header.
 */
class OfficialTtsProvider(
    private val ttsDir: File,
    private val transport: OfficialWsTransport = OkHttpOfficialWsTransport(),
    private val timeoutMs: Long = 30_000,
) {

    fun synthesizeToFile(config: OfficialWsConfig, text: String, fileName: String): TtsFileResult {
        val pcm = synthesizePcm(config, text)
        if (pcm == null || pcm.isEmpty()) return TtsFileResult(success = false)
        val wav = PcmWavWriter.toWav(pcm)
        return try {
            if (!ttsDir.exists()) ttsDir.mkdirs()
            val out = File(ttsDir, fileName)
            out.writeBytes(wav)
            if (out.length() > 0L) {
                TtsFileResult(success = true, filePath = out.absolutePath, sizeBytes = out.length())
            } else {
                runCatching { out.delete() }
                TtsFileResult(success = false)
            }
        } catch (_: Throwable) {
            TtsFileResult(success = false)
        }
    }

    private fun synthesizePcm(config: OfficialWsConfig, text: String): ByteArray? {
        if (!config.isConfigured || text.isBlank()) return null
        val segments = OfficialTtsWsProtocol.segmentText(text)
        if (segments.isEmpty()) return null
        val merged = ByteArrayOutputStream()
        for (segment in segments) {
            val params = OfficialTtsWsProtocol.TtsParams(
                userId = config.userId.ifBlank { "classmateuser" },
                requestId = UUID.randomUUID().toString(),
                systemTimeMs = System.currentTimeMillis(),
            )
            val request = OfficialTtsWsProtocol.TtsRequest(
                text = segment,
                reqId = abs(Random.nextLong()) % 1_000_000_000L,
            )
            val latch = CountDownLatch(1)
            var segmentPcm: ByteArray? = null
            val session = OfficialTtsWsSession(transport)
            val started = session.synthesize(
                config,
                params,
                request,
                onComplete = { segmentPcm = it; latch.countDown() },
                onError = { latch.countDown() },
            )
            if (!started) return null
            val finished = runCatching { latch.await(timeoutMs, TimeUnit.MILLISECONDS) }.getOrDefault(false)
            session.cancel()
            val pcm = segmentPcm
            if (!finished || pcm == null || pcm.isEmpty()) return null
            merged.write(pcm)
        }
        return merged.toByteArray().takeIf { it.isNotEmpty() }
    }
}
