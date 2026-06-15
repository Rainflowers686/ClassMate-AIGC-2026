package com.classmate.core.tools

import com.classmate.core.exporting.StudyReportBuilder
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewEventType
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InternalFunctionRouterTest {
    private val now = 1_700_000_000_000L
    private val session = SampleCourses.seriesSession(now)
    private val result = SampleCourses.seriesAnalysis(now)
    private val snapshot = LearningSnapshot()
    private val report = StudyReportBuilder.build(
        courseTitle = session.title,
        result = result,
        session = session,
        snapshot = snapshot,
        askAnswers = emptyList(),
        sourceSummaryLine = "sources",
        transcriptSummaryLine = null,
        sourceTypeLabels = listOf("text"),
        providerLabel = "Official BlueLM / qwen3.5-plus",
        generatedAtEpochMs = now,
    )

    @Test
    fun searchEvidenceDoesNotRequireConfirmation() {
        val tool = InternalFunctionRouter().execute(
            InternalToolCall(InternalToolName.SEARCH_EVIDENCE, query = "级数"),
            result,
            snapshot,
        )

        assertTrue(tool.success)
        assertFalse(tool.requiresUserConfirmation)
    }

    @Test
    fun mutatingToolsRequireConfirmation() {
        val router = InternalFunctionRouter()
        val createPractice = router.execute(InternalToolCall(InternalToolName.CREATE_PRACTICE, courseTitle = session.title, now = now), result, snapshot)
        val updateMastery = router.execute(
            InternalToolCall(InternalToolName.UPDATE_MASTERY, knowledgePointId = result.knowledgePoints.first().id, eventType = ReviewEventType.WRONG_ANSWER, now = now),
            result,
            snapshot,
        )
        val review = router.execute(InternalToolCall(InternalToolName.CREATE_REVIEW_TASK, courseTitle = session.title, now = now), result, snapshot)

        assertTrue(createPractice.requiresUserConfirmation)
        assertTrue(updateMastery.requiresUserConfirmation)
        assertTrue(review.requiresUserConfirmation)
    }

    @Test
    fun reportAndAudioScriptToolsReturnPayloads() {
        val router = InternalFunctionRouter()
        val export = router.execute(InternalToolCall(InternalToolName.EXPORT_STUDY_REPORT), result, snapshot, report)
        val audioScript = router.execute(InternalToolCall(InternalToolName.CREATE_ESSENCE_AUDIO_SCRIPT), result, snapshot, report)

        assertTrue(export.success)
        assertTrue(audioScript.success)
        assertTrue(export.requiresUserConfirmation)
        assertTrue(audioScript.requiresUserConfirmation)
    }
}
