package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * One actionable review step. Every step answers the three product questions:
 *  - 做什么 -> [title] + [activity]
 *  - 为什么 -> [rationale]
 *  - 预计几分钟 -> [estimatedMinutes]
 * and is bound to the knowledge point(s) it serves ([knowledgePointIds], required).
 */
@Serializable
data class ReviewStep(
    val id: String,
    val order: Int,
    val activity: ReviewActivityType,
    val title: String,
    val rationale: String,
    val estimatedMinutes: Int,
    val knowledgePointIds: List<String>,
    val relatedQuestionIds: List<String> = emptyList(),
)

/** Records *why* the plan looks the way it does, so the UI can explain itself to the judge/learner. */
@Serializable
data class ReviewBasis(
    val consideredImportance: Boolean = true,
    val consideredDifficulty: Boolean = true,
    val consideredWrongAnswers: Boolean = true,
    val consideredFeedback: Boolean = true,
    val feedbackCount: Int = 0,
    val wrongAnswerCount: Int = 0,
)

/**
 * A personalised review path, generated from the analysis + learning state + feedback.
 * Bound to its knowledge points (via each step), so nothing in the plan is unanchored.
 */
@Serializable
data class ReviewPlan(
    val id: String,
    val sessionId: String,
    val steps: List<ReviewStep>,
    val basis: ReviewBasis,
    val generatedAtEpochMs: Long,
    val schemaVersion: Int = ClassMateSchema.VERSION,
) {
    val totalEstimatedMinutes: Int get() = steps.sumOf { it.estimatedMinutes }
    val knowledgePointIds: List<String> get() = steps.flatMap { it.knowledgePointIds }.distinct()
}
