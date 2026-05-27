package com.classmate.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level model output. Mirrors spec §7.2.
 *
 * The sub-types live in their own files — CourseSegment, KnowledgePoint, Quiz,
 * ReviewPlanItem — so reviewers can navigate the data model without scrolling
 * past one giant file. The split is documented in spec §15.1 / task §3.2.
 */
@Serializable
data class CourseAnalysisResult(
    @SerialName("course_title") val courseTitle: String,
    @SerialName("summary") val summary: String,
    @SerialName("segments") val segments: List<CourseSegment>,
    @SerialName("quizzes") val quizzes: List<Quiz>,
    @SerialName("review_plan") val reviewPlan: List<ReviewPlanItem>
)
