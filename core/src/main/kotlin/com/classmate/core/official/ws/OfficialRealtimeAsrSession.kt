package com.classmate.core.official.ws

/**
 * Coordinates one official real-time ASR stream over a WebSocket: open → `started` control frame → PCM
 * binary frames (fed by the app's AudioRecord capture) → `--end--` on stop. Events are parsed by
 * [OfficialAsrWsProtocol] and delivered as partial/final/error. Pure (no Android, no OkHttp) so it is
 * unit-testable with a fake transport; the app injects the real OkHttp transport + PCM capture.
 *
 * Honest contract: returns false (and reports a safe error) when not configured or when the transport is
 * absent, so the caller falls back to the system SpeechRecognizer — the official path never blocks recording.
 */
class OfficialRealtimeAsrSession(private val transport: OfficialWsTransport) {

    private var connection: OfficialWsConnection? = null
    @Volatile private var streaming: Boolean = false

    val isStreaming: Boolean get() = streaming

    fun start(
        config: OfficialWsConfig,
        engine: OfficialAsrWsProtocol.AsrEngine,
        requestId: String,
        systemTimeMs: Long,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit,
    ): Boolean {
        if (!config.isConfigured) {
            onError("官方实时转写需要配置后使用，已改用系统实时转写。")
            return false
        }
        val url = OfficialAsrWsProtocol.buildUrl(
            config.baseUrl,
            OfficialAsrWsProtocol.AsrParams(engine, config.userId, requestId, systemTimeMs),
        )
        val headers = mapOf(OfficialAsrWsProtocol.authHeader(config.appKey))
        val conn = transport.open(
            url,
            headers,
            object : OfficialWsListener {
                override fun onOpen() {
                    streaming = true
                    connection?.sendText(OfficialAsrWsProtocol.buildStartFrame(requestId = requestId))
                }
                override fun onText(text: String) {
                    when (val event = OfficialAsrWsProtocol.parseEvent(text)) {
                        is OfficialAsrWsProtocol.AsrEvent.Partial -> onPartial(event.text)
                        is OfficialAsrWsProtocol.AsrEvent.Final -> onFinal(event.text)
                        is OfficialAsrWsProtocol.AsrEvent.Error -> onError(event.safeMessage)
                        else -> Unit
                    }
                }
                override fun onClosed(code: Int) { streaming = false }
                override fun onFailure(safeReason: String) {
                    streaming = false
                    onError(safeReason)
                }
            },
        )
        if (conn == null) {
            onError("当前设备不支持官方实时转写，已改用系统实时转写。")
            return false
        }
        connection = conn
        return true
    }

    /** Feed one raw PCM frame (16k/16bit mono). Ignored before the stream is open. */
    fun feedPcm(frame: ByteArray) {
        if (streaming && frame.isNotEmpty()) connection?.sendBinary(frame)
    }

    /** Normal stop: send the end marker so the server emits the final result, then close. */
    fun stop() {
        connection?.let {
            it.sendBinary(OfficialAsrWsProtocol.END_MARKER)
            it.close()
        }
        reset()
    }

    /** Cancel: close without an end marker — no final result, mirroring "cancel = no evidence". */
    fun cancel() {
        connection?.close()
        reset()
    }

    private fun reset() {
        connection = null
        streaming = false
    }
}
