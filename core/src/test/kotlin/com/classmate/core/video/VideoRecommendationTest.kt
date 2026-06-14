package com.classmate.core.video

import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRecommendationTest {
    @Test
    fun weakPointGeneratesWhitelistedRecommendations() {
        val recs = VideoRecommendationEngine.recommendationsForTask(task(counters = FeedbackCounters(wrongAnswer = 2)))
        val whitelist = VideoRecommendationEngine.whitelist.map { it.name }.toSet()

        assertTrue(recs.isNotEmpty())
        assertTrue(recs.size <= 2)
        assertTrue(recs.all { it.source in whitelist })
        assertEquals("wrong_answer", recs.first().triggeredBy)
    }

    @Test
    fun needExampleAndHighPriorityTriggerRecommendations() {
        val needExample = VideoRecommendationEngine.recommendationsForTask(task(counters = FeedbackCounters(needExample = 1)))
        val highPriority = VideoRecommendationEngine.recommendationsForTask(task(priority = 10))

        assertEquals("need_example", needExample.first().triggeredBy)
        assertEquals("priority_high", highPriority.first().triggeredBy)
    }

    @Test
    fun inactiveTaskDoesNotRecommendAnything() {
        val recs = VideoRecommendationEngine.recommendationsForTask(task(priority = 2))

        assertTrue(recs.isEmpty())
    }

    @Test
    fun searchUrlIsEncodedAndUsesOnlyWhitelistedSources() {
        val recs = VideoRecommendationEngine.recommendationsForTask(
            task(title = "p series \u7ea7\u6570", counters = FeedbackCounters(wrongAnswer = 2)),
        )

        assertTrue(recs.first().searchUrl.contains("p+series+%E7%BA%A7%E6%95%B0"))
        assertFalse(recs.first().searchUrl.contains(" "))
        assertTrue(recs.none { it.source == "Untrusted Video Site" })
    }

    private fun task(
        title: String = "\u7ea7\u6570\u6536\u655b",
        priority: Int = 5,
        counters: FeedbackCounters = FeedbackCounters(),
    ): ReviewTask =
        ReviewTask(
            taskId = "task_1",
            knowledgePointId = "kp_1",
            courseSessionId = "session_1",
            courseTitle = "course",
            title = title,
            reason = "review",
            priority = priority,
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
        )
}
