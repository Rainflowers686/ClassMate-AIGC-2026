package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * A learner correction that closes the loop. Feedback can target the analysis as a whole,
 * a specific knowledge point, a quiz question, or the review plan ([targetKind]/[targetId]).
 *
 * Feedback drives two things:
 *  - [com.classmate.core.feedback.LearningStateUpdater] (e.g. ALREADY_MASTERED raises mastery,
 *    NOT_ACCURATE / EVIDENCE_WRONG flag a point), and
 *  - [com.classmate.core.review.ReviewPlanner] (flagged points are re-prioritised).
 */
@Serializable
data class FeedbackEvent(
    val id: String,
    val type: FeedbackType,
    val targetKind: FeedbackTargetKind,
    val targetId: String? = null,
    val note: String = "",
    val createdAtEpochMs: Long,
)
