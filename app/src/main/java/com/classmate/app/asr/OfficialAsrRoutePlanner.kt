package com.classmate.app.asr

import com.classmate.app.platform.ProviderConfigSummary

enum class OfficialAsrRouteKind {
    OFFICIAL_REALTIME,
    OFFICIAL_LONG_AFTER_RECORDING,
    MANUAL_TRANSCRIPT,
}

data class OfficialAsrRoutePlan(
    val primary: OfficialAsrRouteKind,
    val systemFallbackAvailable: Boolean,
    val headline: String,
    val detail: String,
    val primaryRecordingButton: String,
    val afterRecordingAction: String,
    val showOfficialLongAction: Boolean,
    val showManualTranscriptAction: Boolean = true,
    val showSystemFallbackAction: Boolean,
)

object OfficialAsrRoutePlanner {
    fun plan(summary: ProviderConfigSummary, systemRecognizerAvailable: Boolean): OfficialAsrRoutePlan {
        val official = summary.officialProviders
        return when {
            official.realtimeAsrConfigured -> OfficialAsrRoutePlan(
                primary = OfficialAsrRouteKind.OFFICIAL_REALTIME,
                systemFallbackAvailable = systemRecognizerAvailable,
                headline = "官方实时 ASR 已配置",
                detail = "主路线使用官方实时 ASR；录音仍会保存，失败时可改用官方长语音转写或手动粘贴转写文本。",
                primaryRecordingButton = "录音并尝试官方转写",
                afterRecordingAction = "官方 ASR 转写录音",
                showOfficialLongAction = official.asrLongConfigured,
                showSystemFallbackAction = systemRecognizerAvailable,
            )
            official.asrLongConfigured -> OfficialAsrRoutePlan(
                primary = OfficialAsrRouteKind.OFFICIAL_LONG_AFTER_RECORDING,
                systemFallbackAvailable = systemRecognizerAvailable,
                headline = "官方长语音转写已配置",
                detail = "主路线先保存录音，停止后使用官方长语音转写；系统实时识别只作为可选实验 fallback。",
                primaryRecordingButton = "开始录音",
                afterRecordingAction = "官方 ASR 转写录音",
                showOfficialLongAction = true,
                showSystemFallbackAction = systemRecognizerAvailable,
            )
            else -> OfficialAsrRoutePlan(
                primary = OfficialAsrRouteKind.MANUAL_TRANSCRIPT,
                systemFallbackAvailable = systemRecognizerAvailable,
                headline = "官方 ASR 未配置",
                detail = "录音仍会保存；请粘贴转写文本，或在开发者设置配置官方 ASR。系统实时识别仅作为可选设备能力。",
                primaryRecordingButton = "开始录音",
                afterRecordingAction = "粘贴转写文本",
                showOfficialLongAction = false,
                showSystemFallbackAction = systemRecognizerAvailable,
            )
        }
    }
}
