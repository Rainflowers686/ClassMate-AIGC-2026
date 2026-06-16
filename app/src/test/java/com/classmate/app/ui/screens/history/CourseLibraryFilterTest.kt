package com.classmate.app.ui.screens.history

import com.classmate.app.data.HistoryRecord
import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import com.classmate.core.library.CourseSummary
import com.classmate.core.sample.SampleCourses
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseLibraryFilterTest {

    @Test
    fun searchMatchesTitleSubjectProviderAndKnowledgePoint() {
        val summaries = listOf(mathSummary(), localSummary())
        val history = listOf(record("1", "高等数学：数项级数", "BLUELM", false), record("2", "C++: 指针", "LOCAL_FALLBACK", true))

        assertEquals("高等数学", filterCourseLibrary(summaries, history, LearningSnapshot(), "高等数学", CourseLibraryFilter.ALL, CourseLibrarySort.RECENT).single().courseName)
        assertEquals("高等数学", filterCourseLibrary(summaries, history, LearningSnapshot(), "云端蓝心", CourseLibraryFilter.ALL, CourseLibrarySort.RECENT).single().courseName)
        assertEquals("C++", filterCourseLibrary(summaries, history, LearningSnapshot(), "安全占位", CourseLibraryFilter.ALL, CourseLibrarySort.RECENT).single().courseName)
        assertTrue(filterCourseLibrary(summaries, history, LearningSnapshot(), "级数", CourseLibraryFilter.ALL, CourseLibrarySort.RECENT).isNotEmpty())
    }

    @Test
    fun filtersDueWeakOfficialAndLocalCourses() {
        val summaries = listOf(mathSummary(due = 2), localSummary())
        val history = listOf(record("1", "高等数学：数项级数", "BLUELM", false), record("2", "C++: 指针", "LOCAL_FALLBACK", true))
        val weak = LearningSnapshot(tasks = listOf(task("高等数学", FeedbackCounters(wrongAnswer = 1))))

        assertEquals("高等数学", filterCourseLibrary(summaries, history, weak, "", CourseLibraryFilter.DUE, CourseLibrarySort.RECENT).single().courseName)
        assertEquals("高等数学", filterCourseLibrary(summaries, history, weak, "", CourseLibraryFilter.WEAK, CourseLibrarySort.RECENT).single().courseName)
        assertEquals("高等数学", filterCourseLibrary(summaries, history, weak, "", CourseLibraryFilter.OFFICIAL, CourseLibrarySort.RECENT).single().courseName)
        assertEquals("C++", filterCourseLibrary(summaries, history, weak, "", CourseLibraryFilter.LOCAL, CourseLibrarySort.RECENT).single().courseName)
    }

    @Test
    fun sortByRecentAndTitle() {
        val summaries = listOf(localSummary(), mathSummary(latest = 20))
        val history = emptyList<HistoryRecord>()

        assertEquals("高等数学", filterCourseLibrary(summaries, history, LearningSnapshot(), "", CourseLibraryFilter.ALL, CourseLibrarySort.RECENT).first().courseName)
        assertEquals("C++", filterCourseLibrary(summaries, history, LearningSnapshot(), "", CourseLibraryFilter.ALL, CourseLibrarySort.TITLE).first().courseName)
    }

    @Test
    fun emptyFilteredStateHasClearFilterCopy() {
        val source = listOf(
            File("src/main/java/com/classmate/app/ui/screens/history/HistoryScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/history/HistoryScreen.kt"),
        ).first { it.exists() }.readText()

        assertTrue(source.contains("没有找到课程"))
        assertTrue(source.contains("清除筛选"))
    }

    private fun mathSummary(latest: Long = 10, due: Int = 0) = CourseSummary(
        courseKey = "高等数学",
        courseName = "高等数学",
        subject = "高等数学",
        latestLearningTime = latest,
        lessonCount = 2,
        knowledgePointTotal = 8,
        quizTotal = 6,
        dueReviewTaskCount = due,
        recentProvider = "云端蓝心",
        recentFallbackUsed = false,
    )

    private fun localSummary() = CourseSummary(
        courseKey = "c++",
        courseName = "C++",
        subject = "C++",
        latestLearningTime = 5,
        lessonCount = 1,
        knowledgePointTotal = 4,
        quizTotal = 4,
        dueReviewTaskCount = 0,
        recentProvider = "Local fallback",
        recentFallbackUsed = true,
    )

    private fun record(id: String, title: String, provider: String, fallback: Boolean): HistoryRecord =
        HistoryRecord(
            id = id,
            title = title,
            createdAtEpochMs = id.hashCode().toLong(),
            providerName = provider,
            profileLabel = if (fallback) "local_only" else "official_bluelm",
            model = "qwen3.5-plus",
            knowledgePointCount = 6,
            quizCount = 6,
            fallbackUsed = fallback,
            validationStatus = "PASS",
            session = SampleCourses.seriesSession(1L).copy(title = title),
            result = SampleCourses.seriesAnalysis(1L),
        )

    private fun task(course: String, counters: FeedbackCounters) = ReviewTask(
        taskId = "task_1",
        knowledgePointId = "kp_1",
        courseSessionId = "session_1",
        courseTitle = course,
        title = "薄弱点",
        reason = "刚答错，优先复习",
        priority = 3,
        difficulty = "MEDIUM",
        estimatedMinutes = 5,
        createdAt = 1L,
        dueAt = 1L,
        nextReviewAt = 1L,
        status = ReviewTaskStatus.DUE,
        sourceProvider = "BLUELM",
        sourceProfile = "official_bluelm",
        sourceModel = "qwen3.5-plus",
        counters = counters,
    )
}
