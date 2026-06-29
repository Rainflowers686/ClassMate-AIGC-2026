package com.classmate.core.model

/**
 * Normalizes "which option is correct" from whatever shape a model returns it in. Per-option `isCorrect`
 * flags are trusted first; otherwise a separate answer expression is matched against the options by id
 * (A/B/C/D), by 1-/0-based index, by option text, or by true/false synonyms. This is the root fix for
 * true/false questions where the model puts the answer in a separate field (e.g. `"answer":"错误"`) and
 * the options therefore carry NO correct flag — which produced an empty correct answer + always-wrong
 * judging. Pure + unit-testable.
 */
object QuizAnswerNormalizer {

    private val TRUE_WORDS = setOf("正确", "对", "是", "对的", "正确的", "true", "t", "yes", "y", "√", "✓")
    private val FALSE_WORDS = setOf("错误", "错", "否", "不对", "错的", "错误的", "false", "f", "no", "n", "×", "✗", "x")

    /** Correct option ids, preferring per-option flags, then the raw [rawAnswer] expression. */
    fun resolveCorrectIds(options: List<QuizOption>, rawAnswer: String?): List<String> {
        val flagged = options.filter { it.isCorrect }.map { it.id }
        if (flagged.isNotEmpty()) return flagged
        if (rawAnswer.isNullOrBlank()) return emptyList()
        val tokens = rawAnswer.split(',', '、', '，', ';', '；', '/', ' ', '\n').map { it.trim() }.filter { it.isNotBlank() }
        val ids = LinkedHashSet<String>()
        tokens.forEach { token -> matchOption(options, token)?.let { ids += it } }
        return ids.toList()
    }

    /** Return [options] with `isCorrect` set from [rawAnswer] when NONE were flagged; otherwise unchanged. */
    fun withResolvedCorrect(options: List<QuizOption>, rawAnswer: String?): List<QuizOption> {
        if (options.any { it.isCorrect }) return options
        val correct = resolveCorrectIds(options, rawAnswer).toSet()
        if (correct.isEmpty()) return options
        return options.map { if (it.id in correct) it.copy(isCorrect = true) else it }
    }

    private fun matchOption(options: List<QuizOption>, token: String): String? {
        val t = token.trim().trim('.', '。', '、', ')', '）', ':', '：')
        if (t.isEmpty()) return null
        // 1. Exact option id (A/B/C/D), case-insensitive.
        options.firstOrNull { it.id.equals(t, ignoreCase = true) }?.let { return it.id }
        // 2. A leading/contained A–Z letter ("选项B", "B 错误", "B."), matched against option ids.
        t.uppercase().firstOrNull { it in 'A'..'Z' }?.let { letter ->
            options.firstOrNull { it.id.equals(letter.toString(), ignoreCase = true) }?.let { return it.id }
        }
        // 3. 1-based then 0-based index.
        t.toIntOrNull()?.let { n ->
            options.getOrNull(n - 1)?.let { return it.id }
            options.getOrNull(n)?.let { return it.id }
        }
        // 4. Option text (exact, then containment either way).
        options.firstOrNull { it.text.isNotBlank() && it.text.equals(t, ignoreCase = true) }?.let { return it.id }
        options.firstOrNull { it.text.isNotBlank() && (it.text.contains(t) || t.contains(it.text)) }?.let { return it.id }
        // 5. True/false synonyms -> the option whose own text is a true/false word.
        val low = t.lowercase()
        if (low in TRUE_WORDS) options.firstOrNull { it.text.trim().lowercase() in TRUE_WORDS }?.let { return it.id }
        if (low in FALSE_WORDS) options.firstOrNull { it.text.trim().lowercase() in FALSE_WORDS }?.let { return it.id }
        return null
    }
}
