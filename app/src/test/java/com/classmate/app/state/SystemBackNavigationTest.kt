package com.classmate.app.state

import com.classmate.app.platform.ConfigRepository
import com.classmate.app.ui.screens.settings.SettingsPage
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Android system back key / gesture must route through [AppViewModel.handleSystemBack] and stay
 * inside the app: settings sub-pages walk up their tree, screens pop the back stack, and only the very
 * root is left unhandled (so the OS exits).
 */
class SystemBackNavigationTest {

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-back").resolve("config.local.json").toFile()),
    )

    private fun onSettingsSubPage(page: SettingsPage): AppViewModel {
        val viewModel = vm()
        viewModel.navigateTo(Screen.SETTINGS)
        viewModel.openSettingsPage(page)
        return viewModel
    }

    @Test
    fun appearanceThemeBackGoesToGeneralSettingsNotExit() {
        val viewModel = onSettingsSubPage(SettingsPage.APPEARANCE_THEME)
        assertTrue(viewModel.canHandleSystemBack)
        assertTrue(viewModel.handleSystemBack())
        assertEquals(SettingsPage.GENERAL_SETTINGS, viewModel.settingsPage)
        assertEquals(Screen.SETTINGS, viewModel.currentScreen) // stayed in the app
    }

    @Test
    fun aiModelConfigBackGoesToGeneralSettings() {
        val viewModel = onSettingsSubPage(SettingsPage.AI_MODEL_CONFIG)
        viewModel.handleSystemBack()
        assertEquals(SettingsPage.GENERAL_SETTINGS, viewModel.settingsPage)
    }

    @Test
    fun learningExportBackGoesToGeneralSettings() {
        val viewModel = onSettingsSubPage(SettingsPage.LEARNING_EXPORT)
        viewModel.handleSystemBack()
        assertEquals(SettingsPage.GENERAL_SETTINGS, viewModel.settingsPage)
    }

    @Test
    fun advancedColorBackGoesToAppearanceThenGeneral() {
        val viewModel = onSettingsSubPage(SettingsPage.ADVANCED_COLOR_CUSTOMIZATION)
        viewModel.handleSystemBack()
        assertEquals(SettingsPage.APPEARANCE_THEME, viewModel.settingsPage)
        viewModel.handleSystemBack()
        assertEquals(SettingsPage.GENERAL_SETTINGS, viewModel.settingsPage)
    }

    @Test
    fun settingsHomeBackPopsTheAppStack() {
        val viewModel = vm()
        viewModel.navigateTo(Screen.SETTINGS) // stack: HOME, SETTINGS; settingsPage = HOME
        assertTrue(viewModel.canHandleSystemBack)
        viewModel.handleSystemBack()
        assertEquals(Screen.HOME, viewModel.currentScreen)
    }

    @Test
    fun courseDetailBackDoesNotExit() {
        val viewModel = vm()
        viewModel.navigateTo(Screen.COURSE_DETAIL)
        assertTrue(viewModel.handleSystemBack())
        assertEquals(Screen.HOME, viewModel.currentScreen)
    }

    @Test
    fun evidenceDetailBackReturnsToPreviousScreen() {
        val viewModel = vm()
        viewModel.navigateTo(Screen.COURSE_DETAIL)
        viewModel.navigateTo(Screen.EVIDENCE)
        viewModel.handleSystemBack()
        assertEquals(Screen.COURSE_DETAIL, viewModel.currentScreen)
    }

    @Test
    fun rootHomeBackIsNotHandledSoTheOsCanExit() {
        val viewModel = vm() // backStack = [HOME], settings home, no recording
        assertFalse(viewModel.canHandleSystemBack)
        assertFalse(viewModel.handleSystemBack())
    }

    @Test
    fun switchingTabsResetsSettingsSubPage() {
        val viewModel = onSettingsSubPage(SettingsPage.AI_MODEL_CONFIG)
        viewModel.selectTab(Tab.HOME)
        assertEquals(SettingsPage.SETTINGS_HOME, viewModel.settingsPage)
    }

    // ---- P1-1: TopBar back guard (goBackOrHome) for the detail screens ----

    @Test
    fun topBarBackFromADetailScreenPopsOnePage() {
        // CourseDetail -> EvidenceDetail / Knowledge / Quiz / Import all pop to the previous page.
        listOf(Screen.EVIDENCE, Screen.KNOWLEDGE, Screen.QUIZ, Screen.IMPORT).forEach { detail ->
            val viewModel = vm()
            viewModel.navigateTo(Screen.COURSE_DETAIL)
            viewModel.navigateTo(detail)
            viewModel.goBackOrHome()
            assertEquals("back from $detail pops to the previous page", Screen.COURSE_DETAIL, viewModel.currentScreen)
        }
    }

    @Test
    fun topBarBackWithNoPreviousPageGoesHomeNotNothing() {
        // A rootless detail page (stack reset to just this screen) must fall back to Home, never strand.
        val viewModel = vm()
        viewModel.resetTo(Screen.EVIDENCE)
        assertFalse(viewModel.canGoBack)
        viewModel.goBackOrHome()
        assertEquals(Screen.HOME, viewModel.currentScreen)
    }
}
