package com.classmate.core.mindmap

import com.classmate.core.exporting.ContentExporter
import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MindMapModelTest {
    @Test
    fun buildsMindMapFromAnalysisResult() {
        val result = SampleCourses.seriesAnalysis()
        val model = MindMapBuilder.fromAnalysis(result, SampleCourses.SERIES_TITLE)

        assertEquals(SampleCourses.SERIES_TITLE, model.root)
        assertEquals(result.knowledgePoints.size, model.children.size)
        assertEquals(result.knowledgePoints.first().id, model.children.first().knowledgePointId)
        assertTrue(model.children.first().evidenceSegmentId.isNotBlank())
    }

    @Test
    fun weakPointFlagComesFromLearningSnapshot() {
        val result = SampleCourses.seriesAnalysis()
        val kp = result.knowledgePoints.first()
        val model = MindMapBuilder.fromAnalysis(
            result = result,
            courseTitle = "course",
            learningSnapshot = LearningSnapshot(tasks = listOf(taskFor(kp.id, kp.title))),
        )

        assertTrue(model.children.first { it.knowledgePointId == kp.id }.weakPoint)
    }

    @Test
    fun markdownExportContainsRootAndKnowledgePoint() {
        val result = SampleCourses.seriesAnalysis()
        val model = MindMapBuilder.fromAnalysis(result, "course root")
        val markdown = ContentExporter().markdownMindMap(model)

        assertTrue(markdown.contains("course root"))
        assertTrue(markdown.contains(result.knowledgePoints.first().title))
    }

    private fun taskFor(kpId: String, title: String): ReviewTask =
        ReviewTask(
            taskId = "task_1",
            knowledgePointId = kpId,
            courseSessionId = "session_1",
            courseTitle = "course",
            title = title,
            reason = "weak",
            priority = 10,
            difficulty = "HARD",
            estimatedMinutes = 5,
            createdAt = 0,
            dueAt = 0,
            nextReviewAt = 0,
            status = ReviewTaskStatus.DUE,
            sourceProvider = "BLUELM",
            sourceProfile = "official_bluelm",
            sourceModel = "qwen3.5-plus",
            counters = FeedbackCounters(wrongAnswer = 2),
        )
}
