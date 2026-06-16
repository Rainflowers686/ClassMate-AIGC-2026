package com.classmate.core.translation

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.capture.CaptureHttpResponse
import com.classmate.core.capture.CaptureProviderConfig
import com.classmate.core.capture.CaptureTransport
import com.classmate.core.capture.CaptureTransportNotConfigured
import com.classmate.core.capture.NotConfiguredCaptureTransport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class VivoTranslationProvider(
    private val config: CaptureProviderConfig = CaptureProviderConfig.ABSENT,
    private val transport: CaptureTransport = NotConfiguredCaptureTransport,
    private val timeoutMs: Long = 10_000,
) : TranslationProvider {
    override fun translate(text: String, sourceLanguage: String, targetLanguage: String): TranslationProviderResult {
        if (!config.isConfigured) return ConfigMissingTranslationProvider().translate(text, sourceLanguage, targetLanguage)
        if (text.isBlank()) {
            return TranslationProviderResult(TranslationStatus.FAILED, "", AiExecutionSource.SAFE_PLACEHOLDER, "No text to translate.")
        }
        return try {
            parseResponse(
                transport.postJson(
                    "https://${config.domain}/translation/text",
                    mapOf("Authorization" to config.authHeader(), "Content-Type" to "application/json"),
                    body(text, sourceLanguage, targetLanguage),
                    timeoutMs,
                ),
            )
        } catch (_: CaptureTransportNotConfigured) {
            ConfigMissingTranslationProvider().translate(text, sourceLanguage, targetLanguage)
        } catch (_: Exception) {
            TranslationProviderResult(TranslationStatus.FAILED, "", AiExecutionSource.SAFE_PLACEHOLDER, "Translation request failed.")
        }
    }

    fun parseResponse(response: CaptureHttpResponse): TranslationProviderResult {
        if (response.status !in 200..299) {
            return TranslationProviderResult(TranslationStatus.FAILED, "", AiExecutionSource.SAFE_PLACEHOLDER, "Translation service returned a non-success status.")
        }
        val root = parseTranslationObject(response.body) ?: return TranslationProviderResult(TranslationStatus.FAILED, "", AiExecutionSource.SAFE_PLACEHOLDER, "Translation response could not be parsed.")
        val code = root.str("code") ?: root.str("error_code") ?: "0"
        if (code != "0" && !code.equals("success", ignoreCase = true)) {
            return TranslationProviderResult(TranslationStatus.FAILED, "", AiExecutionSource.SAFE_PLACEHOLDER, "Translation service returned an error code.")
        }
        val data = root.obj("data") ?: root
        val translated = data.str("translation") ?: data.str("translatedText") ?: data.str("targetText") ?: data.str("dst")
        return if (translated.isNullOrBlank()) {
            TranslationProviderResult(TranslationStatus.FAILED, "", AiExecutionSource.SAFE_PLACEHOLDER, "Translation response was empty.")
        } else {
            TranslationProviderResult(TranslationStatus.TRANSLATED, translated, AiExecutionSource.CLOUD, "Official translation provider returned a derived note.")
        }
    }

    private fun body(text: String, sourceLanguage: String, targetLanguage: String): String =
        """{"text":${quote(text)},"sourceLanguage":${quote(sourceLanguage)},"targetLanguage":${quote(targetLanguage)}}"""

    private fun quote(value: String): String =
        buildString {
            append('"')
            value.forEach { c ->
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
            append('"')
        }
}

private val translationJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun parseTranslationObject(body: String): JsonObject? =
    try { translationJson.parseToJsonElement(body).jsonObject } catch (_: Exception) { null }

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
