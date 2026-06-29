package com.classmate.core.official.ws

/**
 * Functional seam for an official WebSocket connection. The app supplies a real implementation (OkHttp);
 * `core` stays transport-free and unit-testable. No credentials are ever stored on these types — the auth
 * header is passed per-open and redacted in diagnostics.
 */
interface OfficialWsTransport {
    /** Open a connection; returns null when the transport itself is unavailable (e.g. no WS client). */
    fun open(url: String, headers: Map<String, String>, listener: OfficialWsListener): OfficialWsConnection?
}

interface OfficialWsConnection {
    fun sendText(text: String): Boolean
    fun sendBinary(bytes: ByteArray): Boolean
    /** Graceful close; safe to call more than once. */
    fun close()
}

interface OfficialWsListener {
    fun onOpen()
    fun onText(text: String)
    fun onClosed(code: Int)
    /** [safeReason] is already user/diagnostics-safe — never a raw stacktrace or response body. */
    fun onFailure(safeReason: String)
}

/** Config for an official WS capability. Carries the AppKey for the handshake only; never exported/logged. */
data class OfficialWsConfig(
    val baseUrl: String = "",
    val appKey: String = "",
    val userId: String = "",
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && appKey.isNotBlank()
}

/** Honest readiness for an official WS capability, surfaced to diagnostics (never the key itself). */
enum class OfficialWsReadiness(val displayZh: String) {
    READY("官方能力已配置"),
    CONFIG_MISSING("需要配置后使用"),
    TRANSPORT_MISSING("当前设备不支持"),
}

object OfficialWsReadinessPolicy {
    fun evaluate(config: OfficialWsConfig, transportPresent: Boolean): OfficialWsReadiness = when {
        !transportPresent -> OfficialWsReadiness.TRANSPORT_MISSING
        !config.isConfigured -> OfficialWsReadiness.CONFIG_MISSING
        else -> OfficialWsReadiness.READY
    }
}
