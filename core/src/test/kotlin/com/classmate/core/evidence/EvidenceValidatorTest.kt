package com.classmate.core.evidence

import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.model.EvidenceSpan
import com.classmate.core.validation.EvidenceProblem
import com.classmate.core.validation.EvidenceValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EvidenceValidatorTest {

    private val session = CourseSegmenter.buildSession(
        id = "s", title = "t",
        rawText = "级数收敛与发散很重要。\n部分和数列是关键概念。",
        nowMs = 0L,
    )
    private val resolver = EvidenceResolver()
    private val validator = EvidenceValidator()

    @Test
    fun resolvesQuoteToSpanAndValidates() {
        val span = resolver.resolve(session.segment("seg_1")!!, "级数收敛")
        assertNotNull(span)
        assertNull(validator.validate(session, span!!))
    }

    @Test
    fun whitespaceInsensitiveResolutionStillExact() {
        // Quote with extra spaces should still map back to an exact source substring.
        val span = resolver.resolve(session.segment("seg_2")!!, "部分 和 数列")
        assertNotNull(span)
        assertEquals("部分和数列", span!!.quote)
        assertNull(validator.validate(session, span))
    }

    @Test
    fun missingSegmentIsReported() {
        val bad = EvidenceSpan("seg_99", 0, 2, "级数")
        assertEquals(EvidenceProblem.MISSING_SEGMENT, validator.validate(session, bad))
    }

    @Test
    fun quoteMismatchIsReported() {
        val bad = EvidenceSpan("seg_1", 0, 2, "错误")
        assertEquals(EvidenceProblem.QUOTE_MISMATCH, validator.validate(session, bad))
    }

    @Test
    fun unresolvableQuoteReturnsNull() {
        assertNull(resolver.resolve(session.segment("seg_1")!!, "这句话根本不在原文里"))
    }
}
