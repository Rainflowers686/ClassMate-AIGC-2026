package com.classmate.core.exporting

import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyReportTest {
    private val now = 1_700_000_000_000L
    private val session = SampleCourses.seriesSession(now)
    private val result = SampleCourses.seriesAnalysis(now)

    private fun snapshotWithPractice(): LearningSnapshot {
        val kp = result.knowledgePoints.first()
        val task = ReviewTask(
            taskId = "t1",
            knowledgePointId = kp.id,
            courseSessionId = result.sessionId,
            courseTitle = "高等数学 - 数项级数",
            title = kp.title,
            reason = "需要多练",
            priority = 8,
            difficulty = "MEDIUM",
            estimatedMinutes = 10,
            createdAt = now,
            dueAt = now,
            nextReviewAt = now,
            status = ReviewTaskStatus.DUE,
            sourceProvider = "LOCAL",
            sourceProfile = "local_only",
            sourceModel = "",
            counters = FeedbackCounters(needExample = 1, wrongAnswer = 1),
        )
        return LearningSnapshot(tasks = listOf(task))
    }

    private fun report(): StudyReport = StudyReportBuilder.build(
        courseTitle = session.title,
        result = result,
        session = session,
        snapshot = snapshotWithPractice(),
        askAnswers = emptyList(),
        sourceSummaryLine = "资料来源：粘贴文本 · 来源数 1 · 片段数 3",
        transcriptSummaryLine = null,
        sourceTypeLabels = listOf("粘贴文本"),
        providerLabel = "Official BlueLM / qwen3.5-plus",
        generatedAtEpochMs = now,
    )

    private fun reportWith(localSuggestion: String?): StudyReport = StudyReportBuilder.build(
        courseTitle = session.title,
        result = result,
        session = session,
        snapshot = snapshotWithPractice(),
        askAnswers = emptyList(),
        sourceSummaryLine = null,
        transcriptSummaryLine = null,
        sourceTypeLabels = listOf("粘贴文本"),
        providerLabel = "Official BlueLM / qwen3.5-plus",
        generatedAtEpochMs = now,
        localSuggestion = localSuggestion,
    )

    @Test
    fun onDeviceLocalSuggestionRendersInEveryFormatWhenPresent() {
        val r = reportWith("由端侧 BlueLM 生成：先复习 p 级数判别法。")
        assertTrue(StudyReportRenderer.renderMarkdown(r).contains("学习建议（端侧本地智能）"))
        assertTrue(StudyReportRenderer.renderMarkdown(r).contains("由端侧 BlueLM 生成：先复习 p 级数判别法。"))
        assertTrue(StudyReportRenderer.renderPlainText(r).contains("由端侧 BlueLM 生成"))
        assertTrue(StudyReportRenderer.renderHtml(r).contains("学习建议（端侧本地智能）"))
        assertTrue(StudyReportRenderer.renderSlidesHtml(r).contains("学习建议（端侧本地智能）"))
    }

    @Test
    fun absentSuggestionMeansNoSuggestionSection() {
        assertFalse(StudyReportRenderer.renderMarkdown(report()).contains("学习建议（端侧本地智能）"))
    }

    @Test
    fun safetyPlaceholderSuggestionIsRenderedHonestlyNotFabricated() {
        val r = reportWith("模型不可用，建议先复习高优先级任务并重新测试。")
        val md = StudyReportRenderer.renderMarkdown(r)
        assertTrue(md.contains("模型不可用，建议先复习高优先级任务并重新测试。"))
        assertFalse(md.contains("由端侧 BlueLM 生成"))
    }

    @Test
    fun markdownHasAllStudySectionsAndPracticeList() {
        val md = StudyReportRenderer.renderMarkdown(report())
        listOf(
            "ClassMate 学习报告", "一、课程概要", "二、核心知识点", "三、证据链",
            "四、微测题", "五、需要多练清单", "六、复习计划", "七、问这节课", "八、资料来源摘要", "九、隐私与说明",
        ).forEach { assertTrue("missing section: $it", md.contains(it)) }
        assertTrue(md.contains(result.knowledgePoints.first().title))
        assertTrue(md.contains("练习题")) // recommended search keywords present in 需要多练清单
    }

    @Test
    fun usesNeedMorePracticeNotNeedExample() {
        val md = StudyReportRenderer.renderMarkdown(report())
        assertTrue(md.contains("需要多练"))
        assertFalse(md.contains("需要例题"))
    }

    @Test
    fun containsNoUiDumpOrForbiddenTokens() {
        val md = StudyReportRenderer.renderMarkdown(report())
        val html = StudyReportRenderer.renderHtml(report())
        val txt = StudyReportRenderer.renderPlainText(report())
        val combined = md + "\n" + html + "\n" + txt
        listOf(
            "evidence_segment_id", "weak_point", "reasoning_content", "Authorization", "Bearer",
            "appKey", "apiKey", "app_id", "prompt", "messages", "vendor body",
        ).forEach { assertFalse("must not contain $it", combined.contains(it, ignoreCase = true)) }
    }

    @Test
    fun htmlIsStructuredAndPlainTextHasNoMarkdownHashes() {
        val html = StudyReportRenderer.renderHtml(report())
        assertTrue(html.contains("<!doctype html>"))
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("@page")) // A4 print style

        val txt = StudyReportRenderer.renderPlainText(report())
        assertFalse(txt.contains("## "))
        assertTrue(txt.contains("核心知识点"))
    }

    @Test
    fun reviewQueueOnlyReportRendersWithoutCourse() {
        val report = StudyReportBuilder.reviewQueueOnly(snapshotWithPractice(), "LocalFallback", now)
        val md = StudyReportRenderer.renderMarkdown(report)
        assertTrue(md.contains("需要多练清单"))
        assertTrue(md.contains("复习计划"))
        assertFalse(md.contains("需要例题"))
    }
}
