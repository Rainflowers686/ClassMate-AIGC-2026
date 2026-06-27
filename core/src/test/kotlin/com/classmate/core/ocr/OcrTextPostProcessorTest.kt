package com.classmate.core.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTextPostProcessorTest {

    @Test
    fun normalizesMathSymbolsAndKeepsRealOnes() {
        val r = OcrTextPostProcessor.clean("当 x <= 3 且 y >= 2 时，a != b，结果 -> 收敛。")
        assertTrue(r.text.contains("≤"))
        assertTrue(r.text.contains("≥"))
        assertTrue(r.text.contains("≠"))
        assertTrue(r.text.contains("→"))
        assertFalse(r.needsReview)
    }

    @Test
    fun keepsExistingMathSymbols() {
        val r = OcrTextPostProcessor.clean("面积 = π × r²，根号 √2 ≈ 1.41。")
        assertTrue(r.text.contains("×"))
        assertTrue(r.text.contains("√"))
        assertFalse(r.needsReview)
    }

    @Test
    fun preservesNumberedListsAndBullets() {
        val src = "1. 第一点说明\n2. 第二点说明\n· 要点一\n· 要点二"
        val r = OcrTextPostProcessor.clean(src)
        assertTrue(r.text.contains("1. 第一点说明"))
        assertTrue(r.text.contains("2. 第二点说明"))
        assertEquals(4, r.text.lines().size)
    }

    @Test
    fun joinsWrappedProseButKeepsParagraphs() {
        // A wrapped sentence (no terminal punctuation) should rejoin; a real paragraph break stays.
        val src = "光合作用是绿色植物利用光能\n把二氧化碳和水转化为有机物。\n\n下一段开始。"
        val r = OcrTextPostProcessor.clean(src)
        assertTrue("wrapped line rejoined", r.text.contains("利用光能把二氧化碳"))
        assertTrue("paragraph break kept", r.text.contains("\n\n下一段开始"))
    }

    @Test
    fun normalizesFullWidthAscii() {
        val r = OcrTextPostProcessor.clean("ＡＢＣ１２３")
        assertTrue(r.text.contains("ABC123"))
    }

    @Test
    fun highGarbageTriggersReviewHintWithoutDestroyingText() {
        val r = OcrTextPostProcessor.clean("¥€¤§¶‡†˜˄˅ǂǁ‖¦×÷ ®©™ 的 ")
        assertTrue(r.needsReview)
        assertEquals(OcrTextPostProcessor.REVIEW_HINT, r.reviewHint)
    }

    @Test
    fun cleanChineseTextIsNotFlagged() {
        val r = OcrTextPostProcessor.clean("电磁感应定律说明：磁通量变化会产生感应电动势。")
        assertFalse(r.needsReview)
        assertEquals("", r.reviewHint)
    }

    @Test
    fun blankInputIsSafe() {
        val r = OcrTextPostProcessor.clean("   ")
        assertEquals("", r.text)
        assertFalse(r.needsReview)
    }
}
