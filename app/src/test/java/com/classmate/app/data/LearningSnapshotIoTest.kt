package com.classmate.app.data

import com.classmate.core.learning.LearningStore
import com.classmate.core.learning.ReviewEventType
import com.classmate.core.sample.SampleCourses
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The persisted cross-course learning file (filesDir/classmate_learning_state.json) must hold ONLY
 * learning business data — never credentials, auth headers, prompts, or vendor request/response
 * bodies. This guards that contract at the file-content level.
 */
class LearningSnapshotIoTest {

    private val now = 1_700_000_000_000L

    @Test
    fun fileRoundTripsBusinessDataWithNoSecrets() {
        val file = Files.createTempDirectory("cm-learn").resolve("classmate_learning_state.json").toFile()
        val result = SampleCourses.seriesAnalysis(now)
        val store = LearningStore(FileSnapshotIo(file)) { now }
        store.addTasksFromAnalysis(result, "无穷级数", "BLUELM", "官方 BlueLM", "qwen3.5-plus")
        store.recordQuizAttempt(result.sessionId, result.knowledgePoints[0].id, "q_1", "B", "A", isCorrect = false)
        store.recordFeedbackForTask(store.snapshot().tasks.first().taskId, ReviewEventType.MASTERED)

        val text = file.readText()
        // It really persisted the learning queue …
        assertTrue(file.exists() && text.isNotBlank())
        assertTrue(text.contains("task_"))
        assertTrue(text.contains("\"tasks\""))

        // … and it leaks nothing sensitive.
        listOf(
            "Authorization", "Bearer", "app_id", "appId", "appKey", "apiKey", "sk-",
            "X-AI-Gateway", "reasoning_content", "prompt", "messages", "systemPrompt",
        ).forEach { needle ->
            assertFalse("learning_state.json must not contain \"$needle\"", text.contains(needle))
        }
    }
}
