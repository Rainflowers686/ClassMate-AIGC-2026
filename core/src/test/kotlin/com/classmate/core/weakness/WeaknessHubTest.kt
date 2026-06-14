package com.classmate.core.weakness

import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeaknessHubTest {
    @Test
    fun wrongTooHardAndNeedExampleEnterWeaknessHub() {
        val snapshot = LearningSnapshot(
            tasks = listOf(
                task("wrong", counters = FeedbackCounters(wrongAnswer = 1)),
                task("hard", counters = FeedbackCounters(tooHard = 1)),
                task("example", counters = FeedbackCounters(needExample = 1)),
            ),
        )

        val items = WeaknessHub.fromSnapshot(snapshot)

        assertEquals(3, items.size)
        assertTrue(items.any { "video_search" in it.suggestedActions })
        assertTrue(items.any { "find_examples" in it.suggestedActions })
    }

    @Test
    fun masteredRemovesWeaknessAndEvidenceWrongNeedsHumanReview() {
        val snapshot = LearningSnapshot(
            tasks = listOf(
                task("mastered", counters = FeedbackCounters(wrongAnswer = 2, mastered = 1)),
                task("evidence", counters = FeedbackCounters(evidenceWrong = 1), needsHumanReview = true),
            ),
        )

        val items = WeaknessHub.fromSnapshot(snapshot)

        assertEquals(1, items.size)
        assertEquals("evidence", items.single().title)
        assertTrue(items.single().needsHumanReview)
        assertTrue("check_evidence" in items.single().suggestedActions)
        assertFalse(items.any { it.title == "mastered" })
    }

    private fun task(title: String, counters: FeedbackCounters, needsHumanReview: Boolean = false): ReviewTask =
        ReviewTask(
            taskId = "task_$title",
            knowledgePointId = "kp_$title",
            courseSessionId = "session_1",
            courseTitle = "Calculus",
            title = title,
            reason = "review",
            priority = 5,
            difficulty = "MEDIUM",
            estimatedMinutes = 5,
            createdAt = 0,
            dueAt = 0,
            nextReviewAt = 0,
            status = ReviewTaskStatus.DUE,
            sourceProvider = "BLUELM",
            sourceProfile = "official_bluelm",
            sourceModel = "qwen3.5-plus",
            counters = counters,
            needsHumanReview = needsHumanReview,
        )
}
