package com.classmate.core.sample

import com.classmate.core.validation.EvidenceValidator
import com.classmate.core.validation.ResultValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the hand-authored demo content: its evidence must really anchor to the text. */
class SampleCourseTest {

    @Test
    fun sessionSplitsIntoEightAddressableSegments() {
        assertEquals(8, SampleCourses.seriesSession().segments.size)
    }

    @Test
    fun everyKnowledgePointHasResolvableEvidence() {
        val session = SampleCourses.seriesSession()
        val analysis = SampleCourses.seriesAnalysis()
        val evidence = EvidenceValidator()
        analysis.knowledgePoints.forEach { kp ->
            assertTrue("KP ${kp.id} (${kp.title}) should have evidence", kp.hasEvidence)
            kp.evidence.forEach { span ->
                assertNull("KP ${kp.id} evidence must anchor to source", evidence.validate(session, span))
            }
        }
    }

    @Test
    fun everyQuestionIsBoundEvidencedAndAnswerable() {
        val analysis = SampleCourses.seriesAnalysis()
        val kpIds = analysis.knowledgePoints.map { it.id }.toSet()
        analysis.quizQuestions.forEach { q ->
            assertTrue(q.testedKnowledgePointIds.isNotEmpty())
            assertTrue(q.testedKnowledgePointIds.all { it in kpIds })
            assertTrue(q.correctOptionIds.isNotEmpty())
            assertTrue("Question ${q.id} must cite evidence", q.hasEvidence)
        }
    }

    @Test
    fun questionsCoverMultipleLearningTypes() {
        val types = SampleCourses.seriesAnalysis().quizQuestions.map { it.type }.toSet()
        assertTrue("Expected varied question types, got $types", types.size >= 4)
    }

    @Test
    fun analysisPassesTheSameValidatorAsModelOutput() {
        val report = ResultValidator().validate(SampleCourses.seriesAnalysis(), SampleCourses.seriesSession())
        assertTrue(report.issues.toString(), report.ok)
    }
}
