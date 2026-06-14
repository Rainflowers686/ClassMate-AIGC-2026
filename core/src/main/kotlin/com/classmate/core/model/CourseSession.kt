package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * One imported lecture. The immutable input to the whole pipeline.
 *
 * Round 1 is **text only** by design (see [sourceKind]); there is deliberately no audio
 * capture. The raw text is segmented up-front so every downstream conclusion can be
 * anchored to a [CourseSegment].
 */
@Serializable
data class CourseSession(
    val id: String,
    val title: String,
    val rawText: String,
    val segments: List<CourseSegment>,
    val createdAtEpochMs: Long,
    val language: String = "zh",
    val sourceKind: SourceKind = SourceKind.PASTED_TEXT,
    val schemaVersion: Int = ClassMateSchema.VERSION,
) {
    fun segment(id: String): CourseSegment? = segments.firstOrNull { it.id == id }

    val wordlikeCount: Int get() = rawText.count { !it.isWhitespace() }
}
