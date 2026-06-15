package com.classmate.core.audio

import com.classmate.core.exporting.StudyReportBuilder
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseEssenceAudioTest {
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
    fun scriptGenerationUsesReportKnowledgeAndAvoidsVoiceCloneCopy() {
        val script = CourseEssenceAudioExporter.buildScript(report())
        val text = script.toPlainText()

        assertTrue(text.contains(session.title))
        assertTrue(text.contains(result.knowledgePoints.first().title))
        assertFalse(text.contains("声音" + "复刻"))
        assertFalse(text.contains("老师" + "声音"))
    }

    @Test
    fun configMissingTtsReturnsScriptOnly() {
        val audio = CourseEssenceAudioExporter.generate(report(), ConfigMissingTtsProvider())

        assertTrue(audio.status == CourseEssenceAudioStatus.SCRIPT_ONLY_CONFIG_MISSING)
        assertFalse(audio.hasAudio)
        assertTrue(audio.script.toPlainText().isNotBlank())
    }

    @Test
    fun fakeTtsSuccessReturnsAudioBytes() {
        val audio = CourseEssenceAudioExporter.generate(report(), FakeTtsProvider("audio".toByteArray()))

        assertTrue(audio.status == CourseEssenceAudioStatus.AUDIO_READY)
        assertTrue(audio.hasAudio)
    }
}
