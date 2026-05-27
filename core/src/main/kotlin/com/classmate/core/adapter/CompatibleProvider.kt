package com.classmate.core.adapter

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.network.HttpEngine
import com.classmate.core.network.HttpError
import com.classmate.core.network.HttpRequest
import com.classmate.core.validation.ResultValidator
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Real HTTP provider against any OpenAI-compatible chat/completions endpoint.
 *
 * Works with DashScope, DeepSeek, SiliconFlow, Together, vLLM, etc. — any
 * vendor that accepts:
 *   POST {api_base_url}
 *   Authorization: Bearer {api_key}
 *   { "model": "...", "messages": [{role,content}], "temperature": 0,
 *     "response_format": {"type": "json_object"} }
 * and returns:
 *   { "choices": [ { "message": { "content": "..." } } ] }
 *
 * The chain (spec §11 / task §6):
 *   PromptBuilder ─► HTTP ─► JsonExtractor ─► CourseAnalysisResult ─►
 *   ResultValidator ─► (caller runs EvidenceValidator + RedactedLogger)
 *
 * Everything that can go wrong is translated into [ModelCallException] with a
 * specific [ModelCallException.Reason] so the ViewModel can decide whether to
 * surface the error or fall back to DemoProvider.
 */
class CompatibleProvider(
    private val config: CompatibleConfig,
    private val httpEngine: HttpEngine,
    private val json: Json = DefaultJson
) : ModelProvider {

    override val name: String = "compatible"

    override suspend fun analyzeCourse(input: CourseAnalysisInput): CourseAnalysisResult {
        if (!config.isUsable()) {
            throw ModelCallException(
                ModelCallException.Reason.CONFIG_MISSING,
                "compatible.api_base_url / api_key / model must be set in config.local.json"
            )
        }

        val prompt = PromptBuilder.build(input)
        val requestBody = buildRequestBody(prompt)

        val response = try {
            httpEngine.execute(
                HttpRequest(
                    method = "POST",
                    url = config.apiBaseUrl,
                    headers = mapOf(
                        "Authorization" to "Bearer ${config.apiKey}",
                        "Content-Type" to "application/json; charset=utf-8",
                        "Accept" to "application/json"
                    ),
                    body = requestBody
                )
            )
        } catch (e: HttpError) {
            throw ModelCallException(ModelCallException.Reason.HTTP_ERROR, e.message ?: "HTTP error", e)
        }

        if (!response.isSuccess) {
            // Only echo a short prefix of the response body so debugging is
            // possible without dumping vendor-side error messages wholesale.
            throw ModelCallException(
                ModelCallException.Reason.HTTP_ERROR,
                "HTTP ${response.statusCode}; body prefix=${response.body.take(200)}"
            )
        }

        val modelContent = extractMessageContent(response.body)
        val rawJson = JsonExtractor.extract(modelContent)

        val result = try {
            json.decodeFromString(CourseAnalysisResult.serializer(), rawJson)
        } catch (e: SerializationException) {
            throw ModelCallException(
                ModelCallException.Reason.DESERIALIZE_FAILED,
                "could not deserialize model JSON to CourseAnalysisResult: ${e.message}",
                e
            )
        }

        val validation = ResultValidator.validate(result)
        if (!validation.passed) {
            throw ModelCallException(
                ModelCallException.Reason.VALIDATION_FAILED,
                "ResultValidator issues: ${validation.issues.joinToString("; ")}"
            )
        }

        return result
    }

    /**
     * Builds the JSON request body. We force temperature=0 and (where the
     * vendor honors it) response_format=json_object — both are best-effort
     * nudges; the JsonExtractor still has to handle prose-wrapped responses.
     */
    private fun buildRequestBody(prompt: String): String {
        val payload = buildJsonObject {
            put("model", JsonPrimitive(config.model))
            put("temperature", JsonPrimitive(0))
            put("response_format", buildJsonObject { put("type", JsonPrimitive("json_object")) })
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("system"))
                            put("content", JsonPrimitive(PromptBuilder.SYSTEM_RULES.trim()))
                        }
                    )
                    add(
                        buildJsonObject {
                            put("role", JsonPrimitive("user"))
                            put("content", JsonPrimitive(prompt))
                        }
                    )
                }
            )
        }
        return payload.toString()
    }

    /**
     * Pulls choices[0].message.content out of the response envelope. Falls
     * back to the raw body when shape doesn't match — some vendors return
     * a bare JSON object that IS the assistant output (no envelope).
     */
    private fun extractMessageContent(responseBody: String): String {
        val element = try {
            json.parseToJsonElement(responseBody)
        } catch (e: SerializationException) {
            return responseBody
        }
        val obj = element as? JsonObject ?: return responseBody
        val choices = obj["choices"] as? JsonArray ?: return responseBody
        val first = choices.firstOrNull() as? JsonObject ?: return responseBody
        val message = first["message"] as? JsonObject ?: return responseBody
        val content = message["content"] as? JsonPrimitive ?: return responseBody
        return content.content
    }

    companion object {
        val DefaultJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
}
