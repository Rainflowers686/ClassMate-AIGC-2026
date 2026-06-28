package com.classmate.app.l3

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale

class AndroidLocalTtsPlayer(context: Context) : LocalTtsPlayer, TextToSpeech.OnInitListener {
    private val appContext: Context = context.applicationContext

    @Volatile
    private var ready: Boolean = false
    private val tts: TextToSpeech? = runCatching {
        TextToSpeech(appContext, this)
    }.getOrNull()

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            // Chinese first; fall back to the device default if a Chinese voice is unavailable.
            runCatching {
                val zh = tts?.setLanguage(Locale.CHINESE)
                if (zh == TextToSpeech.LANG_MISSING_DATA || zh == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }
        }
    }

    override fun canAttemptLocalPlayback(): Boolean = tts != null

    override fun speak(id: String, text: String): Boolean {
        if (!ready || text.isBlank()) return false
        return tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS
    }

    /** Real file synthesis: TextToSpeech.synthesizeToFile -> app-private classmate_tts/<fileName>. */
    override fun synthesizeToFile(id: String, text: String, fileName: String, onResult: (TtsFileResult) -> Unit) {
        val engine = tts
        if (!ready || engine == null || text.isBlank()) {
            onResult(TtsFileResult(success = false))
            return
        }
        val dir = File(appContext.filesDir, "classmate_tts").apply { mkdirs() }
        val outFile = File(dir, fileName)
        runCatching { if (outFile.exists()) outFile.delete() }

        fun finish(success: Boolean) {
            val ok = success && outFile.exists() && outFile.length() > 0L
            if (!ok) runCatching { if (outFile.exists()) outFile.delete() }
            onResult(
                if (ok) TtsFileResult(success = true, filePath = outFile.absolutePath, sizeBytes = outFile.length())
                else TtsFileResult(success = false),
            )
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { if (utteranceId == id) finish(true) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { if (utteranceId == id) finish(false) }
            override fun onError(utteranceId: String?, errorCode: Int) { if (utteranceId == id) finish(false) }
        })

        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id) }
        val result = runCatching { engine.synthesizeToFile(text, params, outFile, id) }.getOrDefault(TextToSpeech.ERROR)
        if (result != TextToSpeech.SUCCESS) finish(false)
    }

    override fun stop() {
        runCatching { tts?.stop() }
    }

    override fun release() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
    }
}
