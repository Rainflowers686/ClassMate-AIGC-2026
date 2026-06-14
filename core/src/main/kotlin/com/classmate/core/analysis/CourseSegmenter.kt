package com.classmate.core.analysis

import com.classmate.core.model.CourseSegment
import com.classmate.core.model.CourseSession
import com.classmate.core.model.Ids
import com.classmate.core.model.SourceKind

/**
 * Splits raw pasted course text into addressable [CourseSegment]s (one per non-blank line),
 * preserving character offsets. Segmenting up front is what lets every later conclusion cite
 * a precise origin. Line endings are normalised so evidence offsets are stable across OSes.
 */
object CourseSegmenter {

    fun segment(rawText: String): List<CourseSegment> {
        val normalized = normalize(rawText)
        val segments = mutableListOf<CourseSegment>()
        var index = 0
        var offset = 0
        for (line in normalized.split('\n')) {
            val leading = line.length - line.trimStart().length
            val core = line.trim()
            if (core.isNotEmpty()) {
                index++
                val start = offset + leading
                segments += CourseSegment(
                    id = Ids.segment(index),
                    index = index,
                    text = core,
                    charStart = start,
                    charEnd = start + core.length,
                )
            }
            offset += line.length + 1 // + the consumed '\n'
        }
        return segments
    }

    fun buildSession(
        id: String,
        title: String,
        rawText: String,
        nowMs: Long,
        sourceKind: SourceKind = SourceKind.PASTED_TEXT,
    ): CourseSession {
        val normalized = normalize(rawText)
        return CourseSession(
            id = id,
            title = title.trim(),
            rawText = normalized,
            segments = segment(normalized),
            createdAtEpochMs = nowMs,
            sourceKind = sourceKind,
        )
    }

    private fun normalize(text: String): String = text.replace("\r\n", "\n").replace("\r", "\n")
}
