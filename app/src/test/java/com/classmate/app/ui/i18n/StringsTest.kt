package com.classmate.app.ui.i18n

import com.classmate.app.state.ClassMateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
