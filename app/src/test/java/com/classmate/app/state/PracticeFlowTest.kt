package com.classmate.app.state

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.practice.PracticeMode
import com.classmate.core.practice.PracticeOutcome
import com.classmate.core.sample.SampleCourses
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeFlowTest {
    private val now = 1_700_000_000_000L

    private fun record(): HistoryRecord {
        val session = SampleCourses.seriesSession(now)
        val result = SampleCourses.seriesAnalysis(now)
        return HistoryRecord(
            id = "hist_1", title = session.title, createdAtEpochMs = now,
            providerName = "BLUELM", profileLabel = "official_bluelm", model = "qwen3.5-plus",
            knowledgePointCount = result.knowledgePoints.size, quizCount = result.quizQuestions.size,
            fallbackUsed = false, validationStatus = "PASS", session = session, result = result,
        )
    }

    private fun vm(): AppViewModel {
        val record = record()
        val learningStore = InMemoryLearningStore { now }
        learningStore.addTasksFromAnalysis(record.result, record.title, "BLUELM", "official_bluelm", "qwen3.5-plus")
        return AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-practice").resolve("config.local.json").toFile()),
            historyStore = InMemoryHistoryStore(listOf(record)),
            learningStore = learningStore,
            exportStore = InMemoryExportStore(),
        )
    }

    @Test
    fun startPracticeBuildsSessionAndNavigates() {
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first()) // loads result/session
        viewModel.startPractice(PracticeMode.QUICK_REVIEW)

        assertNotNull(viewModel.ui.practiceSession)
        assertTrue(viewModel.ui.practiceSession!!.items.isNotEmpty())
        assertEquals(Screen.PRACTICE, viewModel.currentScreen)
    }

    @Test
    fun startPracticeWithoutLoadedCourseFallsBackOrToasts() {
        // No course opened: still resolves a course from history (tasks exist) or toasts safely; never crashes.
        val viewModel = vm()
        viewModel.startPractice(PracticeMode.WEAKNESS_DRILL)
        // Either a session was built (course auto-loaded) or a guidance toast was shown.
        val ok = viewModel.ui.practiceSession != null || viewModel.ui.toast != null
        assertTrue(ok)
    }

    @Test
    fun completingPracticeProducesResultAndRecordsHistory() {
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.startPractice(PracticeMode.QUICK_REVIEW)
        val itemCount = viewModel.ui.practiceSession!!.items.size

        repeat(itemCount) { viewModel.answerPractice(PracticeOutcome.CORRECT) }

        assertNotNull(viewModel.ui.practiceResult)
        assertEquals(itemCount, viewModel.ui.practiceResult!!.correctCount)
        assertEquals(1, viewModel.ui.learningSnapshot.practiceHistory.size)
        assertTrue(viewModel.isPracticeComplete())
    }

    @Test
    fun exitPracticeClearsSession() {
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.startPractice(PracticeMode.QUICK_REVIEW)
        viewModel.exitPractice()
        assertNull(viewModel.ui.practiceSession)
        assertNull(viewModel.ui.practiceResult)
    }

    @Test
    fun reviewAndPracticeUiExposeExpectedEntries() {
        val review = source("app/src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt")
        listOf("开始练习", "错题重练", "需要多练").forEach { assertTrue("Review missing $it", review.contains(it)) }
        val practice = source("app/src/main/java/com/classmate/app/ui/screens/practice/PracticeSessionScreen.kt")
        listOf("我答对了", "我答错了", "已掌握", "需要多练", "本轮结果").forEach { assertTrue("Practice screen missing $it", practice.contains(it)) }
        // Stage 6 guarantee still holds: no "需要例题" in user-visible review/practice copy.
        assertFalse(review.contains("需要例题"))
        assertFalse(practice.contains("需要例题"))
    }

    private fun source(path: String): String =
        listOf(File(path), File(path.removePrefix("app/"))).first { it.exists() }.readText()
}
