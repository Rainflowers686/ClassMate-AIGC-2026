package com.classmate.core

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSegment
import com.classmate.core.model.InputSegment
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.Quiz
import com.classmate.core.model.ReviewPlanItem

internal object TestFixtures {
    fun input(): CourseAnalysisInput = CourseAnalysisInput(
        courseTitle = "Calculus Review",
        hotwords = listOf("Taylor formula", "derivative"),
        segments = listOf(
            InputSegment(
                segmentId = "seg_001",
                timeRange = "00:00-00:30",
                text = "The Taylor formula expands a function near a point into a polynomial."
            ),
            InputSegment(
                segmentId = "seg_002",
                timeRange = "00:30-01:00",
                text = "A derivative describes the instantaneous rate of change near a point."
            ),
            InputSegment(
                segmentId = "seg_003",
                timeRange = "01:00-01:30",
                text = "The remainder term helps estimate the error introduced by approximation."
            )
        )
    )

    fun validResult(): CourseAnalysisResult {
        val kp1 = KnowledgePoint(
            kpId = "kp_001",
            name = "Taylor formula",
            importance = 5,
            difficulty = 3,
            sourceSegmentId = "seg_001",
            evidenceSpan = "The Taylor formula expands a function near a point into a polynomial.",
            explanation = "From segment one"
        )
        val kp2 = KnowledgePoint(
            kpId = "kp_002",
            name = "Derivative",
            importance = 4,
            difficulty = 2,
            sourceSegmentId = "seg_002",
            evidenceSpan = "A derivative describes the instantaneous rate of change near a point.",
            explanation = "From segment two"
        )
        return CourseAnalysisResult(
            courseTitle = "Calculus Review",
            summary = "Test summary",
            segments = listOf(
                CourseSegment(
                    segmentId = "seg_001",
                    timeRange = "00:00-00:30",
                    correctedText = "The Taylor formula expands a function near a point into a polynomial.",
                    knowledgePoints = listOf(kp1)
                ),
                CourseSegment(
                    segmentId = "seg_002",
                    timeRange = "00:30-01:00",
                    correctedText = "A derivative describes the instantaneous rate of change near a point.",
                    knowledgePoints = listOf(kp2)
                )
            ),
            quizzes = listOf(
                Quiz(
                    quizId = "q_001",
                    question = "Which option best matches the source?",
                    options = listOf("Taylor formula", "Unrelated option"),
                    answerIndex = 0,
                    explanation = "See the source text",
                    sourceSegmentId = "seg_001",
                    relatedKpId = "kp_001",
                    evidenceSpan = "The Taylor formula expands a function near a point into a polynomial."
                )
            ),
            reviewPlan = listOf(
                ReviewPlanItem(
                    stepId = "rp_001",
                    durationMinutes = 5,
                    task = "Restate the Taylor formula",
                    relatedKpIds = listOf("kp_001")
                )
            )
        )
    }
}
