package com.classmate.core.validation

import com.classmate.core.TestFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultValidatorTest {

    @Test
    fun validate_passesForClosedReferences() {
        val validation = ResultValidator.validate(TestFixtures.validResult(), TestFixtures.input())

        assertTrue(validation.issues.toString(), validation.passed)
    }

    @Test
    fun validate_reportsBrokenKnowledgeQuizAndReviewPlanReferences() {
        val valid = TestFixtures.validResult()
        val brokenKp = valid.segments.first().knowledgePoints.first().copy(
            sourceSegmentId = "seg_missing",
            importance = 6
        )
        val brokenQuiz = valid.quizzes.first().copy(
            sourceSegmentId = "seg_missing",
            relatedKpId = "kp_missing",
            answerIndex = 99
        )
        val brokenReview = valid.reviewPlan.first().copy(
            relatedKpIds = listOf("kp_missing")
        )
        val brokenResult = valid.copy(
            segments = valid.segments.mapIndexed { index, segment ->
                if (index == 0) segment.copy(knowledgePoints = listOf(brokenKp)) else segment
            },
            quizzes = listOf(brokenQuiz),
            reviewPlan = listOf(brokenReview)
        )

        val validation = ResultValidator.validate(brokenResult, TestFixtures.input())
        val kinds = validation.issues.map { it.kind }.toSet()

        assertFalse(validation.passed)
        assertTrue(ValidationIssueKind.KP_SOURCE_SEGMENT_NOT_IN_RESULT in kinds)
        assertTrue(ValidationIssueKind.KP_SOURCE_SEGMENT_NOT_IN_INPUT in kinds)
        assertTrue(ValidationIssueKind.KP_IMPORTANCE_OUT_OF_RANGE in kinds)
        assertTrue(ValidationIssueKind.QUIZ_SOURCE_SEGMENT_NOT_IN_RESULT in kinds)
        assertTrue(ValidationIssueKind.QUIZ_SOURCE_SEGMENT_NOT_IN_INPUT in kinds)
        assertTrue(ValidationIssueKind.QUIZ_RELATED_KP_MISSING in kinds)
        assertTrue(ValidationIssueKind.QUIZ_ANSWER_INDEX_OUT_OF_RANGE in kinds)
        assertTrue(ValidationIssueKind.REVIEW_PLAN_KP_MISSING in kinds)
    }
}
