package com.classmate.core.network

/**
 * Transport-layer failure. Distinct from a non-2xx HTTP response (which is
 * still a valid [HttpResponse] with statusCode>=400) — this represents
 * network-level problems: DNS, timeout, socket close, TLS, etc.
 *
 * Providers translate this into [com.classmate.core.adapter.ModelCallException]
 * with reason=HTTP_ERROR so the ViewModel's fallback policy can pick it up.
 */
class HttpError(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
