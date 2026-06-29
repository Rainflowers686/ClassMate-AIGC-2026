package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Short, enumerated provider failure reasons. This is intentionally a *closed* set so that
 * errors can be logged and shown without ever embedding a vendor response body, prompt,
 * stack trace, or secret. See SECURITY.md and [com.classmate.core.logging.RedactedLogger].
 */
enum class ProviderErrorType {
    CONFIG_MISSING,     // no usable config / placeholder credentials / no transport
    NETWORK,            // connection refused, DNS, TLS, etc.
    SOCKET_TIMEOUT,     // transport timed out while connecting/writing/reading
    TIMEOUT,            // request exceeded timeoutMs
    HTTP_NON_2XX,       // server returned a non-2xx status (numeric status kept, body dropped)
    UNAUTHORIZED,       // 401 / 403
    APP_ID_HEADER_MISSING, // vivo auth failed because app_id header is absent/empty
    RATE_LIMITED,       // 429 or vendor rate-limit code
    PARAM_ERROR,        // vendor parameter error, e.g. missing request_id/requestId
    CONTENT_BLOCKED,    // vendor content moderation rejected the request/response
    MODEL_ACCESS_DENIED, // model access permission missing or expired
    USAGE_LIMIT,        // vendor daily quota exhausted
    EMPTY_RESPONSE,     // 2xx but no usable text content
    PARSE_ERROR,        // could not extract/parse JSON from the response
    VALIDATION_ERROR,   // parsed, but failed evidence / reference closure checks
    UNKNOWN,
}

/**
 * A provider failure. Carries only safe, non-sensitive fields: the error type, the
 * provider, an optional numeric HTTP status, and an optional vendor error code. By construction
 * there is nowhere to put a response body or message that could leak data.
 */
data class ProviderError(
    val type: ProviderErrorType,
    val provider: ProviderKind,
    val httpStatus: Int? = null,
    val vendorCode: String? = null,
    val requestProfile: String? = null,
    val timeoutMs: Long? = null,
    val networkSubtype: String? = null,
    val model: String? = null,
    val maxTokens: Int? = null,
) {
    /**
     * Compact, log-safe code e.g. `BLUELM:PARAM_ERROR:400:1001` or `BLUELM:NETWORK:DNS`.
     * The [networkSubtype] (DNS / TLS / CONNECT / WRITE / READ / SOCKET_TIMEOUT) is appended for
     * transport failures so a bare `BLUELM:NETWORK` can be told apart from DNS vs. TLS vs. refused —
     * this is what the analysis source report shows the user, so the failure must stay diagnosable.
     */
    val shortCode: String
        get() = buildString {
            append(provider.name)
            append(':')
            append(type.name)
            httpStatus?.let { append(':'); append(it) }
            vendorCode?.let { append(':'); append(it) }
            networkSubtype?.takeIf { it.isNotBlank() && it != type.name }?.let { append(':'); append(it) }
        }

    companion object {
        /** Maps HTTP + vendor status to the most specific safe error type. */
        fun fromStatus(provider: ProviderKind, status: Int, body: String? = null): ProviderError {
            if (status == 401 && bodyLooksLikeMissingAppId(body)) {
                return ProviderError(ProviderErrorType.APP_ID_HEADER_MISSING, provider, status)
            }
            parseVendorError(body)?.let { vendor ->
                mapVendorError(provider, status, vendor)?.let { return it }
                return ProviderError(httpErrorType(status), provider, status, vendor.code)
            }
            return ProviderError(httpErrorType(status), provider, status)
        }

        fun shouldRetryWithRequestId(status: Int, body: String?): Boolean {
            val vendor = parseVendorError(body) ?: return false
            return status !in 200..299 &&
                vendor.code == "1001" &&
                vendor.message.contains("requestId", ignoreCase = true) &&
                vendor.message.contains("empty", ignoreCase = true)
        }

        private fun mapVendorError(provider: ProviderKind, status: Int, vendor: VendorError): ProviderError? =
            when (vendor.code) {
                "401" -> if (vendor.message.looksLikeMissingAppId()) {
                    ProviderError(ProviderErrorType.APP_ID_HEADER_MISSING, provider, status, vendor.code)
                } else {
                    null
                }
                "1001" -> ProviderError(ProviderErrorType.PARAM_ERROR, provider, status, vendor.code)
                "1007" -> ProviderError(ProviderErrorType.CONTENT_BLOCKED, provider, status, vendor.code)
                "2003" -> ProviderError(ProviderErrorType.USAGE_LIMIT, provider, status, vendor.code)
                "30001" -> {
                    val type = if (vendor.message.contains("rate limit", ignoreCase = true)) {
                        ProviderErrorType.RATE_LIMITED
                    } else {
                        ProviderErrorType.MODEL_ACCESS_DENIED
                    }
                    ProviderError(type, provider, status, vendor.code)
                }
                else -> null
            }

        private fun parseVendorError(body: String?): VendorError? {
            if (body.isNullOrBlank()) return null
            val root = try {
                json.parseToJsonElement(body)
            } catch (e: Exception) {
                return null
            }
            val obj = root as? JsonObject ?: return null
            val error = obj["error"] as? JsonObject
            val code = obj.errorStr("code")
                ?: obj.errorStr("errorCode")
                ?: error.errorStr("code")
                ?: error.errorStr("errorCode")
                ?: return null
            val message = obj.errorStr("message")
                ?: obj.errorStr("msg")
                ?: error.errorStr("message")
                ?: error.errorStr("msg")
                ?: ""
            return VendorError(code, message)
        }

        private fun bodyLooksLikeMissingAppId(body: String?): Boolean =
            body?.looksLikeMissingAppId() == true

        private fun httpErrorType(status: Int): ProviderErrorType =
            when (status) {
                401, 403 -> ProviderErrorType.UNAUTHORIZED
                429 -> ProviderErrorType.RATE_LIMITED
                else -> ProviderErrorType.HTTP_NON_2XX
            }

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

private data class VendorError(val code: String, val message: String)

private fun JsonObject?.errorStr(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

private fun String.looksLikeMissingAppId(): Boolean {
    val normalized = lowercase()
    val mentionsAppId = normalized.contains("app_id") ||
        normalized.contains("appid") ||
        normalized.contains("app id")
    val saysMissing = normalized.contains("missing") ||
        normalized.contains("empty") ||
        normalized.contains("required") ||
        normalized.contains("blank") ||
        normalized.contains("null") ||
        normalized.contains("缺少") ||
        normalized.contains("为空") ||
        normalized.contains("不能为空")
    return mentionsAppId && saysMissing
}
