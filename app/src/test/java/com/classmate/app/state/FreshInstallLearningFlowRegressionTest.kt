package com.classmate.app.state

import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.OcrImportFileMeta
import com.classmate.app.importing.OcrImportKind
import com.classmate.app.importing.OcrImportStatus
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.practice.PracticeMode
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshInstallLearningFlowRegressionTest {
    private val now = 1_700_000_000_000L
    private val freshPhysicsText = "同学们注意，重点来了，这个地方可能考。下面看牛顿第二定律，物体的加速度与所受合外力成正比，与质量成反比，公式 F=ma。大家记一下，作业截图上传。"
    private val noise = listOf("同学们注意", "重点来了", "大家记一下", "作业截图上传", "下面看")

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-fresh-flow").resolve("fresh_config.json").toFile()),
        historyStore = InMemoryHistoryStore(),
        learningStore = InMemoryLearningStore { now },
        exportStore = InMemoryExportStore(),
    )

    @Test
    fun freshOcrCourseKeepsRawTextButBuildsCleanSubjectLearningFlow() {
        val viewModel = vm()
        viewModel.updateCourseTitle("高中物理")
        val batchId = viewModel.beginImageOcrBatch("图片学习输入", total = 1, now = now)
        viewModel.applyImageOcrBatchItem(
            OcrImportDraft(
                id = "${batchId}_1",
                kind = OcrImportKind.SLIDE_IMAGE,
                fileMeta = OcrImportFileMeta(
                    fileName = "physics_original.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 200_000,
                    displayLabel = "真机课件图",
                    pageIndex = 1,
                ),
                pastedText = freshPhysicsText,
                rawOcrText = freshPhysicsText,
                normalizedOcrText = freshPhysicsText,
                status = OcrImportStatus.OK,
                batchId = batchId,
                pageIndex = 1,
                blockIndex = 1,
                createdAt = now,
                updatedAt = now,
            ),
            total = 1,
        )

        assertTrue("OCR draft should preserve raw classroom prompt text", viewModel.ui.imageDraftRawText.contains("同学们注意"))
        assertTrue("OCR editable text should not be shortened by subject filtering", viewModel.ui.imageDraftText.length >= freshPhysicsText.length * 9 / 10)
        assertTrue(viewModel.confirmImageOcrBatch(now + 10))

        val l3 = viewModel.ui.l3Pipeline
        val knowledgeTitles = l3.knowledgePoints.map { it.title }
        assertTrue(
            "fresh flow should extract subject knowledge, got $knowledgeTitles",
            knowledgeTitles.any { it.contains("牛顿第二定律") || it.contains("加速度") || it.contains("合外力") || it.contains("质量") || it.contains("F=ma") },
        )
        val userFacingKnowledge = listOf(
            l3.summary,
            knowledgeTitles.joinToString("\n"),
            l3.keyTakeaways.joinToString("\n"),
            l3.reviewFocus.joinToString("\n"),
            l3.actionItems.joinToString("\n"),
            l3.reviewQueue.joinToString("\n") { it.arrangementReason + it.recommendedActions.joinToString() },
            l3.relatedKnowledgeSummaries.joinToString("\n") {
                it.sourceKnowledgePointTitle + it.relatedKnowledgePointTitles.joinToString() + it.summary
            },
        ).joinToString("\n")
        noise.forEach { assertFalse("fresh user-facing learning flow leaked prompt word: $it", userFacingKnowledge.contains(it)) }
        assertTrue("evidence may keep original OCR sentence for traceability", l3.evidence.any { it.text.contains("同学们注意") })

        assertEquals("material submission should auto-prepare quizzes", PracticePreparationStatus.READY, viewModel.ui.practicePreparationStatus)
        assertNotNull("auto-prepared quiz session should be stored before the user taps start practice", viewModel.ui.preparedPracticeSession)
        assertTrue(viewModel.ui.practicePreparationMessage.contains("微测已准备"))

        viewModel.startPractice(PracticeMode.QUICK_REVIEW)
        assertEquals(Screen.PRACTICE, viewModel.currentScreen)
        val session = viewModel.ui.practiceSession
        assertNotNull("fresh timeline/course practice should not be empty", session)
        assertTrue(session!!.items.isNotEmpty())
        assertTrue("start practice should reuse prepared quiz instead of waiting for click-time generation", session.routeReason.contains("prepared"))
        val practiceText = session.items.joinToString("\n") { item ->
            item.knowledgePointTitle + "\n" + item.question + "\n" + item.answer + "\n" +
                item.options.joinToString("\n") { it.text }
        }
        noise.forEach { assertFalse("fresh practice leaked prompt word: $it", practiceText.contains(it)) }
        assertTrue(session.items.all { it.knowledgePointTitle.isNotBlank() && it.evidenceQuote?.isNotBlank() == true })
        val answerableItems = session.items.filter { it.options.size >= 2 && it.correctOptionIds.isNotEmpty() }
        assertFalse("fresh micro-quiz should not be all true/false", answerableItems.size > 1 && answerableItems.all { it.options.size == 2 })
        assertFalse("fresh micro-quiz should not put every answer at A", answerableItems.size > 1 && answerableItems.all { it.correctOptionIds == listOf("A") })

        repeat(session.items.size) { index ->
            val item = viewModel.currentPracticeItem()!!
            viewModel.selectPracticeAnswer(item.correctOptionIds.first())
            assertTrue(viewModel.submitPracticeAnswer(now + 100 + index))
            viewModel.nextPracticeQuestion()
        }

        assertEquals("completion should return to review plan", Screen.REVIEW, viewModel.currentScreen)
        assertNotNull(viewModel.ui.practiceResult)
        assertTrue(viewModel.isPracticeComplete())
    }
}
