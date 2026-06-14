package com.classmate.core.feedback

import com.classmate.core.model.FeedbackEvent
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import com.classmate.core.model.KnowledgePointState
import com.classmate.core.model.LearningState
import com.classmate.core.model.MasteryLevel
import com.classmate.core.model.QuizAttempt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningStateUpdaterTest {

    private val updater = LearningStateUpdater(clock = { 1000L })
    private val base = LearningState(
        sessionId = "s",
        pointStates = mapOf("kp_1" to KnowledgePointState("kp_1")),
        updatedAtEpochMs = 0L,
    )

    @Test
    fun correctAnswerRaisesMastery() {
        val after = updater.applyAttempt(
            base,
            QuizAttempt("a1", "q_1", listOf("kp_1"), listOf("opt_A"), isCorrect = true, answeredAtEpochMs = 0L),
        )
        val s = after.stateOf("kp_1")
        assertEquals(1, s.attempts)
        assertEquals(1, s.correct)
        assertTrue(s.mastery > 0.0)
    }

    @Test
    fun wrongAnswerCountsButDoesNotRaiseMastery() {
        val after = updater.applyAttempt(
            base,
            QuizAttempt("a2", "q_1", listOf("kp_1"), listOf("opt_B"), isCorrect = false, answeredAtEpochMs = 0L),
        )
        val s = after.stateOf("kp_1")
        assertEquals(1, s.attempts)
        assertEquals(0, s.correct)
        assertEquals(0.0, s.mastery, 0.0001)
    }

    @Test
    fun alreadyMasteredFeedbackSetsHighMasteryAndUnflags() {
        val flagged = base.copy(pointStates = mapOf("kp_1" to KnowledgePointState("kp_1", flaggedByFeedback = true)))
        val after = updater.applyFeedback(
            flagged,
            FeedbackEvent("f1", FeedbackType.ALREADY_MASTERED, FeedbackTargetKind.KNOWLEDGE_POINT, "kp_1", createdAtEpochMs = 0L),
        )
        val s = after.stateOf("kp_1")
        assertEquals(MasteryLevel.MASTERED, s.level)
        assertFalse(s.flaggedByFeedback)
    }

    @Test
    fun notAccurateFeedbackFlagsForReview() {
        val after = updater.applyFeedback(
            base,
            FeedbackEvent("f2", FeedbackType.NOT_ACCURATE, FeedbackTargetKind.KNOWLEDGE_POINT, "kp_1", createdAtEpochMs = 0L),
        )
        assertTrue(after.stateOf("kp_1").flaggedByFeedback)
    }
}
