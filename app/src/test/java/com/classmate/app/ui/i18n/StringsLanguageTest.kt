package com.classmate.app.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The migrated i18n surfaces (evidence states + help dialogs) must actually follow language: the English
 * pack carries no Chinese, and the two packs differ. (ZH/EN key parity is already compile-enforced by the
 * `Strings` data class, so a missing key cannot exist.)
 */
class StringsLanguageTest {

    private fun hasCjk(s: String) = s.any { it.code in 0x4E00..0x9FFF }

    @Test
    fun chineseLanguageLabelHasNoStrayEnglish() {
        // Chinese mode showed "语言 Language" / "Accent Color" — plain UI labels must not mix English.
        assertEquals("语言", appStrings(AppLanguage.ZH).settingsLanguage)
        assertEquals("Language", appStrings(AppLanguage.EN).settingsLanguage)
        val settings = java.io.File("app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt")
            .let { if (it.exists()) it else java.io.File("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt") }
            .readText()
        assertFalse("'Accent Color' should be 强调色 in plain UI", settings.contains("Accent Color"))
    }

    @Test
    fun englishEvidenceAndHelpHaveNoChinese() {
        val en = appStrings(AppLanguage.EN)
        val texts = listOf(
            en.evidenceView, en.evidenceCheck, en.evidenceNone, en.evidenceWeakNote, en.helpDismiss,
            en.helpTranscriptTitle, en.helpRecordingTitle, en.helpReviewTitle, en.helpExportTitle,
        ) + en.helpTranscriptPoints + en.helpRecordingPoints + en.helpReviewPoints + en.helpExportPoints
        texts.forEach { assertFalse("English string contains Chinese: $it", hasCjk(it)) }
    }

    @Test
    fun evidenceStatesFollowLanguage() {
        val zh = appStrings(AppLanguage.ZH)
        val en = appStrings(AppLanguage.EN)
        assertEquals("证据待核对", zh.evidenceCheck)
        assertEquals("Check evidence", en.evidenceCheck)
        assertNotEquals(zh.evidenceView, en.evidenceView)
        assertNotEquals(zh.evidenceNone, en.evidenceNone)
    }

    @Test
    fun helpPointCountsMatchAcrossLanguages() {
        val zh = appStrings(AppLanguage.ZH)
        val en = appStrings(AppLanguage.EN)
        assertEquals(zh.helpTranscriptPoints.size, en.helpTranscriptPoints.size)
        assertEquals(zh.helpRecordingPoints.size, en.helpRecordingPoints.size)
        assertEquals(zh.helpReviewPoints.size, en.helpReviewPoints.size)
        assertEquals(zh.helpExportPoints.size, en.helpExportPoints.size)
    }
}
