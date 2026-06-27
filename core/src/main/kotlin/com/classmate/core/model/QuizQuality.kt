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
    const val WRONG_RATIONALE = "干扰项：与题干限定不一致。请回到证据核对，向正确说法靠拢。"
    const val DEFAULT_EXPLANATION = "先回到证据核对题干的限定条件，再确认正确选项。"

    /** Below this length a wrong-option rationale is treated as uninformative and rebuilt structurally. */
    private const val MIN_USEFUL_WRONG_RATIONALE = 12

    /** A question can enter practice iff it has >=2 options and >=1 correct option that exists in options. */
    fun isUsable(q: QuizQuestion): Boolean {
        if (q.options.size < 2) return false
        if (q.options.any { it.text.isBlank() }) return false
        val ids = q.options.map { it.id.trim() }
        if (ids.any { it.isBlank() }) return false
        val optionIds = ids.toSet()
        if (optionIds.size != q.options.size) return false
        val correct = q.correctOptionIds
        val validAnswerShape = correct.size == 1 || correct.size >= 2
        return validAnswerShape && correct.all { it in optionIds }
    }

    /**
     * Structured, honest wrong-option rationale: why it is wrong + what the correct understanding is +
     * which direction to fix it. It only references the option the question itself marks correct — it
     * never invents new facts. When no correct text is available it falls back to a neutral check-evidence
     * line.
     */
    fun wrongRationaleFor(correctText: String): String =
        if (correctText.isBlank()) WRONG_RATIONALE
        else "干扰项：与正确说法『$correctText』不一致。正确理解应以此为准；若要改对，请按这一方向回到证据核对题干限定。"

    /** Fill blank / uninformative per-option rationale + overall explanation. Answers are untouched. */
    fun complete(q: QuizQuestion): QuizQuestion {
        val correctText = q.options.firstOrNull { it.isCorrect }?.text?.trim().orEmpty()
        return q.copy(
            options = q.options.map { o ->
                when {
                    o.isCorrect -> if (o.rationale.isNotBlank()) o else o.copy(rationale = CORRECT_RATIONALE)
                    o.rationale.trim().length >= MIN_USEFUL_WRONG_RATIONALE -> o // keep a real model explanation
                    else -> o.copy(rationale = wrongRationaleFor(correctText))
                }
            },
            explanation = q.explanation.ifBlank { DEFAULT_EXPLANATION },
        )
    }

    /** True once every option carries a rationale and every wrong option carries a useful (non-trivial) one. */
    fun hasUsefulOptionExplanations(q: QuizQuestion): Boolean =
        q.options.all { it.rationale.isNotBlank() } &&
            q.options.filterNot { it.isCorrect }.all { it.rationale.trim().length >= MIN_USEFUL_WRONG_RATIONALE }

    /** Drop the unusable, complete the rest — what every practice/quiz surface should consume. */
    fun repairAndFilter(questions: List<QuizQuestion>): List<QuizQuestion> =
        questions.filter { isUsable(it) }.map { complete(it) }
}
