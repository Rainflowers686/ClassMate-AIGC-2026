package com.classmate.app.ui.i18n

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for screens that have been MIGRATED to the [Strings] localization system: they must
 * read copy from `appStrings(...)` and must not re-introduce hardcoded Chinese product strings. The
 * migrated-screen list grows as more pages are localized; un-migrated screens are tracked in the round
 * report, not here (a whole-app hardcoded scan would fail until every page is migrated).
 */
class HardcodedUiStringGuardTest {

    private fun read(rel: String): String =
        listOf(File(rel), File("../$rel")).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)
            ?: error("missing $rel")

    /** Screens fully migrated to appStrings(...). Each value lists Chinese product strings that must be gone. */
    private val migrated: Map<String, List<String>> = mapOf(
        "app/src/main/java/com/classmate/app/ui/screens/quiz/QuizScreen.kt" to listOf(
            "\"微测\"", "\"还没有微测题。\"", "\"上一题\"", "\"下一题\"", "\"去复习计划\"",
            "\"讲解\"", "\"考查知识点\"", "\"太难\"", "\"需要多练\"", "\"正确\"", "\"你的选择\"", "\"已选择\"",
        ),
    )

    @Test
    fun migratedScreensUseAppStrings() {
        migrated.keys.forEach { path ->
            assertTrue("$path should read copy from appStrings(...)", read(path).contains("appStrings("))
        }
    }

    @Test
    fun migratedScreensHaveNoHardcodedChineseProductStrings() {
        migrated.forEach { (path, banned) ->
            val src = read(path)
            banned.forEach { literal ->
                assertFalse("$path still hardcodes product string: $literal", src.contains(literal))
            }
        }
    }
}
