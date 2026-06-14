package com.classmate.core.review

import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.FeedbackEvent
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import com.classmate.core.model.Ids
import com.classmate.core.model.LearningState
import com.classmate.core.model.ReviewActivityType
import com.classmate.core.model.ReviewBasis
import com.classmate.core.model.ReviewPlan
import com.classmate.core.model.ReviewStep

/**
 * Builds a personalised review path from importance, difficulty, wrong answers and feedback.
 * Every knowledge point gets a priority score; the top ones become ordered steps, each bound
 * to its knowledge point(s) and tagged with an activity, a rationale ("why"), and a minute
 * estimate. Deterministic, so the plan is explainable and testable.
 */
class ReviewPlanner(private val clock: () -> Long = System::currentTimeMillis) {

    fun plan(
        result: CourseAnalysisResult,
        state: LearningState,
        feedback: List<FeedbackEvent> = emptyList(),
        maxSteps: Int = 6,
    ): ReviewPlan {
        val flaggedKpIds = feedback
            .filter { it.targetKind == FeedbackTargetKind.KNOWLEDGE_POINT }
            .filter { it.type in REVIEW_TRIGGERS }
            .mapNotNull { it.targetId }
            .toSet()
        val masteredKpIds = feedback
            .filter { it.targetKind == FeedbackTargetKind.KNOWLEDGE_POINT && it.type == FeedbackType.ALREADY_MASTERED }
            .mapNotNull { it.targetId }
            .toSet()

        var totalWrong = 0
        val scored = result.knowledgePoints.map { kp ->
            val s = state.stateOf(kp.id)
            val wrong = (s.attempts - s.correct).coerceAtLeast(0)
            totalWrong += wrong
            val flagged = kp.id in flaggedKpIds || s.flaggedByFeedback
            val score =
                kp.importance.weight * 1.0 +
                    kp.difficulty.weight * 0.6 +
                    (1.0 - s.mastery) * 1.2 +
                    wrong * 0.5 +
                    (if (flagged) 0.8 else 0.0) -
                    (if (kp.id in masteredKpIds) 1.5 else 0.0)
            ScoredPoint(kp.id, score, wrong, flagged)
        }.sortedByDescending { it.score }

        val steps = scored
            .filter { it.score > MIN_SCORE }
            .take(maxSteps)
            .mapIndexed { index, sp -> buildStep(index + 1, sp, result, state) }

        val basis = ReviewBasis(
            feedbackCount = feedback.size,
            wrongAnswerCount = totalWrong,
        )
        return ReviewPlan(
            id = Ids.reviewPlan(result.sessionId, clock()),
            sessionId = result.sessionId,
            steps = steps,
            basis = basis,
            generatedAtEpochMs = clock(),
        )
    }

    private fun buildStep(
        order: Int,
        sp: ScoredPoint,
        result: CourseAnalysisResult,
        state: LearningState,
    ): ReviewStep {
        val kp = result.knowledgePoint(sp.kpId)!!
        val s = state.stateOf(kp.id)
        val (activity, minutes, why) = when {
            sp.flagged -> Triple(
                ReviewActivityType.STUDY_EVIDENCE, 4,
                "你反馈这里需要再确认，先回看原文证据，确认结论是否成立。",
            )
            sp.wrong > 0 -> Triple(
                ReviewActivityType.REDO_QUIZ, 5,
                "上一轮这道题答错了，重做并对照错因说明。",
            )
            s.attempts == 0 -> Triple(
                ReviewActivityType.REVIEW_CONCEPT, 3,
                "这是尚未自测的${kp.importance.displayZh}概念，先过一遍。",
            )
            s.mastery < 0.8 -> Triple(
                ReviewActivityType.SELF_EXPLAIN, 4,
                "掌握度还不稳，用自己的话把「${kp.title}」讲清楚。",
            )
            else -> Triple(
                ReviewActivityType.WORKED_EXAMPLE, 6,
                "已基本掌握，做一道巩固性练习收尾。",
            )
        }
        return ReviewStep(
            id = Ids.reviewStep(order),
            order = order,
            activity = activity,
            title = "${activity.displayZh}：${kp.title}",
            rationale = why,
            estimatedMinutes = minutes,
            knowledgePointIds = listOf(kp.id),
            relatedQuestionIds = result.questionsFor(kp.id).map { it.id },
        )
    }

    private data class ScoredPoint(val kpId: String, val score: Double, val wrong: Int, val flagged: Boolean)

    private companion object {
        const val MIN_SCORE = -1.0
        val REVIEW_TRIGGERS = setOf(
            FeedbackType.NOT_ACCURATE,
            FeedbackType.EVIDENCE_WRONG,
            FeedbackType.TOO_HARD,
            FeedbackType.NEED_MORE_EXAMPLES,
        )
    }
}
