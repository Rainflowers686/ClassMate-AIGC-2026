package com.classmate.core.model

import kotlinx.serialization.Serializable

/** Per-knowledge-point learner progress. [mastery] is the single 0..1 signal; [level] is its bucket. */
@Serializable
data class KnowledgePointState(
    val knowledgePointId: String,
    val mastery: Double = 0.0,
    val attempts: Int = 0,
    val correct: Int = 0,
    val flaggedByFeedback: Boolean = false,
) {
    val level: MasteryLevel get() = MasteryLevel.fromMastery(mastery, attempts)
    val needsReview: Boolean
        get() = flaggedByFeedback || level == MasteryLevel.STRUGGLING || level == MasteryLevel.UNSEEN
}

/**
 * The learner's evolving picture of one course session. Updated incrementally from quiz
 * attempts and feedback; consumed by the review planner. Persisted, hence [schemaVersion].
 */
@Serializable
data class LearningState(
    val sessionId: String,
    val pointStates: Map<String, KnowledgePointState> = emptyMap(),
    val updatedAtEpochMs: Long = 0,
    val schemaVersion: Int = ClassMateSchema.VERSION,
) {
    fun stateOf(knowledgePointId: String): KnowledgePointState =
        pointStates[knowledgePointId] ?: KnowledgePointState(knowledgePointId)

    val overallMastery: Double
        get() = if (pointStates.isEmpty()) 0.0 else pointStates.values.sumOf { it.mastery } / pointStates.size

    companion object {
        /** Seed a fresh state with one entry per knowledge point (all UNSEEN). */
        fun seed(sessionId: String, knowledgePoints: List<KnowledgePoint>, nowMs: Long): LearningState =
            LearningState(
                sessionId = sessionId,
                pointStates = knowledgePoints.associate { it.id to KnowledgePointState(it.id) },
                updatedAtEpochMs = nowMs,
            )
    }
}
