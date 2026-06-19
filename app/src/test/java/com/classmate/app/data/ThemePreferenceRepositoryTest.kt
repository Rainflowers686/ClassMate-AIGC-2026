package com.classmate.app.data

import com.classmate.app.ui.theme.AccentColorPreset
import com.classmate.app.ui.theme.ThemePreset
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceRepositoryTest {

    @Test
    fun defaultPreferenceIsStandardStudy() {
        val repo = ThemePreferenceRepository(tempFile())

        assertEquals(ThemePreset.STANDARD_STUDY, repo.load().themePreset)
        assertEquals(AccentColorPreset.GREEN, repo.load().accentColorPreset)
    }

    @Test
    fun themeAndAccentPersistAcrossRepositoryInstances() {
        val file = tempFile()
        val repo = ThemePreferenceRepository(file)

        repo.saveThemePreset(ThemePreset.FOCUS_IMMERSION)
        repo.saveAccentColorPreset(AccentColorPreset.OCEAN)

        val reopened = ThemePreferenceRepository(file).load()
        assertEquals(ThemePreset.FOCUS_IMMERSION, reopened.themePreset)
        assertEquals(AccentColorPreset.OCEAN, reopened.accentColorPreset)
    }

    private fun tempFile(): File =
        File.createTempFile("classmate-theme-preference", ".json").also { it.deleteOnExit(); it.delete() }
}
