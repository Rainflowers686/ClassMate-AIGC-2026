package com.classmate.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizAnswerNormalizerTest {

    private fun trueFalse() = listOf(QuizOption("A", "正确", false), QuizOption("B", "错误", false))
    private fun abcd() = listOf(
        QuizOption("A", "收敛", false), QuizOption("B", "发散", false),
        QuizOption("C", "条件收敛", false), QuizOption("D", "绝对收敛", false),
    )

    @Test
    fun perOptionFlagsAreTrustedFirst() {
        val opts = listOf(QuizOption("A", "x", false), QuizOption("B", "y", true))
        assertEquals(listOf("B"), QuizAnswerNormalizer.resolveCorrectIds(opts, "A"))
    }

    @Test
    fun pSeriesFalsePropositionResolvesToBFromEveryCommonForm() {
        // p-级数 ∑1/n^p 在 p=1 时收敛 —— 假命题，正确答案 = B 错误。
        listOf("错误", "B", "b", "false", "否", "×", "2", "B 错误", "选项B").forEach {
            assertEquals("answer form '$it' -> B", listOf("B"), QuizAnswerNormalizer.resolveCorrectIds(trueFalse(), it))
        }
    }

    @Test
    fun trueFormsResolveToA() {
        listOf("正确", "A", "true", "对", "是", "1").forEach {
            assertEquals("answer form '$it' -> A", listOf("A"), QuizAnswerNormalizer.resolveCorrectIds(trueFalse(), it))
        }
    }

    @Test
    fun singleChoiceMatchesByLetterIndexOrText() {
        assertEquals(listOf("B"), QuizAnswerNormalizer.resolveCorrectIds(abcd(), "B"))
        assertEquals(listOf("B"), QuizAnswerNormalizer.resolveCorrectIds(abcd(), "2"))
        assertEquals(listOf("B"), QuizAnswerNormalizer.resolveCorrectIds(abcd(), "发散"))
    }

    @Test
    fun multiAnswerSplitsOnCommonSeparators() {
        assertEquals(setOf("A", "C"), QuizAnswerNormalizer.resolveCorrectIds(abcd(), "A,C").toSet())
        assertEquals(setOf("A", "C"), QuizAnswerNormalizer.resolveCorrectIds(abcd(), "A、C").toSet())
    }

    @Test
    fun withResolvedCorrectFlagsTheRightOption() {
        val out = QuizAnswerNormalizer.withResolvedCorrect(trueFalse(), "错误")
        assertTrue("B is correct", out.first { it.id == "B" }.isCorrect)
        assertFalse("A is not correct", out.first { it.id == "A" }.isCorrect)
    }

    @Test
    fun unresolvableAnswerLeavesNoCorrectOption() {
        assertTrue(QuizAnswerNormalizer.resolveCorrectIds(trueFalse(), "完全不相关的内容").isEmpty())
        assertTrue(QuizAnswerNormalizer.resolveCorrectIds(trueFalse(), null).isEmpty())
        assertTrue(QuizAnswerNormalizer.resolveCorrectIds(trueFalse(), "").isEmpty())
    }
}
