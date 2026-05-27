package com.classmate.core.logging

/**
 * Allow-listed fields permitted to appear in a redacted model-call log line.
 *
 * Spec §13.1 says: no real API key, no App key, no PII, no raw audio, no full
 * request headers. The way we enforce that here is with a closed shape — if
 * something doesn't appear in this data class, the logger can't emit it.
 *
 * v0.3.5 added: [structureValid], [fallbackUsed], [apiKeyRedacted]. These
 * surface in the UI as "校验状态" / "是否使用兜底" and serve as the audit
 * marker that no key material ever entered the log line.
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
    /** True iff ResultValidator passed AND EvidenceValidator's schema check passed. */
    val structureValid: Boolean,
    /** Null when the call failed before evidence could be checked. */
    val evidenceMatchRate: Double?,
    /** True iff the run fell back from the requested provider to DemoProvider (or a lower-priority provider). */
    val fallbackUsed: Boolean,
    /** Always true at construction — the field exists so reviewers can grep one log line and confirm. */
    val apiKeyRedacted: Boolean = true,
    val errorType: String?
)
