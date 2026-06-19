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
            "defaultMinSize(minHeight = 88.dp)",
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
            ".padding(bottom = 224.dp)",
            "themeSelectorDescription",
            "defaultMinSize(minHeight = 76.dp)",
            "1.002f",
            "0.3f",
        ).forEach { assertTrue("missing animated accent swatch hook: $it", settings.contains(it)) }
        assertFalse("accent swatch selected outline should stay subtle", settings.contains("1.1.dp"))
        listOf(
            "theme-preview-container",
            "theme-preview-scale",
            "theme-preview-title",
            "Icons.Filled.Check",
            "defaultMinSize(minHeight = 100.dp)",
            "1.001f",
            "0.32f",
            "color = titleColor",
            "if (selected) accentColor.copy(alpha = if (tokens.isDark) 0.09f else 0.08f)",
        ).forEach { assertTrue("missing animated theme preview hook: $it", focusComponents.contains(it)) }
        assertFalse("theme selected card should not use a heavy selected border", focusComponents.contains("BorderStroke(if (selected) 0.9.dp"))
        assertFalse("theme selected card should not use a persistent grey selected panel", focusComponents.contains("targetValue = tokens.surfaceContainerLow"))
        listOf("Color.Gray", "Color.LightGray", "Color.DarkGray").forEach {
            assertFalse("theme selected card should not use gray overlay token: $it", focusComponents.contains(it))
        }
    }

    @Test
    fun appearanceThemeCopyIsShortEnoughForPhoneCards() {
        val source = source()
        listOf(
            "安静留白，适合日常阅读与复习",
            "更明快，适合练习与进度反馈",
            "深色低干扰，适合专注学习",
        ).forEach { assertTrue("missing short theme selector copy: $it", source.contains(it)) }
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
        listOf(
            "BottomNavigationDock(",
            "BottomNavigationDockItem(",
            "RoundedCornerShape(999.dp)",
            ".clip(dockShape)",
            ".padding(horizontal = 14.dp, vertical = 3.dp)",
            "height(52.dp)",
            "modifier = Modifier.weight(1f)",
            "bottom-nav-selected-dot",
            ".size(4.dp)",
            "CircleShape",
            "Color.Transparent",
        ).forEach {
            assertTrue("missing floating bottom nav polish hook: $it", app.contains(it))
        }
        assertFalse("bottom nav should not use the old oversized fixed selected width", app.contains(".width(64.dp)"))
        assertFalse("bottom nav should not wrap the whole item in a large selected surface", app.contains("label = \"bottom-nav-selected-container\""))
        assertFalse("bottom nav should not use an icon background selected pill", app.contains("label = \"bottom-nav-selected-icon-container\""))
        assertFalse("bottom nav should not use Material NavigationBarItem indicator overflow", app.contains("NavigationBarItem("))
        assertFalse("bottom nav should not use Material indicatorColor", app.contains("indicatorColor ="))
        listOf("0xFF2196F3", "0xFF1976D2", "0xFF6200EE").forEach {
            assertFalse("default Material blue/purple hardcode should not be introduced: $it", common.contains(it) || product.contains(it) || app.contains(it))
        }
    }

    @Test
    fun settingsV32KeepsStudentCopyAndLightweightHeaders() {
        val source = source()
        listOf(
            "overline = \"偏好\"",
            "title = \"导入草稿\"",
            "title = \"端侧模型\"",
            "SettingsPageHeader(page = page",
            "Icons.AutoMirrored.Filled.ArrowBack",
            ".size(32.dp)",
            ".padding(horizontal = 2.dp, vertical = 4.dp)",
            ".padding(bottom = 8.dp)",
            "emphasized = true",
            "emphasized -> colors.surface.copy",
        ).forEach { assertTrue("missing V3.2 settings polish hook: $it", source.contains(it)) }

        assertFalse(source.contains("\"设置层级\""))
        assertFalse(source.contains("\"设置首页\""))
        assertFalse("home settings entry should not use a heavy whole-card selected tint", source.contains("emphasized -> colors.primary.copy(alpha = if (colors.isDark) 0.14f else 0.07f)"))
    }

    @Test
    fun generalSettingsUseGroupedContainerInsteadOfScatteredCards() {
        val source = source()
        listOf(
            "SettingsGroupedListCard",
            "SettingsMiniStatusCard(",
            "SettingsEntryRow(\"外观与主题\"",
            "grouped = true",
            "verticalArrangement = Arrangement.spacedBy(3.dp)",
            "border = if (grouped) null else BorderStroke",
        ).forEach { assertTrue("missing grouped settings list guard: $it", source.contains(it)) }
        assertTrue("settings home should group status summary into one surface", source.contains("Modifier.padding(6.dp)"))
        assertTrue("settings home entries should use the grouped row container", source.contains("SettingsEntryRow(\"通用设置\"") && source.contains("emphasized = true, grouped = true"))
    }

    @Test
    fun importInputListAvoidsHardDividersInsideGroupedInputs() {
        val product = productSource()
        val importScreen = importSource()
        listOf(
            "GroupedList(",
            "softly spaced rows",
            "verticalArrangement = Arrangement.spacedBy(3.dp)",
            "label = \"grouped-row-container\"",
        ).forEach { assertTrue("missing grouped input list polish: $it", product.contains(it) || importScreen.contains(it)) }
        assertFalse("grouped input list should not call hard row hairlines", product.contains("RowHairline("))
        assertFalse("import input list should not use a visible Material divider", importScreen.contains("HorizontalDivider"))
        assertFalse("import input list should not use a visible Material divider", importScreen.contains("Divider("))
    }

    @Test
    fun bottomNavigationKeepsMainEntries() {
        val app = appSource()
        val strings = listOf(
            File("src/main/java/com/classmate/app/ui/i18n/Strings.kt"),
            File("app/src/main/java/com/classmate/app/ui/i18n/Strings.kt"),
        ).first { it.exists() }.readText()

        assertTrue(app.contains("Tab.entries.forEach"))
        assertTrue(app.contains("onSelect = { viewModel.selectTab(it) }"))
        assertTrue(app.contains("onClick = { onSelect(tab) }"))
        listOf("首页", "资料", "复习", "历史", "设置").forEach {
            assertTrue("missing bottom nav label: $it", strings.contains(it))
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

    private fun productSource(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/product/ProductUi.kt"),
            File("app/src/main/java/com/classmate/app/ui/product/ProductUi.kt"),
        ).first { it.exists() }.readText()

    private fun importSource(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt"),
        ).first { it.exists() }.readText()
}
