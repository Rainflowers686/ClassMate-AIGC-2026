package com.classmate.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizQualityTest {

    private fun opt(id: String, correct: Boolean, rationale: String = "") = QuizOption(id, "text $id", correct, rationale)

    private fun q(id: String, options: List<QuizOption>, explanation: String = "exp") = QuizQuestion(
        id = id,
        type = QuestionType.CONCEPT_UNDERSTANDING,
        stem = "stem $id",
        options = options,
        testedKnowledgePointIds = listOf("kp1"),
        evidence = emptyList(),
        explanation = explanation,
        difficulty = Difficulty.MEDIUM,
    )

    @Test
    fun usableNeedsTwoOptionsAndACorrectAnswer() {
        assertTrue(QuizQuality.isUsable(q("q1", listOf(opt("A", true), opt("B", false)))))
        assertTrue("multi-answer is usable only when at least two correct options are marked", QuizQuality.isUsable(q("q1m", listOf(opt("A", true), opt("B", true), opt("C", false)))))
        assertFalse("one option is not usable", QuizQuality.isUsable(q("q2", listOf(opt("A", true)))))
        assertFalse("no correct option is not usable", QuizQuality.isUsable(q("q3", listOf(opt("A", false), opt("B", false)))))
        assertFalse("blank option id is not usable", QuizQuality.isUsable(q("q4", listOf(opt("", true), opt("B", false)))))
        assertFalse("duplicate option ids are not usable", QuizQuality.isUsable(q("q5", listOf(opt("A", true), opt("A", false)))))
    }

    @Test
    fun completeFillsBlankRationaleAndExplanation() {
        val completed = QuizQuality.complete(q("q4", listOf(opt("A", true, ""), opt("B", false, "")), explanation = ""))
        assertEquals(QuizQuality.CORRECT_RATIONALE, completed.options[0].rationale)
        assertEquals(QuizQuality.WRONG_RATIONALE, completed.options[1].rationale)
        assertEquals(QuizQuality.DEFAULT_EXPLANATION, completed.explanation)
    }

    @Test
    fun completePreservesExistingText() {
        val completed = QuizQuality.complete(q("q5", listOf(opt("A", true, "因为 A 与证据一致"), opt("B", false, "")), explanation = "总解析在此"))
        assertEquals("因为 A 与证据一致", completed.options[0].rationale)
        assertEquals("总解析在此", completed.explanation)
        // The still-blank wrong option is filled.
        assertEquals(QuizQuality.WRONG_RATIONALE, completed.options[1].rationale)
    }

    @Test
    fun repairAndFilterDropsUnusableAndCompletesTheRest() {
        val good = q("good", listOf(opt("A", true, ""), opt("B", false, "")))
        val noCorrect = q("bad1", listOf(opt("A", false), opt("B", false)))
        val oneOption = q("bad2", listOf(opt("A", true)))
        val result = QuizQuality.repairAndFilter(listOf(good, noCorrect, oneOption))
        assertEquals(listOf("good"), result.map { it.id })
        assertTrue("kept question is completed", result.single().options.all { it.rationale.isNotBlank() })
        assertTrue(result.single().explanation.isNotBlank())
    }
}
