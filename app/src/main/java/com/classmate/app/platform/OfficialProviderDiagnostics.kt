package com.classmate.app.platform

import com.classmate.core.provider.BlueLMDiagnosticReport
import com.classmate.core.provider.BlueLMDiagnosticStatus
import com.classmate.core.provider.BlueLMDiagnosticSubtype

enum class ProviderDryRunCategory(val labelZh: String) {
    SUCCESS("成功"),
    SKIP_MISSING_CONFIG("SKIP：未配置"),
    INCOMPLETE_CONFIG("配置不完整"),
    AUTH_FAILED("鉴权失败"),
    NETWORK_FAILED("网络失败"),
    BAD_REQUEST("参数或请求错误"),
    TIMEOUT("超时"),
    SERVER_ERROR("服务端错误"),
    EMPTY_RESPONSE("返回空"),
    PARSE_ERROR("解析失败"),
    PROTOCOL_ERROR("协议错误"),
    READY_CONFIG_PRESENT("配置存在，待真机验证"),
    SKIPPED_NO_AUDIO("SKIP：缺少测试音频"),
}

data class ProviderDryRunResult(
    val capability: String,
    val category: ProviderDryRunCategory,
    val messageZh: String,
    val configured: Boolean,
    val liveRequestAttempted: Boolean,
    val lastSuccessEpochMs: Long? = null,
    val safeCode: String? = null,
) {
    fun displayLine(): String =
        buildString {
            append(category.labelZh)
            if (messageZh.isNotBlank()) append("：").append(messageZh)
            safeCode?.takeIf { it.isNotBlank() }?.let { append("（").append(it).append("）") }
        }
}

object OfficialProviderDiagnostics {
    fun fromBlueLm(report: BlueLMDiagnosticReport, successEpochMs: Long? = null): ProviderDryRunResult {
        val category = when {
            report.status == BlueLMDiagnosticStatus.OK -> ProviderDryRunCategory.SUCCESS
            report.subtype == BlueLMDiagnosticSubtype.CONFIG_MISSING -> ProviderDryRunCategory.SKIP_MISSING_CONFIG
            report.subtype == BlueLMDiagnosticSubtype.HTTP_401 || report.subtype == BlueLMDiagnosticSubtype.HTTP_403 ->
                ProviderDryRunCategory.AUTH_FAILED
            report.subtype == BlueLMDiagnosticSubtype.UNKNOWN_HOST ||
                report.subtype == BlueLMDiagnosticSubtype.CONNECT_EXCEPTION ||
                report.subtype == BlueLMDiagnosticSubtype.SSL ||
                report.subtype == BlueLMDiagnosticSubtype.IO -> ProviderDryRunCategory.NETWORK_FAILED
            report.subtype == BlueLMDiagnosticSubtype.TIMEOUT ||
                report.subtype == BlueLMDiagnosticSubtype.SOCKET_TIMEOUT -> ProviderDryRunCategory.TIMEOUT
            report.subtype == BlueLMDiagnosticSubtype.HTTP_5XX -> ProviderDryRunCategory.SERVER_ERROR
            report.subtype == BlueLMDiagnosticSubtype.EMPTY_RESPONSE -> ProviderDryRunCategory.EMPTY_RESPONSE
            report.subtype == BlueLMDiagnosticSubtype.PARSE_ERROR -> ProviderDryRunCategory.PARSE_ERROR
            report.subtype == BlueLMDiagnosticSubtype.HTTP_429 ||
                report.subtype == BlueLMDiagnosticSubtype.HTTP_NON_2XX ||
                report.subtype == BlueLMDiagnosticSubtype.APP_ID_HEADER_MISSING -> ProviderDryRunCategory.BAD_REQUEST
            else -> ProviderDryRunCategory.PROTOCOL_ERROR
        }
        return ProviderDryRunResult(
            capability = "蓝心云端大模型",
            category = category,
            messageZh = when (category) {
                ProviderDryRunCategory.SUCCESS -> "最小请求已返回可解析结果"
                ProviderDryRunCategory.SKIP_MISSING_CONFIG -> "本机未读取到完整 AppID/AppKey，未发起网络请求"
                ProviderDryRunCategory.AUTH_FAILED -> "服务返回鉴权失败，请检查 AppID/AppKey 与接口权限"
                ProviderDryRunCategory.NETWORK_FAILED -> "无法连接官方服务，请检查网络、代理或域名解析"
                ProviderDryRunCategory.TIMEOUT -> "请求超时，已保留本地结果，可稍后重试"
                ProviderDryRunCategory.SERVER_ERROR -> "官方服务端返回错误，可稍后重试"
                ProviderDryRunCategory.EMPTY_RESPONSE -> "服务返回空内容，未覆盖本地结果"
                ProviderDryRunCategory.PARSE_ERROR -> "返回内容不可解析，未覆盖本地结果"
                ProviderDryRunCategory.BAD_REQUEST -> "请求参数或限流异常，请检查接口配置"
                else -> "诊断失败，已保留本地结果"
            },
            configured = category != ProviderDryRunCategory.SKIP_MISSING_CONFIG,
            liveRequestAttempted = category != ProviderDryRunCategory.SKIP_MISSING_CONFIG,
            lastSuccessEpochMs = successEpochMs.takeIf { category == ProviderDryRunCategory.SUCCESS },
            safeCode = report.subtype?.name ?: report.status.name,
        )
    }

    fun fromCaptureConfig(
        capability: String,
        status: CaptureConfigStatus,
        configuredMessage: String,
        missingMessage: String,
        noAudio: Boolean = false,
    ): ProviderDryRunResult {
        val category = when {
            !status.hasAppId || !status.hasAppKey -> ProviderDryRunCategory.SKIP_MISSING_CONFIG
            !status.configured -> ProviderDryRunCategory.INCOMPLETE_CONFIG
            noAudio -> ProviderDryRunCategory.SKIPPED_NO_AUDIO
            else -> ProviderDryRunCategory.READY_CONFIG_PRESENT
        }
        return ProviderDryRunResult(
            capability = capability,
            category = category,
            messageZh = when (category) {
                ProviderDryRunCategory.SKIP_MISSING_CONFIG -> missingMessage
                ProviderDryRunCategory.INCOMPLETE_CONFIG -> "配置字段存在但不完整或疑似占位值，未发起网络请求"
                ProviderDryRunCategory.SKIPPED_NO_AUDIO -> "配置存在；未选择短录音，本次未发起转写请求"
                else -> configuredMessage
            },
            configured = status.configured,
            liveRequestAttempted = false,
            safeCode = when {
                !status.hasAppId && !status.hasAppKey -> "MISSING_APP_ID_APP_KEY"
                !status.hasAppId -> "MISSING_APP_ID"
                !status.hasAppKey -> "MISSING_APP_KEY"
                !status.configured -> "INCOMPLETE_CONFIG"
                noAudio -> "SKIPPED_NO_AUDIO"
                else -> "CONFIG_PRESENT"
            },
        )
    }

    fun fromOfficialFlag(
        capability: String,
        configured: Boolean,
        configuredMessage: String,
        missingMessage: String,
    ): ProviderDryRunResult =
        ProviderDryRunResult(
            capability = capability,
            category = if (configured) ProviderDryRunCategory.READY_CONFIG_PRESENT else ProviderDryRunCategory.SKIP_MISSING_CONFIG,
            messageZh = if (configured) configuredMessage else missingMessage,
            configured = configured,
            liveRequestAttempted = false,
            safeCode = if (configured) "CONFIG_PRESENT" else "MISSING_CONFIG",
        )
}
