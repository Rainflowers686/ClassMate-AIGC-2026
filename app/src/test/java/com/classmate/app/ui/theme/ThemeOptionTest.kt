package com.classmate.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeOptionTest {

    @Test
    fun defaultThemeIsFocus() {
        assertEquals(ThemeOption.FOCUS, ThemeOption.Default)
    }

    @Test
    fun threeThemesAreAvailable() {
        assertEquals(3, ThemeOption.entries.size)
    }
}
