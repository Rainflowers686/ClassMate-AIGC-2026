package com.classmate.app.l3

import com.classmate.core.model.Difficulty
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizRelevanceGateTest {
    private val kp = L3KnowledgePoint(
        id = "kp_calc",
        title = "导数的几何意义",
        explanation = "导数表示函数图像在某点切线的斜率。",
        sourceEvidenceIds = listOf("ev_calc"),
        masteryState = L3MasteryState.LEARNING,
    )
    private val evidence = Evidence(
        id = "ev_calc",
        sourceId = "lesson",
        sourceType = L3SourceType.TEXT,
        text = "导数表示函数图像在某点切线的斜率，可用于判断函数单调性。",
    )

    @Test
    fun subjectKnowledgeQuizWithEvidenceIsAccepted() {
        val question = L3GeneratedQuestion(
            id = "q_calc",
            lessonId = "lesson",
            knowledgePointId = kp.id,
            stem = "关于导数的几何意义，下列说法正确的是？",
            options = listOf("A. 导数表示切线斜率", "B. 导数表示面积"),
            correctAnswer = "A",
            explanation = "答案详解：A 可由证据推出。证据摘录：导数表示函数图像在某点切线的斜率。",
            evidenceIds = listOf(evidence.id),
            difficulty = Difficulty.MEDIUM,
        )

        assertTrue(QuizRelevanceGate.isRelevant(question, listOf(kp), listOf(evidence)))
    }

    @Test
    fun keywordOnlyOrEmphasisWordQuizIsRejected() {
        val question = L3GeneratedQuestion(
            id = "q_noise",
            lessonId = "lesson",
            knowledgePointId = kp.id,
            stem = "同学们注意，下面哪项最重要？",
            options = listOf("A. 重点来了", "B. 与课程无关"),
            correctAnswer = "A",
            explanation = "没有证据。",
            evidenceIds = listOf(evidence.id),
            difficulty = Difficulty.EASY,
        )

        assertFalse(QuizRelevanceGate.isRelevant(question, listOf(kp), listOf(evidence)))
    }

    @Test
    fun questionWithoutCourseEvidenceIsRejected() {
        val question = L3GeneratedQuestion(
            id = "q_no_ev",
            lessonId = "lesson",
            knowledgePointId = kp.id,
            stem = "关于导数的几何意义，下列说法正确的是？",
            options = listOf("A. 导数表示切线斜率", "B. 导数表示面积"),
            correctAnswer = "A",
            explanation = "答案详解：回到证据核对。",
            evidenceIds = emptyList(),
            difficulty = Difficulty.MEDIUM,
        )

        assertFalse(QuizRelevanceGate.isRelevant(question, listOf(kp), listOf(evidence)))
    }
}
