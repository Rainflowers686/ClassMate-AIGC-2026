package com.classmate.core.practice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-3: a graded quiz question is only usable when it has resolved answer data: either options with a
 * correct option, or an explicit fill-blank answer.
 * This single gate ([isAnswerableQuiz]) is shared by every graded-quiz entry point so that
 * "随机小测无正确答案" can never happen again.
 */
class PracticeAnswerableGateTest {

    private fun item(options: List<PracticeOption>): PracticeItem = PracticeItem(
        id = "i1",
        type = PracticeItemType.QUIZ_RETRY,
        knowledgePointId = "kp1",
        knowledgePointTitle = "KP",
        question = "Q?",
        answer = "",
        options = options,
    )

    @Test
    fun questionWithCorrectOptionIsAnswerable() {
        val it = item(
            listOf(
                PracticeOption("a", "A", correct = false),
                PracticeOption("b", "B", correct = true),
            ),
        )
        assertTrue(it.isAnswerableQuiz())
        assertTrue(it.correctOptionIds.isNotEmpty())
    }

    @Test
    fun questionWithNoCorrectOptionIsRejected() {
        val it = item(
            listOf(
                PracticeOption("a", "A", correct = false),
                PracticeOption("b", "B", correct = false),
            ),
        )
        assertFalse("a question without a correct option must not enter a graded quiz", it.isAnswerableQuiz())
    }

    @Test
    fun questionWithoutEnoughOptionsIsRejected() {
        assertFalse(item(emptyList()).isAnswerableQuiz())
        assertFalse(item(listOf(PracticeOption("a", "A", correct = true))).isAnswerableQuiz())
    }

    @Test
    fun fillBlankWithAnswerIsAnswerable() {
        val fill = item(emptyList()).copy(
            type = PracticeItemType.FILL_BLANK,
            question = "填空题：牛顿第二定律公式是 ____。",
            answer = "正确答案：F=ma。答案详解：公式表示合外力、质量和加速度的关系。",
        )

        assertTrue(fill.isAnswerableQuiz())
    }
}
