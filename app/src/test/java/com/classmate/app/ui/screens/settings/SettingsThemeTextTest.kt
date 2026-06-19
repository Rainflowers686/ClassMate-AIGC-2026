package com.classmate.app.ui.screens.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsThemeTextTest {

    @Test
    fun appearanceSettingsExposeThreeClassMateThemesAndAccentColors() {
        val source = source()
        listOf(
            "默认学习",
            "活力学习",
            "沉浸学习",
            "强调色 / Accent Color",
            "AccentColorGrid",
            "ThemePreset.entries",
            "AccentColorPreset.entries",
        ).forEach { assertTrue("missing appearance copy or hook: $it", source.contains(it)) }
    }

    @Test
    fun appearanceSettingsDoNotReduceThemeChoiceToLightDarkSystem() {
        val source = source()
        assertFalse(source.contains("只有 Light / Dark / System"))
        assertFalse(source.contains("only Light / Dark / System", ignoreCase = true))
    }

    @Test
    fun themePreviewUsesTokensInsteadOfOldHardcodedSwatches() {
        val source = source()
        assertTrue(source.contains("classMateColorScheme"))
        assertFalse(source.contains("ThemeOption.FOCUS"))
        assertFalse(source.contains("ThemeOption.VITALITY"))
        assertFalse(source.contains("ThemeOption.FLOW"))
        assertFalse(source.contains("0xFFF3F4F7"))
    }

    private fun source(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
        ).first { it.exists() }.readText()
}
