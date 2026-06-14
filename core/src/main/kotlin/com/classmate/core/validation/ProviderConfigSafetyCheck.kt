package com.classmate.core.validation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Two jobs, both about keeping secrets out:
 *  1. classify a single value as placeholder vs. real secret (used to decide whether a
 *     networked provider is actually configured), and
 *  2. scan an *example* config to assert it contains only placeholders — the test/CI guard
 *     that config.example.json never ships a real key.
 *
 * Crucially, findings record the offending FIELD NAME only, never the value, so this check
 * can run in CI logs without itself leaking anything.
 */
object ProviderConfigSafetyCheck {

    private val placeholderTokens = listOf(
        "YOUR_BLUELM_APP_ID", "YOUR_BLUELM_APP_KEY", "YOUR_COMPATIBLE_API_KEY",
        "YOUR_COMPATIBLE_ENDPOINT", "YOUR_MODEL_NAME", "REPLACE_ME", "CHANGEME", "PLACEHOLDER",
    )

    private val secretFieldNames = setOf("appid", "appkey", "apikey", "key", "secret", "token", "authorization")

    fun isPlaceholder(value: String): Boolean =
        value.isBlank() || value.startsWith("YOUR_") || placeholderTokens.any { value.contains(it, ignoreCase = true) }

    fun isRealSecret(value: String): Boolean =
        value.isNotBlank() && !isPlaceholder(value) && value.trim().length >= 6

    data class SafetyResult(val isExampleSafe: Boolean, val findings: List<String>)

    /** Returns isExampleSafe=false if any secret-named field holds a real-looking value. */
    fun inspectExampleConfig(jsonText: String): SafetyResult {
        val root = try {
            Json.parseToJsonElement(jsonText)
        } catch (e: Exception) {
            return SafetyResult(false, listOf("INVALID_JSON"))
        }
        val findings = mutableListOf<String>()
        walk(root, "") { key, value ->
            if (key.lowercase() in secretFieldNames && isRealSecret(value)) {
                findings += key
            }
        }
        return SafetyResult(findings.isEmpty(), findings.distinct())
    }

    private fun walk(element: JsonElement, key: String, onLeaf: (String, String) -> Unit) {
        when (element) {
            is JsonObject -> element.forEach { (k, v) -> walk(v, k, onLeaf) }
            is JsonArray -> element.forEach { walk(it, key, onLeaf) }
            is JsonPrimitive -> if (element.isString) onLeaf(key, element.content)
            else -> {}
        }
    }
}
