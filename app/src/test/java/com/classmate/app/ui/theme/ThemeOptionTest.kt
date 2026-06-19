package com.classmate.app.ui.theme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeOptionTest {

    @Test
    fun defaultThemeIsStandardStudy() {
        assertEquals(ThemePreset.STANDARD_STUDY, ThemePreset.Default)
    }

    @Test
    fun threeThemesAreAvailable() {
        assertEquals(3, ThemePreset.entries.size)
        assertEquals(
            listOf(ThemePreset.STANDARD_STUDY, ThemePreset.ACTIVE_STUDY, ThemePreset.FOCUS_IMMERSION),
            ThemePreset.entries.toList(),
        )
    }

    @Test
    fun accentColorPresetsAreAvailable() {
        assertEquals(
            listOf(
                AccentColorPreset.BLUE,
                AccentColorPreset.CYAN,
                AccentColorPreset.GREEN,
                AccentColorPreset.PURPLE,
                AccentColorPreset.AMBER,
                AccentColorPreset.ROSE,
                AccentColorPreset.GRAPHITE,
                AccentColorPreset.OCEAN,
            ),
            AccentColorPreset.entries.toList(),
        )
    }

    @Test
    fun typographyPresetsAreAvailableWithoutAddingThemePresets() {
        assertEquals(3, ThemePreset.entries.size)
        assertEquals(TypographyPreset.SYSTEM_DEFAULT, TypographyPreset.Default)
        assertEquals(
            listOf(
                TypographyPreset.SYSTEM_DEFAULT,
                TypographyPreset.ACADEMIC,
                TypographyPreset.MODERN_ROUNDED,
                TypographyPreset.CLEAN_SANS,
                TypographyPreset.TITLE_PERSONALITY,
            ),
            TypographyPreset.entries.toList(),
        )
    }

    @Test
    fun typographyPresetsDoNotRequireBundledFontFiles() {
        val fontDirs = listOf(File("src/main/res/font"), File("app/src/main/res/font"))
        val fontFiles = fontDirs.filter { it.exists() }.flatMap { it.walkTopDown().filter { f -> f.isFile }.toList() }
        assertTrue("advanced typography should use system font families only", fontFiles.isEmpty())
    }

    @Test
    fun customPaletteSupportsPrimarySecondaryTertiaryAndSafeOnColor() {
        val palette = CustomPalette(
            enabled = true,
            primaryHex = "#F7F7F7",
            secondaryHex = "#111111",
            tertiaryHex = "#006D32",
        )
        val scheme = themeColors(ThemePreset.STANDARD_STUDY, AccentColorPreset.GREEN, customPalette = palette)

        assertEquals(parseHexColorOrNull("#F7F7F7"), scheme.classMate.primary)
        assertEquals(parseHexColorOrNull("#111111"), scheme.classMate.secondary)
        assertEquals(parseHexColorOrNull("#006D32"), scheme.classMate.tertiary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF111111), bestOnColorFor(parseHexColorOrNull("#F7F7F7")!!))
        assertEquals(androidx.compose.ui.graphics.Color.White, bestOnColorFor(parseHexColorOrNull("#111111")!!))
    }

    @Test
    fun customPaletteWarnsForInvalidOrLowContrastValues() {
        val warnings = validateCustomPalette(
            CustomPalette(enabled = true, primaryHex = "#F8FAF3", secondaryHex = "bad", tertiaryHex = "#111111"),
            background = androidx.compose.ui.graphics.Color(0xFFF8FAF3),
            text = androidx.compose.ui.graphics.Color(0xFF191C18),
        )

        assertTrue(warnings.any { it.contains("Primary") })
        assertTrue(warnings.any { it.contains("Secondary") })
        assertFalse(warnings.any { it.contains("AppKey") })
    }

    @Test
    fun focusImmersionUsesAccentPresetNotFixedSourcePink() {
        val focusBlue = classMateColorScheme(ThemePreset.FOCUS_IMMERSION, AccentColorPreset.BLUE)
        val focusRose = classMateColorScheme(ThemePreset.FOCUS_IMMERSION, AccentColorPreset.ROSE)

        assertEquals(AccentColorPreset.BLUE.resolveFor(ThemePreset.FOCUS_IMMERSION, dark = true), focusBlue.primary)
        assertEquals(AccentColorPreset.ROSE.resolveFor(ThemePreset.FOCUS_IMMERSION, dark = true), focusRose.primary)
        assertTrue(focusBlue.primary != androidx.compose.ui.graphics.Color(0xFFFFB1C3))
    }

    @Test
    fun focusImmersionUsesReadableGraphiteDepth() {
        val focus = classMateColorScheme(ThemePreset.FOCUS_IMMERSION, AccentColorPreset.OCEAN)

        assertTrue(focus.background != androidx.compose.ui.graphics.Color(0xFF000000))
        assertTrue(focus.background != androidx.compose.ui.graphics.Color(0xFF0E0E0E))
        assertTrue(focus.background != focus.surface)
        assertTrue(focus.surface != focus.surfaceContainerLow)
        assertTrue(focus.surfaceContainerLow != focus.surfaceContainerHigh)
        assertTrue(focus.outline != androidx.compose.ui.graphics.Color(0xFF5C3F45))
        assertEquals(AccentColorPreset.OCEAN.resolveFor(ThemePreset.FOCUS_IMMERSION, dark = true), focus.primary)
    }
}
