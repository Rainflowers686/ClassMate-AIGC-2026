package com.classmate.core.model

/**
 * Quiz completeness gate. The validators already require a correct option + evidence, but per-option
 * rationale and the overall explanation are NOT enforced there — so a question can reach the UI with a
 * correct answer yet blank "why right / why wrong" text. This gate:
 *  - drops questions that can never be answered (no usable options / no correct option),
 *  - fills blank option rationales and the overall explanation with honest defaults (never invents the
 *    correct answer — it only explains it), and
 *  - leaves single/multi handling to [QuizQuestion.isSingleAnswer] (derived from correct option count).
 *
 * It never fabricates content beyond generic, honest "go back to the evidence" wording.
 */
object QuizQuality {

    const val CORRECT_RATIONALE = "正确选项：与本课证据和知识点一致。"
    const val WRONG_RATIONALE = "干扰项：与本课证据不一致，请回到原文核对题干限定。"
    const val DEFAULT_EXPLANATION = "先回到证据核对题干的限定条件，再确认正确选项。"

    /** A question can enter practice iff it has >=2 options and >=1 correct option that exists in options. */
    fun isUsable(q: QuizQuestion): Boolean {
        if (q.options.size < 2) return false
        val optionIds = q.options.map { it.id }.toSet()
        val correct = q.correctOptionIds
        return correct.isNotEmpty() && correct.all { it in optionIds }
    }

    /** Fill blank per-option rationale + overall explanation with honest defaults. Answers are untouched. */
    fun complete(q: QuizQuestion): QuizQuestion = q.copy(
        options = q.options.map { o ->
            if (o.rationale.isNotBlank()) o
            else o.copy(rationale = if (o.isCorrect) CORRECT_RATIONALE else WRONG_RATIONALE)
        },
        explanation = q.explanation.ifBlank { DEFAULT_EXPLANATION },
    )

    /** Drop the unusable, complete the rest — what every practice/quiz surface should consume. */
    fun repairAndFilter(questions: List<QuizQuestion>): List<QuizQuestion> =
        questions.filter { isUsable(it) }.map { complete(it) }
}
