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
    }
}
