package com.classmate.core.material

/**
 * Safe, bounded context hints that may be prepended to the analyzer's classroom text. This is the
 * low-risk way to give the model subject vocabulary WITHOUT touching the prompt schema or the
 * validators: the hint is just a short, clearly-marked text block that lives inside `rawText`, so
 * any evidence the analyzer cites still resolves against the same text it received.
 *
 * The hint never contains credentials, prompts, or vendor payloads — only a capped list of term
 * names the user already chose.
 */
object LessonContextHints {
    const val MARKER = "[课程术语提示]"
    const val MAX_TERMS = 20

    /**
     * Build a single-line term hint, e.g. "[课程术语提示]（高等数学）本节课可能涉及：极限、级数、收敛。".
     * Returns "" when there are no terms (so callers can simply skip prepending it).
     */
    fun glossaryHint(subject: String, terms: List<String>, max: Int = MAX_TERMS): String {
        val picked = terms.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(max.coerceAtLeast(0))
            .toList()
        if (picked.isEmpty()) return ""
        val subjectPart = if (subject.isBlank()) "" else "（${subject.trim()}）"
        return "$MARKER$subjectPart 本节课可能涉及：" + picked.joinToString("、") + "。"
    }
}
