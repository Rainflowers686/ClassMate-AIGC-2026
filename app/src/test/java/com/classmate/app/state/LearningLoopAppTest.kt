package com.classmate.app.state

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.learning.LearningStore
import com.classmate.core.learning.ReviewEngine
import com.classmate.core.provider.LearnerProfile
import com.classmate.core.sample.SampleCourses
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * App-level wiring of the cross-course learning queue into Home / Review / History. The pure rules
 * live in :core (LearningStoreTest); here we prove the ViewModel surfaces one shared snapshot and
 * that the closed loop survives tab switches and history deletes.
 */
class LearningLoopAppTest {

    private val now = 1_700_000_000_000L

    private fun missingFile() = Files.createTempDirectory("cm-loop").resolve("config.local.json").toFile()

    private fun seededLearningStore(): LearningStore {
        val store = InMemoryLearningStore { now }
        store.addTasksFromAnalysis(
            result = SampleCourses.seriesAnalysis(now),
            courseTitle = "无穷级数",
            sourceProvider = "BLUELM",
            sourceProfile = "官方 BlueLM",
            sourceModel = "qwen3.5-plus",
        )
        return store
    }

    private fun recentRecord(): HistoryRecord {
        val session = SampleCourses.seriesSession(now)
        val result = SampleCourses.seriesAnalysis(now)
        return HistoryRecord(
            id = "hist_1", title = "无穷级数", createdAtEpochMs = now,
            providerName = "BLUELM", profileLabel = "官方 BlueLM", model = "qwen3.5-plus",
            knowledgePointCount = result.knowledgePoints.size, quizCount = result.quizQuestions.size,
            fallbackUsed = false, validationStatus = "PASS", session = session, result = result,
        )
    }

    private fun vm(learningStore: LearningStore, history: List<HistoryRecord> = listOf(recentRecord())) =
        AppViewModel(
            configRepository = ConfigRepository(missingFile()),
            historyStore = InMemoryHistoryStore(history),
            learningStore = learningStore,
        )

    @Test
    fun homeSummaryShowsDueCountAndRecentCourse() {
        val viewModel = vm(seededLearningStore())
        // Home reads the same snapshot the store persists.
        assertTrue(ReviewEngine.dueCount(viewModel.ui.learningSnapshot, now) > 0)
        assertTrue(ReviewEngine.totalDueMinutes(viewModel.ui.learningSnapshot, now) > 0)
        assertEquals("无穷级数", viewModel.ui.history.first().title)
    }

    @Test
    fun reviewSeesDueTasksFromLearningStore() {
        val viewModel = vm(seededLearningStore())
        val due = ReviewEngine.listDueTasks(viewModel.ui.learningSnapshot, now)
        assertEquals(SampleCourses.seriesAnalysis(now).knowledgePoints.size, due.size)
        assertTrue(due.all { it.courseTitle == "无穷级数" && it.sourceModel == "qwen3.5-plus" })
    }

    @Test
    fun tabSwitchKeepsLearningSnapshot() {
        val viewModel = vm(seededLearningStore())
        val before = viewModel.ui.learningSnapshot.tasks.size
        assertTrue(before > 0)
        viewModel.selectTab(Tab.REVIEW)
        viewModel.selectTab(Tab.HISTORY)
        viewModel.selectTab(Tab.SETTINGS)
        viewModel.selectTab(Tab.HOME)
        assertEquals(before, viewModel.ui.learningSnapshot.tasks.size)
    }

    @Test
    fun deleteHistoryKeepsLearningStateAndProviderConfig() {
        val viewModel = vm(seededLearningStore())
        val tasksBefore = viewModel.ui.learningSnapshot.tasks.size
        viewModel.deleteHistory("hist_1")
        assertEquals(0, viewModel.ui.history.size)
        // Deleting a course's history must NOT wipe the cross-course review queue …
        assertEquals(tasksBefore, viewModel.ui.learningSnapshot.tasks.size)
        // … nor the active provider config (default profile from a missing config file).
        assertEquals(LearnerProfile.OFFICIAL_BLUELM, viewModel.activeConfigBundle().profile)
    }
}
