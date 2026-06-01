package com.classmate.app.data

import com.classmate.core.provider.BlueLMDiagnosticStage
import com.classmate.core.provider.BlueLMDiagnosticSubtype
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.TransportDiagnosticException
import com.classmate.core.provider.TransportResponse
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class BlueLMHttpTransport : HttpTransport {
    override fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Long,
    ): TransportResponse {
        val connection = (URL(url).openConnection(Proxy.NO_PROXY) as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = timeoutMs.toInt()
            readTimeout = timeoutMs.toInt()
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        return try {
            try {
                connection.connect()
            } catch (e: IOException) {
                throw e.toDiagnostic(BlueLMDiagnosticStage.CONNECT)
            }
            try {
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            } catch (e: IOException) {
                throw e.toDiagnostic(BlueLMDiagnosticStage.WRITE)
            }
            try {
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                TransportResponse(status, responseBody)
            } catch (e: IOException) {
                throw e.toDiagnostic(BlueLMDiagnosticStage.READ)
            }
        } finally {
            connection.disconnect()
        }
    }
}

private fun IOException.toDiagnostic(stage: BlueLMDiagnosticStage): TransportDiagnosticException =
    when (this) {
        is UnknownHostException ->
            TransportDiagnosticException(BlueLMDiagnosticStage.DNS, BlueLMDiagnosticSubtype.UNKNOWN_HOST, this)
        is SSLException ->
            TransportDiagnosticException(BlueLMDiagnosticStage.TLS, BlueLMDiagnosticSubtype.SSL, this)
        is SocketTimeoutException ->
            TransportDiagnosticException(stage, BlueLMDiagnosticSubtype.SOCKET_TIMEOUT, this)
        is ConnectException ->
            TransportDiagnosticException(BlueLMDiagnosticStage.CONNECT, BlueLMDiagnosticSubtype.CONNECT_EXCEPTION, this)
        else ->
            TransportDiagnosticException(stage, BlueLMDiagnosticSubtype.IO, this)
    }
