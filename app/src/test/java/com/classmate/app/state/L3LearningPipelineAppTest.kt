package com.classmate.app.state

import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.l3.ClassroomAudioRecorder
import com.classmate.app.l3.L3AsrStatus
import com.classmate.app.l3.L3DemoSeeds
import com.classmate.app.l3.L3RecordingStatus
import com.classmate.app.l3.PracticeQuestionMode
import com.classmate.app.l3.RecordingArtifactResult
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
        assertTrue(viewModel.ui.l3Pipeline.similarityMatches.isNotEmpty())
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
    }

    private class FakeRecorder : ClassroomAudioRecorder {
        override fun start(sessionId: String): RecordingArtifactResult =
            RecordingArtifactResult(success = true, fileName = "$sessionId.m4a", safeMessage = "fake recording started")

        override fun stop(): RecordingArtifactResult =
            RecordingArtifactResult(success = true, fileName = null, safeMessage = "fake recording saved")
    }
}
