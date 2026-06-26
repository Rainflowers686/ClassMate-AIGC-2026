package com.classmate.app.ui.i18n

import com.classmate.app.platform.ConfigRepository
import com.classmate.app.state.AppViewModel
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards that language switching is a real, instant capability: [AppViewModel.setLanguage] updates the
 * state, the default is Chinese, SYSTEM resolves to a concrete pack, and the wired product strings flip
 * between Chinese and English (so a switch is not a no-op on the localized pages).
 */
class LanguageSwitchUiGuardTest {

    private fun newViewModel(): AppViewModel {
        val missing = Files.createTempDirectory("cm-lang").resolve("config.local.json").toFile()
        return AppViewModel(configRepository = ConfigRepository(missing))
    }

    @Test
    fun defaultLanguageIsChineseAndCanSwitch() {
        val vm = newViewModel()
        assertEquals(AppLanguage.ZH, vm.ui.language)
        vm.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, vm.ui.language)
        vm.setLanguage(AppLanguage.SYSTEM)
        assertEquals(AppLanguage.SYSTEM, vm.ui.language)
    }

    @Test
    fun switchingLanguageFlipsTheWiredProductStrings() {
        val zh = appStrings(AppLanguage.ZH)
        val en = appStrings(AppLanguage.EN)
        // Spanning nav + home + import + history + settings: a switch must change every one of these.
        assertNotEquals(zh.tabHome, en.tabHome)
        assertNotEquals(zh.homeImport, en.homeImport)
        assertNotEquals(zh.importTitle, en.importTitle)
        assertNotEquals(zh.historyTitle, en.historyTitle)
        assertNotEquals(zh.settingsTitle, en.settingsTitle)
        assertNotEquals(zh.localFallback, en.localFallback)
        // Quiz page (migrated this round) flips too.
        assertNotEquals(zh.quizToReview, en.quizToReview)
        assertNotEquals(zh.quizExplanation, en.quizExplanation)
        assertNotEquals(zh.quizTitle(1, 5), en.quizTitle(1, 5))
    }

    @Test
    fun systemLanguageResolvesToAConcretePack() {
        val resolved = AppLanguage.SYSTEM.resolve()
        assertTrue(resolved == AppLanguage.ZH || resolved == AppLanguage.EN)
        // appStrings never returns a "system" pack — it is always a concrete ZH/EN pack.
        assertNotEquals(appStrings(AppLanguage.ZH).tabHome, appStrings(AppLanguage.EN).tabHome)
    }
}
