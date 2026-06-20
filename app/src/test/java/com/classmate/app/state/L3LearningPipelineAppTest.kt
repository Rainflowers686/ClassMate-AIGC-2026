package com.classmate.app.state

import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.l3.ClassroomAudioRecorder
import com.classmate.app.l3.L3AsrStatus
import com.classmate.app.l3.L3DemoSeeds
import com.classmate.app.l3.L3RecordingStatus
import com.classmate.app.l3.PdfPageStatus
import com.classmate.app.l3.PracticeQuestionMode
import com.classmate.app.l3.RecordingArtifactResult
import com.classmate.app.l3.TranslationProductStatus
import com.classmate.app.l3.TtsPlaybackStatus
import com.classmate.app.importing.OcrImportKind
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.learning.InMemoryLearningStore
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class L3LearningPipelineAppTest {
    private val now = 1_700_000_000_000L

    private fun vm(recorder: ClassroomAudioRecorder = FakeRecorder()) =
        AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-l3").resolve("config.local.json").toFile()),
            historyStore = InMemoryHistoryStore(),
            learningStore = InMemoryLearningStore { now },
            classroomAudioRecorder = recorder,
        )

    @Test
    fun textMaterialClosesL3LoopAndWrongAnswerUpdatesStats() {
        val viewModel = vm()
        viewModel.updateCourseTitle(L3DemoSeeds.lessonTitle)
        viewModel.updateCourseText(L3DemoSeeds.lessonText)

        assertTrue(viewModel.generateL3PipelineFromCurrentMaterial(now))
        assertNotNull(viewModel.ui.result)
        assertTrue(viewModel.ui.l3Pipeline.summary.isNotBlank())
        assertTrue(viewModel.ui.l3Pipeline.evidence.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.questions.size in 3..5)
        assertTrue(viewModel.ui.learningSnapshot.tasks.isNotEmpty())
        assertEquals(Screen.COURSE_DETAIL, viewModel.currentScreen)

        val question = viewModel.ui.result!!.quizQuestions.first()
        val wrong = question.options.first { !it.isCorrect }.id
        viewModel.answer(question, wrong)

        assertEquals(1, viewModel.ui.l3Pipeline.wrongBook.size)
        assertTrue(viewModel.ui.learningSnapshot.attempts.any { !it.isCorrect })
        assertTrue(viewModel.ui.l3Pipeline.masteryStats.any { it.state.name == "WEAK" })
    }

    @Test
    fun questionBankImportCreatesCourseQuizAndReviewQueue() {
        val viewModel = vm()
        viewModel.updateCourseTitle("题库课")
        viewModel.updateQuestionBankDraft(L3DemoSeeds.questionBankMarkdown)

        assertTrue(viewModel.importQuestionBankDraft(now))

        assertTrue(viewModel.ui.questionBankParseResult!!.accepted)
        assertNotNull(viewModel.ui.result)
        assertEquals(3, viewModel.ui.result!!.quizQuestions.size)
        assertEquals(3, viewModel.ui.l3Pipeline.questionBank!!.questions.size)
        assertTrue(viewModel.ui.learningSnapshot.tasks.isNotEmpty())
    }

    @Test
    fun inputSuperhubTextDocxXlsxPdfAndAudioStatusesAreReachable() {
        val viewModel = vm()

        assertTrue(viewModel.importSuperhubFile("课堂材料文本".toByteArray(), "lesson.txt", "text/plain", now))
        assertTrue(viewModel.ui.courseText.contains("课堂材料"))

        assertTrue(
            viewModel.importSuperhubFile(
                zip("word/document.xml" to "<w:document><w:t>DOCX 课堂知识点</w:t></w:document>"),
                "lesson.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                now + 1,
            ),
        )
        assertTrue(viewModel.ui.inputArtifacts.any { it.fileName == "lesson.docx" && it.status.name == "BEST_EFFORT" })

        viewModel.importSuperhubFile(
            zip(
                "xl/sharedStrings.xml" to "<sst><si><t>stem</t></si><si><t>a</t></si><si><t>b</t></si><si><t>c</t></si><si><t>d</t></si><si><t>answer</t></si><si><t>explanation</t></si><si><t>题干</t></si><si><t>甲</t></si><si><t>乙</t></si><si><t>丙</t></si><si><t>丁</t></si><si><t>A</t></si><si><t>解析</t></si></sst>",
                "xl/worksheets/sheet1.xml" to "<worksheet><sheetData><row><c t=\"s\"><v>0</v></c><c t=\"s\"><v>1</v></c><c t=\"s\"><v>2</v></c><c t=\"s\"><v>3</v></c><c t=\"s\"><v>4</v></c><c t=\"s\"><v>5</v></c><c t=\"s\"><v>6</v></c></row><row><c t=\"s\"><v>7</v></c><c t=\"s\"><v>8</v></c><c t=\"s\"><v>9</v></c><c t=\"s\"><v>10</v></c><c t=\"s\"><v>11</v></c><c t=\"s\"><v>12</v></c><c t=\"s\"><v>13</v></c></row></sheetData></worksheet>",
            ),
            "bank.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            now + 2,
        )
        assertTrue(viewModel.ui.questionBankParseResult?.accepted == true)

        assertFalse(viewModel.importSuperhubFile("%PDF".toByteArray(), "handout.pdf", "application/pdf", now + 3))
        assertTrue(viewModel.ui.inputArtifacts.any { it.fileName == "handout.pdf" && it.status.name == "PARSER_PENDING" })
        assertTrue(viewModel.ui.importReports.any { it.sourceType.name == "PDF" && it.fallbackUsed })
        assertTrue(viewModel.ui.pdfDocuments.any { it.artifactId == viewModel.ui.pdfPages.last().artifactId && it.parserStatus == "PDF_TEXT_PARSER_PENDING" })
        assertEquals(PdfPageStatus.PAGE_OCR_SEAM_READY, viewModel.ui.pdfPages.last().status)
        assertTrue(viewModel.addManualPdfPageText(viewModel.ui.pdfPages.last().artifactId, viewModel.ui.pdfPages.last().pageNumber, "PDF page manual learning text", now + 30))
        assertEquals(PdfPageStatus.MANUAL_PAGE_TEXT_READY, viewModel.ui.pdfPages.last().status)
        assertTrue(viewModel.ui.courseText.contains("PDF page manual learning text"))

        assertFalse(viewModel.importSuperhubFile(byteArrayOf(1, 2), "lecture.m4a", "audio/mp4", now + 4))
        assertEquals(L3AsrStatus.ASR_NOT_CONFIGURED, viewModel.ui.asrLongJobs.last().status)
    }

    @Test
    fun imageOcrManualTextEntersEvidencePipeline() {
        val viewModel = vm()
        viewModel.updateCourseTitle("板书课")

        assertTrue(viewModel.addOcrImport(OcrImportKind.BLACKBOARD_PHOTO, "板书照片", "板书写着磁通量变化会产生感应电动势。", now = now))
        assertTrue(viewModel.generateL3PipelineFromCurrentMaterial(now + 1))

        assertTrue(viewModel.ui.l3Pipeline.evidence.any { it.sourceType.name == "OCR_IMAGE" && it.text.contains("磁通量") })
        assertTrue(viewModel.ui.l3Pipeline.stepLogs.any { it.step == "OCR" })
    }

    @Test
    fun realPracticeAnswerLinksWrongBookMasteryReviewQueueAndEvidence() {
        val viewModel = vm()
        viewModel.updateCourseTitle(L3DemoSeeds.lessonTitle)
        viewModel.updateCourseText(L3DemoSeeds.lessonText)
        assertTrue(viewModel.generateL3PipelineFromCurrentMaterial(now))
        viewModel.startPractice(com.classmate.core.practice.PracticeMode.QUICK_REVIEW)
        val item = viewModel.currentPracticeItem()!!
        val wrong = item.options.first { !it.correct }.id

        viewModel.selectPracticeAnswer(wrong)
        assertTrue(viewModel.submitPracticeAnswer(now + 100))

        assertEquals(PracticeQuestionMode.REAL_QUIZ, viewModel.ui.practiceQuestionMode)
        assertEquals(1, viewModel.ui.l3Pipeline.wrongBook.size)
        assertTrue(viewModel.ui.l3Pipeline.masteryStats.any { it.state.name == "WEAK" })
        assertTrue(viewModel.ui.l3Pipeline.reviewQueue.any { it.masteryState.name == "WEAK" })
        val wrongRecord = viewModel.ui.l3Pipeline.wrongBook.single()
        assertTrue(wrongRecord.evidenceIds.isNotEmpty())
        assertNotNull(viewModel.ui.l3Pipeline.evidence.firstOrNull { it.id in wrongRecord.evidenceIds })
    }

    @Test
    fun officialToolAndSupportSeamsAreReachableWithoutNetwork() {
        val viewModel = vm()
        viewModel.updateCourseTitle(L3DemoSeeds.lessonTitle)
        viewModel.updateCourseText(L3DemoSeeds.lessonText)
        assertTrue(viewModel.generateL3PipelineFromCurrentMaterial(now))

        val mainSteps = viewModel.ui.l3Pipeline.stepLogs.map { it.step }.toSet()
        assertTrue(mainSteps.containsAll(listOf("OCR", "QUERY_REWRITE", "EMBEDDING", "TEXT_SIMILARITY")))
        val supportSteps = viewModel.ui.l3Pipeline.supportSeams.map { it.step }.toSet()
        assertTrue(supportSteps.containsAll(listOf("TRANSLATION", "TTS", "FUNCTION_CALLING", "ASR_LONG")))
        assertTrue(viewModel.ui.l3Pipeline.embeddingRecords.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.semanticIndexChunks.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.semanticIndexRecords.isNotEmpty())
        assertTrue(viewModel.ui.l3SemanticSearchResults.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.similarityMatches.isNotEmpty())
        assertNotNull(viewModel.ui.l3ToolOrchestrationPlan)
        assertTrue(viewModel.ui.l3ToolOrchestrationPlan!!.plannedTools.contains("REVIEW_UPDATE"))
        assertTrue(viewModel.ui.l3ToolStepRecords.any { it.toolName == "REVIEW_UPDATE" })
        assertTrue(viewModel.ui.l3Pipeline.diagnostics.any { it.capability == "TTS" && it.status == "LOCAL_TTS_AVAILABLE" })
        assertTrue(viewModel.ui.l3Pipeline.knowledgeGraphEdges.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.similarQuestionRecommendations.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.officialToolSeams.map { it.capability }.containsAll(listOf("TRANSLATION", "TTS", "FUNCTION_CALLING", "ASR_LONG", "EDGE_MODEL")))
        assertFalse(viewModel.prepareL3Translation())
        assertTrue(viewModel.ui.l3TranslationSeams.last().status.name == "NOT_CONFIGURED")
        assertEquals(TranslationProductStatus.OFFICIAL_TRANSLATION_NOT_CONFIGURED, viewModel.ui.l3TranslationResults.last().status)
        assertTrue(viewModel.prepareL3ListenReview())
        assertEquals("OFFICIAL_TTS_NOT_CONFIGURED", viewModel.ui.l3TtsReviewSeam!!.status.name)
        assertEquals(TtsPlaybackStatus.PLAYING, viewModel.ui.l3TtsPlaybackState!!.status)
        viewModel.refreshL3ToolOrchestration(now + 99)
        assertEquals("LOCAL_ORCHESTRATOR", viewModel.ui.l3ToolOrchestrationPlan!!.status.name)
        assertTrue(viewModel.ui.l3ToolStepRecords.isNotEmpty())
        assertEquals("LOCAL_RULE_FALLBACK", viewModel.ui.l3EdgeStudySeam!!.status.name)
    }

    @Test
    fun classroomRecordingCreatesArtifactRecordWithoutClaimingAsrSuccess() {
        val viewModel = vm(FakeRecorder())

        viewModel.startClassroomRecording(now)
        assertEquals(L3RecordingStatus.RECORDING, viewModel.ui.currentRecording!!.status)
        assertEquals("recording_$now.m4a", viewModel.ui.currentRecording!!.artifactFileName)

        viewModel.stopClassroomRecording(now + 5_000)

        assertEquals(null, viewModel.ui.currentRecording)
        assertEquals(1, viewModel.ui.recordingRecords.size)
        assertEquals(L3RecordingStatus.SAVED, viewModel.ui.recordingRecords.single().status)
        assertEquals(L3AsrStatus.ASR_NOT_CONFIGURED, viewModel.ui.recordingRecords.single().asrStatus)
        assertFalse(viewModel.ui.audioCaptureMessage.orEmpty().contains("ASR 成功"))
    }

    @Test
    fun asrTranscriptReadyFillsPipelineAndCreatesTimelineEvidence() {
        val viewModel = vm()

        assertFalse(viewModel.importSuperhubFile(byteArrayOf(1, 2), "lecture.m4a", "audio/mp4", now))
        val job = viewModel.ui.asrLongJobs.last()
        assertTrue(viewModel.applyAsrLongTranscript(job.id, "第一段讲电磁感应。\n第二段讲磁通量变化。", now + 10))

        assertEquals(L3AsrStatus.TRANSCRIPT_READY, viewModel.ui.asrLongStatus)
        assertTrue(viewModel.ui.l3Pipeline.transcriptSegments.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.evidence.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.reviewQueue.isNotEmpty())
    }

    @Test
    fun manualTranscriptFallbackIsExplicitAndCanEnterPipeline() {
        val viewModel = vm()
        viewModel.updateCourseTitle("手动转写课")
        viewModel.updateTranscriptPaste("00:00 第一段讲法拉第定律。\n00:30 第二段讲磁通量变化。")

        assertTrue(viewModel.createManualTranscriptDraftFromPaste(now))
        viewModel.saveTranscriptToTray()

        assertEquals(L3AsrStatus.MANUAL_TRANSCRIPT_FALLBACK, viewModel.ui.asrLongStatus)
        assertTrue(viewModel.generateL3PipelineFromCurrentMaterial(now + 1))
        assertTrue(viewModel.ui.l3Pipeline.transcriptSegments.isNotEmpty())
        assertTrue(viewModel.ui.l3Pipeline.evidence.any { it.sourceType.name == "MANUAL_TRANSCRIPT" })
        assertTrue(viewModel.ui.l3Pipeline.transcriptSegments.all { it.fallbackGenerated })
    }

    @Test
    fun randomQuizUsesRealQuizMode() {
        val viewModel = vm()
        viewModel.updateCourseTitle(L3DemoSeeds.lessonTitle)
        viewModel.updateCourseText(L3DemoSeeds.lessonText)
        assertTrue(viewModel.generateL3PipelineFromCurrentMaterial(now))

        viewModel.startRandomQuiz(questionCount = 2, now = now + 7)

        assertEquals(PracticeQuestionMode.REAL_QUIZ, viewModel.ui.practiceQuestionMode)
        assertEquals(2, viewModel.ui.practiceSession!!.items.size)
        assertTrue(viewModel.ui.practiceSession!!.items.all { it.options.isNotEmpty() })
    }

    private class FakeRecorder : ClassroomAudioRecorder {
        override fun start(sessionId: String): RecordingArtifactResult =
            RecordingArtifactResult(success = true, fileName = "$sessionId.m4a", safeMessage = "fake recording started")

        override fun stop(): RecordingArtifactResult =
            RecordingArtifactResult(success = true, fileName = null, safeMessage = "fake recording saved")
    }

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
