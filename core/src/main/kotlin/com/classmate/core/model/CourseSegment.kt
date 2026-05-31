package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * A contiguous chunk of the original course text (typically a paragraph). Segments give
 * the model and the evidence chain a stable addressing scheme: knowledge points cite a
 * [id], evidence spans index into [text].
 *
 * [charStart]/[charEnd] are offsets into the *whole* course text, kept so the UI can map
 * a segment back onto the full transcript for context if needed.
 */
@Serializable
data class CourseSegment(
    val id: String,
    val index: Int,
    val text: String,
    val charStart: Int,
    val charEnd: Int,
) {
    val length: Int get() = text.length
}
