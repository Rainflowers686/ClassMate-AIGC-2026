package com.classmate.app.l3

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidLocalTtsPlayer(context: Context) : LocalTtsPlayer, TextToSpeech.OnInitListener {
    @Volatile
    private var ready: Boolean = false
    private val tts: TextToSpeech? = runCatching {
        TextToSpeech(context.applicationContext, this)
    }.getOrNull()

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching { tts?.language = Locale.getDefault() }
        }
    }

    override fun canAttemptLocalPlayback(): Boolean = tts != null

    override fun speak(id: String, text: String): Boolean {
        if (!ready || text.isBlank()) return false
        return tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS
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
