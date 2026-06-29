package com.classmate.app.l3

import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-4: a course that HAS knowledge points but whose model returned no quiz must still get a basic,
 * answerable 微测 generated locally — never a silent zero-quiz dead end. (ui.result is rebuilt from this
 * snapshot, so filling the snapshot here is what makes the quiz screen + the timeline button work.)
 */
class LocalFallbackQuizTest {

    private val now = 1_700_000_000_000L
    private val providerSummary = ProviderConfigSummary.defaults()

    @Test
    fun analysisWithKnowledgeButNoQuizGetsBasicAnswerableQuizLocally() {
        val session = SampleCourses.seriesSession(now)
        // Simulate the model returning knowledge points but NO quiz.
        val result = SampleCourses.seriesAnalysis(now).copy(quizQuestions = emptyList())
        assertTrue("precondition: the course has knowledge points", result.knowledgePoints.isNotEmpty())

        val snapshot = L3LearningPipeline().buildFromAnalysis(
            session = session,
            result = result,
            sourceType = L3SourceType.TEXT,
            providerSummary = providerSummary,
            now = now,
        )

        assertFalse("a course with knowledge points is never left with zero quiz", snapshot.questions.isEmpty())
        val q = snapshot.questions.first()
        assertTrue("quiz has a stem", q.stem.isNotBlank())
        assertTrue("quiz has >= 2 options", q.options.size >= 2)
        assertTrue("quiz has a correct answer", q.correctAnswer.isNotBlank())
        assertTrue("quiz has an explanation", q.explanation.isNotBlank())
        assertTrue("quiz is bound to a knowledge point", q.knowledgePointId.isNotBlank())
        assertTrue("quiz is bound to evidence", q.evidenceIds.isNotEmpty())

        // It survives the round-trip to a CourseAnalysisResult — what QuizScreen / the timeline button read.
        val artifacts = L3LearningPipeline().toCourseArtifacts(snapshot, now)
        assertNotNull("artifacts build (not null) once a quiz exists", artifacts)
        assertFalse("result.quizQuestions is populated", artifacts!!.result.quizQuestions.isEmpty())
        assertTrue("the generated quiz has a correct option id", artifacts.result.quizQuestions.first().correctOptionIds.isNotEmpty())
    }
}
