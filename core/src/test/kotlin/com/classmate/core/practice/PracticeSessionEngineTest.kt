package com.classmate.core.practice

import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.PracticeHistoryRecord
import com.classmate.core.learning.ReviewEventType
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.Difficulty
import com.classmate.core.model.EvidenceSpan
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.ProviderKind
import com.classmate.core.model.QuizOption
import com.classmate.core.model.QuizQuestion
import com.classmate.core.model.QuestionType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionEngineTest {
    private val now = 1_700_000_000_000L

    private fun kp(id: String, title: String, quote: String = "证据：$title 的关键描述") = KnowledgePoint(
        id = id,
        title = title,
        summary = "$title 的一句话说明。",
        sourceSegmentId = "seg_$id",
        evidence = listOf(EvidenceSpan("seg_$id", 0, quote.length, quote)),
        importance = Importance.HIGH,
        difficulty = Difficulty.MEDIUM,
    )

    private fun result(kps: List<KnowledgePoint>, quizzes: List<QuizQuestion> = emptyList()) =
        CourseAnalysisResult(
            sessionId = "s1",
            knowledgePoints = kps,
            quizQuestions = quizzes,
            provenance = AnalysisProvenance(ProviderKind.LOCAL_FALLBACK, fallbackUsed = true, modelLabel = "", createdAtEpochMs = now),
        )

    private fun quizFor(kpId: String) = QuizQuestion(
        id = "q_$kpId",
        type = QuestionType.CONCEPT_UNDERSTANDING,
        stem = "关于 $kpId 的理解题",
        options = listOf(QuizOption("A", "对的选项", true), QuizOption("B", "错的选项", false)),
        testedKnowledgePointIds = listOf(kpId),
        evidence = emptyList(),
        explanation = "因为……",
        difficulty = Difficulty.MEDIUM,
    )

    @Test
    fun prioritizesWrongTooHardAndNeedMorePractice() {
        val kps = listOf(kp("k1", "磁通量"), kp("k2", "楞次定律"), kp("k3", "感应电动势"))
        val r = result(kps)
        val store = InMemoryLearningStore { now }
        store.addTasksFromAnalysis(r, "大学物理", "LOCAL", "local_only", "")
        store.recordFeedback("s1", "k2", ReviewEventType.WRONG_ANSWER)   // strongest signal
        store.recordFeedback("s1", "k3", ReviewEventType.NEED_EXAMPLE)

        val session = PracticeSessionEngine.build(r, store.snapshot(), PracticeMode.WEAKNESS_DRILL, now, "大学物理")
        // weakness drill only keeps weak kps (k2 wrong, k3 need-example); k2 (wrong) ranks first.
        assertTrue(session.items.isNotEmpty())
        assertEquals("k2", session.items.first().knowledgePointId)
        assertTrue(session.items.any { it.knowledgePointId == "k3" })
        assertFalse(session.items.any { it.knowledgePointId == "k1" }) // no weakness signal
    }

    @Test
    fun buildsFlashcardOrEvidenceItemWhenNoQuiz() {
        val r = result(listOf(kp("k1", "磁通量")))
        val quick = PracticeSessionEngine.build(r, LearningSnapshot(), PracticeMode.QUICK_REVIEW, now, "大学物理")
        assertEquals(PracticeItemType.FLASHCARD, quick.items.first().type)

        val recall = PracticeSessionEngine.build(r, LearningSnapshot(), PracticeMode.EVIDENCE_RECALL, now, "大学物理")
        assertEquals(PracticeItemType.EVIDENCE_CHECK, recall.items.first().type)
    }

    @Test
    fun reusesExistingQuizWhenAvailable() {
        val r = result(listOf(kp("k1", "磁通量")), listOf(quizFor("k1")))
        val session = PracticeSessionEngine.build(r, LearningSnapshot(), PracticeMode.QUICK_REVIEW, now, "大学物理")
        val item = session.items.first()
        assertEquals(PracticeItemType.QUIZ_RETRY, item.type)
        assertEquals("q_k1", item.quizId)
        assertTrue(item.options.any { it.correct })
    }

    @Test
    fun evidenceWrongItemIsFlaggedForRecheckNotDrilled() {
        val r = result(listOf(kp("k1", "磁通量")), listOf(quizFor("k1")))
        val store = InMemoryLearningStore { now }
        store.addTasksFromAnalysis(r, "大学物理", "LOCAL", "local_only", "")
        store.recordFeedback("s1", "k1", ReviewEventType.EVIDENCE_WRONG)

        val session = PracticeSessionEngine.build(r, store.snapshot(), PracticeMode.QUICK_REVIEW, now, "大学物理")
        val item = session.items.first { it.knowledgePointId == "k1" }
        assertEquals(PracticeItemType.SOURCE_TRACE, item.type)
        assertTrue(item.needsRecheck)
    }

    @Test
    fun summarizeCountsOutcomesAndCollectsNeedMore() {
        val r = result(listOf(kp("k1", "磁通量"), kp("k2", "楞次定律")))
        val session = PracticeSessionEngine.build(r, LearningSnapshot(), PracticeMode.QUICK_REVIEW, now, "大学物理")
        val attempts = listOf(
            PracticeAttempt(session.items[0].id, session.items[0].knowledgePointId, null, PracticeOutcome.CORRECT),
            PracticeAttempt(session.items[1].id, session.items[1].knowledgePointId, null, PracticeOutcome.NEED_MORE_PRACTICE),
        )
        val res = PracticeSessionEngine.summarize(session, attempts, durationMs = 5_000)
        assertEquals(1, res.correctCount)
        assertEquals(1, res.needMorePracticeCount)
        assertEquals(1, res.needPracticeItems.size)
        assertTrue(res.needPracticeItems.first().recommendedSearchQuery.isNotBlank())
    }

    @Test
    fun writeBackRaisesPriorityOnWrongAndLowersOnMastered() {
        val r = result(listOf(kp("k1", "磁通量"), kp("k2", "楞次定律")))
        val store = InMemoryLearningStore { now }
        store.addTasksFromAnalysis(r, "大学物理", "LOCAL", "local_only", "")
        val before = store.snapshot()
        val pK1 = before.tasks.first { it.knowledgePointId == "k1" }.priority
        val pK2 = before.tasks.first { it.knowledgePointId == "k2" }.priority

        val attempts = listOf(
            PracticeAttempt("i1", "k1", null, PracticeOutcome.WRONG),
            PracticeAttempt("i2", "k2", null, PracticeOutcome.MASTERED),
        )
        val after = PracticeSessionEngine.writeBack(before, "s1", attempts, now)
        val k1 = after.tasks.first { it.knowledgePointId == "k1" }
        val k2 = after.tasks.first { it.knowledgePointId == "k2" }
        assertTrue("wrong should raise priority", k1.priority > pK1)
        assertEquals(1, k1.counters.wrongAnswer)
        assertTrue("mastered should lower priority", k2.priority < pK2)
        assertEquals(1, k2.counters.mastered)
    }

    @Test
    fun recommendedSearchQueryHasNoSecretsOrPromptTokens() {
        val r = result(listOf(kp("k1", "磁通量")))
        val session = PracticeSessionEngine.build(r, LearningSnapshot(), PracticeMode.NEED_MORE_PRACTICE, now, "大学物理 appKey prompt")
        val combined = session.items.joinToString(" ") { it.recommendedSearchQuery.orEmpty() }
        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content", "prompt", "messages").forEach {
            assertFalse(combined.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun practiceHistoryRecordSerializesWithoutSensitiveFields() {
        val snap = LearningSnapshot(
            practiceHistory = listOf(
                PracticeHistoryRecord(
                    id = "ph1", courseSessionId = "s1", courseTitle = "大学物理", createdAt = now,
                    mode = PracticeMode.WEAKNESS_DRILL.name, itemCount = 5, correctCount = 3, wrongCount = 1,
                    masteredCount = 1, needMorePracticeCount = 1, relatedKnowledgePointTitles = listOf("磁通量"),
                ),
            ),
        )
        val json = Json.encodeToString(LearningSnapshot.serializer(), snap)
        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content", "prompt", "messages").forEach {
            assertFalse(json.contains(it, ignoreCase = true))
        }
        assertTrue(json.contains("WEAKNESS_DRILL"))
    }
}
