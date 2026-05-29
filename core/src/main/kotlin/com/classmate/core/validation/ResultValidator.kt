package com.classmate.core.validation

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult

/**
 * Structural + cross-reference validator for [CourseAnalysisResult].
 *
 * v0.4 changes (vs v0.3.5):
 *  - Issues are typed via [ValidationIssue] so the UI can show actionable
 *    messages.
 *  - When [validate] receives a non-null [CourseAnalysisInput], it also checks
 *    that every result.segment_id is present in input.segment_id (this is the
 *    root cause of the v0.3.5 "structure_valid=false + 100% match" bug).
 *
 * Scope (still no full JSON-schema engine here):
 *  - segments must not be empty
 *  - every knowledge_point / quiz source_segment_id resolves inside the result
 *  - every knowledge_point source_segment_id appears in input.segments (when given)
 *  - every quiz related_kp_id resolves to a kp_id
 *  - every quiz answer_index is in [0, options.size)
 *  - importance / difficulty are in [1, 5]
 *  - review_plan related_kp_ids resolve
 */
object ResultValidator {

    fun validate(
        result: CourseAnalysisResult,
        input: CourseAnalysisInput? = null
    ): ResultValidation {
        val issues = mutableListOf<ValidationIssue>()

        if (result.segments.isEmpty()) {
            issues += ValidationIssue(
                ValidationIssueKind.EMPTY_SEGMENTS,
                ownerId = "",
                detail = "result.segments is empty"
            )
        }

        val resultSegmentIds = result.segments.map { it.segmentId }.toSet()
        val inputSegmentIds = input?.segments?.map { it.segmentId }?.toSet().orEmpty()
        val kpIds = result.segments.flatMap { it.knowledgePoints }.map { it.kpId }.toSet()

        result.segments.forEach { seg ->
            seg.knowledgePoints.forEach { kp ->
                if (kp.sourceSegmentId !in resultSegmentIds) {
                    issues += ValidationIssue(
                        ValidationIssueKind.KP_SOURCE_SEGMENT_NOT_IN_RESULT,
                        ownerId = kp.kpId,
                        detail = "source_segment_id=${kp.sourceSegmentId}"
                    )
                }
                if (input != null && kp.sourceSegmentId !in inputSegmentIds) {
                    issues += ValidationIssue(
                        ValidationIssueKind.KP_SOURCE_SEGMENT_NOT_IN_INPUT,
                        ownerId = kp.kpId,
                        detail = "source_segment_id=${kp.sourceSegmentId} (input segments: ${inputSegmentIds.size})"
                    )
                }
                if (kp.importance !in 1..5) {
                    issues += ValidationIssue(
                        ValidationIssueKind.KP_IMPORTANCE_OUT_OF_RANGE,
                        ownerId = kp.kpId,
                        detail = "importance=${kp.importance}"
                    )
                }
                if (kp.difficulty !in 1..5) {
                    issues += ValidationIssue(
                        ValidationIssueKind.KP_DIFFICULTY_OUT_OF_RANGE,
                        ownerId = kp.kpId,
                        detail = "difficulty=${kp.difficulty}"
                    )
                }
            }
        }

        result.quizzes.forEach { quiz ->
            if (quiz.sourceSegmentId !in resultSegmentIds) {
                issues += ValidationIssue(
                    ValidationIssueKind.QUIZ_SOURCE_SEGMENT_NOT_IN_RESULT,
                    ownerId = quiz.quizId,
                    detail = "source_segment_id=${quiz.sourceSegmentId}"
                )
            }
            if (input != null && quiz.sourceSegmentId !in inputSegmentIds) {
                issues += ValidationIssue(
                    ValidationIssueKind.QUIZ_SOURCE_SEGMENT_NOT_IN_INPUT,
                    ownerId = quiz.quizId,
                    detail = "source_segment_id=${quiz.sourceSegmentId}"
                )
            }
            if (quiz.relatedKpId !in kpIds) {
                issues += ValidationIssue(
                    ValidationIssueKind.QUIZ_RELATED_KP_MISSING,
                    ownerId = quiz.quizId,
                    detail = "related_kp_id=${quiz.relatedKpId}"
                )
            }
            if (quiz.answerIndex !in quiz.options.indices) {
                issues += ValidationIssue(
                    ValidationIssueKind.QUIZ_ANSWER_INDEX_OUT_OF_RANGE,
                    ownerId = quiz.quizId,
                    detail = "answer_index=${quiz.answerIndex}, options=${quiz.options.size}"
                )
            }
        }

        result.reviewPlan.forEach { step ->
            step.relatedKpIds.forEach { kpId ->
                if (kpId !in kpIds) {
                    issues += ValidationIssue(
                        ValidationIssueKind.REVIEW_PLAN_KP_MISSING,
                        ownerId = step.stepId,
                        detail = "related_kp_id=$kpId"
                    )
                }
            }
        }

        return ResultValidation(passed = issues.isEmpty(), issues = issues)
    }
}

data class ResultValidation(
    val passed: Boolean,
    val issues: List<ValidationIssue>
)
