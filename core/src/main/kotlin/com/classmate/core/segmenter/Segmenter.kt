package com.classmate.core.segmenter

import com.classmate.core.model.InputSegment

/**
 * Rule-based course-text segmenter (spec §9 / task §4.2).
 *
 * v0.3 strategy — intentionally NOT semantic:
 *   1. Split the raw text by blank lines into paragraphs.
 *   2. If a single paragraph exceeds [maxChars], cut it at the nearest
 *      sentence boundary (。！？!?；;) past [targetChars].
 *   3. Greedy-merge adjacent paragraphs into chunks of [targetChars]..[maxChars].
 *   4. Emit seg_001 / seg_002 / … and an estimated time_range string
 *      ("段落 N" — we don't claim real timestamp recovery).
 *
 * Constants below come from spec §9 item 2 ("尽量 150-300 字").
 *
 * Why not regex-by-punctuation as the primary rule? Because pasted course
 * text usually arrives paragraph-formatted (TTS scripts, lecture notes),
 * and paragraph cuts respect author intent better than punctuation cuts.
 * Punctuation is the fallback.
 */
object Segmenter {

    private const val TARGET_CHARS = 150
    private const val MAX_CHARS = 300
    private val PARAGRAPH_BREAK = Regex("\\n\\s*\\n+")
    // Match CJK sentence enders + ASCII counterparts. Semicolon counts as a
    // soft break, so an overlong paragraph with no full stops still cuts.
    private val SENTENCE_END = Regex("[。！？!?；;]")

    fun segment(rawText: String): List<InputSegment> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return emptyList()

        val paragraphs = PARAGRAPH_BREAK.split(trimmed)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Step 1: chop oversize paragraphs so the merge step never has to handle
        // a single chunk bigger than MAX_CHARS.
        val normalized = paragraphs.flatMap { splitLongParagraph(it) }

        // Step 2: greedy merge.
        val merged = mergeAdjacent(normalized)

        return merged.mapIndexed { index, chunk ->
            val ordinal = index + 1
            InputSegment(
                segmentId = "seg_%03d".format(ordinal),
                timeRange = "段落 $ordinal",
                text = chunk
            )
        }
    }

    /**
     * Split a single paragraph into sub-chunks no larger than MAX_CHARS.
     * Prefer cutting at sentence enders past TARGET_CHARS; if none found,
     * hard-cut at MAX_CHARS so we never overrun.
     */
    private fun splitLongParagraph(paragraph: String): List<String> {
        if (paragraph.length <= MAX_CHARS) return listOf(paragraph)

        val out = mutableListOf<String>()
        var cursor = 0
        while (cursor < paragraph.length) {
            val remaining = paragraph.length - cursor
            if (remaining <= MAX_CHARS) {
                out += paragraph.substring(cursor).trim()
                break
            }
            // Search for a sentence end between cursor+TARGET_CHARS and cursor+MAX_CHARS.
            val searchStart = cursor + TARGET_CHARS
            val searchEnd = (cursor + MAX_CHARS).coerceAtMost(paragraph.length)
            val cut = findBoundary(paragraph, searchStart, searchEnd)
                ?: (cursor + MAX_CHARS) // fall back to hard cut
            out += paragraph.substring(cursor, cut).trim()
            cursor = cut
        }
        return out.filter { it.isNotEmpty() }
    }

    /** First sentence-end character in [start, endExclusive). */
    private fun findBoundary(text: String, start: Int, endExclusive: Int): Int? {
        if (start >= endExclusive) return null
        val window = text.substring(start, endExclusive)
        val match = SENTENCE_END.find(window) ?: return null
        // include the punctuation itself so the next chunk doesn't start with it
        return start + match.range.last + 1
    }

    /**
     * Greedy merge: append the next paragraph if the combined size stays
     * under MAX_CHARS; otherwise flush the accumulator and start a new chunk.
     * Short trailing chunks are tolerated rather than force-merged backward,
     * because the last segment is often a conclusion sentence the user wants
     * to see as its own row.
     */
    private fun mergeAdjacent(paragraphs: List<String>): List<String> {
        if (paragraphs.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var buffer = StringBuilder()

        fun flush() {
            if (buffer.isNotEmpty()) {
                out += buffer.toString().trim()
                buffer = StringBuilder()
            }
        }

        for (p in paragraphs) {
            if (buffer.isEmpty()) {
                buffer.append(p)
                continue
            }
            val joined = buffer.length + 1 + p.length
            if (joined <= MAX_CHARS && buffer.length < TARGET_CHARS) {
                // Still below the target — keep merging. Once we cross target,
                // we stop merging so the chunk doesn't drift toward MAX_CHARS
                // on every iteration.
                buffer.append('\n').append(p)
            } else {
                flush()
                buffer.append(p)
            }
        }
        flush()
        return out
    }
}
