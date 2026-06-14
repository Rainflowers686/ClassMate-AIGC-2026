package com.classmate.core.feedback

import com.classmate.core.model.FeedbackEvent
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import com.classmate.core.model.LearningState
import com.classmate.core.model.QuizAttempt

/**
 * Pure, deterministic reducer that evolves a [LearningState] from quiz attempts and learner
 * feedback. Keeping this side-effect-free makes the feedback loop unit-testable and makes the
 * review planner's behaviour predictable.
 */
class LearningStateUpdater(private val clock: () -> Long = System::currentTimeMillis) {

    /** A correct answer raises mastery for the tested points; a wrong one lowers it. */
    fun applyAttempt(state: LearningState, attempt: QuizAttempt): LearningState {
        if (attempt.testedKnowledgePointIds.isEmpty()) return state
        val updated = state.pointStates.toMutableMap()
        attempt.testedKnowledgePointIds.forEach { kpId ->
            val s = state.stateOf(kpId)
            val delta = if (attempt.isCorrect) CORRECT_DELTA else WRONG_DELTA
            updated[kpId] = s.copy(
                mastery = (s.mastery + delta).coerceIn(0.0, 1.0),
                attempts = s.attempts + 1,
                correct = s.correct + if (attempt.isCorrect) 1 else 0,
            )
        }
        return state.copy(pointStates = updated, updatedAtEpochMs = clock())
    }

    /** Feedback nudges mastery and/or flags a point for review. */
    fun applyFeedback(state: LearningState, feedback: FeedbackEvent): LearningState {
        val kpId = feedback.targetId?.takeIf { feedback.targetKind == FeedbackTargetKind.KNOWLEDGE_POINT }
            ?: return state.copy(updatedAtEpochMs = clock()) // analysis-level feedback handled by the planner
        val s = state.stateOf(kpId)
        val updated = when (feedback.type) {
            FeedbackType.ALREADY_MASTERED -> s.copy(mastery = maxOf(s.mastery, 0.92), flaggedByFeedback = false)
            FeedbackType.TOO_EASY -> s.copy(mastery = (s.mastery + 0.1).coerceAtMost(1.0))
            FeedbackType.TOO_HARD, FeedbackType.NEED_MORE_EXAMPLES ->
                s.copy(flaggedByFeedback = true, mastery = (s.mastery - 0.05).coerceAtLeast(0.0))
            FeedbackType.NOT_ACCURATE, FeedbackType.EVIDENCE_WRONG -> s.copy(flaggedByFeedback = true)
            FeedbackType.MISSING_KEY_POINT -> s
        }
        return state.copy(
            pointStates = state.pointStates + (kpId to updated),
            updatedAtEpochMs = clock(),
        )
    }

    private companion object {
        const val CORRECT_DELTA = 0.25
        const val WRONG_DELTA = -0.15
    }
}
