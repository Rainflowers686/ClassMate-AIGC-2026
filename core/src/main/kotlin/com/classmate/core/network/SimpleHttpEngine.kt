package com.classmate.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * JDK-only [HttpEngine] backed by [HttpURLConnection].
 *
 * Lives in `core` (pure JVM) so the module stays free of OkHttp / Ktor /
 * Android dependencies. HttpURLConnection is available on Android API 26+
 * which matches our minSdk.
 *
 * Scope:
 *  - POST/GET with a string body;
 *  - bearer-style headers (passed by caller);
 *  - reads UTF-8;
 *  - no retry, no connection pooling tuning — the caller is responsible for
 *    backoff and fallback semantics.
 *
 * Errors that come from the transport (DNS, timeout, socket close, TLS) are
 * wrapped in [HttpError]. A non-2xx status is NOT an [HttpError] — it returns
 * a normal [HttpResponse] so providers can read the error body if useful.
 */
class SimpleHttpEngine : HttpEngine {

    override suspend fun execute(request: HttpRequest): HttpResponse =
        withContext(Dispatchers.IO) {
            val connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
                requestMethod = request.method.uppercase()
                connectTimeout = request.connectTimeoutMs
                readTimeout = request.readTimeoutMs
                doInput = true
                doOutput = request.body != null
                useCaches = false
                instanceFollowRedirects = true
                request.headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }

            try {
                request.body?.let { body ->
                    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.let {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use(BufferedReader::readText)
                }.orEmpty()
                val headers = connection.headerFields
                    .filterKeys { it != null }
                    .mapValues { (_, v) -> v.joinToString(",") }

                HttpResponse(statusCode = code, body = body, headers = headers)
            } catch (e: java.net.SocketTimeoutException) {
                throw HttpError("HTTP timeout after ${request.readTimeoutMs}ms: ${request.url}", e)
            } catch (e: java.io.IOException) {
                throw HttpError("HTTP I/O error: ${e.message ?: e::class.simpleName}", e)
            } finally {
                connection.disconnect()
            }
        }
}
