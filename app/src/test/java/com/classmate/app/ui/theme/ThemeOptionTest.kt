package com.classmate.app.ui.theme

import org.junit.Assert.assertEquals
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
