package com.classmate.app.l3

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.practice.PracticeItem
import com.classmate.core.practice.PracticeItemType
import com.classmate.core.practice.PracticeOption
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizQualityGateTest {
    @Test
    fun lowQualityGenericQuizIsRejected() {
        val item = item(
            question = "Which option best matches the material?",
            options = listOf(
                PracticeOption("A", "Newton's second law", correct = true),
                PracticeOption("B", "Unrelated classroom note", correct = false),
                PracticeOption("C", "Random guess", correct = false),
                PracticeOption("D", "Ignore the definition", correct = false),
            ),
        )

        assertFalse(QuizQualityGate.isHighQuality(item))
    }

    @Test
    fun metaWordingIsRejectedBeforePracticeUi() {
        val item = item(
            options = listOf(
                PracticeOption("A", "OCR original text is evidence supported", correct = true),
                PracticeOption("B", "Only recite the classroom sentence", correct = false),
                PracticeOption("C", "Follow literal order", correct = false),
                PracticeOption("D", "Use provider fallback", correct = false),
            ),
        )

        assertFalse(QuizQualityGate.isHighQuality(item))
    }

    @Test
    fun subjectSpecificNewtonQuizPassesGate() {
        val item = item(
            question = "关于牛顿第二定律，下列说法正确的是？",
            options = listOf(
                PracticeOption("A", "物体加速度与合外力成正比，与质量成反比，可用 F=ma 表示", correct = true),
                PracticeOption("B", "质量越大，在相同合外力下加速度越大", correct = false),
                PracticeOption("C", "没有合外力时物体一定产生加速度", correct = false),
                PracticeOption("D", "F=ma 只描述速度大小，与力没有关系", correct = false),
            ),
        )

        assertTrue(QuizQualityGate.isHighQuality(item))
    }

    @Test
    fun fillBlankAnswerMustBeSubjectTermOrFormula() {
        val good = item(
            type = PracticeItemType.FILL_BLANK,
            question = "填空题：牛顿第二定律的常用公式是：____。",
            answer = "正确答案：F=ma。答案详解：该公式表达合外力、质量和加速度之间的定量关系。证据摘录：物体的加速度与所受合外力成正比，与质量成反比，公式 F=ma。",
            options = emptyList(),
        )
        val bad = good.copy(answer = "正确答案：OCR。答案详解：内部质量检查。证据摘录：OCR 原文。")

        assertTrue(QuizQualityGate.isHighQuality(good))
        assertFalse(QuizQualityGate.isHighQuality(bad))
    }

    private fun item(
        type: PracticeItemType = PracticeItemType.QUIZ_RETRY,
        question: String = "关于牛顿第二定律，下列说法正确的是？",
        answer: String = "答案详解：A 正确。知识点：牛顿第二定律。为什么正确：它说明合外力、质量和加速度的关系。其他选项为什么错误：它们颠倒或忽略了条件。证据摘录：物体的加速度与所受合外力成正比，与质量成反比，公式 F=ma。",
        options: List<PracticeOption> = listOf(
            PracticeOption("A", "物体加速度与合外力成正比，与质量成反比，可用 F=ma 表示", correct = true),
            PracticeOption("B", "质量越大，在相同合外力下加速度越大", correct = false),
            PracticeOption("C", "没有合外力时物体一定产生加速度", correct = false),
            PracticeOption("D", "F=ma 只描述速度大小，与力没有关系", correct = false),
        ),
    ): PracticeItem = PracticeItem(
        id = "pi_test",
        type = type,
        knowledgePointId = "kp_newton",
        knowledgePointTitle = "牛顿第二定律",
        question = question,
        answer = answer,
        evidenceQuote = "物体的加速度与所受合外力成正比，与质量成反比，公式 F=ma。",
        quizId = "q_newton",
        options = options,
        source = AiExecutionSource.SAFE_PLACEHOLDER,
    )
}
