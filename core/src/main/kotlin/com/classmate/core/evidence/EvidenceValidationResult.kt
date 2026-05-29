package com.classmate.core.evidence

/**
 * Outcome of running EvidenceValidator over one CourseAnalysisResult.
 *
 * v0.4 separates two match rates so a UI panel can never again say
 * "structure invalid + 100% match":
 *  - [strictEvidenceMatchRate]: span found verbatim in the ORIGINAL input
 *    segment's text. This is the truthful number for "is the model quoting
 *    real source material".
 *  - [lenientEvidenceMatchRate]: span found in either the original input
 *    text OR the model's correctedText. Tolerant of ASR-fix rewrites.
 *
 * - [schemaPassed]: all referential integrity checks succeeded.
 * - [spanMismatches]: items the UI should downgrade per spec §11.2 item 4.
 */
data class EvidenceValidationResult(
    val schemaPassed: Boolean,
    val missingKpSegmentRefs: List<String>,
    val missingQuizSegmentRefs: List<String>,
    val missingRelatedKpRefs: List<String>,
    val spanMismatches: List<EvidenceSpanMismatch>,
    val strictEvidenceMatchRate: Double,
    val lenientEvidenceMatchRate: Double
)

data class EvidenceSpanMismatch(
    val ownerKind: String, // "knowledge_point" | "quiz"
    val ownerId: String,
    val segmentId: String,
    val span: String
)
