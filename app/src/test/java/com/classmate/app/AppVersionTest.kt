package com.classmate.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The competition demo build must report version 1.7.3 / 100, not older release lines. The build commit is
 * kept separately via BuildConfig.GIT_COMMIT (not asserted here).
 */
class AppVersionTest {

    private val gradle: String =
        listOf(File("app/build.gradle.kts"), File("../app/build.gradle.kts"))
            .firstOrNull { it.exists() }?.readText(Charsets.UTF_8) ?: error("missing app/build.gradle.kts")

    @Test
    fun versionNameIsCurrentAndNotOld() {
        assertTrue(gradle.contains("versionName = \"1.7.3\""))
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
    }

    @Test
    fun versionCodeIsCurrent() {
        assertTrue(gradle.contains("versionCode = 100"))
    }

    @Test
    fun buildKeepsGitCommitField() {
        assertTrue(gradle.contains("GIT_COMMIT"))
    }
}
