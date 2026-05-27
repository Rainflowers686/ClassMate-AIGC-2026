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
 * Spec §13.1 forbids logging real API/App keys, user PII, raw audio, or full
 * request headers. The log record is constrained by [ModelCallLog] — we
 * encode that struct rather than accepting a free-form map, so widening the
 * payload requires editing a typed data class.
 */
class RedactedLogger(
    private val sink: (String) -> Unit
) {

    fun courseAnalysisCall(log: ModelCallLog) {
        val record: Map<String, JsonElement> = buildMap {
            put("timestamp", JsonPrimitive(log.timestamp))
            put("provider", JsonPrimitive(log.provider))
            put("task", JsonPrimitive(log.task))
            put("input_segment_count", JsonPrimitive(log.inputSegmentCount))
            put("hotword_count", JsonPrimitive(log.hotwordCount))
            put("success", JsonPrimitive(log.success))
            put("latency_ms", JsonPrimitive(log.latencyMs))
            put("schema_valid", JsonPrimitive(log.schemaValid))
            put("evidence_match_rate", log.evidenceMatchRate?.let(::JsonPrimitive) ?: JsonNull)
            put("error_type", log.errorType?.let(::JsonPrimitive) ?: JsonNull)
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
