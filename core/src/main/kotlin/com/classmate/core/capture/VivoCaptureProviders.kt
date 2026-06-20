package com.classmate.core.capture

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Vivo AIGC HTTP provider implementations (contracts taken from the LOCAL doc mirror; no doc full-text
 * copied). Every provider is config-gated: with no [CaptureProviderConfig] or no [CaptureTransport] it
 * returns [CaptureError.ConfigMissing] — never crashes — so the manual paste / on-device draft paths
 * keep working. Credentials reach only the transport's Authorization header; nothing here logs a key.
 */

private val captureJson = Json { ignoreUnknownKeys = true; isLenient = true }

private const val ASR_SLICE_SIZE = 5 * 1024 * 1024 // 5 MB per slice (doc), max 100 slices / 500 MB
private const val ASR_MAX_SLICES = 100

/** Run [block]; translate the transport's "not configured" into ConfigMissing and anything else into Network. */
private inline fun <T> safeCall(block: () -> CaptureResult<T>): CaptureResult<T> =
    try {
        block()
    } catch (_: CaptureTransportNotConfigured) {
        CaptureResult.fail(CaptureError.ConfigMissing)
    } catch (_: Exception) {
        CaptureResult.fail(CaptureError.NetworkUnavailable)
    }

private fun JsonObject.intField(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
private fun JsonObject.strField(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

// ── ASR: 长语音转写 (1739) ────────────────────────────────────────────────────────────────────────
class VivoAsrProvider(
    private val config: CaptureProviderConfig = CaptureProviderConfig.ABSENT,
    private val transport: CaptureTransport = NotConfiguredCaptureTransport,
    private val userId: String = "00000000000000000000000000000000",
    private val packageName: String = "com.classmate.app",
    private val clientVersion: String = "1.0.0",
    private val engineId: String = "fileasrrecorder",
    private val timeoutMs: Long = 120_000,
    private val maxPolls: Int = 60,
    private val pollSleepMs: Long = 2_000,
    private val requestIdGen: () -> String = { java.util.UUID.randomUUID().toString() },
    private val sessionIdGen: () -> String = { java.util.UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val sleeper: (Long) -> Unit = { if (it > 0) Thread.sleep(it) },
) : SpeechToTextProvider {

    override val isConfigured: Boolean get() = config.isConfigured

    override fun transcribeLongAudio(
        audioBytes: ByteArray,
        fileName: String,
        audioFormat: String,
        onProgress: (Int) -> Unit,
    ): CaptureResult<AsrTranscriptResult> {
        if (!config.isConfigured) return CaptureResult.fail(CaptureError.ConfigMissing)
        if (audioBytes.isEmpty()) return CaptureResult.fail(CaptureError.InvalidAudio)
        val sliceNum = ((audioBytes.size + ASR_SLICE_SIZE - 1) / ASR_SLICE_SIZE).coerceAtLeast(1)
        if (sliceNum > ASR_MAX_SLICES) return CaptureResult.fail(CaptureError.AudioTooLong)

        val sessionId = sessionIdGen()
        val headers = mapOf("Authorization" to config.authHeader())

        return safeCall {
            // 1) create audio
            val createUrl = url("/lasr/create")
            val created = transport.postForm(createUrl, headers, mapOf(
                "audio_type" to if (audioFormat.equals("pcm", true)) "pcm" else "auto",
                "x-sessionId" to sessionId,
                "slice_num" to sliceNum.toString(),
            ), timeoutMs)
            val audioId = envelopeData(created)?.strField("audio_id")
                ?: return@safeCall envelopeFailure(created, CaptureError.ServiceUnavailable)

            // 2) slice upload
            for (index in 0 until sliceNum) {
                val from = index * ASR_SLICE_SIZE
                val to = minOf(from + ASR_SLICE_SIZE, audioBytes.size)
                val slice = audioBytes.copyOfRange(from, to)
                val upUrl = url("/lasr/upload", "audio_id" to audioId, "slice_index" to index.toString(), "x-sessionId" to sessionId)
                val up = transport.postMultipart(upUrl, headers, "file", slice, fileName, timeoutMs)
                if (!isOk(up)) return@safeCall envelopeFailure(up, CaptureError.ServiceUnavailable)
            }

            // 3) create task / run
            val run = transport.postForm(url("/lasr/run"), headers, mapOf("audio_id" to audioId, "x-sessionId" to sessionId), timeoutMs)
            val taskId = envelopeData(run)?.strField("task_id")
                ?: return@safeCall envelopeFailure(run, CaptureError.ServiceUnavailable)

            // 4) poll progress (bounded; off main thread)
            var progress = 0
            var polls = 0
            while (progress < 100 && polls < maxPolls) {
                val pr = transport.postForm(url("/lasr/progress"), headers, mapOf("task_id" to taskId, "x-sessionId" to sessionId), timeoutMs)
                val data = envelopeData(pr) ?: return@safeCall envelopeFailure(pr, CaptureError.ServiceUnavailable)
                progress = data.intField("progress") ?: progress
                onProgress(progress.coerceIn(0, 100))
                if (progress >= 100) break
                polls++
                sleeper(pollSleepMs)
            }
            if (progress < 100) return@safeCall CaptureResult.fail(CaptureError.ServiceUnavailable)

            // 5) result
            val res = transport.postForm(url("/lasr/result"), headers, mapOf("task_id" to taskId, "x-sessionId" to sessionId), timeoutMs)
            parseAsrResult(res)
        }
    }

    /** Parse the `/lasr/result` envelope into [AsrTranscriptResult]. */
    fun parseAsrResult(resp: CaptureHttpResponse): CaptureResult<AsrTranscriptResult> {
        if (resp.status !in 200..299) return CaptureResult.fail(CaptureError.fromHttpStatus(resp.status, resp.body), resp.status)
        val root = parseObject(resp.body) ?: return CaptureResult.fail(CaptureError.ParseFailed)
        val code = root.intField("code") ?: 0
        if (code != 0) return CaptureResult.fail(CaptureError.fromAsrBusinessCode(code), vendorCode = code.toString())
        val arr = (root["data"] as? JsonObject)?.get("result") as? JsonArray ?: return CaptureResult.fail(CaptureError.ParseFailed)
        val segments = arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val text = o.strField("onebest")?.trim().orEmpty()
            if (text.isEmpty()) return@mapNotNull null
            AsrSegment(text = text, startMs = (o["bg"] as? JsonPrimitive)?.intOrNull?.toLong(), endMs = (o["ed"] as? JsonPrimitive)?.intOrNull?.toLong(), speakerIndex = o.intField("speaker"))
        }
        if (segments.isEmpty()) return CaptureResult.fail(CaptureError.ParseFailed)
        return CaptureResult.Success(AsrTranscriptResult(segments, AsrJobStatus.SUCCESS))
    }

    private fun envelopeData(resp: CaptureHttpResponse): JsonObject? {
        if (!isOk(resp)) return null
        val root = parseObject(resp.body) ?: return null
        if ((root.intField("code") ?: 0) != 0) return null
        return root["data"] as? JsonObject
    }

    private fun envelopeFailure(resp: CaptureHttpResponse, fallback: CaptureError): CaptureResult.Failure {
        if (resp.status !in 200..299) return CaptureResult.fail(resp.status.let { CaptureError.fromHttpStatus(it, resp.body) }, resp.status)
        val code = parseObject(resp.body)?.intField("code")
        return if (code != null && code != 0) CaptureResult.fail(CaptureError.fromAsrBusinessCode(code), vendorCode = code.toString())
        else CaptureResult.fail(fallback, resp.status)
    }

    private fun isOk(resp: CaptureHttpResponse): Boolean = resp.status in 200..299

    private fun url(path: String, vararg extra: Pair<String, String>): String {
        val params = (commonParams() + extra).joinToString("&") { (k, v) -> "$k=${urlEncode(v)}" }
        return "https://${config.domain}$path?$params"
    }

    private fun commonParams(): List<Pair<String, String>> = listOf(
        "client_version" to clientVersion,
        "package" to packageName,
        "user_id" to userId,
        "system_time" to clock().toString(),
        "engineid" to engineId,
        "requestId" to requestIdGen(),
    )
}

// ── OCR: 通用OCR (1737) ──────────────────────────────────────────────────────────────────────────
class VivoOcrProvider(
    private val config: CaptureProviderConfig = CaptureProviderConfig.ABSENT,
    private val transport: CaptureTransport = NotConfiguredCaptureTransport,
    private val timeoutMs: Long = 15_000,
    private val requestIdGen: () -> String = { java.util.UUID.randomUUID().toString() },
) : OcrProvider {

    override val isConfigured: Boolean get() = config.isConfigured

    override fun recognize(imageBytes: ByteArray): CaptureResult<OcrResult> {
        if (!config.isConfigured) return CaptureResult.fail(CaptureError.ConfigMissing)
        if (imageBytes.isEmpty()) return CaptureResult.fail(CaptureError.UnsupportedFormat)
        val url = "https://${config.domain}/ocr/general_recognition?requestId=${urlEncode(requestIdGen())}"
        val headers = mapOf("Authorization" to config.authHeader(), "Content-Type" to "application/x-www-form-urlencoded")
        val form = mapOf("image" to base64(imageBytes), "pos" to "2", "businessid" to config.businessId())
        return safeCall { parseOcr(transport.postForm(url, headers, form, timeoutMs)) }
    }

    /** Parse the OCR envelope (`error_code` + `result.OCR[]` / `result.words[]`) into [OcrResult]. */
    fun parseOcr(resp: CaptureHttpResponse): CaptureResult<OcrResult> {
        if (resp.status !in 200..299) return CaptureResult.fail(CaptureError.fromHttpStatus(resp.status, resp.body), resp.status)
        val root = parseObject(resp.body) ?: return CaptureResult.fail(CaptureError.ParseFailed)
        val errorCode = root.intField("error_code") ?: 0
        if (errorCode != 0) return CaptureResult.fail(CaptureError.fromOcrErrorCode(errorCode), vendorCode = errorCode.toString())
        val result = root["result"] as? JsonObject ?: return CaptureResult.fail(CaptureError.ParseFailed)
        val angle = result.intField("angle") ?: 0
        val ocrArr = result["OCR"] as? JsonArray
        val blocks = if (ocrArr != null) {
            ocrArr.mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val words = o.strField("words")?.trim().orEmpty()
                if (words.isEmpty()) return@mapNotNull null
                val loc = (o["location"] as? JsonObject)?.get("top_left") as? JsonObject
                val location = loc?.let { OcrTextLocation((it["x"] as? JsonPrimitive)?.doubleOrNull ?: 0.0, (it["y"] as? JsonPrimitive)?.doubleOrNull ?: 0.0) }
                OcrTextBlock(words, location)
            }
        } else {
            (result["words"] as? JsonArray).orEmptyArray().mapNotNull { el ->
                val w = (el as? JsonObject)?.strField("words")?.trim().orEmpty()
                if (w.isEmpty()) null else OcrTextBlock(w)
            }
        }
        if (blocks.isEmpty()) return CaptureResult.fail(CaptureError.ParseFailed)
        return CaptureResult.Success(OcrResult(blocks, angle))
    }
}

// ── Retrieval (reserved): similarity 2060 / rewrite 2061 / embedding 1734 ────────────────────────────
/** Reserved: official rerank. Request shape `{model_name, query, sentences}`; ConfigMissing until wired. */
class VivoTextSimilarityProvider(
    private val config: CaptureProviderConfig = CaptureProviderConfig.ABSENT,
    private val transport: CaptureTransport = NotConfiguredCaptureTransport,
    private val modelName: String = "bge_large_zh",
    private val timeoutMs: Long = 10_000,
) : TextSimilarityProvider {
    override val isConfigured: Boolean get() = config.isConfigured
    override fun similarity(query: String, candidates: List<String>): CaptureResult<List<Double>> {
        if (!config.isConfigured) return CaptureResult.fail(CaptureError.ConfigMissing)
        if (candidates.isEmpty()) return CaptureResult.Success(emptyList())
        val body = captureJson.encodeToString(JsonObject.serializer(), buildJson {
            put("model_name", modelName); put("query", query); putArray("sentences", candidates)
        })
        return safeCall {
            val resp = transport.postJson("https://${config.domain}/similarity-model-api/predict", mapOf("Authorization" to config.authHeader(), "Content-Type" to "application/json"), body, timeoutMs)
            if (resp.status !in 200..299) return@safeCall CaptureResult.fail(CaptureError.fromHttpStatus(resp.status, resp.body), resp.status)
            val arr = (parseObject(resp.body)?.get("data") as? JsonObject)?.get("scores") as? JsonArray ?: return@safeCall CaptureResult.fail(CaptureError.ParseFailed)
            CaptureResult.Success(arr.map { (it as? JsonPrimitive)?.doubleOrNull ?: 0.0 })
        }
    }
}

/** Reserved: official query rewrite. Request shape `{prompts:[{q,a}...]}`; ConfigMissing until wired. */
class VivoQueryRewriteProvider(
    private val config: CaptureProviderConfig = CaptureProviderConfig.ABSENT,
    private val transport: CaptureTransport = NotConfiguredCaptureTransport,
    private val timeoutMs: Long = 10_000,
) : QueryRewriteProvider {
    override val isConfigured: Boolean get() = config.isConfigured
    override fun rewrite(question: String, history: List<Pair<String, String>>): CaptureResult<String> {
        if (!config.isConfigured) return CaptureResult.fail(CaptureError.ConfigMissing)
        return safeCall {
            val body = captureJson.encodeToString(JsonObject.serializer(), queryRewriteBody(question, history))
            val resp = transport.postJson("https://${config.domain}/query-rewrite-api/predict", mapOf("Authorization" to config.authHeader(), "Content-Type" to "application/json"), body, timeoutMs)
            if (resp.status !in 200..299) return@safeCall CaptureResult.fail(CaptureError.fromHttpStatus(resp.status, resp.body), resp.status)
            val rewritten = parseQueryRewrite(resp.body)
            if (rewritten.isNullOrBlank()) CaptureResult.fail(CaptureError.ParseFailed) else CaptureResult.Success(rewritten)
        }
    }

    private fun queryRewriteBody(question: String, history: List<Pair<String, String>>): JsonObject {
        val qa = history.takeLast(3).flatMap { listOf(it.first, it.second) }
        val padded = (List((6 - qa.size).coerceAtLeast(0)) { "" } + qa).takeLast(6)
        return JsonObject(
            mapOf(
                "prompts" to JsonArray(
                    listOf(
                        JsonArray(padded.map { JsonPrimitive(it) }),
                        JsonArray(listOf(JsonPrimitive(question))),
                    ),
                ),
            ),
        )
    }

    private fun parseQueryRewrite(body: String): String? {
        val root = parseObject(body) ?: return null
        val data = root["data"]
        return when (data) {
            is JsonObject -> data.strField("query")
                ?: data.strField("rewritten_query")
                ?: data.strField("rewrite")
                ?: data.strField("result")
            is JsonArray -> data.firstNotNullOfOrNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { value -> value.isNotBlank() } }
            is JsonPrimitive -> data.contentOrNull
            else -> null
        } ?: root.strField("query")
            ?: root.strField("rewritten_query")
            ?: root.strField("rewrite")
            ?: root.strField("result")
    }
}

/** Reserved: official batch embeddings (`/embedding-model-api/predict/batch`, `{model_name, sentences}`). */
class VivoEmbeddingProvider(
    private val config: CaptureProviderConfig = CaptureProviderConfig.ABSENT,
    private val transport: CaptureTransport = NotConfiguredCaptureTransport,
    private val modelName: String = "m3e-base",
    private val timeoutMs: Long = 10_000,
) : EmbeddingProvider {
    override val isConfigured: Boolean get() = config.isConfigured
    override fun embed(sentences: List<String>): CaptureResult<List<List<Double>>> {
        if (!config.isConfigured) return CaptureResult.fail(CaptureError.ConfigMissing)
        if (sentences.isEmpty()) return CaptureResult.Success(emptyList())
        return safeCall {
            val body = captureJson.encodeToString(JsonObject.serializer(), buildJson { put("model_name", modelName); putArray("sentences", sentences) })
            val resp = transport.postJson("https://${config.domain}/embedding-model-api/predict/batch", mapOf("Authorization" to config.authHeader(), "Content-Type" to "application/json"), body, timeoutMs)
            if (resp.status !in 200..299) return@safeCall CaptureResult.fail(CaptureError.fromHttpStatus(resp.status, resp.body), resp.status)
            val data = parseObject(resp.body)?.get("data") as? JsonArray ?: return@safeCall CaptureResult.fail(CaptureError.ParseFailed)
            CaptureResult.Success(data.map { row -> (row as? JsonArray).orEmptyArray().map { (it as? JsonPrimitive)?.doubleOrNull ?: 0.0 } })
        }
    }
}

// ── small shared helpers ───────────────────────────────────────────────────────────────────────────
private fun parseObject(body: String): JsonObject? =
    try { captureJson.parseToJsonElement(body).jsonObject } catch (_: Exception) { null }

private fun JsonArray?.orEmptyArray(): JsonArray = this ?: JsonArray(emptyList())

private fun urlEncode(v: String): String = java.net.URLEncoder.encode(v, "UTF-8")

/** Minimal, dependency-free, platform-safe Base64 (avoids java.util.Base64 API-26 / android.util split). */
private fun base64(bytes: ByteArray): String {
    val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val sb = StringBuilder((bytes.size + 2) / 3 * 4)
    var i = 0
    while (i < bytes.size) {
        val b0 = bytes[i].toInt() and 0xFF
        val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
        sb.append(table[b0 shr 2])
        sb.append(table[(b0 and 0x03 shl 4) or (b1 shr 4)])
        sb.append(if (i + 1 < bytes.size) table[(b1 and 0x0F shl 2) or (b2 shr 6)] else '=')
        sb.append(if (i + 2 < bytes.size) table[b2 and 0x3F] else '=')
        i += 3
    }
    return sb.toString()
}

// Tiny JSON object builder (kotlinx.serialization) so providers stay readable.
private class JsonBuilderScope {
    val map = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>()
    fun put(key: String, value: String) { map[key] = JsonPrimitive(value) }
    fun putArray(key: String, values: List<String>) { map[key] = JsonArray(values.map { JsonPrimitive(it) }) }
}
private fun buildJson(block: JsonBuilderScope.() -> Unit): JsonObject = JsonObject(JsonBuilderScope().apply(block).map)
