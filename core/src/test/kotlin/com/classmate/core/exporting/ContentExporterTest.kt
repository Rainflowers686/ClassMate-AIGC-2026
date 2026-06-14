package com.classmate.core.exporting

import com.classmate.core.mindmap.MindMapBuilder
import com.classmate.core.model.LearningState
import com.classmate.core.review.ReviewPlanner
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentExporterTest {
    private val session = SampleCourses.seriesSession()
    private val result = SampleCourses.seriesAnalysis()
    private val state = LearningState.seed(result.sessionId, result.knowledgePoints, 1_700_000_000_000L)
    private val plan = ReviewPlanner { 1_700_000_000_000L }.plan(result, state)
    private val exporter = ContentExporter()

    @Test
    fun exportsTimeline() {
        val doc = exporter.exportTimeline(session, result, ExportFormat.MARKDOWN)

        assertTrue(doc.fileName.endsWith(".md"))
        assertTrue(doc.content.contains("Knowledge timeline"))
        assertTrue(doc.content.contains(result.knowledgePoints.first().title))
        assertTrue(doc.content.contains("evidence"))
    }

    @Test
    fun exportsQuizWithAnswersAndExplanations() {
        val doc = exporter.exportQuiz(session, result, ExportFormat.PLAIN_TEXT)

        assertTrue(doc.fileName.endsWith(".txt"))
        assertTrue(doc.content.contains("Micro quiz"))
        assertTrue(doc.content.contains(result.quizQuestions.first().stem))
        assertTrue(doc.content.contains("answer"))
        assertTrue(doc.content.contains("explanation"))
    }

    @Test
    fun exportsReviewPlan() {
        val doc = exporter.exportReviewPlan(session, plan, ExportFormat.HTML)

        assertTrue(doc.fileName.endsWith(".html"))
        assertTrue(doc.content.contains("<!doctype html>"))
        assertTrue(doc.content.contains("Review plan"))
        assertTrue(doc.content.contains("knowledge_point_ids"))
    }

    @Test
    fun exportsFullReportWithMindMapAndVideos() {
        val mindMap = MindMapBuilder.fromAnalysis(result, session.title, state)
        val doc = exporter.exportFullReport(session, result, plan, mindMap, emptyList(), ExportFormat.MARKDOWN)

        assertTrue(doc.content.contains("Knowledge timeline"))
        assertTrue(doc.content.contains("Micro quiz"))
        assertTrue(doc.content.contains("Review plan"))
        assertTrue(doc.content.contains("思维导图"))
        assertTrue(doc.content.contains("Video recommendations"))
    }

    @Test
    fun exportRedactsSensitiveTokens() {
        val dirtySession = session.copy(title = "Authorization Bearer appKey apiKey app_id reasoning_content")
        val mindMap = MindMapBuilder.fromAnalysis(result, dirtySession.title, state)
        val doc = exporter.exportFullReport(dirtySession, result, plan, mindMap, emptyList(), ExportFormat.MARKDOWN)

        listOf("appKey", "apiKey", "Authorization", "Bearer", "app_id", "reasoning_content").forEach {
            assertFalse("export must not contain $it", doc.content.contains(it, ignoreCase = true))
            assertFalse("filename must not contain $it", doc.fileName.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun fileNameIsSafe() {
        val name = ExportFileNames.safe("bad:/name*? appKey <report>", ExportFormat.MARKDOWN)

        assertTrue(name.endsWith(".md"))
        listOf(":", "/", "\\", "*", "?", "<", ">", "|", "appKey").forEach {
            assertFalse(name.contains(it, ignoreCase = true))
        }
    }
}
