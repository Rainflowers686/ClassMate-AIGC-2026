package com.classmate.core.evidence

import com.classmate.core.model.CourseSegment
import com.classmate.core.model.CourseSession
import com.classmate.core.model.EvidenceSpan

/**
 * Turns a verbatim quote from the model into a precise [EvidenceSpan] by locating it in the
 * source text. This is what makes the evidence chain real: a quote that cannot be found is
 * not resolved (returns null), so the conclusion it backs will fail validation.
 *
 * The produced span's `quote` is always the ACTUAL source substring (not the model's copy),
 * so downstream validation is exact even when the model differed by whitespace.
 */
class EvidenceResolver {

    /** Resolve [quote] within a specific [segment]. */
    fun resolve(segment: CourseSegment, quote: String): EvidenceSpan? {
        val q = quote.trim()
        if (q.isEmpty()) return null

        val exact = segment.text.indexOf(q)
        if (exact >= 0) {
            return EvidenceSpan(segment.id, exact, exact + q.length, segment.text.substring(exact, exact + q.length))
        }
        return resolveWhitespaceInsensitive(segment, q)
    }

    /** Resolve [quote] anywhere in the session (used for question evidence). */
    fun resolveAnywhere(session: CourseSession, quote: String): EvidenceSpan? {
        for (segment in session.segments) {
            resolve(segment, quote)?.let { return it }
        }
        return null
    }

    private fun resolveWhitespaceInsensitive(segment: CourseSegment, quote: String): EvidenceSpan? {
        val normalizedQuote = quote.filterNot { it.isWhitespace() }
        if (normalizedQuote.isEmpty()) return null

        val map = ArrayList<Int>(segment.text.length)
        val sb = StringBuilder()
        segment.text.forEachIndexed { i, c ->
            if (!c.isWhitespace()) {
                sb.append(c)
                map.add(i)
            }
        }
        val idx = sb.indexOf(normalizedQuote)
        if (idx < 0) return null

        val startOrig = map[idx]
        val endOrig = map[idx + normalizedQuote.length - 1] + 1
        val actual = segment.text.substring(startOrig, endOrig)
        return EvidenceSpan(segment.id, startOrig, endOrig, actual)
    }
}
