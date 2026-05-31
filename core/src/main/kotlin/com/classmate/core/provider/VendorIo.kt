package com.classmate.core.provider

import com.classmate.core.prompt.Prompt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Builds a chat-style request body (system + user messages). This is a reasonable, vendor
 * agnostic shape; the exact vivo BlueLM field names are confirmed when the real endpoint is
 * wired (see BlueLMProvider). Kept tiny and dependency-free.
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
 * Tolerant reader that pulls the assistant's text out of a vendor response envelope without
 * assuming one exact shape. Handles the common BlueLM (`data.content`) and OpenAI
 * (`choices[0].message.content`) layouts. Returns null if no text is found.
 */
object VendorResponseReader {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun extractAssistantText(body: String): String? {
        val root = try {
            json.parseToJsonElement(body)
        } catch (e: Exception) {
            return null
        }
        val obj = root as? JsonObject ?: return (root as? JsonPrimitive)?.takeIf { it.isString }?.content

        // BlueLM style: { "data": "..." } or { "data": { "content": "..." } }
        obj["data"]?.let { data ->
            (data as? JsonPrimitive)?.takeIf { it.isString }?.let { return it.content }
            (data as? JsonObject)?.let { d ->
                d.str("content")?.let { return it }
                d.str("text")?.let { return it }
                d.str("reply")?.let { return it }
            }
        }

        // OpenAI-compatible style: { "choices": [ { "message": { "content": "..." } } ] }
        (obj["choices"] as? JsonArray)?.firstOrNull()?.let { choice ->
            (choice as? JsonObject)?.let { c ->
                (c["message"] as? JsonObject)?.str("content")?.let { return it }
                c.str("text")?.let { return it }
            }
        }

        // Flat fallbacks
        obj.str("content")?.let { return it }
        obj.str("result")?.let { return it }
        obj.str("output")?.let { return it }
        return null
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
}
