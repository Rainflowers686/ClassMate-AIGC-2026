package com.classmate.core.capture

/**
 * Classroom Capture — the shared error model, result wrapper, provider config and HTTP seam for the
 * Vivo ASR / OCR / retrieval providers. Built per the LOCAL official-doc mirror (vivo AIGC); no doc
 * full-text is copied here, only the contract fields needed to implement and map errors.
 *
 * Security: credentials are read from an injected [CaptureProviderConfig] (app layer loads them from
 * config.local.json / env). This module never hardcodes, logs, serializes, or `toString`s a key, and
 * UI only ever shows present/absent. When a provider is unconfigured it returns [CaptureError.ConfigMissing]
 * instead of crashing — the manual paste / on-device draft paths always remain usable.
 */

/** A closed, user-presentable failure set. Gentle messages; never embeds a vendor body or a secret. */
enum class CaptureError {
    ConfigMissing,
    NetworkUnavailable,
    AuthFailed,
    QuotaExceeded,
    InvalidAudio,
    AudioTooLong,
    UnsupportedFormat,
    ServiceUnavailable,
    ParseFailed,
    Unknown;

    /** A calm, honest Chinese message safe to show in the UI (no blame, no jargon, no secrets). */
    fun userMessageZh(): String = when (this) {
        ConfigMissing -> "当前未配置官方服务，仍可粘贴转写文本或保留端侧蓝心草稿继续编辑。"
        NetworkUnavailable -> "网络暂不可用，请检查连接后重试。"
        AuthFailed -> "服务鉴权未通过，请检查官方配置后重试。"
        QuotaExceeded -> "今日额度已用完，可稍后再试，或先手动编辑。"
        InvalidAudio -> "音频无法识别，请检查文件后重试。"
        AudioTooLong -> "音频过长，请分段上传。"
        UnsupportedFormat -> "暂不支持的格式，请更换文件后重试。"
        ServiceUnavailable -> "语音服务暂不可用，可稍后重试。"
        ParseFailed -> "结果解析失败，可重试或手动编辑。"
        Unknown -> "出了点问题，可稍后重试。"
    }

    companion object {
        /** Map a raw HTTP status (auth/network layer) to a capture error. Body is inspected only for the
         *  well-known vivo auth messages, never logged. */
        fun fromHttpStatus(status: Int, body: String? = null): CaptureError = when {
            status == 401 || status == 403 -> AuthFailed
            status == 429 -> QuotaExceeded
            status == 408 -> ServiceUnavailable
            status in 500..599 -> ServiceUnavailable
            status in 200..299 -> Unknown
            body != null && looksLikeAuthFailure(body) -> AuthFailed
            else -> Unknown
        }

        /** Map a vivo long-ASR business code (tables: 100xx/101xx/102xx/103xx/104xx) to a capture error. */
        fun fromAsrBusinessCode(code: Int): CaptureError = when (code) {
            0 -> Unknown // 0 is success; callers should not map success
            10002 -> AudioTooLong // slice_num > 100 (audio too large)
            10001, 10101, 10201, 10301, 10401 -> InvalidAudio // client request-param validation failed
            in 10003..10006, in 10102..10106, in 10202..10203, 10302, 10402 -> ServiceUnavailable // server-side
            10000, 10100, 10200, 10300, 10400 -> Unknown // our public-param build error
            else -> Unknown
        }

        /** Map a vivo general-OCR error_code (0 succ / 1 ocr fail / 2 image error) to a capture error. */
        fun fromOcrErrorCode(code: Int): CaptureError = when (code) {
            1 -> ParseFailed // ocr fail
            2 -> UnsupportedFormat // image error / unsupported image
            else -> Unknown
        }

        private fun looksLikeAuthFailure(body: String): Boolean {
            val b = body.lowercase()
            return b.contains("app_id") || b.contains("api-key") || b.contains("invalid api") ||
                b.contains("not having this ability")
        }
    }
}

/** A log-safe failure detail: the closed error + optional numeric status / vendor code. No body, no message. */
data class CaptureFailure(
    val error: CaptureError,
    val httpStatus: Int? = null,
    val vendorCode: String? = null,
) {
    /** Compact log code, e.g. `ASR:ServiceUnavailable:500` — safe to log. */
    val shortCode: String
        get() = buildString {
            append(error.name)
            httpStatus?.let { append(':'); append(it) }
            vendorCode?.let { append(':'); append(it) }
        }
}

/** A capture result: success value or a [CaptureFailure]. */
sealed interface CaptureResult<out T> {
    data class Success<T>(val value: T) : CaptureResult<T>
    data class Failure(val failure: CaptureFailure) : CaptureResult<Nothing>

    companion object {
        fun fail(error: CaptureError, httpStatus: Int? = null, vendorCode: String? = null): Failure =
            Failure(CaptureFailure(error, httpStatus, vendorCode))
    }
}

inline fun <T> CaptureResult<T>.onSuccess(block: (T) -> Unit): CaptureResult<T> {
    if (this is CaptureResult.Success) block(value)
    return this
}

/**
 * Provider credentials. The app layer constructs this from config.local.json / env. [appKey] is
 * sensitive: [toString] is redacted so it can never leak into logs, snapshots, or crash reports.
 */
class CaptureProviderConfig(
    appId: String?,
    appKey: String?,
    val domain: String = "api-ai.vivo.com.cn",
) {
    private val appIdValue: String = appId?.trim().orEmpty()
    private val appKeyValue: String = appKey?.trim().orEmpty()

    val isConfigured: Boolean get() = appIdValue.isNotEmpty() && appKeyValue.isNotEmpty()

    /** "aigc"+appId — the OCR/ASR businessid; only meaningful when configured. */
    fun businessId(): String = "aigc$appIdValue"
    fun appId(): String = appIdValue
    /** The Authorization header value. Only ever passed to the transport, never logged. */
    fun authHeader(): String = "Bearer $appKeyValue"

    override fun toString(): String = "CaptureProviderConfig(configured=$isConfigured)"

    companion object {
        /** An explicitly empty config — every provider returns ConfigMissing. */
        val ABSENT = CaptureProviderConfig(null, null)
    }
}

/** A minimal multi-content-type HTTP seam, kept out of `core`'s dependencies. App injects the real one. */
interface CaptureTransport {
    fun postForm(url: String, headers: Map<String, String>, form: Map<String, String>, timeoutMs: Long): CaptureHttpResponse
    fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): CaptureHttpResponse
    fun postMultipart(url: String, headers: Map<String, String>, fileField: String, fileBytes: ByteArray, fileName: String, timeoutMs: Long): CaptureHttpResponse
}

/** A raw HTTP response: numeric status + body (treated as sensitive — never logged wholesale). */
data class CaptureHttpResponse(val status: Int, val body: String)

/** Thrown by [NotConfiguredCaptureTransport]; providers translate it to [CaptureError.ConfigMissing]. */
class CaptureTransportNotConfigured : RuntimeException("capture transport not configured")

/** Default seam: no network client wired. Honest stand-in — providers fall back to ConfigMissing. */
object NotConfiguredCaptureTransport : CaptureTransport {
    override fun postForm(url: String, headers: Map<String, String>, form: Map<String, String>, timeoutMs: Long) = throw CaptureTransportNotConfigured()
    override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long) = throw CaptureTransportNotConfigured()
    override fun postMultipart(url: String, headers: Map<String, String>, fileField: String, fileBytes: ByteArray, fileName: String, timeoutMs: Long) = throw CaptureTransportNotConfigured()
}
