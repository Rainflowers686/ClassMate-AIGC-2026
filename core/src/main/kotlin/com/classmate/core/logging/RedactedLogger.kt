package com.classmate.core.logging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Emits structured, English-only JSON log lines.
 *
 * Spec §13 forbids logging:
 *   - real API keys / App keys
 *   - user PII
 *   - raw classroom audio
 *   - full request headers
 *
 * Therefore this logger only accepts a small, hand-listed set of fields and
 * NEVER takes a free-form payload. If you find yourself wanting to pass more
 * context, add a named parameter — don't widen the API to a map.
 */
class RedactedLogger(
    private val sink: (String) -> Unit
) {

    fun courseAnalysisCall(
        timestamp: String,
        provider: String,
        inputSegmentCount: Int,
        hotwordCount: Int,
        success: Boolean,
        latencyMs: Long,
        schemaValid: Boolean,
        evidenceMatchRate: Double?,
        errorType: String?
    ) {
        val record = buildMap<String, JsonElement> {
            put("timestamp", JsonPrimitive(timestamp))
            put("provider", JsonPrimitive(provider))
            put("task", JsonPrimitive("course_analysis"))
            put("input_segment_count", JsonPrimitive(inputSegmentCount))
            put("hotword_count", JsonPrimitive(hotwordCount))
            put("success", JsonPrimitive(success))
            put("latency_ms", JsonPrimitive(latencyMs))
            put("schema_valid", JsonPrimitive(schemaValid))
            put("evidence_match_rate", evidenceMatchRate?.let(::JsonPrimitive) ?: JsonNull)
            put("error_type", errorType?.let(::JsonPrimitive) ?: JsonNull)
        }
        sink(JSON.encodeToString(JsonObject(record)))
    }

    companion object {
        private val JSON = Json {
            encodeDefaults = true
            prettyPrint = false
        }
    }
}
