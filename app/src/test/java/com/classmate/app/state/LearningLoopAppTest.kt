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
            id = "hist_1",
            title = "无穷级数",
            createdAtEpochMs = now,
            providerName = "BLUELM",
            profileLabel = "官方 BlueLM",
            model = "qwen3.5-plus",
            knowledgePointCount = result.knowledgePoints.size,
            quizCount = result.quizQuestions.size,
            fallbackUsed = false,
            validationStatus = "PASS",
            session = session,
            result = result,
        )
    }

    private fun vm(learningStore: LearningStore, history: List<HistoryRecord> = listOf(recentRecord())) =
        AppViewModel(
            configRepository = ConfigRepository(missingFile()),
            historyStore = InMemoryHistoryStore(history),
            learningStore = learningStore,
        )

    @Test
    fun markingReviewDoneAndWeakActuallyChangesState() {
        // Real-device #16: marking a review task 完成 / 太难 must change state, not just toast — the loop closes.
        val store = seededLearningStore()
        val viewModel = vm(store)
        val dueBefore = store.listDueTasks()
        assertTrue("seed has due tasks", dueBefore.isNotEmpty())

        val doneId = dueBefore.first().taskId
        viewModel.reviewMarkDone(doneId)
        assertTrue("completed task leaves the due list", store.listDueTasks().none { it.taskId == doneId })

        store.listDueTasks().firstOrNull()?.let { weakTask ->
            viewModel.reviewTaskFeedback(weakTask.taskId, com.classmate.core.learning.ReviewEventType.TOO_HARD)
            val after = store.listDueTasks().firstOrNull { it.taskId == weakTask.taskId }
            // The weakness counter was recorded (or the task was re-prioritised out of today's due list).
            assertTrue(after == null || after.counters.tooHard > 0)
        }
    }

    @Test
    fun homeSummaryShowsDueCountAndRecentCourse() {
        val viewModel = vm(seededLearningStore())

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
    fun deleteHistoryRemovesCourseLearningStateAndKeepsProviderConfig() {
        val viewModel = vm(seededLearningStore())
        assertTrue(viewModel.ui.learningSnapshot.tasks.isNotEmpty())

        viewModel.deleteHistory("hist_1")

        assertEquals(0, viewModel.ui.history.size)
        assertEquals(0, viewModel.ui.learningSnapshot.tasks.size)
        assertEquals(LearnerProfile.OFFICIAL_BLUELM, viewModel.activeConfigBundle().profile)
    }
}
