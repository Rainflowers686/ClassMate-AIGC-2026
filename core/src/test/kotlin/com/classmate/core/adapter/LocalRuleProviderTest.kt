package com.classmate.core.adapter

import com.classmate.core.TestFixtures
import com.classmate.core.evidence.EvidenceValidator
import com.classmate.core.validation.ResultValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRuleProviderTest {

    @Test
    fun analyzeCourse_closesAllResultReferencesAgainstInput() = runBlocking {
        val input = TestFixtures.input()
        val result = LocalRuleProvider().analyzeCourse(input)

        val inputSegmentIds = input.segments.map { it.segmentId }.toSet()
        val knowledgePoints = result.segments.flatMap { it.knowledgePoints }
        val knowledgePointIds = knowledgePoints.map { it.kpId }.toSet()

        assertEquals(input.segments.map { it.segmentId }, result.segments.map { it.segmentId })
        assertTrue(knowledgePoints.isNotEmpty())
        assertTrue(result.quizzes.isNotEmpty())
        assertTrue(result.reviewPlan.isNotEmpty())
        assertTrue(knowledgePoints.all { it.sourceSegmentId in inputSegmentIds })
        assertTrue(result.quizzes.all { it.sourceSegmentId in inputSegmentIds })
        assertTrue(result.quizzes.all { it.relatedKpId in knowledgePointIds })
        assertTrue(result.reviewPlan.flatMap { it.relatedKpIds }.all { it in knowledgePointIds })

        val structural = ResultValidator.validate(result, input)
        val evidence = EvidenceValidator.validate(input, result)
        assertTrue(structural.issues.toString(), structural.passed)
        assertTrue(evidence.spanMismatches.toString(), evidence.schemaPassed)
        assertEquals(1.0, evidence.strictEvidenceMatchRate, 0.0)
        assertEquals(1.0, evidence.lenientEvidenceMatchRate, 0.0)
    }
}
