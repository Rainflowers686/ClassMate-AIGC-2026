package com.classmate.core.logging

/**
 * Allow-listed fields permitted to appear in a redacted model-call log line.
 *
 * Spec §13.1 says: no real API key, no App key, no PII, no raw audio, no full
 * request headers. The way we enforce that here is with a closed shape — if
 * something doesn't appear in this data class, the logger can't emit it.
 *
 * Edit with care. Adding a field is an audit-relevant change.
 */
data class ModelCallLog(
    val timestamp: String,
    val provider: String,
    val task: String,
    val inputSegmentCount: Int,
    val hotwordCount: Int,
    val success: Boolean,
    val latencyMs: Long,
    val schemaValid: Boolean,
    val evidenceMatchRate: Double?,
    val errorType: String?
)
