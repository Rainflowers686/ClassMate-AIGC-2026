package com.classmate.core.formatting

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ask.LessonAnswer
import com.classmate.core.ask.LessonAnswerEvidenceRef
import com.classmate.core.practice.PracticeFeedback
import com.classmate.core.practice.PracticeFeedbackCorrectness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningReadableFormatterTest {
    @Test
    fun askAnswerBecomesStudyReadableOutputWithEvidenceAndNextAction() {
        val answer = LessonAnswer(
            answer = "级数收敛要看部分和是否有有限极限。",
            relatedKnowledgePointTitles = listOf("级数收敛"),
            evidenceRefs = listOf(LessonAnswerEvidenceRef("部分和趋于有限极限", "seg_1", "级数收敛")),
            groundedness = "grounded",
            followUpSuggestion = "继续追问判别法。",
            fallbackUsed = false,
            providerName = "BLUELM",
            modelName = "qwen3.5-plus",
            suggestedFollowUps = listOf("比值判别法什么时候用？"),
        )

        val readable = LearningReadableFormatter.fromAsk(answer)

        assertEquals(AiExecutionSource.CLOUD, readable.source)
        assertTrue(readable.bulletPoints.any { it.contains("级数收敛") })
        assertTrue(readable.evidenceQuotes.contains("部分和趋于有限极限"))
        assertTrue(readable.nextActions.any { it.contains("follow-up") })
    }

    @Test
    fun practiceFeedbackKeepsEvidenceAndAction() {
        val feedback = PracticeFeedback(
            correctness = PracticeFeedbackCorrectness.INCORRECT,
            explanation = "Correct answer: A. Evidence: 部分和趋于有限极限。",
            knowledgePointId = "kp_1",
            evidenceQuote = "部分和趋于有限极限",
            nextAction = "add_to_review",
        )

        val readable = LearningReadableFormatter.fromPracticeFeedback(feedback)

        assertTrue(readable.conclusion.contains("Incorrect"))
        assertTrue(readable.evidenceQuotes.contains("部分和趋于有限极限"))
        assertTrue(readable.nextActions.contains("add_to_review"))
    }
}
