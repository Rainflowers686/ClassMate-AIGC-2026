package com.classmate.app.ui.i18n

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F0-4: the help "?" popups must be localized — in Chinese mode they show Chinese, in English mode they
 * show English. Every HelpHint(...) call site must source title/points from appStrings(...) (no hardcoded
 * English literal), and the help packs themselves must flip between the ZH and EN [Strings].
 */
class HelpHintLocalizationGuardTest {

    private fun uiFilesCallingHelpHint(): List<File> {
        val roots = listOf(File("app/src/main/java/com/classmate/app/ui"), File("src/main/java/com/classmate/app/ui"))
        val root = roots.firstOrNull { it.exists() } ?: error("missing ui source root")
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name != "HelpHint.kt" && it.readText().contains("HelpHint(") }
            .toList()
    }

    @Test
    fun everyHelpHintCallSiteUsesLocalizedCopy() {
        val files = uiFilesCallingHelpHint()
        assertTrue("expected at least one HelpHint call site", files.isNotEmpty())
        files.forEach { file ->
            val src = file.readText()
            // A hardcoded literal title (HelpHint(title = "…") ) would not flip with language.
            assertFalse(
                "${file.name}: HelpHint must not hardcode a literal title",
                src.contains(Regex("""HelpHint\(\s*title\s*=\s*"""")),
            )
            assertTrue(
                "${file.name}: HelpHint should source copy from appStrings(...)",
                src.contains("appStrings(") || src.contains("s.help"),
            )
        }
    }

    @Test
    fun helpPacksFlipBetweenChineseAndEnglish() {
        val zh = appStrings(AppLanguage.ZH)
        val en = appStrings(AppLanguage.EN)
        assertNotEquals(zh.helpTranscriptTitle, en.helpTranscriptTitle)
        assertNotEquals(zh.helpRecordingTitle, en.helpRecordingTitle)
        assertNotEquals(zh.helpReviewTitle, en.helpReviewTitle)
        assertNotEquals(zh.helpExportTitle, en.helpExportTitle)
        // The Chinese pack must actually contain Chinese (not leftover English).
        assertTrue(zh.helpTranscriptPoints.all { it.any { ch -> ch.code in 0x4E00..0x9FFF } })
    }
}
