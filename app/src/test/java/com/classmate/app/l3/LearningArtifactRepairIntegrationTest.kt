package com.classmate.app.l3

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.Difficulty
import com.classmate.core.model.EvidenceSpan
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.ProviderKind
import com.classmate.core.model.QuestionType
import com.classmate.core.model.QuizOption
import com.classmate.core.model.QuizQuestion
import com.classmate.core.practice.PracticeMode
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningArtifactRepairIntegrationTest {
    private val now = 1_700_000_000_000L
    private val noisyPhysicsText = "同学们注意，重点来了，这个地方可能考。下面看牛顿第二定律，物体的加速度与所受合外力成正比，与质量成反比，公式 F=ma。大家记一下，作业截图上传。"
    private val noise = listOf("同学们注意", "重点来了", "大家记一下", "作业截图上传")

    @Test
    fun legacyDirtySnapshotIsRepairedBeforeUserSurfaces() {
        val old = legacyDirtySnapshot()
        val dirtyBefore = listOf(
            old.summary,
            old.reviewQueue.joinToString("\n") { it.arrangementReason },
            old.relatedKnowledgeSummaries.joinToString("\n") { it.sourceKnowledgePointTitle + it.summary },
            old.questions.joinToString("\n") { it.stem + it.options.joinToString() },
        ).joinToString("\n")
        assertTrue("fixture must reproduce dirty old device state", noise.any { dirtyBefore.contains(it) })

        val repaired = LearningArtifactRepairer.repair(old, ProviderConfigSummary.defaults(), now + 1)
        val userFacing = listOf(
            repaired.summary,
            repaired.knowledgePoints.joinToString("\n") { it.title + it.explanation },
            repaired.reviewQueue.joinToString("\n") { it.arrangementReason + it.recommendedActions.joinToString() },
            repaired.relatedKnowledgeSummaries.joinToString("\n") { it.sourceKnowledgePointTitle + it.relatedKnowledgePointTitles.joinToString() + it.summary },
            repaired.questions.joinToString("\n") { it.stem + it.options.joinToString() + it.explanation },
        ).joinToString("\n")

        noise.forEach { assertFalse("noise leaked after repair: $it", userFacing.contains(it)) }
        assertTrue(userFacing.contains("牛顿第二定律") || userFacing.contains("加速度") || userFacing.contains("F=ma"))
        assertTrue(repaired.knowledgePoints.isNotEmpty())
        assertTrue(repaired.reviewQueue.isNotEmpty())
        assertTrue(repaired.questions.isNotEmpty())
        assertTrue(repaired.questions.all { it.evidenceIds.isNotEmpty() && it.knowledgePointId.isNotBlank() })
        assertTrue(repaired.stepLogs.any { it.step == "LEARNING_ARTIFACT_REPAIR" && it.status == LearningArtifactRepairer.ARTIFACT_VERSION })
    }

    @Test
    fun oldHistoryOpenAndCoursePracticeUseRepairedArtifacts() {
        val record = legacyDirtyHistoryRecord()
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-old-repair").resolve("config.local.json").toFile()),
            historyStore = InMemoryHistoryStore(listOf(record)),
            learningStore = InMemoryLearningStore { now },
            exportStore = InMemoryExportStore(),
        )

        viewModel.openHistory(viewModel.ui.history.first())
        val courseText = viewModel.ui.l3Pipeline.knowledgePoints.joinToString("\n") { it.title + it.explanation } +
            viewModel.ui.l3Pipeline.reviewQueue.joinToString("\n") { it.arrangementReason } +
            viewModel.ui.l3Pipeline.relatedKnowledgeSummaries.joinToString("\n") { it.sourceKnowledgePointTitle + it.summary }
        noise.forEach { assertFalse("old course still leaked noise after open: $it", courseText.contains(it)) }
        assertTrue(viewModel.ui.result!!.knowledgePoints.any { it.title.contains("牛顿") || it.title.contains("加速度") || it.summary.contains("F=ma") })

        viewModel.startPractice(PracticeMode.QUICK_REVIEW)
        val debugPracticeState = "screen=${viewModel.currentScreen}, toast=${viewModel.ui.toast}, " +
            "l3Kp=${viewModel.ui.l3Pipeline.knowledgePoints.map { it.title }}, " +
            "l3Evidence=${viewModel.ui.l3Pipeline.evidence.map { it.id to it.text.take(40) }}, " +
            "l3Questions=${viewModel.ui.l3Pipeline.questions.map { it.stem to (it.correctAnswer + it.options.joinToString()) }}, " +
            "resultQuestions=${viewModel.ui.result?.quizQuestions?.size}"
        assertEquals(
            debugPracticeState,
            Screen.PRACTICE,
            viewModel.currentScreen,
        )
        val session = viewModel.ui.practiceSession
        assertNotNull(session)
        assertTrue("course-level practice should reuse repaired evidence-backed questions", session!!.items.isNotEmpty())
        val practiceText = session.items.joinToString("\n") { it.knowledgePointTitle + it.question + it.answer + it.options.joinToString() { option -> option.text } }
        noise.forEach { assertFalse("practice leaked legacy noise: $it", practiceText.contains(it)) }
        assertTrue(session.items.all { it.evidenceQuote?.isNotBlank() == true && it.knowledgePointTitle.isNotBlank() })
    }

    @Test
    fun completingPracticeReturnsToReviewPlan() {
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-old-complete").resolve("config.local.json").toFile()),
            historyStore = InMemoryHistoryStore(listOf(legacyDirtyHistoryRecord())),
            learningStore = InMemoryLearningStore { now },
            exportStore = InMemoryExportStore(),
        )
        viewModel.openHistory(viewModel.ui.history.first())
        viewModel.startPractice(PracticeMode.QUICK_REVIEW)
        val practiceSession = viewModel.ui.practiceSession
        val debugPracticeState = "screen=${viewModel.currentScreen}, toast=${viewModel.ui.toast}, " +
            "l3Kp=${viewModel.ui.l3Pipeline.knowledgePoints.map { it.title }}, " +
            "l3Evidence=${viewModel.ui.l3Pipeline.evidence.map { it.id to it.text.take(40) }}, " +
            "l3Questions=${viewModel.ui.l3Pipeline.questions.map { it.stem to (it.correctAnswer + it.options.joinToString()) }}, " +
            "resultQuestions=${viewModel.ui.result?.quizQuestions?.size}"
        assertNotNull(
            debugPracticeState,
            practiceSession,
        )
        val itemCount = practiceSession!!.items.size

        repeat(itemCount) { index ->
            val item = viewModel.currentPracticeItem()!!
            viewModel.selectPracticeAnswer(item.correctOptionIds.first())
            assertTrue(viewModel.submitPracticeAnswer(now + index))
            viewModel.nextPracticeQuestion()
        }

        assertEquals(Screen.REVIEW, viewModel.currentScreen)
        assertNotNull(viewModel.ui.practiceSession)
        assertNotNull(viewModel.ui.practiceResult)
        assertTrue(viewModel.isPracticeComplete())

        viewModel.exitPractice()
        assertFalse(viewModel.currentScreen == Screen.PRACTICE)
    }

    private fun legacyDirtySnapshot(): L3PipelineSnapshot {
        val source = LessonSource(
            id = "lesson_old",
            title = "高中物理：牛顿第二定律",
            type = L3SourceType.OCR_IMAGE,
            createdAt = now,
            rawText = noisyPhysicsText,
            status = "LEGACY_DIRTY",
        )
        val evidence = Evidence(
            id = "ev_old",
            sourceId = source.id,
            sourceType = L3SourceType.OCR_IMAGE,
            text = noisyPhysicsText,
            blockIndex = 1,
        )
        val dirtyKp = L3KnowledgePoint(
            id = "kp_old",
            title = "同学们注意",
            explanation = "重点来了，大家记一下。",
            sourceEvidenceIds = listOf(evidence.id),
            masteryState = L3MasteryState.LEARNING,
        )
        val dirtyQuestion = L3GeneratedQuestion(
            id = "q_old",
            lessonId = source.id,
            knowledgePointId = dirtyKp.id,
            stem = "大家记一下，下面哪项最重要？",
            options = listOf("A. 重点来了", "B. 与课程无关"),
            correctAnswer = "A",
            explanation = "没有可靠详解。",
            evidenceIds = listOf(evidence.id),
            difficulty = Difficulty.MEDIUM,
        )
        return L3PipelineSnapshot(
            lessonSource = source,
            summary = "同学们注意，重点来了。",
            evidence = listOf(evidence),
            knowledgePoints = listOf(dirtyKp),
            questions = listOf(dirtyQuestion),
            reviewQueue = listOf(
                ReviewQueueItem(
                    id = "rq_old",
                    knowledgePointId = dirtyKp.id,
                    dueAt = now,
                    masteryState = L3MasteryState.LEARNING,
                    sourceLessonId = source.id,
                    arrangementReason = "重点来了，大家记一下。",
                    evidenceId = evidence.id,
                    recommendedActions = listOf("作业截图上传"),
                ),
            ),
            relatedKnowledgeSummaries = listOf(
                RelatedKnowledgeSummary(
                    id = "related_old",
                    sourceKnowledgePointId = dirtyKp.id,
                    sourceKnowledgePointTitle = "作业截图上传",
                    relatedKnowledgePointTitles = listOf("重点来了"),
                    evidenceQuotes = listOf(noisyPhysicsText),
                    confidence = 0.9,
                    needsReview = false,
                    summary = "从作业截图上传扩展相关知识点。",
                ),
            ),
        )
    }

    private fun legacyDirtyHistoryRecord(): HistoryRecord {
        val session = CourseSegmenter.buildSession(
            id = "lesson_old",
            title = "高中物理：牛顿第二定律",
            rawText = noisyPhysicsText,
            nowMs = now,
        )
        val quoteStart = session.segments.first().text.indexOf("下面看").coerceAtLeast(0)
        val span = EvidenceSpan.of(session.segments.first().id, quoteStart, session.segments.first().text.substring(quoteStart).take(80))
        val dirtyResult = CourseAnalysisResult(
            sessionId = session.id,
            knowledgePoints = listOf(
                KnowledgePoint(
                    id = "kp_old",
                    title = "同学们注意",
                    summary = "重点来了，大家记一下。",
                    sourceSegmentId = session.segments.first().id,
                    evidence = listOf(span),
                    importance = Importance.HIGH,
                    difficulty = Difficulty.MEDIUM,
                ),
            ),
            quizQuestions = listOf(
                QuizQuestion(
                    id = "q_old",
                    type = QuestionType.CONCEPT_UNDERSTANDING,
                    stem = "大家记一下，下面哪项最重要？",
                    options = listOf(
                        QuizOption("A", "重点来了", true),
                        QuizOption("B", "与课程无关", false),
                    ),
                    testedKnowledgePointIds = listOf("kp_old"),
                    evidence = listOf(span),
                    explanation = "没有可靠详解。",
                    difficulty = Difficulty.MEDIUM,
                ),
            ),
            provenance = AnalysisProvenance(
                provider = ProviderKind.LOCAL_FALLBACK,
                fallbackUsed = true,
                modelLabel = "legacy",
                createdAtEpochMs = now,
            ),
        )
        return HistoryRecord(
            id = "hist_old",
            title = session.title,
            createdAtEpochMs = now,
            providerName = "LOCAL_FALLBACK",
            profileLabel = "legacy",
            model = "",
            knowledgePointCount = dirtyResult.knowledgePoints.size,
            quizCount = dirtyResult.quizQuestions.size,
            fallbackUsed = true,
            validationStatus = "PASS",
            session = session,
            result = dirtyResult,
        )
    }
}
