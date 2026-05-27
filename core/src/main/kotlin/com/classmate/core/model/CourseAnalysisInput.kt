package com.classmate.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Input passed to a ModelProvider for one course-analysis call.
 *
 * Mirrors spec §7.1. Wire format stays snake_case for symmetry with the
 * model's JSON output and the schema file; Kotlin sees camelCase via
 * @SerialName. Edit both sides together if you rename anything here.
 */
@Serializable
data class CourseAnalysisInput(
    @SerialName("course_title") val courseTitle: String,
    @SerialName("hotwords") val hotwords: List<String>,
    @SerialName("segments") val segments: List<InputSegment>
)

@Serializable
data class InputSegment(
    @SerialName("segment_id") val segmentId: String,
    @SerialName("time_range") val timeRange: String,
    @SerialName("text") val text: String
)
