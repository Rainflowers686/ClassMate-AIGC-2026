package com.classmate.app.asr

import com.classmate.core.official.ws.OfficialWsConnection
import com.classmate.core.official.ws.OfficialWsListener
import com.classmate.core.official.ws.OfficialWsTransport
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * Real OkHttp WebSocket implementation of [OfficialWsTransport] for the official vivo `/asr/v2` stream.
 * Failures are mapped to user/diagnostics-safe Chinese reasons — a raw throwable message or response body is
 * NEVER surfaced (it could contain a URL or token). The official path is config-gated: any failure here
 * makes the session report a safe error and the caller keeps the recording/manual transcript fallback.
 */
class OkHttpOfficialWsTransport(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build(),
) : OfficialWsTransport {

    override fun open(url: String, headers: Map<String, String>, listener: OfficialWsListener): OfficialWsConnection? {
        return try {
            // OkHttp's Request URL must be http/https; it upgrades to a WebSocket. Normalize ws(s) → http(s).
            val httpUrl = url.replaceFirst("wss://", "https://").replaceFirst("ws://", "http://")
            val builder = Request.Builder().url(httpUrl)
            headers.forEach { (k, v) -> builder.addHeader(k, v) }
            val webSocket = client.newWebSocket(
                builder.build(),
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) = listener.onOpen()
                    override fun onMessage(webSocket: WebSocket, text: String) = listener.onText(text)
                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) = listener.onText(bytes.utf8())
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        runCatching { webSocket.close(code, null) }
                    }
                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = listener.onClosed(code)
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                        listener.onFailure(safeReason(t))
                },
            )
            object : OfficialWsConnection {
                override fun sendText(text: String): Boolean = webSocket.send(text)
                override fun sendBinary(bytes: ByteArray): Boolean = webSocket.send(bytes.toByteString(0, bytes.size))
                override fun close() { runCatching { webSocket.close(NORMAL_CLOSURE, null) } }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun safeReason(t: Throwable): String = when (t) {
        is UnknownHostException -> "网络不可用，录音会保留，可稍后重试官方转写或粘贴转写文本。"
        is SocketTimeoutException -> "官方实时转写连接超时，录音会保留，可稍后重试官方转写或粘贴转写文本。"
        else -> "官方实时转写连接失败，录音会保留，可稍后重试官方转写或粘贴转写文本。"
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
