package com.classmate.core.exporting

import com.classmate.core.audio.CourseEssenceAudioExporter
import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import com.classmate.core.safety.TextSafetyResult
import com.classmate.core.safety.TextSafetyStatus
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.sample.SampleCourses
import com.classmate.core.translation.TranslationNote
import com.classmate.core.translation.TranslationStatus
import com.classmate.core.translation.TranslationTargetType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyReportP1Test {
    private val now = 1_700_000_000_000L
    private val session = SampleCourses.seriesSession(now)
    private val result = SampleCourses.seriesAnalysis(now)

    private fun snapshot() = LearningSnapshot(
        tasks = listOf(
            ReviewTask(
                taskId = "t1",
                knowledgePointId = result.knowledgePoints.first().id,
                courseSessionId = result.sessionId,
                courseTitle = session.title,
                title = result.knowledgePoints.first().title,
                reason = "needs review",
                priority = 8,
                difficulty = "MEDIUM",
                estimatedMinutes = 5,
                createdAt = now,
                dueAt = now,
                nextReviewAt = now,
                status = ReviewTaskStatus.DUE,
                sourceProvider = "BLUELM",
                sourceProfile = "official",
                sourceModel = "qwen3.5-plus",
                counters = FeedbackCounters(wrongAnswer = 2, correctAnswer = 1),
            ),
        ),
    )

    @Test
    fun reportContainsWeaknessTranslationSafetyAndAudioScript() {
        val base = StudyReportBuilder.build(
            courseTitle = session.title,
            result = result,
            session = session,
            snapshot = snapshot(),
            askAnswers = emptyList(),
            sourceSummaryLine = "sources",
            transcriptSummaryLine = null,
            sourceTypeLabels = listOf("text"),
            providerLabel = "Official BlueLM / qwen3.5-plus",
            generatedAtEpochMs = now,
            translationNotes = listOf(
                TranslationNote(
                    targetType = TranslationTargetType.EVIDENCE_QUOTE,
                    targetId = "ev_1",
                    sourceText = "series",
                    translatedText = "级数",
                    sourceLanguage = "en",
                    targetLanguage = "zh-CN",
                    source = AiExecutionSource.CLOUD,
                    status = TranslationStatus.TRANSLATED,
                ),
            ),
            safetyResult = TextSafetyResult(TextSafetyStatus.SAFE, AiExecutionSource.SAFE_PLACEHOLDER, "checked"),
        )
        val report = base.copy(courseEssenceScript = CourseEssenceAudioExporter.buildScript(base))
        val md = StudyReportRenderer.renderMarkdown(report)
        val html = StudyReportRenderer.renderHtml(report)

        listOf("薄弱点", "双语学习注记", "课程精华音频脚本", "文本安全检查").forEach {
            assertTrue("missing $it", md.contains(it))
            assertTrue("html missing $it", html.contains(it))
        }
        listOf("Authorization", "Bearer", "appKey", "apiKey", "reasoning_content", "prompt", "messages").forEach {
            assertFalse(md.contains(it, ignoreCase = true))
            assertFalse(html.contains(it, ignoreCase = true))
        }
    }
}
