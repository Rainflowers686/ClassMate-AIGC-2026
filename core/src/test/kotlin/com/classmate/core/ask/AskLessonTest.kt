package com.classmate.core.ask

import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.evidence.EvidenceResolver
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.Difficulty
import com.classmate.core.model.Importance
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AskLessonTest {
    private val session = CourseSegmenter.buildSession(
        id = "s1",
        title = "Calculus",
        rawText = "A convergent series has a finite partial-sum limit.\nThe harmonic series diverges.",
        nowMs = 0,
    )
    private val resolver = EvidenceResolver()
    private val evidence = resolver.resolve(session.segments.first(), "finite partial-sum limit")!!
    private val result = CourseAnalysisResult(
        sessionId = session.id,
        knowledgePoints = listOf(
            KnowledgePoint(
                id = "kp_1",
                title = "Series convergence",
                summary = "Convergence means partial sums approach a finite limit.",
                sourceSegmentId = session.segments.first().id,
                evidence = listOf(evidence),
                importance = Importance.HIGH,
                difficulty = Difficulty.MEDIUM,
            ),
        ),
        quizQuestions = emptyList(),
        provenance = AnalysisProvenance(ProviderKind.BLUELM, fallbackUsed = false, modelLabel = "qwen3.5-plus"),
    )

    @Test
    fun parsesGroundedJsonWithLocatedEvidence() {
        val raw = """{"answer":"It has a finite partial-sum limit.","relatedKnowledgePoints":["Series convergence"],"evidenceQuotes":["finite partial-sum limit"],"groundedness":"grounded","followUpSuggestion":"Review the definition."}"""

        val answer = AskLessonResultParser.parse(raw, session, result)

        assertNotNull(answer)
        assertEquals("grounded", answer!!.groundedness)
        assertEquals(1, answer.evidenceRefs.size)
        assertFalse(answer.fallbackUsed)
    }

    @Test
    fun naturalLanguageSafelyFailsForFallback() {
        assertNull(AskLessonResultParser.parse("The answer is convergence.", session, result))
    }

    @Test
    fun unlocatedEvidenceIsNotFabricated() {
        val raw = """{"answer":"Maybe.","relatedKnowledgePoints":["Series convergence"],"evidenceQuotes":["not in the lesson"],"groundedness":"grounded","followUpSuggestion":""}"""

        val answer = AskLessonResultParser.parse(raw, session, result)

        assertNotNull(answer)
        assertTrue(answer!!.evidenceRefs.isEmpty())
        assertEquals("partial", answer.groundedness)
    }

    @Test
    fun localFallbackUsesExtractedEvidenceOnly() {
        val answer = LocalAskLessonEngine.answer("What is convergence?", session, result)

        assertTrue(answer.fallbackUsed)
        assertEquals("grounded", answer.groundedness)
        assertTrue(answer.answer.contains("Based on the extracted lesson notes"))
    }
}
