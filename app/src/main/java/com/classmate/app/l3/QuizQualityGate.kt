package com.classmate.app.l3

import com.classmate.core.practice.PracticeItem
import com.classmate.core.practice.PracticeItemType
import com.classmate.core.ai.AiExecutionSource

/**
 * Final quality gate for graded practice shown to students. It rejects generic stems, internal
 * quality-control wording, duplicate options, missing evidence, and unusable fill-in answers.
 */
object QuizQualityGate {
    private val genericStemPatterns = listOf(
        "某知识点",
        "哪项最符合本课知识点",
        "下列哪项最符合材料",
        "这段话说明了什么",
        "围绕知识点",
        "best matches the material",
        "best fits the material",
        "which best matches the material",
        "which option best fits",
    )

    private val lowValueOptionPatterns = listOf(
        "与课程无关",
        "无关废话",
        "随机猜测",
        "只记住名称",
        "只停留在名称",
        "忽略定义",
        "出题策略",
    )

    fun isHighQuality(item: PracticeItem): Boolean = score(item) >= 70

    fun score(item: PracticeItem): Int {
        if (item.source == AiExecutionSource.SAFE_PLACEHOLDER) return 0
        if (item.type == PracticeItemType.QUIZ_RETRY) return 0
        if (!StudentVisibleQuizSanitizer.isStudentSafe(item)) return 0
        if (item.knowledgePointTitle.isBlank()) return 0
        if (LearningArtifactRepairer.hasForbiddenTitleText(item.knowledgePointTitle)) return 0
        if (genericStemPatterns.any { item.question.contains(it, ignoreCase = true) }) return 0
        if (item.evidenceQuote.isNullOrBlank()) return 0

        var score = 35
        val titleTokens = subjectTokens(item.knowledgePointTitle)
        val visible = "${item.question} ${item.answer} ${item.options.joinToString(" ") { it.text }}"
        if (titleTokens.any { visible.contains(it, ignoreCase = true) }) score += 20
        if (item.answer.contains("为什么正确") || item.answer.contains("答案详解") || item.answer.contains("知识点")) score += 10
        if (item.answer.contains("证据摘录")) score += 10

        if (item.type == PracticeItemType.FILL_BLANK && item.options.isEmpty()) {
            val answer = item.answer.substringAfter("正确答案：", item.answer).substringBefore("。").trim()
            return if (answer.length in 2..32 && !StudentVisibleQuizSanitizer.hasBlockedText(answer)) {
                (score + 25).coerceIn(0, 100)
            } else {
                0
            }
        }

        if (item.options.size < 2) return 0
        val normalized = item.options.map { normalize(it.text) }
        if (normalized.distinct().size != normalized.size) return 0
        if (item.options.any { option -> lowValueOptionPatterns.any { option.text.contains(it, ignoreCase = true) } }) return 0
        if (item.options.any { StudentVisibleQuizSanitizer.hasBlockedText(it.text) }) return 0
        if (item.options.any { it.text.length < 4 }) score -= 15
        if (item.options.count { it.correct } >= 1) score += 15
        if (item.options.any { option -> titleTokens.any { option.text.contains(it, ignoreCase = true) } }) score += 10
        return score.coerceIn(0, 100)
    }

    private fun subjectTokens(title: String): List<String> =
        title.split(Regex("[\\s/、，,。；;:：()（）\\[\\]【】]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .ifEmpty { listOf(title.trim()).filter { it.length >= 2 } }

    private fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[a-dA-D][.、．]\\s*"), "")
            .replace(Regex("\\s+"), "")
            .trim()
}
