package com.classmate.app.asr

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * Captures raw 16 kHz / 16-bit / mono PCM frames for the official WebSocket ASR (the format the demos
 * require: `16k/16b 单声道 PCM`). Emits ~1280-byte frames (the demo's 40 ms chunk). This is a separate
 * pipeline from the MediaRecorder file recording, so the two never share AudioRecord state.
 *
 * The caller MUST hold RECORD_AUDIO before start(); start() returns false (never crashes) when the mic is
 * unavailable or busy, so the caller can fall back to the system SpeechRecognizer.
 */
class PcmAudioCapture(private val onFrame: (ByteArray) -> Unit) {

    @Volatile private var recording = false
    private var record: AudioRecord? = null
    private var worker: Thread? = null

    @SuppressLint("MissingPermission") // caller gates on RECORD_AUDIO
    fun start(): Boolean {
        if (recording) return true
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) return false
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, FRAME_BYTES * 4),
            )
        } catch (_: Throwable) {
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { rec.release() }
            return false
        }
        record = rec
        recording = true
        runCatching { rec.startRecording() }
        worker = Thread {
            val buffer = ByteArray(FRAME_BYTES)
            while (recording) {
                val read = runCatching { rec.read(buffer, 0, buffer.size) }.getOrDefault(-1)
                if (read > 0) onFrame(if (read == buffer.size) buffer.copyOf() else buffer.copyOf(read))
                else if (read < 0) break
            }
        }.apply { isDaemon = true; start() }
        return true
    }

    fun stop() {
        recording = false
        runCatching { worker?.join(300) }
        runCatching { record?.stop() }
        runCatching { record?.release() }
        worker = null
        record = null
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_BYTES = 1280 // 40 ms @ 16k/16bit mono, matching the official demo
    }
}
