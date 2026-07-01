package com.classmate.app.state

/**
 * Stage 8D — an honest, content-free breakdown of which AI produced (or failed to produce) a course
 * analysis, so the UI never collapses an on-device failure into the cloud's "BLUELM:CONFIG_MISSING".
 *
 *  - [cloudStatus]: the cloud short code ("OK" / "CONFIG_MISSING" / "HTTP_NON_2XX" ...).
 *  - [onDeviceAttempted]: whether the on-device BlueLM 3B fallback was tried (true once cloud failed).
 *  - [onDeviceReason]: ACCEPTED / INVALID_JSON / VALIDATION_FAILED / UNAVAILABLE (null when not tried).
 *  - [finalSource]: 云端蓝心 / 端侧蓝心 / 安全占位.
 */
data class AnalysisSourceReport(
    val cloudStatus: String,
    val onDeviceAttempted: Boolean,
    val onDeviceReason: String?,
    val finalSource: String,
) {
    companion object {
        const val SOURCE_CLOUD = "云端蓝心"
        const val SOURCE_ON_DEVICE = "端侧蓝心"
        const val SOURCE_PLACEHOLDER = "安全占位"

        /** Derive the report (incl. the honest [finalSource]) from the analysis run facts. */
        fun of(
            cloudStatus: String,
            onDeviceAttempted: Boolean,
            onDeviceReason: String?,
            analysisSucceeded: Boolean,
        ): AnalysisSourceReport {
            val finalSource = when {
                !analysisSucceeded -> SOURCE_PLACEHOLDER
                onDeviceAttempted -> SOURCE_ON_DEVICE
                else -> SOURCE_CLOUD
            }
            return AnalysisSourceReport(cloudStatus, onDeviceAttempted, onDeviceReason, finalSource)
        }

        /**
         * Honest Chinese explanation of an on-device CourseAnalysis result code. Stage 8D-2: each
         * failure keeps its REAL cause — permission advice appears ONLY for PERMISSION_MISSING, file
         * advice ONLY for MODEL_FILES_MISSING; bad output / generate failures are never dressed up
         * as "model unavailable".
         */
        fun onDeviceReasonZh(reason: String?): String = when (reason) {
            null -> "未尝试"
            "ACCEPTED" -> "已通过校验并落库"
            "INVALID_JSON" -> "端侧输出不是合法 JSON（INVALID_JSON）"
            "VALIDATION_FAILED" -> "端侧结果未通过校验（证据/引用不达标，VALIDATION_FAILED）"
            "SDK_MISSING" -> "未检测到端侧 SDK（SDK_MISSING）"
            "PERMISSION_MISSING" -> "请授予模型目录访问权限（PERMISSION_MISSING）"
            "MODEL_FILES_MISSING" -> "请检查 /sdcard/1225/1.7.0.4_1225_mtk9500 模型文件是否完整可读（MODEL_FILES_MISSING）"
            "INIT_FAILED" -> "端侧模型初始化失败（INIT_FAILED，可在端侧诊断查看 SDK 错误码）"
            "GENERATE_FAILED" -> "端侧生成失败（GENERATE_FAILED）"
            "TIMEOUT" -> "端侧生成超时（TIMEOUT，可重试或缩短课程文本）"
            "UNAVAILABLE" -> "端侧模型不可用"
            "UNKNOWN" -> "未知原因（UNKNOWN）"
            else -> reason
        }

        /**
         * User-facing cloud status. The raw short code stays in diagnostics, but ordinary pages need
         * a concise reason plus the next step, without AppKey or endpoint details.
         */
        fun cloudStatusZh(status: String): String {
            val code = status.uppercase()
            return when {
                code == "OK" -> "已连接并返回结果"
                code == "LOCAL_RULE" -> "未调用云端，已使用本地基础整理"
                code.contains("CONFIG_MISSING") -> "未配置 AppID/AppKey，已使用本地整理版"
                code.contains("APP_ID_HEADER_MISSING") || code.contains("HTTP_401") || code.contains("HTTP_403") ->
                    "鉴权未通过，请在开发者设置检查 AppID/AppKey"
                code.contains("UNKNOWN_HOST") || code.contains("CONNECT_EXCEPTION") || code.contains("NETWORK") ->
                    "网络不可达，已保留本地结果，可检查网络后重试"
                code.contains("TIMEOUT") || code.contains("SOCKET_TIMEOUT") ->
                    "请求超时，已保留本地结果，可缩短资料或重试"
                code.contains("HTTP_429") -> "请求过于频繁，请稍后重试"
                code.contains("HTTP_5") -> "服务暂时不可用，已保留本地结果"
                code.contains("HTTP_NON_2XX") -> "服务返回异常，已保留本地结果"
                code.contains("PARSE_ERROR") -> "返回内容无法解析，已保留本地结果"
                code.contains("EMPTY_RESPONSE") -> "模型返回为空，已保留本地结果"
                else -> "云端暂时不可用，已使用本地整理版"
            }
        }
    }
}
