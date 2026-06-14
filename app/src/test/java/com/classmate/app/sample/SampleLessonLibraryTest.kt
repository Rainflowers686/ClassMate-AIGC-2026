package com.classmate.app.sample

import com.classmate.app.platform.ConfigRepository
import com.classmate.app.state.AppViewModel
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleLessonLibraryTest {

    @Test
    fun sampleLessonCountIsSix() {
        assertEquals(6, SampleLessonLibrary.lessons.size)
    }

    @Test
    fun everySampleIsLongEnoughForFullPipelineSmoke() {
        SampleLessonLibrary.lessons.forEach { lesson ->
            val visibleChars = lesson.body.count { !it.isWhitespace() }
            assertTrue("${lesson.id} should be a long lesson, got $visibleChars", visibleChars > 800)
        }
    }

    @Test
    fun selectingSampleFillsCourseTitleSubjectAndBody() {
        val missing = Files.createTempDirectory("classmate-sample-vm").resolve("config.local.json").toFile()
        val viewModel = AppViewModel(configRepository = ConfigRepository(missing))
        val lesson = SampleLessonLibrary.lessons.first { it.id == "physics_electromagnetic_induction" }

        assertTrue(viewModel.loadSampleLesson(lesson.id))

        assertEquals(lesson.title, viewModel.ui.courseTitle)
        assertEquals(lesson.subject, viewModel.ui.selectedSubject)
        assertEquals(lesson.body, viewModel.ui.courseText)
    }

    @Test
    fun sampleTextDoesNotContainSensitiveMarkers() {
        val forbidden = listOf("Auth" + "orization", "Bear" + "er", "app" + "Key", "api" + "Key", "reasoning" + "_content")
        SampleLessonLibrary.lessons.forEach { lesson ->
            forbidden.forEach { marker ->
                assertFalse("${lesson.id} contains forbidden marker $marker", lesson.body.contains(marker, ignoreCase = true))
            }
        }
    }
}
