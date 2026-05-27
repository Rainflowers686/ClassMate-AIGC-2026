package com.classmate.core.network

/**
 * Provider-shaped HTTP request. Intentionally tiny: providers only need POST
 * with a JSON body + bearer-style auth, so we don't model multipart, query
 * params, or response streaming here.
 *
 * [headers] should already contain Authorization / Content-Type when needed.
 * Logging code MUST NOT print this map verbatim — it carries the API key.
 */
data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val connectTimeoutMs: Int = 10_000,
    val readTimeoutMs: Int = 60_000
)
