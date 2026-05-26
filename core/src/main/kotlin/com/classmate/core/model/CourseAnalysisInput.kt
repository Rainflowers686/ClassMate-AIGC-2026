package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * Input passed to a ModelProvider for one course-analysis call.
 *
 * Mirrors section 7.1 of the v0.2 spec. Segmentation is performed BEFORE this
 * struct is built: the caller is expected to have already produced segment_id
 * + time_range + text for each chunk.
 */
@Serializable
data class CourseAnalysisInput(
    val course_title: String,
    val hotwords: List<String>,
    val segments: List<InputSegment>
)

@Serializable
data class InputSegment(
    val segment_id: String,
    val time_range: String,
    val text: String
)
