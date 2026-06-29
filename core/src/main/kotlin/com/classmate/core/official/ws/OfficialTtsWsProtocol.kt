package com.classmate.core.official.ws

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Pure, dependency-light protocol for the official vivo WebSocket TTS (`wss://api-ai.vivo.com.cn/tts`,
 * docId 1735), per the official接口文档 + Python 参考:
 *   - handshake headers `Authorization: Bearer <AppKey>` + `X-AI-GATEWAY-SIGNATURE: developers-aigc`
 *   - URL query: engineid/system_time/user_id/model/product/package/client_version/system_version/
 *     sdk_version/android_version/requestId
 *   - request text frame JSON: {aue:0, auf:"audio/L16;rate=24000", vcn, speed, volume, text:base64(utf8),
 *     encoding:"utf8", reqId}
 *   - response text frames: {error_code, error_msg, sid, data:{audio:base64, status:0|1|2, progress, slice}}
 *   - audio: 24kHz / 16-bit / mono / PCM
 *
 * Builds URLs/headers/frames and parses chunks WITHOUT any network — fully unit-testable. The AppKey is
 * never logged: [redactAuthValue] renders presence only.
 */
object OfficialTtsWsProtocol {

    const val DEFAULT_URL = "wss://api-ai.vivo.com.cn/tts"
    const val DEFAULT_ENGINE = "long_audio_synthesis_screen"
    const val DEFAULT_VCN = "x2_yige"
    const val AUF_L16_24K = "audio/L16;rate=24000"
    const val SIGNATURE_HEADER = "X-AI-GATEWAY-SIGNATURE"
    const val SIGNATURE_VALUE = "developers-aigc"

    /** Audio properties of the returned PCM. */
    const val SAMPLE_RATE = 24_000
    const val CHANNELS = 1
    const val BITS_PER_SAMPLE = 16

    /** Max UTF-8 bytes of text per request BEFORE base64; longer text is segmented. */
    const val MAX_TEXT_BYTES = 2048

    data class TtsParams(
        val userId: String,
        val requestId: String,
        val systemTimeMs: Long,
        val engineId: String = DEFAULT_ENGINE,
        val model: String = "x_synthesis",
        val product: String = "classmate",
        val packageName: String = "com.classmate.app",
        val clientVersion: String = "unknown",
        val systemVersion: String = "unknown",
        val sdkVersion: String = "unknown",
        val androidVersion: String = "unknown",
    )

    data class TtsRequest(
        val text: String,
        val reqId: Long,
        val vcn: String = DEFAULT_VCN,
        val speed: Int = 50,
        val volume: Int = 50,
    )

    fun buildUrl(baseWssUrl: String, params: TtsParams): String {
        val ordered = linkedMapOf(
            "engineid" to params.engineId,
            "system_time" to params.systemTimeMs.toString(),
            "user_id" to params.userId,
            "model" to params.model,
            "product" to params.product,
            "package" to params.packageName,
            "client_version" to params.clientVersion,
            "system_version" to params.systemVersion,
            "sdk_version" to params.sdkVersion,
            "android_version" to params.androidVersion,
            "requestId" to params.requestId,
        )
        val query = ordered.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
        val base = baseWssUrl.trimEnd('?', '&')
        val sep = if (base.contains("?")) "&" else "?"
        return "$base$sep$query"
    }

    /** Handshake headers. The Authorization value carries the AppKey — never log it; see [redactAuthValue]. */
    fun authHeaders(appKey: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer ${appKey.trim()}",
        SIGNATURE_HEADER to SIGNATURE_VALUE,
    )

    fun redactAuthValue(value: String): String = if (value.isBlank()) "<absent>" else "Bearer ***"

    /** The request text frame: text is base64(UTF-8). */
    fun buildRequestJson(request: TtsRequest): String = buildJsonObject {
        put("aue", 0)
        put("auf", AUF_L16_24K)
        put("vcn", request.vcn)
        put("speed", request.speed)
        put("volume", request.volume)
        put("text", base64Utf8(request.text))
        put("encoding", "utf8")
        put("reqId", request.reqId)
    }.toString()

    fun base64Utf8(text: String): String =
        Base64.getEncoder().encodeToString(text.toByteArray(StandardCharsets.UTF_8))

    /**
     * Split [text] into segments whose UTF-8 byte length is <= [MAX_TEXT_BYTES] (so the pre-base64 payload
     * fits), never cutting a Unicode code point. Each segment is synthesized as its own request and the
     * audio is concatenated.
     */
    fun segmentText(text: String, maxBytes: Int = MAX_TEXT_BYTES): List<String> {
        if (text.isBlank()) return emptyList()
        if (text.toByteArray(StandardCharsets.UTF_8).size <= maxBytes) return listOf(text)
        val segments = mutableListOf<String>()
        val current = StringBuilder()
        var currentBytes = 0
        for (cp in text.codePoints()) {
            val piece = String(Character.toChars(cp))
            val pieceBytes = piece.toByteArray(StandardCharsets.UTF_8).size
            if (currentBytes + pieceBytes > maxBytes && current.isNotEmpty()) {
                segments += current.toString()
                current.setLength(0)
                currentBytes = 0
            }
            current.append(piece)
            currentBytes += pieceBytes
        }
        if (current.isNotEmpty()) segments += current.toString()
        return segments
    }

    sealed interface TtsChunk {
        /** A base64-decoded PCM slice (may be empty for a pure status frame). [isLast] true at status==2. */
        data class Audio(val pcm: ByteArray, val isLast: Boolean) : TtsChunk
        data class Error(val code: Int, val safeMessage: String) : TtsChunk
        /** Handshake / status-only frame with no audio. */
        object Status : TtsChunk
        object Ignored : TtsChunk
    }

    fun parseChunk(raw: String): TtsChunk {
        val obj = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return TtsChunk.Ignored
        val errorCode = obj.intField("error_code")
        if (errorCode != null && errorCode != 0) {
            return TtsChunk.Error(errorCode, safeErrorMessage(errorCode))
        }
        val data = obj["data"] as? JsonObject ?: return TtsChunk.Status
        val status = data.intField("status") ?: -1
        val audioB64 = data.str("audio")
        val pcm = if (audioB64.isNotBlank()) {
            runCatching { Base64.getDecoder().decode(audioB64) }.getOrNull() ?: ByteArray(0)
        } else {
            ByteArray(0)
        }
        return if (pcm.isEmpty() && status != 2) TtsChunk.Status else TtsChunk.Audio(pcm, isLast = status == 2)
    }

    fun safeErrorMessage(code: Int?): String = when (code) {
        null -> "官方语音合成返回了无法识别的状态。"
        else -> "官方语音合成暂时不可用（错误码 $code），已改用系统 TTS 或保留文稿。"
    }

    private fun enc(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun JsonObject.str(key: String): String =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()

    private fun JsonObject.intField(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
}
