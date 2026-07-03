package com.classmate.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The competition demo build must report version 1.14.17 / 130, not older release lines. Build metadata is
 * kept separately via BuildConfig fields.
 */
class AppVersionTest {

    private val gradle: String =
        listOf(File("app/build.gradle.kts"), File("../app/build.gradle.kts"))
            .firstOrNull { it.exists() }?.readText(Charsets.UTF_8) ?: error("missing app/build.gradle.kts")

    @Test
    fun versionNameIsCurrentAndNotOld() {
        assertTrue(gradle.contains("versionName = \"1.14.17\""))
        assertFalse(gradle.contains("versionName = \"0.1.0\""))
        assertFalse(gradle.contains("versionName = \"1.0.0\""))
        assertFalse(gradle.contains("versionName = \"1.0.1\""))
        assertFalse(gradle.contains("versionName = \"1.1.1\""))
        assertFalse(gradle.contains("versionName = \"1.1.2\""))
        assertFalse(gradle.contains("versionName = \"1.2.2\""))
        assertFalse(gradle.contains("versionName = \"1.3.2\""))
        assertFalse(gradle.contains("versionName = \"1.3.3\""))
        assertFalse(gradle.contains("versionName = \"1.4.3\""))
        assertFalse(gradle.contains("versionName = \"1.5.3\""))
        assertFalse(gradle.contains("versionName = \"1.6.3\""))
        assertFalse(gradle.contains("versionName = \"1.7.3\""))
        assertFalse(gradle.contains("versionName = \"1.8.3\""))
        assertFalse(gradle.contains("versionName = \"1.9.3\""))
        assertFalse(gradle.contains("versionName = \"1.10.3\""))
        assertFalse(gradle.contains("versionName = \"1.11.3\""))
        assertFalse(gradle.contains("versionName = \"1.11.4\""))
        assertFalse(gradle.contains("versionName = \"1.11.5\""))
        assertFalse(gradle.contains("versionName = \"1.12.5\""))
        assertFalse(gradle.contains("versionName = \"1.13.5\""))
        assertFalse(gradle.contains("versionName = \"1.13.6\""))
        assertFalse(gradle.contains("versionName = \"1.13.7\""))
        assertFalse(gradle.contains("versionName = \"1.13.8\""))
        assertFalse(gradle.contains("versionName = \"1.13.9\""))
        assertFalse(gradle.contains("versionName = \"1.14.0\""))
        assertFalse(gradle.contains("versionName = \"1.14.1\""))
        assertFalse(gradle.contains("versionName = \"1.14.2\""))
        assertFalse(gradle.contains("versionName = \"1.14.3\""))
        assertFalse(gradle.contains("versionName = \"1.14.4\""))
        assertFalse(gradle.contains("versionName = \"1.14.5\""))
        assertFalse(gradle.contains("versionName = \"1.14.6\""))
        assertFalse(gradle.contains("versionName = \"1.14.7\""))
        assertFalse(gradle.contains("versionName = \"1.14.8\""))
        assertFalse(gradle.contains("versionName = \"1.14.9\""))
        assertFalse(gradle.contains("versionName = \"1.14.10\""))
        assertFalse(gradle.contains("versionName = \"1.14.11\""))
        assertFalse(gradle.contains("versionName = \"1.14.12\""))
        assertFalse(gradle.contains("versionName = \"1.14.13\""))
        assertFalse(gradle.contains("versionName = \"1.14.14\""))
        assertFalse(gradle.contains("versionName = \"1.14.15\""))
        assertFalse(gradle.contains("versionName = \"1.14.16\""))
    }

    @Test
    fun versionCodeIsCurrent() {
        assertTrue(gradle.contains("versionCode = 130"))
    }

    @Test
    fun buildKeepsSafeMetadataFields() {
        assertTrue(gradle.contains("GIT_COMMIT"))
        assertTrue(gradle.contains("BUILD_TIME"))
    }

    @Test
    fun developerSettingsExposeBuildMetadataAndDebugEvents() {
        val settings = listOf(
            File("app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
            File("../app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
        ).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)
            ?: error("missing SettingsScreen.kt")

        assertTrue(settings.contains("BuildInfo.versionName"))
        assertTrue(settings.contains("BuildInfo.versionCode"))
        assertTrue(settings.contains("BuildInfo.gitCommitShort"))
        assertTrue(settings.contains("BuildInfo.buildTime"))
        assertTrue(settings.contains("DebugEventLog"))
        assertTrue(settings.contains("debugEventLogText"))
        assertTrue(settings.contains("DiagnosticsAndLogsCard(viewModel)"))
        assertTrue(settings.contains("诊断与日志"))
        assertTrue(settings.contains("复制完整诊断包"))
        assertTrue(settings.contains("复制最近 200 条事件"))
        assertTrue(settings.contains("复制上次崩溃"))
        assertTrue(settings.contains("清空诊断日志"))
    }
}
