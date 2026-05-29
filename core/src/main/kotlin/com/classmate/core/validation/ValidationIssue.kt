package com.classmate.core.validation

/**
 * Typed reason for one validator finding. Lets the UI render an actionable
 * message instead of a free-form string, and lets logs aggregate.
 *
 * If you add a new kind, also extend the UI's human-readable mapper in
 * AnalyzeScreen / HeroCard — the typed list is the source of truth.
 */
enum class ValidationIssueKind {
    EMPTY_SEGMENTS,
    KP_SOURCE_SEGMENT_NOT_IN_RESULT,
    KP_SOURCE_SEGMENT_NOT_IN_INPUT,
    KP_IMPORTANCE_OUT_OF_RANGE,
    KP_DIFFICULTY_OUT_OF_RANGE,
    QUIZ_SOURCE_SEGMENT_NOT_IN_RESULT,
    QUIZ_SOURCE_SEGMENT_NOT_IN_INPUT,
    QUIZ_RELATED_KP_MISSING,
    QUIZ_ANSWER_INDEX_OUT_OF_RANGE,
    REVIEW_PLAN_KP_MISSING
}

/**
 * One validator finding. [ownerId] is the local id (kp_id / quiz_id / step_id)
 * the issue belongs to, or empty for issues that don't have an owner.
 */
data class ValidationIssue(
    val kind: ValidationIssueKind,
    val ownerId: String,
    val detail: String
)
