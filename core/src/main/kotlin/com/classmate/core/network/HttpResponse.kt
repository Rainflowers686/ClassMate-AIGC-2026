package com.classmate.core.network

/**
 * Response from [HttpEngine.execute].
 *
 * [body] is the raw string body. Providers are expected to feed it to
 * [com.classmate.core.adapter.JsonExtractor] before deserializing — model
 * outputs frequently wrap JSON in ```json fences or prose.
 */
data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
) {
    val isSuccess: Boolean get() = statusCode in 200..299
}
