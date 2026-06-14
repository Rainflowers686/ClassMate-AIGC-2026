package com.classmate.app.state

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.exporting.ExportFormat
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.sample.SampleCourses
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportEntrypointTest {
    private val now = 1_700_000_000_000L

    @Test
    fun homeExportEntrySavesFullReportFromLatestHistory() {
        val exportStore = InMemoryExportStore()
        val viewModel = vm(exportStore = exportStore, history = listOf(record()))

        viewModel.exportLearningReport()

        assertEquals(1, exportStore.saved.size)
        assertTrue(exportStore.saved.single().content.contains("Knowledge timeline"))
        assertNotNull(viewModel.ui.lastExportReceipt)
        assertFalse(exportStore.saved.single().content.contains("Authorization"))
    }

    @Test
    fun historyExportEntrySupportsHtml() {
        val exportStore = InMemoryExportStore()
        val record = record()
        val viewModel = vm(exportStore = exportStore, history = listOf(record))

        viewModel.exportHistoryReport(record, ExportFormat.HTML)

        assertEquals(1, exportStore.saved.size)
        assertTrue(exportStore.saved.single().fileName.endsWith(".html"))
        assertTrue(exportStore.saved.single().content.contains("<!doctype html>"))
    }

    @Test
    fun reviewExportEntryDoesNotCrashWithoutCurrentResult() {
        val exportStore = InMemoryExportStore()
        val learningStore = InMemoryLearningStore { now }
        learningStore.addTasksFromAnalysis(
            result = SampleCourses.seriesAnalysis(now),
            courseTitle = "course",
            sourceProvider = "BLUELM",
            sourceProfile = "official_bluelm",
            sourceModel = "qwen3.5-plus",
        )
        val viewModel = vm(exportStore = exportStore, learningStore = learningStore)

        viewModel.exportReviewReport(ExportFormat.PLAIN_TEXT)

        assertEquals(1, exportStore.saved.size)
        assertTrue(exportStore.saved.single().fileName.endsWith(".txt"))
        assertTrue(exportStore.saved.single().content.contains("Review queue"))
    }

    private fun vm(
        exportStore: InMemoryExportStore,
        history: List<HistoryRecord> = emptyList(),
        learningStore: com.classmate.core.learning.LearningStore = InMemoryLearningStore { now },
    ): AppViewModel =
        AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-export").resolve("config.local.json").toFile()),
            historyStore = InMemoryHistoryStore(history),
            learningStore = learningStore,
            exportStore = exportStore,
        )

    private fun record(): HistoryRecord {
        val session = SampleCourses.seriesSession(now)
        val result = SampleCourses.seriesAnalysis(now)
        return HistoryRecord(
            id = "hist_1",
            title = "course",
            createdAtEpochMs = now,
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
}
