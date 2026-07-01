package com.classmate.app.l3

import com.classmate.core.model.Difficulty
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningLoopRefinementEnginesTest {
    private fun snapshot(): L3PipelineSnapshot {
        val source = LessonSource(
            id = "lesson_math",
            title = "导数与切线",
            type = L3SourceType.TEXT,
            createdAt = 1L,
            rawText = "导数表示瞬时变化率。切线斜率可以用导数计算。极值点需要结合导数符号变化判断。",
            status = "READY",
        )
        val evidence = listOf(
            Evidence("ev_1", source.id, L3SourceType.TEXT, "导数表示瞬时变化率，切线斜率可以用导数计算。", 1),
            Evidence("ev_2", source.id, L3SourceType.TEXT, "极值点需要结合导数符号变化判断。", 2),
        )
        val knowledge = listOf(
            L3KnowledgePoint("kp_derivative", "导数与瞬时变化率", "导数刻画函数在一点附近的变化速度。", listOf("ev_1"), L3MasteryState.LEARNING),
            L3KnowledgePoint("kp_tangent", "切线斜率", "切线斜率可以由导数计算。", listOf("ev_1"), L3MasteryState.LEARNING),
            L3KnowledgePoint("kp_extreme", "极值与导数符号", "极值点要观察导数符号变化。", listOf("ev_2"), L3MasteryState.LEARNING),
        )
        val question = L3GeneratedQuestion(
            id = "q_1",
            lessonId = source.id,
            knowledgePointId = "kp_derivative",
            stem = "导数在本课中主要表示什么？",
            options = listOf("A. 瞬时变化率", "B. 固定常数", "C. 图像颜色", "D. 题目编号"),
            correctAnswer = "A",
            explanation = "答案详解：A 来自课堂证据；B/C/D 与材料不符。",
            evidenceIds = listOf("ev_1"),
            difficulty = Difficulty.MEDIUM,
        )
        return L3PipelineSnapshot(
            lessonSource = source,
            summary = "导数、切线和极值是本节课核心。",
            evidence = evidence,
            knowledgePoints = knowledge,
            questions = listOf(question),
        )
    }

    @Test
    fun relatedKnowledgeUsesOnlyCurrentCourseEvidence() {
        val related = RelatedKnowledgeSummaryEngine.build(snapshot())

        assertTrue(related.isNotEmpty())
        assertTrue(related.first().summary.contains("本课知识点") || related.first().summary.contains("本课证据"))
        assertTrue(related.flatMap { it.evidenceQuotes }.all { it.contains("导数") || it.contains("极值") })
        assertFalse(related.joinToString("\n") { it.summary }.contains("外部 API"))
    }

    @Test
    fun relatedKnowledgeIgnoresClassroomPromptNoise() {
        val base = snapshot()
        val noisy = base.copy(
            evidence = base.evidence + Evidence("ev_noise", base.lessonSource!!.id, L3SourceType.TEXT, "同学们注意，重点来了，大家记一下。", 3),
            knowledgePoints = base.knowledgePoints + L3KnowledgePoint(
                id = "kp_noise",
                title = "同学们注意",
                explanation = "重点来了，大家记一下。",
                sourceEvidenceIds = listOf("ev_noise"),
                masteryState = L3MasteryState.LEARNING,
            ),
        )

        val related = RelatedKnowledgeSummaryEngine.build(noisy)

        assertTrue(related.isNotEmpty())
        assertFalse(related.any { it.sourceKnowledgePointTitle.contains("同学们注意") })
        assertFalse(related.flatMap { it.relatedKnowledgePointTitles }.any { it.contains("重点来了") })
    }

    @Test
    fun questionFeedbackRetiresAndCreatesReplacementQuizWithDetailedExplanation() {
        val base = snapshot()
        val outcome = FeedbackLearningOptimizer.optimize(
            snapshot = base,
            type = FeedbackType.NOT_ACCURATE,
            targetKind = FeedbackTargetKind.QUIZ_QUESTION,
            targetId = "q_1",
            note = "题目不准确",
            now = 123L,
        )

        val result = outcome.result
        assertEquals("q_1", result.retiredQuestionId)
        assertTrue(result.createdQuestionId!!.startsWith("q_feedback_"))
        val replacement = outcome.snapshot.questions.first { it.id == result.createdQuestionId }
        assertNotEquals("q_1", replacement.id)
        assertTrue(replacement.explanation.contains("答案详解"))
        assertTrue(replacement.explanation.contains("证据摘录"))
        assertEquals("kp_derivative", replacement.knowledgePointId)
        assertTrue(replacement.evidenceIds.isNotEmpty())
        assertFalse(replacement.options.joinToString("\n").contains("无关概念"))
        assertFalse(replacement.options.joinToString("\n").contains("直接背答案"))
        assertTrue(outcome.snapshot.feedbackOptimizationResults.last().message.contains("生成新的练习题"))
    }

    @Test
    fun questionFeedbackDoesNotGenerateReplacementFromNoiseEvidence() {
        val base = snapshot()
        val noisyQuestion = base.questions.first().copy(
            id = "q_noise",
            knowledgePointId = "kp_noise",
            evidenceIds = listOf("ev_noise"),
        )
        val noisy = base.copy(
            evidence = base.evidence + Evidence("ev_noise", base.lessonSource!!.id, L3SourceType.TEXT, "同学们注意，重点来了，大家记一下。", 3),
            knowledgePoints = base.knowledgePoints + L3KnowledgePoint(
                id = "kp_noise",
                title = "同学们注意",
                explanation = "重点来了，大家记一下。",
                sourceEvidenceIds = listOf("ev_noise"),
                masteryState = L3MasteryState.LEARNING,
            ),
            questions = base.questions + noisyQuestion,
        )

        val outcome = FeedbackLearningOptimizer.optimize(
            snapshot = noisy,
            type = FeedbackType.NOT_ACCURATE,
            targetKind = FeedbackTargetKind.QUIZ_QUESTION,
            targetId = "q_noise",
            note = "题目无关",
            now = 789L,
        )

        assertEquals("q_noise", outcome.result.retiredQuestionId)
        assertTrue(outcome.result.createdQuestionId == null)
        assertTrue(outcome.result.needsReview)
    }

    @Test
    fun knowledgeFeedbackRewritesSummaryAndQueuesReview() {
        val outcome = FeedbackLearningOptimizer.optimize(
            snapshot = snapshot(),
            type = FeedbackType.NOT_ACCURATE,
            targetKind = FeedbackTargetKind.KNOWLEDGE_POINT,
            targetId = "kp_derivative",
            note = "总结不准",
            now = 456L,
        )

        val updated = outcome.snapshot.knowledgePoints.first { it.id == "kp_derivative" }
        assertTrue(updated.explanation.contains("已根据反馈重新核对"))
        assertTrue(outcome.snapshot.reviewQueue.any { it.knowledgePointId == "kp_derivative" })
        assertTrue(outcome.snapshot.relatedKnowledgeSummaries.isNotEmpty())
        assertTrue(outcome.result.message.contains("更新知识点摘要"))
    }
}
