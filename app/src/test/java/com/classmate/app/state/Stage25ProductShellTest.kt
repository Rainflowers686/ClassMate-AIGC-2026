package com.classmate.app.state

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.learning.FeedbackCounters
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.learning.ReviewTask
import com.classmate.core.learning.ReviewTaskStatus
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.LearnerProfile
import com.classmate.core.provider.TransportResponse
import com.classmate.core.sample.SampleCourses
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage25ProductShellTest {
    private val now = 1_700_000_000_000L

    @Test
    fun liveCompanionManualSegmentsProduceCourseText() {
        val viewModel = vm()

        viewModel.updateLiveTitle("Calculus")
        viewModel.startLiveClass()
        viewModel.updateLiveSegment("First manual transcript segment.")
        viewModel.appendLiveSegment()
        viewModel.updateLiveSegment("Second manual transcript segment.")
        viewModel.appendLiveSegment()
        viewModel.endLiveClass()

        assertEquals(2, viewModel.ui.liveTranscript?.segments?.size)
        assertTrue(viewModel.ui.courseText.contains("First manual transcript segment."))
        assertTrue(viewModel.ui.courseText.contains("Second manual transcript segment."))
        assertFalse(viewModel.ui.courseText.contains("appKey", ignoreCase = true))
    }

    @Test
    fun textAndMarkdownImportUpdateDraftWithoutNetwork() {
        var calls = 0
        val transport = HttpTransport { _, _, _, _ ->
            calls++
            TransportResponse(200, "{}")
        }
        val viewModel = vm(transport = transport)

        assertTrue(viewModel.importTextDraft("Txt", "txt content", ImportSourceType.TXT_FILE, "lesson.txt"))
        assertEquals("txt content", viewModel.ui.courseText)
        assertTrue(viewModel.importTextDraft("Md", "# md content", ImportSourceType.MARKDOWN_FILE, "lesson.md"))
        assertEquals("# md content", viewModel.ui.courseText)
        viewModel.showImportPlaceholder(ImportSourceType.AUDIO_FILE)

        assertEquals(0, calls)
        assertTrue(viewModel.ui.toast.orEmpty().isNotBlank())
        assertFalse(viewModel.ui.toast.orEmpty().contains("not connected", ignoreCase = true))
    }

    @Test
    fun courseLibraryAggregatesHistoryAndDeleteRemovesCourseReviewTasks() {
        val learningStore = InMemoryLearningStore { now }
        learningStore.addTasksFromAnalysis(SampleCourses.seriesAnalysis(now), "Calculus - Series", "BLUELM", "official_bluelm", "qwen3.5-plus")
        val viewModel = vm(
            history = listOf(record("a", "Calculus - Series"), record("b", "Calculus - Limits")),
            learningStore = learningStore,
        )

        val summaries = viewModel.courseSummaries()
        assertEquals(1, summaries.size)
        assertEquals(2, summaries.single().lessonCount)
        assertTrue(viewModel.ui.learningSnapshot.tasks.isNotEmpty())

        viewModel.deleteHistory("a")

        assertEquals(0, viewModel.ui.learningSnapshot.tasks.size)
        assertEquals(LearnerProfile.OFFICIAL_BLUELM, viewModel.activeConfigBundle().profile)
    }

    @Test
    fun askThisLessonUsesLocalEvidenceWithoutNetwork() {
        // local_only / no-network style seam returns null -> the grounded engine answers from local
        // evidence only, never reaching the network and never inventing course-external facts.
        val session = SampleCourses.seriesSession(now)
        val result = SampleCourses.seriesAnalysis(now)
        val question = result.knowledgePoints.first().title
        val outcome = com.classmate.core.ask.GroundedAskLessonEngine.answer(
            question, session, result, com.classmate.core.ask.AskChatSeam { _, _ -> null },
        )
        assertTrue(outcome.answer.fallbackUsed)
        assertEquals("local", outcome.answer.providerName)
        assertFalse(outcome.answer.answer.contains("Authorization", ignoreCase = true))
        assertFalse(outcome.telemetry.safeLine().contains("Bearer", ignoreCase = true))
    }

    @Test
    fun exportIncludesCourseLibraryWeaknessAndNoSensitiveTokens() {
        val exportStore = InMemoryExportStore()
        val learningStore = InMemoryLearningStore { now }
        learningStore.addTasksFromAnalysis(SampleCourses.seriesAnalysis(now), "Calculus", "BLUELM", "official_bluelm", "qwen3.5-plus")
        val firstTask = learningStore.snapshot().tasks.first()
        learningStore.recordFeedbackForTask(firstTask.taskId, com.classmate.core.learning.ReviewEventType.NEED_EXAMPLE)
        val viewModel = vm(
            history = listOf(record("a", "Calculus")),
            learningStore = learningStore,
            exportStore = exportStore,
        )
        viewModel.openHistory(viewModel.ui.history.single())

        viewModel.exportLearningReport()

        val content = exportStore.saved.single().content
        assertTrue(content.contains("Course library"))
        assertTrue(content.contains("Weakness summary"))
        assertTrue(content.contains("Review tasks"))
        listOf("appKey", "apiKey", "Authorization", "Bearer", "reasoning_content", "messages").forEach {
            assertFalse("export must not contain $it", content.contains(it, ignoreCase = true))
        }
        assertTrue(viewModel.ui.lastExportReceipt?.pathSummary?.startsWith("memory/") == true)
    }

    private fun vm(
        transport: HttpTransport? = null,
        history: List<HistoryRecord> = emptyList(),
        learningStore: com.classmate.core.learning.LearningStore = InMemoryLearningStore { now },
        exportStore: InMemoryExportStore = InMemoryExportStore(),
    ): AppViewModel =
        AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-stage25").resolve("config.local.json").toFile()),
            transport = transport ?: HttpTransport { _, _, _, _ -> TransportResponse(200, "{}") },
            historyStore = InMemoryHistoryStore(history),
            learningStore = learningStore,
            exportStore = exportStore,
        )

    private fun record(id: String, title: String): HistoryRecord {
        val session = SampleCourses.seriesSession(now).copy(title = title)
        val result = SampleCourses.seriesAnalysis(now)
        return HistoryRecord(
            id = id,
            title = title,
            createdAtEpochMs = now + id.hashCode(),
            providerName = "BLUELM",
            profileLabel = "official_bluelm",
            model = "qwen3.5-plus",
            knowledgePointCount = result.knowledgePoints.size,
            quizCount = result.quizQuestions.size,
            fallbackUsed = false,
            validationStatus = "PASS",
            session = session,
            result = result,
        )
    }

    @Suppress("unused")
    private fun weakTask(): ReviewTask =
        ReviewTask(
            taskId = "task_1",
            knowledgePointId = "kp_1",
            courseSessionId = "session_1",
            courseTitle = "Calculus",
            title = "Series convergence",
            reason = "review",
            priority = 5,
            difficulty = "MEDIUM",
            estimatedMinutes = 5,
            createdAt = 0,
            dueAt = 0,
            nextReviewAt = 0,
            status = ReviewTaskStatus.DUE,
            sourceProvider = "BLUELM",
            sourceProfile = "official_bluelm",
            sourceModel = "qwen3.5-plus",
            counters = FeedbackCounters(wrongAnswer = 2),
        )
}
