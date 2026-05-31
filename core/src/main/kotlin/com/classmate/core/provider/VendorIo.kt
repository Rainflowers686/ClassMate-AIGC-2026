package com.classmate.core.provider

import com.classmate.core.prompt.Prompt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class BlueLMRequestOptions(
    val stream: Boolean = false,
    val temperature: Double = 0.3,
    val maxTokens: Int = 4096,
)

fun interface BlueLMRequestFactory {
    fun build(model: String, prompt: Prompt, options: BlueLMRequestOptions): String
}

fun interface BlueLMResponseReader {
    fun extractAssistantText(body: String): String?
}

data class BlueLMMessageRead(
    val content: String?,
    val reasoningContentPresent: Boolean,
    val reasoningContentLength: Int,
)

data class BlueLMStreamRead(
    val content: String,
    val reasoningContentPresent: Boolean,
    val reasoningContentLength: Int,
    val done: Boolean,
)

/** Official vivo AIGC cloud text model request shape: OpenAI-compatible chat completions. */
object VivoOpenAIChatRequestFactory : BlueLMRequestFactory {
    override fun build(model: String, prompt: Prompt, options: BlueLMRequestOptions): String =
        buildJsonObject {
            put("model", model)
            put("stream", options.stream)
            put("temperature", options.temperature)
            put("max_tokens", options.maxTokens)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", prompt.system)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", prompt.user)
                }
            }
        }.toString()
}

/** Official sync response reader: choices[0].message.content, with reasoning detected only. */
object VivoOpenAIChatResponseReader : BlueLMResponseReader {
    override fun extractAssistantText(body: String): String? = readMessage(body).content

    fun readMessage(body: String): BlueLMMessageRead {
        val obj = parseObject(body) ?: return BlueLMMessageRead(null, false, 0)
        val message = ((obj["choices"] as? JsonArray)
            ?.firstOrNull() as? JsonObject)
            ?.get("message") as? JsonObject
        val content = message.str("content")
        val reasoning = message.str("reasoning_content")
        return BlueLMMessageRead(
            content = content,
            reasoningContentPresent = reasoning != null,
            reasoningContentLength = reasoning?.length ?: 0,
        )
    }
}

/** Parser for OpenAI-compatible SSE lines. It does not expose reasoning text, only metadata. */
object VivoOpenAIChatStreamParser {
    fun parseLines(lines: Iterable<String>): BlueLMStreamRead {
        val content = StringBuilder()
        var reasoningPresent = false
        var reasoningLength = 0
        var done = false
        for (line in lines) {
            val chunk = parseLine(line) ?: continue
            if (chunk.done) {
                done = true
                continue
            }
            content.append(chunk.content)
            reasoningPresent = reasoningPresent || chunk.reasoningContentPresent
            reasoningLength += chunk.reasoningContentLength
        }
        return BlueLMStreamRead(content.toString(), reasoningPresent, reasoningLength, done)
    }

    fun parseLine(line: String): BlueLMStreamRead? {
        val payload = line.trim()
            .removePrefix("data:")
            .trim()
            .takeIf { it.isNotBlank() } ?: return null
        if (payload == "[DONE]") return BlueLMStreamRead("", false, 0, done = true)
        val obj = parseObject(payload) ?: return null
        val delta = ((obj["choices"] as? JsonArray)
            ?.firstOrNull() as? JsonObject)
            ?.get("delta") as? JsonObject
        val content = delta.str("content").orEmpty()
        val reasoning = delta.str("reasoning_content")
        return BlueLMStreamRead(
            content = content,
            reasoningContentPresent = reasoning != null,
            reasoningContentLength = reasoning?.length ?: 0,
            done = false,
        )
    }
}

/**
 * Backward-compatible chat-style request body for the secondary compatible provider.
 * BlueLMProvider now uses [VivoOpenAIChatRequestFactory].
 */
object ChatRequestFactory {
    fun build(model: String, prompt: Prompt, temperature: Double = 0.3): String =
        buildJsonObject {
            put("model", model)
            put("temperature", temperature)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", prompt.system)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", prompt.user)
                }
            }
        }.toString()
}

/**
 * Tolerant reader for non-primary compatible providers. The primary vivo path uses
 * [VivoOpenAIChatResponseReader], which follows the official choices[0].message.content path.
 */
object VendorResponseReader {
    fun extractAssistantText(body: String): String? {
        val obj = parseObject(body) ?: return body.takeIf { it.isNotBlank() }

        obj["data"]?.let { data ->
            (data as? JsonPrimitive)?.takeIf { it.isString }?.let { return it.content }
            (data as? JsonObject)?.let { d ->
                d.str("content")?.let { return it }
                d.str("text")?.let { return it }
                d.str("reply")?.let { return it }
            }
        }

        (obj["choices"] as? JsonArray)?.firstOrNull()?.let { choice ->
            (choice as? JsonObject)?.let { c ->
                (c["message"] as? JsonObject)?.str("content")?.let { return it }
                c.str("text")?.let { return it }
            }
        }

        obj.str("content")?.let { return it }
        obj.str("result")?.let { return it }
        obj.str("output")?.let { return it }
        return null
    }
}

internal fun parseObject(text: String): JsonObject? =
    try {
        json.parseToJsonElement(text) as? JsonObject
    } catch (e: Exception) {
        null
    }

internal fun JsonObject?.str(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

internal fun JsonObject?.int(key: String): Int? =
    (this?.get(key) as? JsonPrimitive)?.intOrNull

internal fun JsonObject?.double(key: String): Double? =
    (this?.get(key) as? JsonPrimitive)?.doubleOrNull

private val json = Json { ignoreUnknownKeys = true; isLenient = true }
