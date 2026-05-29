package com.classmate.core.logging

/**
 * Allow-listed fields permitted to appear in a redacted model-call log line.
 *
 * Spec §13.1 forbids logging real API key, App key, PII, raw audio, or full
 * request headers. The closed shape is the enforcement: if it doesn't appear
 * here, the logger can't emit it.
 *
 * v0.4 added: [strictEvidenceMatchRate] to surface the honest-quote rate
 * alongside the tolerant rate; [structureValid] / [fallbackUsed] /
 * [apiKeyRedacted] from v0.3.5 are retained.
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
    /** ResultValidator passed AND EvidenceValidator schema check passed. */
    val structureValid: Boolean,
    /** span found in INPUT text only — the "is the model quoting source" number. */
    val strictEvidenceMatchRate: Double?,
    /** span found in input OR result.correctedText — tolerates ASR-fix rewrites. */
    val lenientEvidenceMatchRate: Double?,
    /** true iff the run fell back from the requested provider to a local provider. */
    val fallbackUsed: Boolean,
    /** Always true at construction — the field exists so reviewers can grep one log line and confirm. */
    val apiKeyRedacted: Boolean = true,
    val errorType: String?
)
