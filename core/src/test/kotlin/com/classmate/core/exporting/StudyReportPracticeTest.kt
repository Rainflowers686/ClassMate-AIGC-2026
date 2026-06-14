package com.classmate.core.exporting

import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.practice.PracticeMode
import com.classmate.core.practice.PracticeNeedItem
import com.classmate.core.practice.PracticeResult
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyReportPracticeTest {
    private val now = 1_700_000_000_000L
    private val session = SampleCourses.seriesSession(now)
    private val result = SampleCourses.seriesAnalysis(now)

    private fun lastPractice() = PracticeResult(
        sessionId = "practice_1",
        courseSessionId = result.sessionId,
        courseTitle = session.title,
        mode = PracticeMode.NEED_MORE_PRACTICE,
        itemCount = 5,
        correctCount = 2,
        wrongCount = 2,
        masteredCount = 0,
        needMorePracticeCount = 1,
        durationMs = 60_000,
        relatedKnowledgePointTitles = result.knowledgePoints.take(2).map { it.title },
        needPracticeItems = listOf(PracticeNeedItem(result.knowledgePoints.first().title, "高等数学 数项级数 练习题")),
        nextSuggestion = "重点复习答错的知识点，并做同类练习题。",
    )

    private fun report() = StudyReportBuilder.build(
        courseTitle = session.title,
        result = result,
        session = session,
        snapshot = LearningSnapshot(),
        askAnswers = emptyList(),
        sourceSummaryLine = "资料来源：粘贴文本 · 来源数 1 · 片段数 3",
        transcriptSummaryLine = null,
        sourceTypeLabels = listOf("粘贴文本"),
        providerLabel = "LocalFallback",
        generatedAtEpochMs = now,
        lastPractice = lastPractice(),
    )

    @Test
    fun markdownHasPracticeSectionWithNeedMorePractice() {
        val md = StudyReportRenderer.renderMarkdown(report())
        assertTrue(md.contains("十、专项练习与错题本"))
        assertTrue(md.contains("本轮练习模式：需要多练"))
        assertTrue(md.contains("推荐搜索词"))
        assertTrue(md.contains("练习题"))
        assertTrue(md.contains("需要多练"))
        assertFalse(md.contains("需要例题"))
    }

    @Test
    fun htmlHasPracticeSectionAndNoForbiddenTokens() {
        val html = StudyReportRenderer.renderHtml(report())
        val md = StudyReportRenderer.renderMarkdown(report())
        val txt = StudyReportRenderer.renderPlainText(report())
        assertTrue(html.contains("十、专项练习与错题本"))
        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content", "prompt", "messages", "evidence_segment_id").forEach {
            assertFalse((html + md + txt).contains(it, ignoreCase = true))
        }
    }
}
