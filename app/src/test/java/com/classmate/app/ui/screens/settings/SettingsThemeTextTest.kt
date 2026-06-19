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

    @Test
    fun settingsIaUsesSinglePageStateWithoutDuplicateLayerCards() {
        val source = source()
        listOf(
            "private enum class SettingsPage",
            "SETTINGS_HOME",
            "GENERAL_SETTINGS",
            "APPEARANCE_THEME",
            "AI_MODEL_CONFIG",
            "PRIVACY_PERMISSIONS",
            "LEARNING_EXPORT",
            "AMBIENT_SOUND",
            "DEVELOPER_SETTINGS",
        ).forEach { assertTrue("missing Settings IA page state: $it", source.contains(it)) }

        listOf(
            "SettingsTopLevelNav(",
            "GeneralSettingsNav(",
            "SettingsHomeV2Card(",
            "GeneralSettingsHomeCard(",
            "SettingsLandingRow(",
            "\"设置层级\"",
            "\"设置首页\"",
        ).forEach { assertFalse("old duplicated Settings UI remains: $it", source.contains(it)) }
    }

    @Test
    fun generalAndDeveloperSettingsUseSeparatePages() {
        val source = source()
        listOf(
            "SettingsHomeCard(",
            "GeneralSettingsListCard(",
            "DeveloperSettingsHomeCard(viewModel)",
            "SettingsPage.DEVELOPER_SETTINGS",
            "SettingsPage.GENERAL_SETTINGS",
            "SettingsPageHeader(page = page",
        ).forEach { assertTrue("missing Settings page separation hook: $it", source.contains(it)) }
    }

    @Test
    fun settingsRowsConstrainTextAndCardHeights() {
        val source = source()
        listOf(
            "defaultMinSize(minHeight = 72.dp)",
            "defaultMinSize(minHeight = 104.dp)",
            "maxLines = 1",
            "maxLines = 2",
            "TextOverflow.Ellipsis",
        ).forEach { assertTrue("missing text wrapping or stable height guard: $it", source.contains(it)) }
    }

    private fun source(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
        ).first { it.exists() }.readText()
}
