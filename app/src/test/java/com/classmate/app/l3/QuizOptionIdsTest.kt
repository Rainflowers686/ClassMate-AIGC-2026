package com.classmate.app.l3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizOptionIdsTest {

    @Test
    fun letterIdsArePositionBasedAndUnique() {
        val options = listOf("第一项", "第二项", "第三项", "第四项")
        val ids = options.indices.map { QuizOptionIds.letterId(it) }
        assertEquals(listOf("A", "B", "C", "D"), ids)
        assertEquals("ids must be unique so one tap selects one option", ids.size, ids.toSet().size)
    }

    @Test
    fun optionsSharingALeadingCharStillGetUniqueIds() {
        // The old bug: "对称"/"对角" both became id "对" → one tap highlighted both.
        val options = listOf("对称关系", "对角矩阵", "传递关系", "自反关系")
        val ids = options.indices.map { QuizOptionIds.letterId(it) }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun cleanTextStripsLeadingLabelButKeepsPlainText() {
        assertEquals("部分和数列的极限", QuizOptionIds.cleanText("A. 部分和数列的极限"))
        assertEquals("TCP", QuizOptionIds.cleanText("TCP"))
    }

    @Test
    fun isAnswerMatchesByLetter() {
        val options = listOf("A. 正确表述", "B. 干扰项", "C. 干扰项", "D. 干扰项")
        assertTrue(QuizOptionIds.isAnswer(0, options[0], "A"))
        assertFalse(QuizOptionIds.isAnswer(1, options[1], "A"))
    }

    @Test
    fun isAnswerMatchesByOptionTextWhenAnswerIsNotALetter() {
        // Plain-text options + text correctAnswer (e.g. an imported bank) must still resolve correctly,
        // and NOT collapse onto a single first-character id.
        val options = listOf("UDP", "TCP", "TLS", "ARP")
        assertTrue(QuizOptionIds.isAnswer(1, options[1], "TCP"))
        assertFalse(QuizOptionIds.isAnswer(2, options[2], "TCP")) // TLS shares first char 'T' but is not the answer
        assertFalse(QuizOptionIds.isAnswer(0, options[0], "TCP"))
    }

    @Test
    fun isAnswerSupportsMultipleCorrectTokens() {
        val options = listOf("A. one", "B. two", "C. three", "D. four")
        assertTrue(QuizOptionIds.isAnswer(0, options[0], "A,C"))
        assertTrue(QuizOptionIds.isAnswer(2, options[2], "A,C"))
        assertFalse(QuizOptionIds.isAnswer(1, options[1], "A,C"))
    }
}
