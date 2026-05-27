package com.classmate.core.validation

import com.classmate.core.model.CourseAnalysisResult

/**
 * Lightweight Kotlin-side validator for [CourseAnalysisResult].
 *
 * Scope (task §4.6):
 *  - segments must not be empty
 *  - every knowledgePoint.sourceSegmentId resolves to one of the result's
 *    segments (NOT the input segments — that's EvidenceValidator's job, which
 *    sees both sides)
 *  - every quiz.sourceSegmentId resolves
 *  - every quiz.relatedKpId resolves
 *  - quiz.answerIndex lies inside its options list
 *  - importance and difficulty are in [1, 5]
 *
 * We deliberately do NOT pull a full JSON-schema engine in here; that's
 * v0.3.5+ and tracked in docs/v0.3-tasklist.md. This validator catches the
 * structural mistakes the demo flow actually hits.
 */
object ResultValidator {

    fun validate(result: CourseAnalysisResult): ResultValidation {
        val issues = mutableListOf<String>()

        if (result.segments.isEmpty()) {
            issues += "segments is empty"
        }

        val segmentIds = result.segments.map { it.segmentId }.toSet()
        val kpIds = result.segments.flatMap { it.knowledgePoints }.map { it.kpId }.toSet()

        result.segments.forEach { seg ->
            seg.knowledgePoints.forEach { kp ->
                if (kp.sourceSegmentId !in segmentIds) {
                    issues += "knowledge_point ${kp.kpId} source_segment_id ${kp.sourceSegmentId} not in result.segments"
                }
                if (kp.importance !in 1..5) {
                    issues += "knowledge_point ${kp.kpId} importance ${kp.importance} out of [1,5]"
                }
                if (kp.difficulty !in 1..5) {
                    issues += "knowledge_point ${kp.kpId} difficulty ${kp.difficulty} out of [1,5]"
                }
            }
        }

        result.quizzes.forEach { quiz ->
            if (quiz.sourceSegmentId !in segmentIds) {
                issues += "quiz ${quiz.quizId} source_segment_id ${quiz.sourceSegmentId} not in result.segments"
            }
            if (quiz.relatedKpId !in kpIds) {
                issues += "quiz ${quiz.quizId} related_kp_id ${quiz.relatedKpId} not in any knowledge_point"
            }
            if (quiz.answerIndex !in quiz.options.indices) {
                issues += "quiz ${quiz.quizId} answer_index ${quiz.answerIndex} not in options range 0..${quiz.options.lastIndex}"
            }
        }

        return ResultValidation(passed = issues.isEmpty(), issues = issues)
    }
}

data class ResultValidation(
    val passed: Boolean,
    val issues: List<String>
)
