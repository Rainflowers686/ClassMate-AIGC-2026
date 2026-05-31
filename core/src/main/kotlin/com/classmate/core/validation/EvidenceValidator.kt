package com.classmate.core.validation

import com.classmate.core.model.CourseSession
import com.classmate.core.model.EvidenceSpan

/** Why an evidence span failed to anchor to the source. */
enum class EvidenceProblem { MISSING_SEGMENT, MALFORMED, OUT_OF_RANGE, QUOTE_MISMATCH }

/**
 * Checks that an [EvidenceSpan] actually points at the original text: the segment exists,
 * the offsets are in range, and the quote matches the source substring. This is the
 * guarantee behind "no conclusion without evidence" — if a span can't be located, the
 * conclusion that cited it cannot pass [ResultValidator].
 */
class EvidenceValidator {

    /** @return the problem, or null if the span is valid. */
    fun validate(session: CourseSession, span: EvidenceSpan): EvidenceProblem? {
        val segment = session.segment(span.sourceSegmentId) ?: return EvidenceProblem.MISSING_SEGMENT
        if (!span.isWellFormed()) return EvidenceProblem.MALFORMED
        if (span.startChar < 0 || span.endChar > segment.text.length) return EvidenceProblem.OUT_OF_RANGE
        val actual = segment.text.substring(span.startChar, span.endChar)
        if (!quotesMatch(actual, span.quote)) return EvidenceProblem.QUOTE_MISMATCH
        return null
    }

    fun isResolvable(session: CourseSession, span: EvidenceSpan): Boolean = validate(session, span) == null

    private fun quotesMatch(a: String, b: String): Boolean =
        a == b || a.filterNot { it.isWhitespace() } == b.filterNot { it.isWhitespace() }
}
