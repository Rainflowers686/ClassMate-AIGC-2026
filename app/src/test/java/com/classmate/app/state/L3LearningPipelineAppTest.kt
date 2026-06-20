package com.classmate.app.state

import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.l3.ClassroomAudioRecorder
import com.classmate.app.l3.L3AsrStatus
import com.classmate.app.l3.L3DemoSeeds
import com.classmate.app.l3.L3RecordingStatus
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

    private class FakeRecorder : ClassroomAudioRecorder {
        override fun start(sessionId: String): RecordingArtifactResult =
            RecordingArtifactResult(success = true, fileName = "$sessionId.m4a", safeMessage = "fake recording started")

        override fun stop(): RecordingArtifactResult =
            RecordingArtifactResult(success = true, fileName = null, safeMessage = "fake recording saved")
    }
}
