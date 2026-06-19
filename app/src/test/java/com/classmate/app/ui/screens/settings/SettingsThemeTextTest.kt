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

    @Test
    fun settingsEntriesUseMappedIconsInsteadOfInitialPlaceholders() {
        val source = source()
        listOf(
            "private enum class SettingsEntryIcon",
            "SettingsEntryIcon.APPEARANCE_THEME -> Icons.Filled.Star",
            "SettingsEntryIcon.AI_MODEL_CONFIG -> Icons.Filled.PlayArrow",
            "SettingsEntryIcon.PRIVACY_PERMISSIONS -> Icons.Filled.CheckCircle",
            "SettingsEntryIcon.LEARNING_EXPORT -> Icons.Filled.DateRange",
            "SettingsEntryIcon.AMBIENT_SOUND -> Icons.Filled.Add",
            "Icon(icon.imageVector()",
        ).forEach { assertTrue("missing Settings icon mapping: $it", source.contains(it)) }
        assertFalse(source.contains("Text(title.take(1)"))
    }

    @Test
    fun themeAndAccentSelectionHaveAnimatedSelectedStates() {
        val settings = source()
        val focusComponents = componentSource("FocusComponents.kt")
        listOf(
            "animateColorAsState",
            "animateFloatAsState",
            "accent-swatch-scale",
            "Icons.Filled.Check",
        ).forEach { assertTrue("missing animated accent swatch hook: $it", settings.contains(it)) }
        listOf(
            "theme-preview-container",
            "theme-preview-scale",
            "Icons.Filled.Check",
            "defaultMinSize(minHeight = 116.dp)",
        ).forEach { assertTrue("missing animated theme preview hook: $it", focusComponents.contains(it)) }
    }

    @Test
    fun publicComponentsUseThemeTokensAndFloatingBottomNav() {
        val common = componentSource("CommonUi.kt")
        val product = componentSource("ProductComponents.kt")
        val app = appSource()
        listOf("card-surface", "buttonElevation", "surfaceContainerLow").forEach {
            assertTrue("missing polished common component hook: $it", common.contains(it))
        }
        listOf("status-chip-container", "CircleShape", "content.copy(alpha").forEach {
            assertTrue("missing status chip polish hook: $it", product.contains(it))
        }
        listOf("RoundedCornerShape(999.dp)", "Color.Transparent", "indicatorColor = themeColors.primaryContainer.copy").forEach {
            assertTrue("missing floating bottom nav polish hook: $it", app.contains(it))
        }
        listOf("0xFF2196F3", "0xFF1976D2", "0xFF6200EE").forEach {
            assertFalse("default Material blue/purple hardcode should not be introduced: $it", common.contains(it) || product.contains(it) || app.contains(it))
        }
    }

    private fun source(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
        ).first { it.exists() }.readText()

    private fun componentSource(name: String): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/components/$name"),
            File("app/src/main/java/com/classmate/app/ui/components/$name"),
        ).first { it.exists() }.readText()

    private fun appSource(): String =
        listOf(
            File("src/main/java/com/classmate/app/ClassMateApp.kt"),
            File("app/src/main/java/com/classmate/app/ClassMateApp.kt"),
        ).first { it.exists() }.readText()
}
