package com.classmate.app.data

import com.classmate.app.l3.ExamResultReport
import com.classmate.app.l3.EvidenceAsset
import com.classmate.app.l3.EvidenceAssetType
import com.classmate.app.l3.L3DemoSeeds
import com.classmate.app.l3.L3LearningPipeline
import com.classmate.app.l3.L3MasteryState
import com.classmate.app.l3.L3PipelineSnapshot
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
        val asset = EvidenceAsset(
            id = "asset_doc_1",
            type = EvidenceAssetType.DOCUMENT,
            sourceType = L3SourceType.DOCUMENT,
            text = base.evidence.first().text,
            sourceLabel = "lesson doc",
            fileName = "lesson.md",
            fileExt = "md",
            mimeType = "text/markdown",
            pageHint = "section 1",
            snippet = base.evidence.first().text.take(120),
            createdAt = now,
        )
        val imageAsset = EvidenceAsset(
            id = "asset_image_1",
            type = EvidenceAssetType.OCR_IMAGE,
            sourceType = L3SourceType.OCR_IMAGE,
            text = "OCR board text",
            sourceLabel = "board image",
            fileName = "board.jpg",
            fileExt = "jpg",
            mimeType = "image/jpeg",
            imageRef = "app-private/board.jpg",
            thumbnailRef = "app-private/board_thumb.jpg",
            snippet = "OCR board text",
            createdAt = now,
        )
        val snapshot = wrong.copy(
            examReports = listOf(examReport),
            evidenceAssets = listOf(asset),
            evidence = wrong.evidence.mapIndexed { index, evidence ->
                if (index == 0) {
                    evidence.copy(
                        assetId = asset.id,
                        sourceLabel = asset.sourceLabel,
                        fileName = asset.fileName,
                        fileExt = asset.fileExt,
                        mimeType = asset.mimeType,
                        pageHint = asset.pageHint,
                    )
                } else {
                    evidence
                }
            },
        )
        val file = Files.createTempDirectory("cm-l3-store").resolve("classmate_l3_store.json").toFile()
        val withAssets = snapshot.copy(evidenceAssets = snapshot.evidenceAssets + imageAsset)

        L3PersistenceRepository(file).saveSnapshot(withAssets)
        val reloaded = L3PersistenceRepository(file).loadSnapshot()

        assertEquals(withAssets.lessonSource!!.id, reloaded.lessonSource!!.id)
        assertEquals(withAssets.questions.first().id, reloaded.questions.first().id)
        assertEquals(1, reloaded.wrongBook.size)
        assertTrue(reloaded.wrongBook.single().mistakeReason.contains("错因分析"))
        assertTrue(reloaded.wrongBook.single().remediationHint.contains("补救建议"))
        assertTrue(reloaded.wrongBook.single().relatedKnowledgePointIds.contains(base.questions.first().knowledgePointId))
        assertEquals(L3MasteryState.WEAK, reloaded.reviewQueue.first { it.knowledgePointId == base.questions.first().knowledgePointId }.masteryState)
        assertTrue(reloaded.reviewQueue.first { it.knowledgePointId == base.questions.first().knowledgePointId }.arrangementReason.isNotBlank())
        assertEquals(base.questions.first().evidenceIds.first(), reloaded.reviewQueue.first { it.knowledgePointId == base.questions.first().knowledgePointId }.evidenceId)
        assertTrue(reloaded.reviewQueue.first { it.knowledgePointId == base.questions.first().knowledgePointId }.recommendedActions.isNotEmpty())
        assertTrue(reloaded.masteryHistory.isNotEmpty())
        assertEquals("exam_report_1", reloaded.examReports.single().id)
        assertEquals("# Exam Report", reloaded.examReports.single().markdownReport)
        assertEquals("asset_doc_1", reloaded.evidenceAssets.first { it.id == "asset_doc_1" }.id)
        assertEquals("app-private/board.jpg", reloaded.evidenceAssets.first { it.id == "asset_image_1" }.imageRef)
        assertEquals("app-private/board_thumb.jpg", reloaded.evidenceAssets.first { it.id == "asset_image_1" }.thumbnailRef)
        assertEquals("OCR board text", reloaded.evidenceAssets.first { it.id == "asset_image_1" }.snippet)
        assertEquals("lesson.md", reloaded.evidence.first().fileName)
        assertEquals("asset_doc_1", reloaded.evidence.first().assetId)
    }

    @Test
    fun corruptedStoreFallsBackToEmptySnapshot() {
        val file = Files.createTempDirectory("cm-l3-store-bad").resolve("classmate_l3_store.json").toFile()
        file.writeText("{bad json", Charsets.UTF_8)

        val reloaded = L3PersistenceRepository(file).loadSnapshot()

        assertEquals(null, reloaded.lessonSource)
        assertTrue(reloaded.wrongBook.isEmpty())
    }

    @Test
    fun clearIfMatchesRemovesOnlyMatchingSnapshot() {
        val file = Files.createTempDirectory("cm-l3-store-clear").resolve("classmate_l3_store.json").toFile()
        val repo = L3PersistenceRepository(file)
        val snapshot = L3PipelineSnapshot(
            lessonSource = com.classmate.app.l3.LessonSource(
                id = "lesson_physics",
                title = "Physics",
                type = L3SourceType.TEXT,
                createdAt = now,
                rawText = "Physics lesson",
                status = "READY",
            ),
        )
        repo.saveSnapshot(snapshot)

        assertTrue(repo.clearIfMatches(sessionIds = setOf("lesson_math"), courseTitles = setOf("Math")))
        assertEquals("lesson_physics", repo.loadSnapshot().lessonSource!!.id)

        assertTrue(repo.clearIfMatches(sessionIds = setOf("lesson_physics"), courseTitles = emptySet()))
        assertEquals(null, repo.loadSnapshot().lessonSource)
    }
}
