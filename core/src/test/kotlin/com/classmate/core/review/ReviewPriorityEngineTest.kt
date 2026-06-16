package com.classmate.core.review

import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPriorityEngineTest {
    private val now = 1_700_000_000_000L

    @Test
    fun wrongAndDueTasksSortFirstWithReasonAndAction() {
        val snapshot = LearningSnapshot(
            tasks = listOf(
                task("calm", priority = 1, counters = FeedbackCounters(correctAnswer = 2), nextReviewAt = now + 99_000),
                task("wrong", priority = 3, counters = FeedbackCounters(wrongAnswer = 2), nextReviewAt = now),
                task("evidence", priority = 2, counters = FeedbackCounters(evidenceWrong = 1), needsHumanReview = true, nextReviewAt = now),
            ),
        )

        val priorities = ReviewPriorityEngine.prioritize(snapshot, now)

        assertEquals("wrong", priorities.first().title)
        assertTrue(priorities.first().dueReason.contains("wrong answers"))
        assertEquals("retry_practice", priorities.first().recommendedAction)
        assertTrue(priorities.first().estimatedMinutes >= 5)
        assertTrue(priorities.any { it.title == "evidence" && it.recommendedAction == "check_evidence" && it.evidenceReference != null })
    }

    private fun task(
        title: String,
        priority: Int,
        counters: FeedbackCounters,
        nextReviewAt: Long,
        needsHumanReview: Boolean = false,
    ): ReviewTask =
        ReviewTask(
            taskId = "task_$title",
            knowledgePointId = "kp_$title",
            courseSessionId = "course_1",
            courseTitle = "Course",
            title = title,
            reason = "review",
            priority = priority,
            difficulty = "MEDIUM",
            estimatedMinutes = 5,
            createdAt = now - 3L * 24 * 60 * 60 * 1000,
            dueAt = nextReviewAt,
            nextReviewAt = nextReviewAt,
            status = ReviewTaskStatus.DUE,
            sourceProvider = "BLUELM",
            sourceProfile = "official",
            sourceModel = "qwen3.5-plus",
            counters = counters,
            needsHumanReview = needsHumanReview,
        )
}
