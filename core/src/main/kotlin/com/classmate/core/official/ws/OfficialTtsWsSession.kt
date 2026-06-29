package com.classmate.core.official.ws

import java.io.ByteArrayOutputStream

/**
 * Drives ONE official TTS synthesis over a WebSocket: open → send the request JSON → accumulate base64 PCM
 * slices → complete at `status==2`. Pure (no Android/OkHttp) so it is unit-testable with a fake transport;
 * the app wraps it (with the OkHttp transport + a latch) into a blocking [com.classmate]-side provider.
 *
 * Honest contract: returns false + a safe error when unconfigured or when the transport is absent, so the
 * caller falls back to the system TextToSpeech — the official path never blocks listen-review audio.
 */
class OfficialTtsWsSession(private val transport: OfficialWsTransport) {

    private var connection: OfficialWsConnection? = null

    fun synthesize(
        config: OfficialWsConfig,
        params: OfficialTtsWsProtocol.TtsParams,
        request: OfficialTtsWsProtocol.TtsRequest,
        onComplete: (ByteArray) -> Unit,
        onError: (String) -> Unit,
    ): Boolean {
        if (!config.isConfigured) {
            onError("官方语音合成需要配置后使用，已改用系统 TTS。")
            return false
        }
        val url = OfficialTtsWsProtocol.buildUrl(config.baseUrl, params)
        val headers = OfficialTtsWsProtocol.authHeaders(config.appKey)
        val pcm = ByteArrayOutputStream()
        var completed = false
        val conn = transport.open(
            url,
            headers,
            object : OfficialWsListener {
                override fun onOpen() {
                    connection?.sendText(OfficialTtsWsProtocol.buildRequestJson(request))
                }
                override fun onText(text: String) {
                    when (val chunk = OfficialTtsWsProtocol.parseChunk(text)) {
                        is OfficialTtsWsProtocol.TtsChunk.Audio -> {
                            if (chunk.pcm.isNotEmpty()) pcm.write(chunk.pcm)
                            if (chunk.isLast && !completed) {
                                completed = true
                                connection?.close()
                                onComplete(pcm.toByteArray())
                            }
                        }
                        is OfficialTtsWsProtocol.TtsChunk.Error -> {
                            if (!completed) {
                                completed = true
                                connection?.close()
                                onError(chunk.safeMessage)
                            }
                        }
                        else -> Unit
                    }
                }
                override fun onClosed(code: Int) = Unit
                override fun onFailure(safeReason: String) {
                    if (!completed) {
                        completed = true
                        onError(safeReason)
                    }
                }
            },
        )
        if (conn == null) {
            onError("当前设备不支持官方语音合成，已改用系统 TTS。")
            return false
        }
        connection = conn
        return true
    }

    fun cancel() {
        connection?.close()
        connection = null
    }
}
