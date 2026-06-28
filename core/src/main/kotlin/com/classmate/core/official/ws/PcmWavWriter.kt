package com.classmate.core.official.ws

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wraps raw little-endian PCM samples in a canonical RIFF/WAVE container so the official TTS audio plays in
 * any player. Pure + testable. Defaults match the official TTS output: 24 kHz / 16-bit / mono.
 */
object PcmWavWriter {

    fun toWav(
        pcm: ByteArray,
        sampleRate: Int = OfficialTtsWsProtocol.SAMPLE_RATE,
        channels: Int = OfficialTtsWsProtocol.CHANNELS,
        bitsPerSample: Int = OfficialTtsWsProtocol.BITS_PER_SAMPLE,
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm.size
        val out = ByteArrayOutputStream(44 + dataSize)

        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        out.write(le32(36 + dataSize))
        out.write("WAVE".toByteArray(Charsets.US_ASCII))

        out.write("fmt ".toByteArray(Charsets.US_ASCII))
        out.write(le32(16))                 // PCM fmt chunk size
        out.write(le16(1))                  // audioFormat = 1 (PCM)
        out.write(le16(channels))
        out.write(le32(sampleRate))
        out.write(le32(byteRate))
        out.write(le16(blockAlign))
        out.write(le16(bitsPerSample))

        out.write("data".toByteArray(Charsets.US_ASCII))
        out.write(le32(dataSize))
        out.write(pcm)
        return out.toByteArray()
    }

    private fun le32(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun le16(value: Int): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()
}
