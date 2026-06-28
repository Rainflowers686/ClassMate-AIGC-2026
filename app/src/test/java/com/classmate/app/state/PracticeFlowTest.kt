package com.classmate.app.state

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.exporting.ExportFileFormat
import com.classmate.app.l3.PracticeQuestionMode
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.audio.CourseEssenceAudioStatus
import com.classmate.core.practice.PracticeMode
import com.classmate.core.practice.PracticeOutcome
import com.classmate.core.practice.isAnswerableQuiz
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
        assertEquals(PracticeQuestionMode.REAL_QUIZ, viewModel.ui.practiceQuestionMode)
        assertTrue(viewModel.ui.practiceSession!!.items.all { it.options.isNotEmpty() })
        assertEquals(Screen.PRACTICE, viewModel.currentScreen)
    }

    @Test
    fun quizFeedbackEnhancementGuardsWhenNoAttempt() {
        // P0-2 guard: with no practice attempt, the AI feedback entry must not run — it guides instead.
        val viewModel = vm()
        viewModel.generateQuizFeedbackEnhancement()
        assertFalse(viewModel.ui.quizFeedbackEnhancement.running)
        assertNotNull(viewModel.ui.toast)
    }

    @Test
    fun studyPackEnhancementGuardsWhenNoCourse() {
        // P0-1 guard: with no analyzed course loaded, the AI study-material entry must not run.
        val viewModel = vm()
        viewModel.generateStudyPackEnhancement()
        assertFalse(viewModel.ui.studyPackEnhancement.running)
        assertNotNull(viewModel.ui.toast)
    }

    @Test
    fun weaknessRemediationGuardsWhenNoWeakPoint() {
        // P0-2 guard: no weak knowledge point -> AI remediation entry guides instead of running.
        val viewModel = vm()
        viewModel.generateWeaknessRemediation()
        assertFalse(viewModel.ui.weaknessEnhancement.running)
        assertNotNull(viewModel.ui.toast)
    }

    @Test
    fun aiStudyMaterialExportGuardsWithoutAResult() {
        // P0-3 guard: exporting the AI material before generating it returns null and guides the user.
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())
        assertNull(viewModel.buildAiStudyMaterialArtifact(ExportFileFormat.MARKDOWN))
        assertNotNull(viewModel.ui.toast)
    }

    @Test
    fun evidenceExplanationGuardsWithoutSelectedEvidence() {
        // P0-1 guard: no open evidence -> the explanation entry guides instead of running.
        val viewModel = vm()
        viewModel.generateEvidenceExplanation()
        assertFalse(viewModel.ui.evidenceEnhancement.running)
        assertNotNull(viewModel.ui.toast)
    }

    @Test
    fun randomQuizOnlyContainsAnswerableQuestions() {
        // P0-3: every question that enters the random quiz must have a resolved correct answer.
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.startRandomQuiz(now = now)

        val session = viewModel.ui.practiceSession
        assertNotNull(session)
        assertTrue("random quiz produced questions", session!!.items.isNotEmpty())
        assertTrue(
            "every random-quiz question must have a correct answer",
            session.items.all { it.isAnswerableQuiz() && it.correctOptionIds.isNotEmpty() },
        )
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

        repeat(itemCount) { index ->
            val item = viewModel.currentPracticeItem()!!
            viewModel.selectPracticeAnswer(item.correctOptionIds.first())
            assertTrue(viewModel.submitPracticeAnswer(now + index))
            viewModel.nextPracticeQuestion()
        }

        assertNotNull(viewModel.ui.practiceResult)
        assertEquals(itemCount, viewModel.ui.practiceResult!!.correctCount)
        assertEquals(1, viewModel.ui.learningSnapshot.practiceHistory.size)
        assertTrue(viewModel.isPracticeComplete())
        assertTrue(viewModel.ui.practiceAttempts.all { it.feedback != null })
    }

    @Test
    fun realQuizWrongAnswerWritesBackBeforeSummary() {
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.startPractice(PracticeMode.QUICK_REVIEW)
        val item = viewModel.currentPracticeItem()!!
        val wrong = item.options.first { !it.correct }.id

        viewModel.selectPracticeAnswer(wrong)
        assertTrue(viewModel.submitPracticeAnswer(now))

        assertEquals(PracticeQuestionMode.REAL_QUIZ, viewModel.ui.practiceQuestionMode)
        assertTrue(viewModel.ui.practiceSubmittedAnswers[item.id]!!.correct.not())
        assertTrue(viewModel.ui.learningSnapshot.attempts.any { !it.isCorrect })
    }

    @Test
    fun selfAssessmentModeKeepsSelfReportedButtonsSeparateFromPracticeDefault() {
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.startSelfAssessment(PracticeMode.EVIDENCE_RECALL)

        assertEquals(PracticeQuestionMode.SELF_ASSESSMENT, viewModel.ui.practiceQuestionMode)
        viewModel.answerPractice(PracticeOutcome.WRONG)
        assertEquals(1, viewModel.ui.practiceAttempts.size)
    }

    @Test
    fun examSessionStartSubmitAndScores() {
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.startExam()
        val itemCount = viewModel.ui.practiceSession!!.items.size

        repeat(itemCount) { index ->
            val item = viewModel.currentPracticeItem()!!
            viewModel.selectPracticeAnswer(item.correctOptionIds.first())
            assertTrue(viewModel.submitPracticeAnswer(now + index))
            viewModel.nextPracticeQuestion()
        }

        assertEquals(PracticeQuestionMode.EXAM, viewModel.ui.practiceQuestionMode)
        assertNotNull(viewModel.ui.examSession)
        assertEquals("SUBMITTED", viewModel.ui.examSession!!.status.name)
        assertEquals(100, viewModel.ui.examSession!!.score)
    }

    @Test
    fun courseEssenceAudioScriptFallsBackToScriptOnlyWhenTtsIsNotConfigured() {
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())

        viewModel.generateCourseEssenceAudioScript()

        assertNotNull(viewModel.ui.courseEssenceScript)
        assertEquals(CourseEssenceAudioStatus.SCRIPT_ONLY_CONFIG_MISSING, viewModel.ui.courseEssenceAudioResult!!.status)
        assertTrue(viewModel.ui.courseEssenceScript!!.toPlainText().contains("ClassMate course essence review"))
    }

    @Test
    fun refinedExportDraftShowsAiProcessingBeforeFormatGeneration() {
        val viewModel = vm()
        viewModel.openHistory(viewModel.ui.history.first())

        assertTrue(viewModel.prepareRefinedExportDraft())
        val artifact = viewModel.buildCurrentReportArtifact(ExportFileFormat.DOCX)

        assertTrue(viewModel.ui.exportDraftReady)
        assertNotNull(artifact)
        assertEquals(ExportFileFormat.DOCX, artifact!!.format)
        assertNotNull(viewModel.ui.exportDraftMessage)
        assertTrue(viewModel.ui.exportDraftSource!!.isNotBlank())
        assertTrue(viewModel.ui.aiProcessing.visible)
        assertTrue(viewModel.ui.aiProcessing.title.contains("提炼课堂精华"))
        assertTrue(viewModel.ui.aiProcessing.steps.contains("生成学习报告草稿"))
        assertTrue(viewModel.ui.aiProcessing.steps.contains("等待选择导出格式"))
        assertTrue(viewModel.ui.aiProcessing.fallbackMessage!!.contains("HTML"))
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
        listOf("提交答案", "回忆复盘", "掌握度自评", "我答对了", "我答错了", "已掌握", "本轮结果").forEach { assertTrue("Practice screen missing $it", practice.contains(it)) }
        // Stage 6 guarantee still holds: no "需要例题" in user-visible review/practice copy.
        assertFalse(review.contains("需要例题"))
        assertFalse(practice.contains("需要例题"))
    }

    private fun source(path: String): String =
        listOf(File(path), File(path.removePrefix("app/"))).first { it.exists() }.readText()
}
