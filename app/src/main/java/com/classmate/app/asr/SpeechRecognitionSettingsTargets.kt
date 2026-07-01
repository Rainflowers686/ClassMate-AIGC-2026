package com.classmate.app.asr

/**
 * Android-free descriptor list for guiding users to system speech settings. The UI turns these action
 * strings into Intents at runtime; tests can verify the fallback order without invoking Android APIs.
 */
data class SpeechRecognitionSettingsTarget(
    val action: String,
    val label: String,
    val requiresAppPackageUri: Boolean = false,
)

object SpeechRecognitionSettingsTargets {
    const val ACTION_VOICE_INPUT_SETTINGS: String = "android.settings.VOICE_INPUT_SETTINGS"
    const val ACTION_INPUT_METHOD_SETTINGS: String = "android.settings.INPUT_METHOD_SETTINGS"
    const val ACTION_APPLICATION_DETAILS_SETTINGS: String = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val ACTION_SETTINGS: String = "android.settings.SETTINGS"

    fun ordered(): List<SpeechRecognitionSettingsTarget> = listOf(
        SpeechRecognitionSettingsTarget(ACTION_VOICE_INPUT_SETTINGS, "语音输入设置"),
        SpeechRecognitionSettingsTarget(ACTION_INPUT_METHOD_SETTINGS, "输入法与语音设置"),
        SpeechRecognitionSettingsTarget(ACTION_APPLICATION_DETAILS_SETTINGS, "应用详情设置", requiresAppPackageUri = true),
        SpeechRecognitionSettingsTarget(ACTION_SETTINGS, "系统设置"),
    )

    fun unavailableGuidance(): String =
        "当前设备未提供系统语音识别服务。录音仍会保存；你可以打开系统语音/输入设置检查服务，或继续仅录音并手动粘贴转写文本。"
}
