package com.classmate.core.evidence

/**
 * Outcome of running EvidenceValidator over one CourseAnalysisResult.
 *
 * - [schemaPassed]: all referential integrity checks succeeded
 *   (segment_id and related_kp_id all resolve).
 * - [evidenceMatchRate]: fraction of evidence_span values that appeared
 *   verbatim in their declared source segment. 1.0 means every quote checks
 *   out; 0.0 means none did.
 * - [spanMismatches]: items the UI should NOT highlight (spec §14.3) —
 *   show the source-segment animation only.
 */
data class EvidenceValidationResult(
    val schemaPassed: Boolean,
    val missingKpSegmentRefs: List<String>,
    val missingQuizSegmentRefs: List<String>,
    val missingRelatedKpRefs: List<String>,
    val spanMismatches: List<EvidenceSpanMismatch>,
    val evidenceMatchRate: Double
)

data class EvidenceSpanMismatch(
    val ownerKind: String, // "knowledge_point" | "quiz"
    val ownerId: String,
    val segmentId: String,
    val span: String
)
