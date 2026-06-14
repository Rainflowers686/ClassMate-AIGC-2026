package com.classmate.app.asr

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Real [SpeechRecognizerEngine] backed by the system [SpeechRecognizer]. It performs continuous
 * dictation by restarting after each result/no-match. It does NOT save raw audio, does NOT upload
 * audio itself, does NOT record in the background (stopped on [stop]/[destroy]), and does NOT do
 * voiceprint identification. Whether recognition runs on-device or via the system's cloud is decided
 * by the OEM recognizer, not by ClassMate.
 *
 * Must be created/used on the main thread. Not unit-tested (Android dependency); the state machine in
 * [AsrSessionController] is tested with a fake instead.
 */
class AndroidSpeechRecognizerClient(private val context: Context) : SpeechRecognizerEngine {

    private var recognizer: SpeechRecognizer? = null
    private var listener: AsrEventListener? = null
    private var active = false

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(listener: AsrEventListener) {
        this.listener = listener
        active = true
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        }
        beginListening()
    }

    override fun stop() {
        active = false
        runCatching { recognizer?.stopListening() }
        runCatching { recognizer?.cancel() }
    }

    override fun destroy() {
        active = false
        runCatching { recognizer?.destroy() }
        recognizer = null
        listener = null
    }

    private fun beginListening() {
        if (!active) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // prefer on-device when available
            }
        }
        runCatching { recognizer?.startListening(intent) }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { listener?.onListening() }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {} // not stored
        override fun onEndOfSpeech() { listener?.onEndOfSpeech() }

        override fun onPartialResults(partialResults: Bundle?) {
            firstText(partialResults)?.let { listener?.onPartial(it) }
        }

        override fun onResults(results: Bundle?) {
            val text = firstText(results)
            val confidence = firstConfidence(results)
            if (text != null) listener?.onFinal(text, confidence)
            // Continuous dictation: listen again for the next utterance.
            if (active) beginListening() else listener?.onListening()
        }

        override fun onError(error: Int) {
            // Transient errors during continuous dictation: just restart, do not surface as a failure.
            if (active && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                beginListening()
                return
            }
            listener?.onError(messageFor(error))
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun firstText(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private fun firstConfidence(bundle: Bundle?): Double? =
        bundle?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()?.toDouble()

    private fun messageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "未授权麦克风，仍可手动记录或导入转写稿。"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "系统语音识别不可用（网络问题），请改用手动转写或字幕导入。"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "系统语音识别忙，请稍后重试。"
        SpeechRecognizer.ERROR_AUDIO -> "麦克风读取出错，请重试或改用手动转写。"
        else -> "系统语音识别出错，请改用手动转写或字幕导入。"
    }
}
