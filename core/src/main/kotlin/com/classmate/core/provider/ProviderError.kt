package com.classmate.core.provider

import com.classmate.core.model.ProviderKind

/**
 * Short, enumerated provider failure reasons. This is intentionally a *closed* set so that
 * errors can be logged and shown without ever embedding a vendor response body, prompt,
 * stack trace, or secret. See SECURITY.md and [com.classmate.core.logging.RedactedLogger].
 */
enum class ProviderErrorType {
    CONFIG_MISSING,     // no usable config / placeholder credentials / no transport
    NETWORK,            // connection refused, DNS, TLS, etc.
    TIMEOUT,            // request exceeded timeoutMs
    HTTP_NON_2XX,       // server returned a non-2xx status (numeric status kept, body dropped)
    UNAUTHORIZED,       // 401 / 403
    RATE_LIMITED,       // 429
    EMPTY_RESPONSE,     // 2xx but no usable text content
    PARSE_ERROR,        // could not extract/parse JSON from the response
    VALIDATION_ERROR,   // parsed, but failed evidence / reference closure checks
    UNKNOWN,
}

/**
 * A provider failure. Carries only safe, non-sensitive fields: the error type, the
 * provider, and an optional numeric HTTP status. By construction there is nowhere to put a
 * response body or message that could leak data.
 */
data class ProviderError(
    val type: ProviderErrorType,
    val provider: ProviderKind,
    val httpStatus: Int? = null,
) {
    /** Compact, log-safe code e.g. `BLUELM:HTTP_NON_2XX:503`. */
    val shortCode: String
        get() = buildString {
            append(provider.name)
            append(':')
            append(type.name)
            httpStatus?.let { append(':'); append(it) }
        }

    companion object {
        /** Maps an HTTP status to the most specific safe error type. */
        fun fromStatus(provider: ProviderKind, status: Int): ProviderError = when (status) {
            401, 403 -> ProviderError(ProviderErrorType.UNAUTHORIZED, provider, status)
            429 -> ProviderError(ProviderErrorType.RATE_LIMITED, provider, status)
            else -> ProviderError(ProviderErrorType.HTTP_NON_2XX, provider, status)
        }
    }
}
