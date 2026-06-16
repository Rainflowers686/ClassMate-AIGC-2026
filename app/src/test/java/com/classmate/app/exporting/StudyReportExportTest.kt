package com.classmate.app.exporting

import com.classmate.core.exporting.StudyReportBuilder
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

/** Stage 6: the printable export is built from one StudyReport for every format, and is dump-free. */
class StudyReportExportTest {
    private val now = 1_700_000_000_000L
    private val session = SampleCourses.seriesSession(now)
    private val result = SampleCourses.seriesAnalysis(now)

    private fun report() = StudyReportBuilder.build(
        courseTitle = session.title,
        result = result,
        session = session,
        snapshot = LearningSnapshot(
            tasks = listOf(
                ReviewTask(
                    taskId = "t1", knowledgePointId = result.knowledgePoints.first().id,
                    courseSessionId = result.sessionId, courseTitle = "高等数学 - 数项级数",
                    title = result.knowledgePoints.first().title, reason = "需要多练", priority = 8,
                    difficulty = "MEDIUM", estimatedMinutes = 10, createdAt = now, dueAt = now, nextReviewAt = now,
                    status = ReviewTaskStatus.DUE, sourceProvider = "LOCAL", sourceProfile = "local_only", sourceModel = "",
                    counters = FeedbackCounters(needExample = 1),
                ),
            ),
        ),
        askAnswers = emptyList(),
        sourceSummaryLine = "资料来源：粘贴文本 · 来源数 1 · 片段数 3",
        transcriptSummaryLine = "转写来源：音频转写 · 字幕段落 2 · 有时间戳 1 · 说话人 教师1/学生0/未知1",
        sourceTypeLabels = listOf("粘贴文本", "音频转写"),
        providerLabel = "Official BlueLM / qwen3.5-plus",
        generatedAtEpochMs = now,
    )

    @Test
    fun pdfStartsWithPdfHeader() {
        val artifact = ExportCenter.artifactFromStudyReport(report(), ExportFileFormat.PDF, createdAt = 1L)
        assertTrue(artifact.mimeType == "application/pdf")
        assertTrue(artifact.bytes.decodeToString().startsWith("%PDF"))
    }

    @Test
    fun docxIsRealOpenXmlPackage() {
        val artifact = ExportCenter.artifactFromStudyReport(report(), ExportFileFormat.DOCX, createdAt = 1L)
        val entries = unzip(artifact.bytes)
        val document = entries.getValue("word/document.xml")

        assertTrue(artifact.fileName.endsWith(".docx"))
        assertTrue(artifact.mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("word/document.xml"))
        assertTrue(document.contains("AI 来源说明"))
        assertTrue(document.contains("Evidence"))
        assertFalse(document.contains("Authorization"))
        assertFalse(document.contains("Bearer"))
        assertFalse(document.contains("reasoning_content"))
    }

    @Test
    fun markdownHasStructuredSectionsAndNeedMorePractice() {
        val md = ExportCenter.artifactFromStudyReport(report(), ExportFileFormat.MARKDOWN, createdAt = 1L).bytes.decodeToString()
        listOf("核心知识点", "微测题", "需要多练清单", "复习计划", "资料来源摘要").forEach { assertTrue(md.contains(it)) }
        assertTrue(md.contains("需要多练"))
        assertFalse(md.contains("需要例题"))
        assertTrue(md.contains("转写来源")) // OCR/字幕/Ask/Review source summary carried through
    }

    @Test
    fun htmlIsStructuredNotUiDump() {
        val html = ExportCenter.artifactFromStudyReport(report(), ExportFileFormat.HTML, createdAt = 1L).bytes.decodeToString()
        assertTrue(html.contains("<!doctype html>"))
        assertTrue(html.contains("<h2>"))
        assertFalse(html.contains("evidence_segment_id"))
        assertFalse(html.contains("weak_point"))
    }

    @Test
    fun textHasNoMarkdownHashes() {
        val txt = ExportCenter.artifactFromStudyReport(report(), ExportFileFormat.TEXT, createdAt = 1L).bytes.decodeToString()
        assertFalse(txt.contains("## "))
        assertTrue(txt.contains("核心知识点"))
    }

    @Test
    fun allFormatsAreSafeAndForbiddenTokenFree() {
        ExportFileFormat.entries.forEach { format ->
            val artifact = ExportCenter.artifactFromStudyReport(report(), format, createdAt = 1L)
            assertTrue(artifact.bytes.isNotEmpty())
            assertFalse("$format leaked sensitive content", artifact.containsSensitiveContent)
            val text = artifact.bytes.decodeToString()
            listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content", "prompt", "messages").forEach {
                assertFalse("$format contains $it", text.contains(it, ignoreCase = true))
            }
        }
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
