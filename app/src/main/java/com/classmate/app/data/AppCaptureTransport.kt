package com.classmate.app.data

import com.classmate.core.capture.CaptureHttpResponse
import com.classmate.core.capture.CaptureTransport
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.net.URLEncoder

/**
 * Real app-side [CaptureTransport] for the vivo OCR/ASR providers. Plain [HttpURLConnection] (no new
 * dependency), mirroring [BlueLMHttpTransport]. Returns a [CaptureHttpResponse] for ANY HTTP status (the
 * provider maps 4xx/5xx) and throws on network failure (the provider's safeCall maps that to
 * NetworkUnavailable). The Authorization header is forwarded to the connection but NEVER logged or stored.
 * Callers must invoke these off the main thread (the providers are blocking by design).
 */
class AppCaptureTransport : CaptureTransport {

    override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): CaptureHttpResponse =
        execute(url, headers, "application/json", timeoutMs) { it.write(body.toByteArray(Charsets.UTF_8)) }

    override fun postForm(url: String, headers: Map<String, String>, form: Map<String, String>, timeoutMs: Long): CaptureHttpResponse {
        val encoded = form.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        return execute(url, headers, "application/x-www-form-urlencoded", timeoutMs) { it.write(encoded.toByteArray(Charsets.UTF_8)) }
    }

    override fun postMultipart(
        url: String,
        headers: Map<String, String>,
        fileField: String,
        fileBytes: ByteArray,
        fileName: String,
        timeoutMs: Long,
    ): CaptureHttpResponse {
        val boundary = "----ClassMateCapture${System.nanoTime()}"
        return execute(url, headers, "multipart/form-data; boundary=$boundary", timeoutMs) { out ->
            val header = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"").append(fileField).append("\"; filename=\"").append(fileName).append("\"\r\n")
                append("Content-Type: application/octet-stream\r\n\r\n")
            }
            out.write(header.toByteArray(Charsets.UTF_8))
            out.write(fileBytes)
            out.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        }
    }

    /** Open a POST connection, apply headers (Content-Type set only if the caller didn't), write, read. */
    private inline fun execute(
        url: String,
        headers: Map<String, String>,
        defaultContentType: String,
        timeoutMs: Long,
        writeBody: (DataOutputStream) -> Unit,
    ): CaptureHttpResponse {
        val connection = (URL(url).openConnection(Proxy.NO_PROXY) as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = timeoutMs.toInt()
            readTimeout = timeoutMs.toInt()
            if (headers.keys.none { it.equals("Content-Type", ignoreCase = true) }) {
                setRequestProperty("Content-Type", defaultContentType)
            }
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        return try {
            DataOutputStream(connection.outputStream).use { writeBody(it) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            CaptureHttpResponse(status, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
