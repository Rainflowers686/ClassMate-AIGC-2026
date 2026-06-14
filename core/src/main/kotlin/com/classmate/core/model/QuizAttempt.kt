package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * A single answer the learner submitted. Feeds [com.classmate.core.feedback.LearningStateUpdater]
 * (mastery) and, indirectly, the review planner (wrong answers raise a point's priority).
 */
@Serializable
data class QuizAttempt(
    val id: String,
    val questionId: String,
    val testedKnowledgePointIds: List<String>,
    val selectedOptionIds: List<String>,
    val isCorrect: Boolean,
    val answeredAtEpochMs: Long,
    val elapsedMs: Long = 0,
)
