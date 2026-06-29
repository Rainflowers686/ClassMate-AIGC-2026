package com.classmate.core.practice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-3: a graded quiz question is only usable when it has options AND a resolved correct answer.
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
}
