package com.classmate.core.model

import kotlinx.serialization.Serializable

/**
 * Result returned by a ModelProvider. Mirrors section 7.2 of the v0.2 spec.
 *
 * Field names use snake_case to match the JSON schema and Prompt contract —
 * we want zero-cost round-trip with the model's JSON output, so the wire
 * format wins over Kotlin's camelCase convention.
 */
@Serializable
data class CourseAnalysisResult(
    val course_title: String,
    val summary: String,
    val segments: List<ResultSegment>,
    val quizzes: List<Quiz>,
    val review_plan: List<ReviewStep>
)

@Serializable
data class ResultSegment(
    val segment_id: String,
    val time_range: String,
    val corrected_text: String,
    val knowledge_points: List<KnowledgePoint>,
    val confusion_points: List<String> = emptyList()
)

@Serializable
data class KnowledgePoint(
    val kp_id: String,
    val name: String,
    val importance: Int,
    val difficulty: Int,
    val source_segment_id: String,
    val evidence_span: String,
    val explanation: String
)

@Serializable
data class Quiz(
    val quiz_id: String,
    val question: String,
    val options: List<String>,
    val answer_index: Int,
    val explanation: String,
    val source_segment_id: String,
    val related_kp_id: String,
    val evidence_span: String
)

@Serializable
data class ReviewStep(
    val step_id: String,
    val duration_minutes: Int,
    val task: String,
    val related_kp_ids: List<String>,
    val reason: String
)
