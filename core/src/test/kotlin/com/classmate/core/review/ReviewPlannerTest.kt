package com.classmate.core.review

import com.classmate.core.feedback.LearningStateUpdater
import com.classmate.core.model.FeedbackEvent
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import com.classmate.core.model.LearningState
import com.classmate.core.model.QuizAttempt
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPlannerTest {

    private val planner = ReviewPlanner(clock = { 5000L })
    private val analysis = SampleCourses.seriesAnalysis()

    @Test
    fun planBindsKnowledgePointsAndEstimatesTime() {
        val state = LearningState.seed(analysis.sessionId, analysis.knowledgePoints, 0L)
        val plan = planner.plan(analysis, state)

        assertTrue(plan.steps.isNotEmpty())
        assertTrue(plan.totalEstimatedMinutes > 0)
        val kpIds = analysis.knowledgePoints.map { it.id }.toSet()
        plan.steps.forEach { step ->
            assertTrue(step.knowledgePointIds.isNotEmpty())
            assertTrue(step.knowledgePointIds.all { it in kpIds })
        }
    }

    @Test
    fun wrongAnswersAreRecordedAndRaisePriority() {
        val updater = LearningStateUpdater(clock = { 1L })
        var state = LearningState.seed(analysis.sessionId, analysis.knowledgePoints, 0L)
        state = updater.applyAttempt(
            state,
            QuizAttempt("a", "q_5", listOf("kp_8"), listOf("opt_B"), isCorrect = false, answeredAtEpochMs = 0L),
        )
        val plan = planner.plan(analysis, state)

        assertTrue(plan.basis.wrongAnswerCount >= 1)
        assertTrue(
            "kp_8 should be prioritised after a wrong answer",
            plan.steps.take(3).any { it.knowledgePointIds.contains("kp_8") },
        )
    }

    @Test
    fun feedbackIsRecordedInBasis() {
        val state = LearningState.seed(analysis.sessionId, analysis.knowledgePoints, 0L)
        val feedback = listOf(
            FeedbackEvent("f", FeedbackType.ALREADY_MASTERED, FeedbackTargetKind.KNOWLEDGE_POINT, "kp_3", createdAtEpochMs = 0L),
        )
        val plan = planner.plan(analysis, state, feedback, maxSteps = 4)
        assertEquals(1, plan.basis.feedbackCount)
        assertTrue(plan.steps.isNotEmpty())
    }
}
