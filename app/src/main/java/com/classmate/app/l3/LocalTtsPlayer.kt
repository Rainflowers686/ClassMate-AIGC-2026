package com.classmate.app.l3

/** Result of a real on-device TTS file synthesis. success=true ONLY when a non-empty file was written. */
data class TtsFileResult(val success: Boolean, val filePath: String = "", val sizeBytes: Long = 0L)

interface LocalTtsPlayer {
    fun canAttemptLocalPlayback(): Boolean
    fun speak(id: String, text: String): Boolean
    fun stop()
    fun release()

    /**
     * Synthesize [text] into an on-device audio file named [fileName] using the system TextToSpeech engine.
     * Calls [onResult] (possibly on a TTS worker thread) with success=true only when a non-empty file was
     * actually written. Default no-op (returns failure) for non-Android / test impls — callers then keep the
     * 听背文稿 text and never present a fake audio file.
     */
    fun synthesizeToFile(id: String, text: String, fileName: String, onResult: (TtsFileResult) -> Unit) {
        onResult(TtsFileResult(success = false))
    }
}

class NoOpLocalTtsPlayer(
    private val canAttempt: Boolean = true,
    private val speakResult: Boolean = true,
) : LocalTtsPlayer {
    override fun canAttemptLocalPlayback(): Boolean = canAttempt
    override fun speak(id: String, text: String): Boolean = speakResult && text.isNotBlank()
    override fun stop() = Unit
    override fun release() = Unit
}
