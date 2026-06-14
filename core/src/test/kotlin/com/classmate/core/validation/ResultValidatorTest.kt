package com.classmate.core.validation

import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultValidatorTest {

    private val validator = ResultValidator()

    @Test
    fun curatedSampleAnalysisIsValid() {
        val report = validator.validate(SampleCourses.seriesAnalysis(), SampleCourses.seriesSession())
        assertTrue(report.issues.toString(), report.ok)
    }

    @Test
    fun knowledgePointWithoutEvidenceIsRejected() {
        val session = SampleCourses.seriesSession()
        val result = SampleCourses.seriesAnalysis()
        val broken = result.copy(
            knowledgePoints = result.knowledgePoints.mapIndexed { i, kp ->
                if (i == 0) kp.copy(evidence = emptyList()) else kp
            },
        )
        val report = validator.validate(broken, session)
        assertFalse(report.ok)
        assertTrue(report.issues.any { it.code == "KP_NO_EVIDENCE" })
    }

    @Test
    fun questionReferencingUnknownKnowledgePointIsRejected() {
        val session = SampleCourses.seriesSession()
        val result = SampleCourses.seriesAnalysis()
        val broken = result.copy(
            quizQuestions = result.quizQuestions.mapIndexed { i, q ->
                if (i == 0) q.copy(testedKnowledgePointIds = listOf("kp_does_not_exist")) else q
            },
        )
        val report = validator.validate(broken, session)
        assertFalse(report.ok)
        assertTrue(report.issues.any { it.code == "Q_KP_UNKNOWN" })
    }
}
