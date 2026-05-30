package com.classmate.core.evidence

import com.classmate.core.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceValidatorTest {

    @Test
    fun validate_reportsPerfectRatesWhenEvidenceSpansQuoteInput() {
        val result = EvidenceValidator.validate(TestFixtures.input(), TestFixtures.validResult())

        assertTrue(result.schemaPassed)
        assertEquals(emptyList<EvidenceSpanMismatch>(), result.spanMismatches)
        assertEquals(1.0, result.strictEvidenceMatchRate, 0.0)
        assertEquals(1.0, result.lenientEvidenceMatchRate, 0.0)
    }

    @Test
    fun validate_reportsSpanMismatchWhenEvidenceSpanCannotBeFound() {
        val valid = TestFixtures.validResult()
        val brokenKp = valid.segments.first().knowledgePoints.first().copy(
            evidenceSpan = "This sentence is not present in source or corrected text"
        )
        val brokenResult = valid.copy(
            segments = valid.segments.mapIndexed { index, segment ->
                if (index == 0) segment.copy(knowledgePoints = listOf(brokenKp)) else segment
            }
        )

        val result = EvidenceValidator.validate(TestFixtures.input(), brokenResult)

        assertTrue(result.schemaPassed)
        assertEquals(1, result.spanMismatches.size)
        assertEquals("knowledge_point", result.spanMismatches.single().ownerKind)
        assertEquals("kp_001", result.spanMismatches.single().ownerId)
        assertTrue(result.strictEvidenceMatchRate < 1.0)
        assertTrue(result.lenientEvidenceMatchRate < 1.0)
    }
}
