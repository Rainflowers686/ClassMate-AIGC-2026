package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * The atom of the evidence chain. Every model conclusion (knowledge point, quiz
 * question, review step) must trace back to one or more EvidenceSpans, and every span
 * must be locatable in the original course text.
 *
 * Offsets are **character offsets into the referenced [CourseSegment.text]** (not the
 * whole course), which keeps spans stable even if segments are re-ordered. [quote] is
 * the exact substring; [EvidenceValidator] re-checks that
 * `segment.text.substring(startChar, endChar) == quote` (after light normalisation).
 */
@Serializable
data class EvidenceSpan(
    val sourceSegmentId: String,
    val startChar: Int,
    val endChar: Int,
    val quote: String,
) {
    val length: Int get() = endChar - startChar

    /** Structural sanity, independent of the source text (offsets ordered, quote present). */
    fun isWellFormed(): Boolean =
        startChar >= 0 && endChar > startChar && quote.isNotBlank() && quote.length == length

    companion object {
        /** Builds a span from a located quote, deriving offsets so callers can't desync them. */
        fun of(segmentId: String, startChar: Int, quote: String): EvidenceSpan =
            EvidenceSpan(segmentId, startChar, startChar + quote.length, quote)
    }
}
