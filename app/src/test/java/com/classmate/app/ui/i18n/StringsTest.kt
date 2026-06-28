package com.classmate.app.ui.i18n

import com.classmate.app.state.ClassMateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsTest {

    @Test
    fun defaultUiLanguageIsChinese() {
        assertEquals(AppLanguage.ZH, ClassMateUiState().language)
    }

    @Test
    fun chinesePackIsChineseAndEnglishPackIsEnglish() {
        val zh = appStrings(AppLanguage.ZH)
        val en = appStrings(AppLanguage.EN)
        assertEquals("首页", zh.tabHome)
        assertEquals("Home", en.tabHome)
        assertEquals("导入学习材料", zh.homeImport)
        assertEquals("Import learning material", en.homeImport)
        // Switching language flips the key strings.
        assertNotEquals(zh.tabReview, en.tabReview)
        assertNotEquals(zh.historyTitle, en.historyTitle)
    }

    @Test
    fun parameterizedStringsInterpolate() {
        assertTrue(appStrings(AppLanguage.ZH).homeDueToday(3).contains("3"))
        assertTrue(appStrings(AppLanguage.ZH).homeDueToday(3).contains("复习"))
        assertTrue(appStrings(AppLanguage.EN).homeDueToday(3).contains("3"))
    }

    @Test
    fun systemResolvesToAConcretePack() {
        // SYSTEM never returns the SYSTEM pack itself; it resolves to ZH or EN.
        val resolved = AppLanguage.SYSTEM.resolve()
        assertTrue(resolved == AppLanguage.ZH || resolved == AppLanguage.EN)
    }

    @Test
    fun evidenceExportEnhancementAndOcrCopyFlipBetweenLanguages() {
        val zh = appStrings(AppLanguage.ZH)
        val en = appStrings(AppLanguage.EN)

        assertEquals("图片证据", zh.evidenceImageTitle)
        assertEquals("Image evidence", en.evidenceImageTitle)
        assertEquals("导出中心", zh.exportCenterTitle)
        assertEquals("Export center", en.exportCenterTitle)
        assertEquals("AI 整理未完成", zh.enhancementAiIncomplete)
        assertEquals("AI organization did not finish", en.enhancementAiIncomplete)
        assertEquals("确认加入课程", zh.ocrConfirmAddCourse)
        assertEquals("Confirm into course", en.ocrConfirmAddCourse)

        assertFalse(en.evidenceAssetMissing.containsChinese())
        assertFalse(en.exportCenterDescription.containsChinese())
        assertFalse(en.ocrShortReviewWarning.containsChinese())
    }

    private fun String.containsChinese(): Boolean =
        any { it in '\u4E00'..'\u9FFF' }
}
