package com.classmate.app.data

import com.classmate.app.ui.i18n.AppLanguage
import com.classmate.app.ui.theme.AccentColorPreset
import com.classmate.app.ui.theme.CustomPalette
import com.classmate.app.ui.theme.ThemePreset
import com.classmate.app.ui.theme.TypographyPreset
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceRepositoryTest {

    @Test
    fun freshInstallDefaultsToImmersiveLearningAndAcademicTypography() {
        val repo = ThemePreferenceRepository(tempFile())

        assertEquals(ThemePreset.FOCUS_IMMERSION, repo.load().themePreset)
        assertEquals(AccentColorPreset.GREEN, repo.load().accentColorPreset)
        assertEquals(TypographyPreset.ACADEMIC, repo.load().typographyPreset)
        assertEquals(false, repo.load().customPalette.enabled)
    }

    @Test
    fun customColorsAndTypographyPersistAcrossRepositoryInstances() {
        val file = tempFile()
        val repo = ThemePreferenceRepository(file)

        repo.saveCustomPalette(
            CustomPalette(
                enabled = true,
                primaryHex = "#123456",
                secondaryHex = "#654321",
                tertiaryHex = "#006D32",
            ),
        )
        repo.saveTypographyPreset(TypographyPreset.ACADEMIC)

        val reopened = ThemePreferenceRepository(file).load()
        assertEquals(true, reopened.customPalette.enabled)
        assertEquals("#123456", reopened.customPalette.primaryHex)
        assertEquals("#654321", reopened.customPalette.secondaryHex)
        assertEquals("#006D32", reopened.customPalette.tertiaryHex)
        assertEquals(TypographyPreset.ACADEMIC, reopened.typographyPreset)
    }

    @Test
    fun resetAdvancedAppearanceKeepsThemeAndAccentButClearsAdvancedChoices() {
        val file = tempFile()
        val repo = ThemePreferenceRepository(file)

        repo.saveThemePreset(ThemePreset.ACTIVE_STUDY)
        repo.saveAccentColorPreset(AccentColorPreset.OCEAN)
        repo.saveCustomPalette(CustomPalette(enabled = true, primaryHex = "#123456"))
        repo.saveTypographyPreset(TypographyPreset.TITLE_PERSONALITY)
        val reset = repo.resetAdvancedAppearance()

        assertEquals(ThemePreset.ACTIVE_STUDY, reset.themePreset)
        assertEquals(AccentColorPreset.OCEAN, reset.accentColorPreset)
        assertEquals(false, reset.customPalette.enabled)
        assertEquals(TypographyPreset.ACADEMIC, reset.typographyPreset)
    }

    @Test
    fun languagePersistsAcrossRepositoryInstances() {
        // P0-1: language used to reset to Chinese on restart because it was never persisted.
        val file = tempFile()
        assertEquals(AppLanguage.ZH, ThemePreferenceRepository(file).load().language)

        ThemePreferenceRepository(file).saveLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, ThemePreferenceRepository(file).load().language)

        ThemePreferenceRepository(file).saveLanguage(AppLanguage.SYSTEM)
        assertEquals(AppLanguage.SYSTEM, ThemePreferenceRepository(file).load().language)
    }

    @Test
    fun officialUserIdPersistsAcrossRepositoryInstances() {
        // The stable non-privacy user_id sent to official TTS/ASR must survive restarts (generated once).
        val file = tempFile()
        assertEquals("", ThemePreferenceRepository(file).load().officialUserId)
        ThemePreferenceRepository(file).saveOfficialUserId("abc123def456")
        assertEquals("abc123def456", ThemePreferenceRepository(file).load().officialUserId)
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
