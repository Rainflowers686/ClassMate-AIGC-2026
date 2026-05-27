package com.classmate.core.network

/**
 * Minimal HTTP abstraction for providers.
 *
 * Providers depend on this interface, never on OkHttp / Ktor / HttpURLConnection
 * directly. That keeps the core module a pure-JVM library AND lets us swap the
 * transport (e.g. to OkHttp on Android, or to a fake in tests) without
 * touching provider code.
 *
 * Implementations MUST be suspend-safe (do their network I/O off the calling
 * thread — typically Dispatchers.IO inside the function body).
 */
interface HttpEngine {
    suspend fun execute(request: HttpRequest): HttpResponse
}
