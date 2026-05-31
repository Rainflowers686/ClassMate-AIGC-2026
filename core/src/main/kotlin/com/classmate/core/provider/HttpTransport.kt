package com.classmate.core.provider

/** A minimal HTTP response: numeric status + body string. The body is treated as sensitive. */
data class TransportResponse(val status: Int, val body: String)

/**
 * Seam that keeps the `core` module free of any HTTP client. The real transport (OkHttp,
 * Ktor, vivo SDK, ...) is implemented in the app/data layer and injected into the networked
 * providers. This makes `core` trivially unit-testable and dependency-light.
 */
fun interface HttpTransport {
    /**
     * Blocking POST of a JSON [body]. Implementations should throw on network/timeout
     * failures; providers translate those throwables into a short [ProviderError] without
     * propagating any message that could contain response content.
     */
    fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Long,
    ): TransportResponse
}

/** Thrown when no real transport is wired (the round-1 default). Maps to CONFIG_MISSING. */
class TransportNotConfiguredException : RuntimeException("HTTP transport not configured")

/**
 * Default transport for round 1: there is no network client, so every call fails fast and
 * the resolver falls back. This is the honest stand-in until a real transport is injected —
 * it never fabricates a model response.
 */
object NoNetworkTransport : HttpTransport {
    override fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Long,
    ): TransportResponse = throw TransportNotConfiguredException()
}
