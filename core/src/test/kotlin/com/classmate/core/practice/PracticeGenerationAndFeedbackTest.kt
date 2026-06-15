package com.classmate.core.practice

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ai.AiExecutionStatus
import com.classmate.core.ai.StageOutcome
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.ProviderKind
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeGenerationAndFeedbackTest {
    private val now = 1_700_000_000_000L
    private val result = SampleCourses.seriesAnalysis(now)

    @Test
    fun safeFallbackBuildsPracticeFromEvidenceAndCarriesSource() {
        val generated = RoutedPracticeGenerationUseCase().generate(
            PracticeGenerationRequest(result, LearningSnapshot(), PracticeMode.QUICK_REVIEW, now, SampleCourses.SERIES_TITLE),
        )

        val session = generated.value!!.session
        assertEquals(AiExecutionSource.SAFE_PLACEHOLDER, generated.source)
        assertTrue(session.items.isNotEmpty())
        assertTrue(session.items.all { it.source == AiExecutionSource.SAFE_PLACEHOLDER })
        assertTrue(session.items.any { !it.evidenceQuote.isNullOrBlank() })
    }

    @Test
    fun cloudFailureFallsBackToOnDeviceGeneratedPractice() {
        val onDeviceSession = PracticeSessionEngine.build(result, LearningSnapshot(), PracticeMode.QUICK_REVIEW, now, SampleCourses.SERIES_TITLE)
            .withSource(AiExecutionSource.ON_DEVICE, "fake on-device")
        val generated = RoutedPracticeGenerationUseCase().generate(
            PracticeGenerationRequest(result, LearningSnapshot(), PracticeMode.QUICK_REVIEW, now, SampleCourses.SERIES_TITLE),
            cloud = { StageOutcome.Unavailable(AiExecutionStatus.NETWORK_UNAVAILABLE) },
            onDevice = { StageOutcome.Produced(onDeviceSession) },
        )

        assertEquals(AiExecutionSource.ON_DEVICE, generated.source)
        assertEquals(AiExecutionSource.ON_DEVICE, generated.value!!.session.source)
    }

    @Test
    fun insufficientEvidenceDoesNotFabricateQuestions() {
        val empty = CourseAnalysisResult(
            sessionId = "empty",
            knowledgePoints = emptyList(),
            quizQuestions = emptyList(),
            provenance = AnalysisProvenance(ProviderKind.BLUELM, fallbackUsed = false, modelLabel = "qwen3.5-plus", createdAtEpochMs = now),
        )

        val generated = RoutedPracticeGenerationUseCase().generate(
            PracticeGenerationRequest(empty, LearningSnapshot(), PracticeMode.QUICK_REVIEW, now, "Empty"),
        )

        assertFalse(generated.isSuccess)
        assertEquals(null, generated.value)
    }

    @Test
    fun feedbackForChoiceReferencesEvidenceAndNextAction() {
        val session = PracticeSessionEngine.build(result, LearningSnapshot(), PracticeMode.QUICK_REVIEW, now, SampleCourses.SERIES_TITLE)
        val item = session.items.first { it.options.isNotEmpty() }

        val wrong = PracticeFeedbackEngine.evaluateSelectedOptions(item, setOf("not-the-answer"))
        val correct = PracticeFeedbackEngine.evaluateSelectedOptions(item, item.correctOptionIds.toSet())

        assertEquals(PracticeFeedbackCorrectness.INCORRECT, wrong.correctness)
        assertEquals("add_to_review", wrong.nextAction)
        assertNotNull(wrong.evidenceQuote)
        assertEquals(PracticeFeedbackCorrectness.CORRECT, correct.correctness)
        assertEquals(item.knowledgePointId, correct.knowledgePointId)
    }
}
