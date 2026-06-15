package com.classmate.app.exporting

import com.classmate.core.exporting.StudyReportBuilder
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseEssenceScriptExportTest {
    private val now = 1_700_000_000_000L
    private val session = SampleCourses.seriesSession(now)
    private val result = SampleCourses.seriesAnalysis(now)

    private fun report() = StudyReportBuilder.build(
        courseTitle = session.title,
        result = result,
        session = session,
        snapshot = LearningSnapshot(),
        askAnswers = emptyList(),
        sourceSummaryLine = "sources",
        transcriptSummaryLine = null,
        sourceTypeLabels = listOf("text"),
        providerLabel = "Official BlueLM / qwen3.5-plus",
        generatedAtEpochMs = now,
    )

    @Test
    fun courseEssenceScriptFormatExportsPlainTextScript() {
        val artifact = ExportCenter.artifactFromStudyReport(report(), ExportFileFormat.COURSE_ESSENCE_SCRIPT_TEXT, createdAt = 1L)
        val text = artifact.bytes.decodeToString()

        assertTrue(artifact.fileName.endsWith(".txt"))
        assertTrue(text.contains("ClassMate course essence review"))
        assertTrue(text.contains(result.knowledgePoints.first().title))
        assertFalse(text.contains("声音" + "复刻"))
        assertFalse(text.contains("老师" + "声音"))
    }
}
