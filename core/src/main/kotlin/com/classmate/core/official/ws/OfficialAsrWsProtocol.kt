package com.classmate.core.official.ws

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Pure, dependency-light protocol for the official vivo WebSocket ASR (`/asr/v2`, docId 1738 short / 1740
 * dictation / 2065 dialect / 2068 simultaneous interpretation). Derived from the official Python接入 demos:
 * a Bearer-header handshake, a JSON `started` control frame, raw PCM binary frames, `--end--` / `--close--`
 * binary markers, and JSON `{action,type,data:{text,is_last},code,...}` events.
 *
 * This object builds URLs/frames and parses events WITHOUT any network or OkHttp, so it is fully
 * unit-testable. It never logs or returns the AppKey — [redactAuthValue] enforces that for diagnostics.
 */
object OfficialAsrWsProtocol {

    const val DEFAULT_PATH = "/asr/v2"
    val END_MARKER: ByteArray = "--end--".toByteArray(StandardCharsets.UTF_8)
    val CLOSE_MARKER: ByteArray = "--close--".toByteArray(StandardCharsets.UTF_8)

    /** Confirmed engine ids from the official demos (all over `/asr/v2`). */
    enum class AsrEngine(val engineId: String, val experimental: Boolean) {
        REALTIME_SHORT("shortasrinput", false),
        DICTATION("longasrlisten", false),
        DIALECT("shortasrinput", true),
        INTERPRETATION("longasrsubtitle", true),
    }

    data class AsrParams(
        val engine: AsrEngine,
        val userId: String,
        val requestId: String,
        val systemTimeMs: Long,
        val product: String = "classmate",
        val clientVersion: String = "unknown",
        val packageName: String = "com.classmate.app",
        val sdkVersion: String = "unknown",
        val androidVersion: String = "unknown",
        val netType: Int = 1,
    )

    /** Build the full `ws(s)://.../asr/v2?...` URL. [baseWsUrl] is the configured endpoint (host + path). */
    fun buildUrl(baseWsUrl: String, params: AsrParams): String {
        val ordered = linkedMapOf(
            "client_version" to params.clientVersion,
            "product" to params.product,
            "package" to params.packageName,
            "sdk_version" to params.sdkVersion,
            "user_id" to params.userId,
            "android_version" to params.androidVersion,
            "system_time" to params.systemTimeMs.toString(),
            "net_type" to params.netType.toString(),
            "engineid" to params.engine.engineId,
            "requestId" to params.requestId,
        )
        val query = ordered.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
        val base = baseWsUrl.trimEnd('?', '&')
        val sep = if (base.contains("?")) "&" else "?"
        return "$base$sep$query"
    }

    /** The handshake auth header. The value carries the AppKey and must NEVER be logged — see [redactAuthValue]. */
    fun authHeader(appKey: String): Pair<String, String> = "Authorization" to "Bearer ${appKey.trim()}"

    /** Diagnostics-safe rendering of an Authorization value: presence only, never the key. */
    fun redactAuthValue(value: String): String = if (value.isBlank()) "<absent>" else "Bearer ***"

    /** The JSON `started` control frame sent once before streaming audio. */
    fun buildStartFrame(
        requestId: String,
        audioType: String = "pcm",
        frontVadMs: Int = 6000,
        endVadMs: Int = 2000,
        chinese2digital: Int = 1,
        punctuation: Int = 2,
    ): String = buildJsonObject {
        put("type", "started")
        put("request_id", requestId)
        put(
            "asr_info",
            buildJsonObject {
                put("front_vad_time", frontVadMs)
                put("end_vad_time", endVadMs)
                put("audio_type", audioType)
                put("chinese2digital", chinese2digital)
                put("punctuation", punctuation)
            },
        )
    }.toString()

    sealed interface AsrEvent {
        data class Partial(val text: String) : AsrEvent
        data class Final(val text: String) : AsrEvent
        data class Vad(val code: Int) : AsrEvent
        data class Error(val code: Int, val safeMessage: String) : AsrEvent
        object Ignored : AsrEvent
    }

    /** Parse one JSON event frame. Never throws (malformed → [AsrEvent.Ignored]); never surfaces raw bodies. */
    fun parseEvent(raw: String): AsrEvent {
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return AsrEvent.Ignored
        return when (obj.str("action")) {
            "result" -> {
                if (obj.str("type") != "asr") return AsrEvent.Ignored
                val data = (obj["data"] as? JsonObject)
                val text = data?.str("text").orEmpty()
                val isLast = data?.get("is_last")?.let { (it as? JsonPrimitive)?.booleanOrNull } ?: false
                if (isLast) AsrEvent.Final(text) else AsrEvent.Partial(text)
            }
            "error" -> AsrEvent.Error(obj.intField("code") ?: -1, safeErrorMessage(obj.intField("code")))
            "vad" -> AsrEvent.Vad(obj.intField("code") ?: 0)
            else -> AsrEvent.Ignored
        }
    }

    /** Map a business code to a user-safe Chinese message (no raw provider body). */
    fun safeErrorMessage(code: Int?): String = when (code) {
        null -> "官方实时转写返回了无法识别的状态。"
        0 -> "官方实时转写处理中。"
        else -> "官方实时转写暂时不可用（错误码 $code），可改用系统实时转写或稍后重试。"
    }

    private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun JsonObject.str(key: String): String =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

    private fun JsonObject.intField(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
}
