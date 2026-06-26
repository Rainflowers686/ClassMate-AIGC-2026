package com.classmate.app.l3

/**
 * Canonical option-id helpers for quiz / practice options.
 *
 * Option ids MUST be position-based (A / B / C / D …) and therefore UNIQUE, so a single-choice click
 * highlights exactly one option. The old code derived the id from the option text
 * (`option.substringBefore(".").take(1).ifBlank { "A" }`), which collapsed to the same id whenever
 * options shared a leading character or lacked an "A." prefix — that is the "tap one option, all four
 * look selected" bug. Position ids make the selected-predicate (`selectedId == option.id`) exact.
 *
 * Correctness is matched against the answer by LETTER (`"A"`) OR by option TEXT (`"TCP"`), so both the
 * letter-prefixed generated questions and plain-text imported options resolve their correct option.
 */
object QuizOptionIds {

    /** Stable position id: index 0 -> "A", 1 -> "B", … */
    fun letterId(index: Int): String = ('A' + index).toString()

    /** Strip a leading "X. " label from an option string, leaving the answer text. */
    fun cleanText(option: String): String = option.substringAfter(". ", option).trim()

    /** True when the option at [index] is the correct answer, given [correctAnswer] (letters and/or text). */
    fun isAnswer(index: Int, option: String, correctAnswer: String): Boolean {
        val id = letterId(index)
        val text = cleanText(option)
        return correctAnswer.split(",", ";", "|", "、", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .any { token ->
                token.equals(id, ignoreCase = true) ||
                    token.equals(text, ignoreCase = true) ||
                    cleanText(token).equals(text, ignoreCase = true)
            }
    }
}
