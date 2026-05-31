package com.classmate.app.data

import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.TransportResponse
import java.net.HttpURLConnection
import java.net.URL

class BlueLMHttpTransport : HttpTransport {
    override fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Long,
    ): TransportResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = timeoutMs.toInt()
            readTimeout = timeoutMs.toInt()
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        return try {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            TransportResponse(status, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
