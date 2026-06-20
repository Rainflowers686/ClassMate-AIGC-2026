package com.classmate.app.data

import com.classmate.app.l3.ExamResultReport
import com.classmate.app.l3.L3DemoSeeds
import com.classmate.app.l3.L3LearningPipeline
import com.classmate.app.l3.L3MasteryState
import com.classmate.app.l3.L3SourceType
import com.classmate.app.platform.OfficialProviderConfigSummary
import com.classmate.app.platform.ProviderConfigSummary
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class L3PersistenceRepositoryTest {
    private val now = 1_700_000_000_000L

    @Test
    fun l3SnapshotReloadKeepsWrongBookReviewMasteryHistoryAndExamReport() {
        val providerSummary = ProviderConfigSummary.defaults().copy(
            officialProviders = OfficialProviderConfigSummary(
                ocrConfigured = true,
                queryRewriteConfigured = true,
                textSimilarityConfigured = true,
                embeddingConfigured = true,
            ),
        )
        val pipeline = L3LearningPipeline()
        val base = pipeline.buildFromText(L3DemoSeeds.lessonTitle, L3DemoSeeds.lessonText, L3SourceType.TEXT, providerSummary, now)
        val wrong = pipeline.submitAnswer(base, base.questions.first().id, "B", now + 1)
        val examReport = ExamResultReport(
            id = "exam_report_1",
            examSessionId = "exam_1",
            score = 50,
            correctCount = 1,
            wrongCount = 1,
            elapsedMs = 30_000,
            weakKnowledgePointIds = listOf(base.questions.first().knowledgePointId),
            wrongQuestionIds = listOf(base.questions.first().id),
            evidenceIds = base.questions.first().evidenceIds,
            markdownReport = "# Exam Report",
            generatedAt = now + 2,
        )
        val snapshot = wrong.copy(examReports = listOf(examReport))
        val file = Files.createTempDirectory("cm-l3-store").resolve("classmate_l3_store.json").toFile()

        L3PersistenceRepository(file).saveSnapshot(snapshot)
        val reloaded = L3PersistenceRepository(file).loadSnapshot()

        assertEquals(snapshot.lessonSource!!.id, reloaded.lessonSource!!.id)
        assertEquals(snapshot.questions.first().id, reloaded.questions.first().id)
        assertEquals(1, reloaded.wrongBook.size)
        assertEquals(L3MasteryState.WEAK, reloaded.reviewQueue.first { it.knowledgePointId == base.questions.first().knowledgePointId }.masteryState)
        assertTrue(reloaded.masteryHistory.isNotEmpty())
        assertEquals("exam_report_1", reloaded.examReports.single().id)
        assertEquals("# Exam Report", reloaded.examReports.single().markdownReport)
    }

    @Test
    fun corruptedStoreFallsBackToEmptySnapshot() {
        val file = Files.createTempDirectory("cm-l3-store-bad").resolve("classmate_l3_store.json").toFile()
        file.writeText("{bad json", Charsets.UTF_8)

        val reloaded = L3PersistenceRepository(file).loadSnapshot()

        assertEquals(null, reloaded.lessonSource)
        assertTrue(reloaded.wrongBook.isEmpty())
    }
}
