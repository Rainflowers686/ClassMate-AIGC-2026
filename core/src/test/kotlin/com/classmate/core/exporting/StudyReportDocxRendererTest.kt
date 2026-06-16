package com.classmate.core.exporting

import com.classmate.core.audio.CourseEssenceAudioExporter
import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import com.classmate.core.sample.SampleCourses
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyReportDocxRendererTest {
    private val now = 1_700_000_000_000L
    private val session = SampleCourses.seriesSession(now)
    private val result = SampleCourses.seriesAnalysis(now)

    private fun report(title: String = "高等数学 <A&B>") = StudyReportBuilder.build(
        courseTitle = title,
        result = result,
        session = session.copy(title = title),
        snapshot = LearningSnapshot(
            tasks = listOf(
                ReviewTask(
                    taskId = "t1",
                    knowledgePointId = result.knowledgePoints.first().id,
                    courseSessionId = result.sessionId,
                    courseTitle = title,
                    title = result.knowledgePoints.first().title,
                    reason = "wrong answer",
                    priority = 8,
                    difficulty = "MEDIUM",
                    estimatedMinutes = 8,
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
        ),
        askAnswers = emptyList(),
        sourceSummaryLine = "资料来源：粘贴文本",
        transcriptSummaryLine = null,
        sourceTypeLabels = listOf("文本"),
        providerLabel = "Official BlueLM / qwen3.5-plus",
        generatedAtEpochMs = now,
    ).let { it.copy(courseEssenceScript = CourseEssenceAudioExporter.buildScript(it)) }

    @Test
    fun rendersRealDocxPackageWithRequiredParts() {
        val entries = unzip(StudyReportDocxRenderer.render(report()))

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("docProps/core.xml"))
        assertTrue(entries.containsKey("docProps/app.xml"))
        assertTrue(entries.containsKey("word/document.xml"))
        assertTrue(entries.containsKey("word/styles.xml"))
    }

    @Test
    fun documentXmlContainsLearningContentAndEscapesXml() {
        val entries = unzip(StudyReportDocxRenderer.render(report()))
        val document = entries.getValue("word/document.xml")

        assertTrue(document.contains("高等数学 &lt;A&amp;B&gt;"))
        assertTrue(document.contains("Evidence"))
        assertTrue(document.contains("AI 来源说明"))
        assertTrue(document.contains("薄弱点"))
        assertTrue(document.contains("复习计划"))
        assertTrue(document.contains("课程精华音频脚本"))
        assertFalse(document.contains("高等数学 <A&B>"))
        assertFalse(document.contains("Authorization"))
        assertFalse(document.contains("Bearer"))
        assertFalse(document.contains("reasoning_content"))
    }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        return entries
    }
}
