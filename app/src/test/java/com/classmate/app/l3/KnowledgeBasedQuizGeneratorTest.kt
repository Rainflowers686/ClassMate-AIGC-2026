package com.classmate.app.l3

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeBasedQuizGeneratorTest {
    private val now = 1_700_000_000_000L

    @Test
    fun localKnowledgeQuizUsesMixedTypesAndDistributedAnswers() {
        val evidence = listOf(
            evidence("ev1", "牛顿第二定律说明物体的加速度与所受合外力成正比，与质量成反比，公式 F=ma。"),
            evidence("ev2", "加速度描述速度随时间变化的快慢，需要结合受力和质量分析。"),
            evidence("ev3", "合外力是多个力的矢量和，决定物体运动状态变化。"),
            evidence("ev4", "质量越大，在相同合外力下加速度越小。"),
            evidence("ev5", "F=ma 可以用来计算合外力、质量或加速度之间的关系。"),
        )
        val knowledge = listOf(
            kp("kp1", "牛顿第二定律", "加速度、合外力和质量之间的定量关系。", "ev1"),
            kp("kp2", "加速度", "速度变化快慢的物理量。", "ev2"),
            kp("kp3", "合外力", "多个力共同作用后的矢量和。", "ev3"),
            kp("kp4", "质量", "反映物体惯性大小的物理量。", "ev4"),
            kp("kp5", "F=ma", "牛顿第二定律的公式表达。", "ev5"),
        )

        val questions = KnowledgeBasedQuizGenerator.generate("lesson", knowledge, evidence, now, maxQuestions = 5)

        assertTrue(questions.size >= 4)
        assertFalse("local fallback must not be all true/false", questions.all { it.options.size == 2 })
        assertFalse("local fallback must not put every correct answer at A", questions.map { it.correctAnswer }.all { it == "A" })
        assertTrue(questions.any { it.options.size >= 4 })
        assertTrue("local fallback should include a fill-blank item", questions.any { it.options.isEmpty() && it.stem.contains("填空题") })
        assertTrue(questions.all { it.explanation.contains("证据") && it.explanation.contains("答案详解") })
        assertTrue(questions.all { it.stem.contains("知识点") || it.stem.contains("理解") || it.stem.contains("判断题") || it.stem.contains("填空题") || it.stem.contains("牛顿第二定律") })
        assertStudentVisibleNoMeta(questions)
    }

    @Test
    fun localKnowledgeQuizDoesNotCopyOcrQuoteAsStemOrFixedOptionA() {
        val rawQuote = "同学们注意，重点来了。下面看狭义相对论的时空观：同时的相对性、时间膨胀和长度收缩都与参考系有关。"
        val evidence = listOf(evidence("ev1", rawQuote))
        val knowledge = listOf(kp("kp1", "狭义相对论的时空观", "同时的相对性、时间膨胀和长度收缩体现参考系相关性。", "ev1"))

        val question = KnowledgeBasedQuizGenerator.generate("lesson", knowledge, evidence, now, maxQuestions = 1).single()

        assertFalse("stem should be knowledge-driven, not raw OCR copy", question.stem.contains(rawQuote.take(18)))
        assertFalse("option A should not be the raw OCR quote", question.options.first().contains(rawQuote.take(18)))
        assertTrue(question.explanation.contains("狭义相对论"))
        assertTrue(question.explanation.contains("证据摘录"))
        assertStudentVisibleNoMeta(listOf(question))
    }

    @Test
    fun localKnowledgeQuizUsesSubjectSpecificMathOptions() {
        val evidence = listOf(
            evidence("ev1", "根值判别法：若 limsup (a_n)^(1/n) < 1，则正项级数收敛；若大于 1，则发散。"),
            evidence("ev2", "绝对收敛级数任意重排后仍收敛且和不变。"),
        )
        val knowledge = listOf(
            kp("kp1", "根值判别法（柯西判别法）", "用根式极限判断正项级数敛散性。", "ev1"),
            kp("kp2", "级数重排的性质", "绝对收敛级数重排后和不变。", "ev2"),
        )

        val questions = KnowledgeBasedQuizGenerator.generate("lesson", knowledge, evidence, now, maxQuestions = 2)
        val visible = questions.joinToString("\n") { it.stem + "\n" + it.options.joinToString("\n") + "\n" + it.explanation }

        assertTrue(visible.contains("limsup") || visible.contains("绝对收敛"))
        assertFalse(visible.contains("只背"))
        assertFalse(visible.contains("OCR"))
        assertFalse(visible.contains("题干限定"))
        assertStudentVisibleNoMeta(questions)
    }

    private fun kp(id: String, title: String, explanation: String, evidenceId: String) =
        L3KnowledgePoint(
            id = id,
            title = title,
            explanation = explanation,
            sourceEvidenceIds = listOf(evidenceId),
            masteryState = L3MasteryState.LEARNING,
        )

    private fun evidence(id: String, text: String) =
        Evidence(
            id = id,
            sourceId = "lesson",
            sourceType = L3SourceType.OCR_IMAGE,
            text = text,
        )

    private fun assertStudentVisibleNoMeta(questions: List<L3GeneratedQuestion>) {
        val banned = listOf("OCR", "原文", "课堂原句", "字面顺序", "证据支持", "题干限定", "只背", "relevance", "fallback")
        val visible = questions.joinToString("\n") { it.stem + "\n" + it.options.joinToString("\n") + "\n" + it.explanation }
        banned.forEach { token -> assertFalse("student-visible quiz leaked $token", visible.contains(token, ignoreCase = true)) }
    }
}
