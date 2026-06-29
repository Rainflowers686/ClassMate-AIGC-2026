package com.classmate.app.asr

/**
 * Pure, Android-free helpers for system speech-recognition readiness + error wording, so they are fully
 * unit-testable. The int codes match `android.speech.SpeechRecognizer.ERROR_*` (stable public constants).
 * The user-facing strings are honest next-steps, never a raw error code or provider/debug token.
 */
data class SpeechRecognitionReadiness(
    val recordAudioGranted: Boolean,
    val recognizerAvailable: Boolean,
    val locale: String = "zh-CN",
) {
    val ready: Boolean get() = recordAudioGranted && recognizerAvailable

    /** What the user should do next. */
    fun userGuidance(): String = when {
        ready -> "系统语音识别就绪，可边录边转写。"
        !recordAudioGranted && !recognizerAvailable ->
            "请先授予录音权限；若系统仍未提供语音识别服务，可手动粘贴或导入字幕，录音会照常保存。"
        !recordAudioGranted ->
            "请先授予录音权限再开始实时转写；也可手动粘贴或导入字幕。"
        else ->
            "当前系统未提供语音识别服务，可在系统设置启用语音服务，或手动粘贴/导入字幕；录音会照常保存。"
    }

    /** One-line readiness summary for the developer diagnostics page (technical wording allowed there). */
    fun diagnosticsLine(): String =
        "RECORD_AUDIO=${if (recordAudioGranted) "granted" else "denied"} · " +
            "recognizer=${if (recognizerAvailable) "available" else "unavailable"} · locale=$locale"
}

object SpeechRecognitionErrorMapper {
    // android.speech.SpeechRecognizer.ERROR_* — stable, documented values.
    const val ERROR_NETWORK_TIMEOUT = 1
    const val ERROR_NETWORK = 2
    const val ERROR_AUDIO = 3
    const val ERROR_SERVER = 4
    const val ERROR_CLIENT = 5
    const val ERROR_SPEECH_TIMEOUT = 6
    const val ERROR_NO_MATCH = 7
    const val ERROR_RECOGNIZER_BUSY = 8
    const val ERROR_INSUFFICIENT_PERMISSIONS = 9
    const val ERROR_SERVER_DISCONNECTED = 11
    const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
    const val ERROR_LANGUAGE_UNAVAILABLE = 13

    /** A SpeechRecognizer error code -> friendly Chinese with a next step. No raw code reaches the user. */
    fun message(code: Int): String = when (code) {
        ERROR_INSUFFICIENT_PERMISSIONS -> "未授予录音权限，请在系统设置授权后重试；也可手动记录或导入转写稿。"
        ERROR_NETWORK, ERROR_NETWORK_TIMEOUT, ERROR_SERVER_DISCONNECTED ->
            "系统语音识别网络异常，请改用手动转写或字幕导入，录音会照常保存。"
        ERROR_RECOGNIZER_BUSY -> "系统语音识别忙，请稍后重试。"
        ERROR_AUDIO -> "麦克风读取出错，请检查录音权限或改用手动转写。"
        ERROR_SERVER, ERROR_CLIENT -> "系统语音识别出错，请重试或改用手动转写。"
        ERROR_SPEECH_TIMEOUT, ERROR_NO_MATCH -> "没有听清，请靠近麦克风再说，或手动补充。"
        ERROR_LANGUAGE_NOT_SUPPORTED, ERROR_LANGUAGE_UNAVAILABLE ->
            "当前语言暂不支持语音识别，可切换系统语言或改用手动转写。"
        else -> "系统语音识别暂不可用，可手动粘贴或导入字幕，录音会照常保存。"
    }
}
