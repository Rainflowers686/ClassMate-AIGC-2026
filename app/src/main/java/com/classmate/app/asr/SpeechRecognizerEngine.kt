package com.classmate.app.asr

/** Callbacks the recognizer seam delivers to the ViewModel; mapped onto [AsrSessionController]. */
interface AsrEventListener {
    fun onListening()
    fun onPartial(text: String)
    fun onFinal(text: String, confidence: Double?)
    fun onEndOfSpeech()
    fun onError(message: String)
}

/**
 * Seam over the system speech recognizer so the state machine can be unit-tested with a fake and the
 * real Android [android.speech.SpeechRecognizer] stays at the edge. Implementations must NOT persist or
 * upload audio.
 */
interface SpeechRecognizerEngine {
    fun isAvailable(): Boolean
    fun start(listener: AsrEventListener)
    fun stop()
    fun destroy()
}
