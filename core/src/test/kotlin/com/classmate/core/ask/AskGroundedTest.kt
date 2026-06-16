package com.classmate.core.ask

import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.evidence.EvidenceResolver
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.Difficulty
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.Prompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AskGroundedTest {

    private val session = CourseSegmenter.buildSession(
        id = "s1",
        title = "高等数学 - 级数",
        rawText = "收敛级数的部分和有有限极限。\n调和级数是发散的。",
        nowMs = 0,
    )
    private val resolver = EvidenceResolver()
    private val evidence = resolver.resolve(session.segments.first(), "有限极限")!!
    private val result = CourseAnalysisResult(
        sessionId = session.id,
        knowledgePoints = listOf(
            KnowledgePoint(
                id = "kp_1",
                title = "级数收敛",
                summary = "收敛指部分和趋于有限极限。",
                sourceSegmentId = session.segments.first().id,
                evidence = listOf(evidence),
                importance = Importance.HIGH,
                difficulty = Difficulty.MEDIUM,
            ),
        ),
        quizQuestions = emptyList(),
        provenance = AnalysisProvenance(ProviderKind.BLUELM, fallbackUsed = false, modelLabel = "qwen3.5-plus"),
    )

    private fun seam(reply: String?, name: String = "BLUELM", model: String? = "qwen3.5-plus") =
        AskChatSeam { _, _ -> reply?.let { AskChatResult(it, name, model) } }

    private val candidateQuote get() = EvidenceRetriever.retrieve("什么是收敛", result).first().quote

    @Test
    fun retrieverHitsRelevantKnowledgePointEvidence() {
        val candidates = EvidenceRetriever.retrieve("收敛是什么意思", result)
        assertTrue(candidates.isNotEmpty())
        assertEquals("级数收敛", candidates.first().knowledgePointTitle)
        assertTrue(candidates.first().segmentId != null)
    }

    @Test
    fun noEvidenceReturnsNotFoundWithoutCallingProvider() {
        var called = false
        val seam = AskChatSeam { _, _ -> called = true; AskChatResult("{}", "BLUELM", null) }
        val outcome = GroundedAskLessonEngine.answer("请讲讲量子纠缠和黑洞蒸发", session, result, seam)
        assertEquals(AskStatus.NOT_FOUND, outcome.telemetry.askStatus)
        assertFalse("provider must not be called when there is no candidate evidence", called)
        assertFalse(outcome.answer.fallbackUsed)
    }

    @Test
    fun groundedJsonWithCandidateQuotePasses() {
        val reply = """{"status":"grounded","answer":"收敛指部分和趋于有限极限。","relatedKnowledgePoints":["级数收敛"],"citations":[{"knowledgePointTitle":"级数收敛","quote":"${candidateQuote}","segmentId":"seg"}],"confidence":0.9}"""
        val outcome = GroundedAskLessonEngine.answer("什么是收敛", session, result, seam(reply))
        assertEquals(AskStatus.GROUNDED, outcome.telemetry.askStatus)
        assertEquals(1, outcome.answer.evidenceRefs.size)
        assertTrue(outcome.answer.suggestedFollowUps.isNotEmpty())
        assertFalse(outcome.answer.fallbackUsed)
        assertEquals("BLUELM", outcome.answer.providerName)
    }

    @Test
    fun fabricatedQuoteFailsValidationThenFallsBack() {
        // Both attempts return a self-invented quote → validation fails → safe local fallback.
        val reply = """{"status":"grounded","answer":"乱编的。","relatedKnowledgePoints":["级数收敛"],"citations":[{"knowledgePointTitle":"级数收敛","quote":"这句话根本不在候选证据里","segmentId":"x"}]}"""
        val outcome = GroundedAskLessonEngine.answer("什么是收敛", session, result, seam(reply))
        assertTrue(outcome.answer.fallbackUsed)
        assertFalse(outcome.answer.answer.contains("乱编"))
        assertTrue(outcome.answer.answer.contains("根据本节课证据"))
    }

    @Test
    fun relatedKnowledgePointOutsideCandidatesFailsValidation() {
        val reply = """{"status":"grounded","answer":"x","relatedKnowledgePoints":["相对论"],"citations":[{"knowledgePointTitle":"级数收敛","quote":"${candidateQuote}","segmentId":"s"}]}"""
        val result0 = GroundedAskParser.parse(reply, EvidenceRetriever.retrieve("什么是收敛", result), "BLUELM", "qwen3.5-plus")
        assertTrue(result0 is GroundedAskParser.Result.Invalid)
    }

    @Test
    fun localOnlyStyleSeamReturnsNullSoEngineUsesLocalEvidence() {
        val outcome = GroundedAskLessonEngine.answer("什么是收敛", session, result, seam(null))
        assertTrue(outcome.answer.fallbackUsed)
        assertEquals("local", outcome.answer.providerName)
        assertTrue(outcome.answer.evidenceRefs.isNotEmpty()) // grounded in local candidates, not invented
        assertTrue(outcome.answer.suggestedFollowUps.isNotEmpty())
    }

    @Test
    fun telemetryAndPromptCarryNoFreeTextLeak() {
        val outcome = GroundedAskLessonEngine.answer("什么是收敛", session, result, seam(null))
        val line = outcome.telemetry.safeLine()
        listOf("收敛", "Authorization", "Bearer", "根据本节课").forEach { assertFalse(line.contains(it)) }
    }

    @Test
    fun promptContainsOnlyCandidatesAndSchemaNotFullLecture() {
        val candidates = EvidenceRetriever.retrieve("什么是收敛", result)
        val prompt: Prompt = AskContextBuilder.buildPrompt("什么是收敛", session.title, candidates)
        assertTrue(prompt.user.contains("级数收敛"))
        assertTrue(prompt.system.contains("not_found"))
        assertFalse(prompt.user.contains("调和级数是发散的")) // a non-candidate lecture line is not leaked
    }
}
