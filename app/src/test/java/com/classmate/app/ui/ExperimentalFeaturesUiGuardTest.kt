package com.classmate.app.ui

import com.classmate.app.state.ClassMateUiState
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentalFeaturesUiGuardTest {
    @Test
    fun experimentalFeatureFlagsDefaultOff() {
        val state = ClassMateUiState()

        assertFalse(state.enableExperimentalImageGeneration)
        assertFalse(state.enableExperimentalVideoGeneration)
        assertFalse(state.enableExperimentalSimultaneousInterpretation)
    }

    @Test
    fun settingsExposeExperimentalFeatureToggles() {
        val settings = read("java/com/classmate/app/ui/screens/settings/SettingsScreen.kt")

        assertTrue(settings.contains("EXPERIMENTAL_FEATURES"))
        assertTrue(settings.contains("setExperimentalImageGeneration"))
        assertTrue(settings.contains("setExperimentalVideoGeneration"))
        assertTrue(settings.contains("setExperimentalSimultaneousInterpretation"))
        assertTrue(settings.contains("学习图解"))
        assertTrue(settings.contains("复习短视频"))
        assertTrue(settings.contains("双语课堂"))
    }

    @Test
    fun courseDetailShowsExperimentalEntriesOnlyBehindFlags() {
        val course = read("java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt")

        assertTrue(course.contains("if (ui.enableExperimentalImageGeneration)"))
        assertTrue(course.contains("if (ui.enableExperimentalVideoGeneration)"))
        assertTrue(course.contains("if (ui.enableExperimentalSimultaneousInterpretation)"))
        assertTrue(course.contains("generateVisualStudyPrompt"))
        assertTrue(course.contains("generateReviewVideoStoryboard"))
        assertTrue(course.contains("generateBilingualTranscriptDraft"))
        assertTrue(course.contains("generateAudioReviewScript"))
    }

    @Test
    fun importCourseExposesDialectModeSelector() {
        val import = read("java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt")

        assertTrue(import.contains("setAudioDialectMode"))
        assertTrue(import.contains("DIALECT_OR_ACCENT_ENHANCED"))
        assertTrue(import.contains("CLASSROOM_MIXED_SPEAKERS"))
        assertTrue(import.contains("方言"))
    }

    private fun read(rel: String): String =
        listOf(File("src/main/$rel"), File("app/src/main/$rel")).first { it.exists() }.readText()
}
