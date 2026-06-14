package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * A real *concept* distilled from the lecture — e.g. "级数收敛与发散", not a copy of a
 * paragraph. The product rule is hard: a knowledge point is only valid if it is bound to
 * its origin ([sourceSegmentId]) and backed by at least one locatable [EvidenceSpan].
 * Conclusions without evidence are rejected before they reach the UI (see ResultValidator).
 */
@Serializable
data class KnowledgePoint(
    val id: String,
    val title: String,
    val summary: String,
    val sourceSegmentId: String,
    val evidence: List<EvidenceSpan>,
    val importance: Importance,
    val difficulty: Difficulty,
    val relatedPointIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val schemaVersion: Int = ClassMateSchema.VERSION,
) {
    /** True iff at least one structurally well-formed evidence span backs this point. */
    val hasEvidence: Boolean get() = evidence.any { it.isWellFormed() }

    /** Composite priority used by the review planner before learner data is available. */
    val intrinsicPriority: Double get() = importance.weight * 0.6 + difficulty.weight * 0.4
}
