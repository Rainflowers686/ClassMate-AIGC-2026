package com.classmate.core.practice

import com.classmate.core.ai.AiExecutionSource

object PracticeFeedbackEngine {
    fun evaluateSelfReported(
        item: PracticeItem,
        outcome: PracticeOutcome,
        submittedAnswer: String = "",
        source: AiExecutionSource = item.source,
    ): PracticeFeedback {
        val correctness = when (outcome) {
            PracticeOutcome.CORRECT, PracticeOutcome.MASTERED -> PracticeFeedbackCorrectness.CORRECT
            PracticeOutcome.WRONG -> PracticeFeedbackCorrectness.INCORRECT
            PracticeOutcome.NEED_MORE_PRACTICE -> PracticeFeedbackCorrectness.NEEDS_REVIEW
        }
        return buildFeedback(item, correctness, submittedAnswer, source)
    }

    fun evaluateSelectedOptions(
        item: PracticeItem,
        selectedOptionIds: Set<String>,
        source: AiExecutionSource = item.source,
    ): PracticeFeedback {
        val correctness = if (item.options.isNotEmpty() && selectedOptionIds == item.correctOptionIds.toSet()) {
            PracticeFeedbackCorrectness.CORRECT
        } else {
            PracticeFeedbackCorrectness.INCORRECT
        }
        return buildFeedback(item, correctness, selectedOptionIds.joinToString(","), source)
    }

    fun evaluateShortAnswer(
        item: PracticeItem,
        submittedAnswer: String,
        source: AiExecutionSource = item.source,
    ): PracticeFeedback {
        val clean = submittedAnswer.trim()
        val correctness = when {
            clean.isBlank() -> PracticeFeedbackCorrectness.NEEDS_REVIEW
            item.answer.isNotBlank() && clean.contains(item.answer, ignoreCase = true) -> PracticeFeedbackCorrectness.CORRECT
            else -> PracticeFeedbackCorrectness.NEEDS_REVIEW
        }
        return buildFeedback(item, correctness, clean, source)
    }

    private fun buildFeedback(
        item: PracticeItem,
        correctness: PracticeFeedbackCorrectness,
        submittedAnswer: String,
        source: AiExecutionSource,
    ): PracticeFeedback {
        val evidence = item.evidenceQuote?.takeIf { it.isNotBlank() }
        val explanation = when (correctness) {
            PracticeFeedbackCorrectness.CORRECT ->
                "Correct answer: ${item.answer}. Your answer matches this lesson point. Review the cited evidence once, then move on."
            PracticeFeedbackCorrectness.INCORRECT ->
                "Correct answer: ${item.answer}. The submitted answer does not match the expected course evidence. Revisit ${item.knowledgePointTitle} and retry one evidence-bound question."
            PracticeFeedbackCorrectness.NEEDS_REVIEW ->
                "Expected answer: ${item.answer}. This needs a self-check against the evidence. Mark it for review if unsure."
        } + evidence?.let { " Evidence: $it" }.orEmpty() +
            item.whyThisQuestionMatters.takeIf { it.isNotBlank() }?.let { " Why it matters: $it" }.orEmpty()
        val nextAction = when (correctness) {
            PracticeFeedbackCorrectness.CORRECT -> "review_later"
            PracticeFeedbackCorrectness.INCORRECT -> "add_to_review"
            PracticeFeedbackCorrectness.NEEDS_REVIEW -> if (submittedAnswer.isBlank()) "retry" else "check_evidence"
        }
        return PracticeFeedback(
            correctness = correctness,
            explanation = explanation,
            knowledgePointId = item.knowledgePointId,
            evidenceQuote = evidence,
            nextAction = nextAction,
            source = source,
        )
    }
}
