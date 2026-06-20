package com.classmate.app.l3

interface LocalTtsPlayer {
    fun canAttemptLocalPlayback(): Boolean
    fun speak(id: String, text: String): Boolean
    fun stop()
    fun release()
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
