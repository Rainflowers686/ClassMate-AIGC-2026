package com.classmate.app.l3

import com.classmate.core.practice.PracticeItem

/**
 * Guard for text shown to students inside quizzes. Internal quality-control language may exist in tests,
 * prompts, and diagnostics, but it must not become a stem, option, rationale, or visible explanation.
 */
object StudentVisibleQuizSanitizer {
    private val blocked = listOf(
        "OCR",
        "原文",
        "课堂原句",
        "字面顺序",
        "证据支持",
        "未被证据支持",
        "题干限定",
        "知识点和证据核对",
        "证据核对",
        "只背",
        "只依据",
        "不能解释",
        "出题依据",
        "质量检查",
        "relevance",
        "fallback",
        "本地整理版",
        "provider",
        "raw id",
        "kp_",
        "q_",
        "ev_",
    )

    fun hasBlockedText(text: String): Boolean =
        blocked.any { token -> guardBody(text).contains(token, ignoreCase = true) }

    private fun guardBody(text: String): String =
        text.substringBefore("证据摘录")

    fun isStudentSafe(item: PracticeItem): Boolean {
        val visible = buildList {
            add(item.question)
            add(item.answer)
            add(item.knowledgePointTitle)
            item.options.forEach { add(it.text) }
        }
        return visible.none(::hasBlockedText)
    }

    fun isStudentSafe(question: L3GeneratedQuestion): Boolean {
        val visible = buildList {
            add(question.stem)
            add(question.explanation)
            question.options.forEach(::add)
        }
        return visible.none(::hasBlockedText)
    }
}
