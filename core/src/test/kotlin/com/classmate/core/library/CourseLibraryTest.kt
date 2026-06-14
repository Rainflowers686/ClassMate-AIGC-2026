package com.classmate.core.library

import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseLibraryTest {
    @Test
    fun recordsWithSameCourseAreAggregated() {
        val summaries = CourseLibraryBuilder.build(
            records = listOf(
                record("a", "Calculus - Series", 1000, 2),
                record("b", "Calculus - Limits", 2000, 3),
            ),
            learningSnapshot = LearningSnapshot(tasks = listOf(task("Calculus"))),
        )

        val calculus = summaries.single()
        assertEquals("Calculus", calculus.courseName)
        assertEquals(2, calculus.lessonCount)
        assertEquals(5, calculus.knowledgePointTotal)
        assertEquals(1, calculus.dueReviewTaskCount)
        assertEquals("BlueLM official", calculus.recentProvider)
    }

    @Test
    fun blankTitlesAreGroupedAsUntitledCourse() {
        val summaries = CourseLibraryBuilder.build(listOf(record("a", "   ", 1000, 1)))

        assertEquals("Untitled Course", summaries.single().courseName)
        assertTrue(summaries.single().courseKey.contains("untitled"))
    }

    private fun record(id: String, title: String, time: Long, kp: Int): CourseRecordSnapshot =
        CourseRecordSnapshot(
            id = id,
            title = title,
            createdAtEpochMs = time,
            providerName = "BLUELM",
            profileLabel = "official_bluelm",
            knowledgePointCount = kp,
            quizCount = kp,
            fallbackUsed = false,
        )

    private fun task(courseTitle: String): ReviewTask =
        ReviewTask(
            taskId = "task_1",
            knowledgePointId = "kp_1",
            courseSessionId = "session_1",
            courseTitle = courseTitle,
            title = "Series convergence",
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
            counters = FeedbackCounters(),
        )
}
