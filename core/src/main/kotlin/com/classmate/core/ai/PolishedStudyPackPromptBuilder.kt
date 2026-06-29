package com.classmate.core.ai

import com.classmate.core.prompt.Prompt

/** One knowledge point fed into the polished-study-pack prompt: title + explanation + one grounded quote. */
data class PolishedStudyMaterial(
    val title: String,
    val explanation: String,
    val evidenceQuote: String,
)

/** All the REAL course material the polished-study-pack pass is allowed to use. Pure data, no ids. */
data class PolishedStudyPackInput(
    val courseTitle: String,
    val sourceSummary: String,
    val knowledgePoints: List<PolishedStudyMaterial>,
    val quizStems: List<String> = emptyList(),
    val weakPoints: List<String> = emptyList(),
    val lowConfidenceNotes: List<String> = emptyList(),
    val hasImageMaterial: Boolean = false,
)

/**
 * Builds the deep-pass prompt that upgrades a course into a structured, print-ready 精修学习包. It is the
 * premium counterpart to [EnhancementPromptBuilder.studyPackHandout]: it asks for a full multi-section
 * markdown handout, but under the SAME anti-fabrication contract — only the supplied material, mark
 * "待核对" when evidence is thin, never pad to a fixed count, and never emit ids / debug / credentials.
 */
object PolishedStudyPackPromptBuilder {

    private const val GROUND_RULE =
        "严格只依据下面提供的课程材料，不要编造课程之外的事实或数据；证据不足时写「待核对」，不要硬凑固定数量；" +
            "不要输出任何编号 id、调试信息、英文标记、密钥或接口字段。"

    fun build(input: PolishedStudyPackInput): Prompt = Prompt(
        system = "你是资深的学习资料编辑，把一节课的材料整理成一份结构化、可直接打印复习的精修学习包。" +
            "用简洁中文输出 markdown 结构文本（用 # / ## 标题和 - 列表），便于导出为 PDF / HTML / Word。$GROUND_RULE",
        user = buildString {
            append("课程标题：").append(input.courseTitle.ifBlank { "未命名课程" }).append('\n')
            if (input.sourceSummary.isNotBlank()) append("资料来源摘要：").append(input.sourceSummary).append('\n')
            if (input.hasImageMaterial) append("（部分内容来自图片/OCR，引用时请注明来源，OCR 不清晰处标「待核对」）\n")
            append('\n')
            append("知识点与证据：\n")
            if (input.knowledgePoints.isEmpty()) {
                append("- （暂无已确认知识点，请基于资料来源谨慎整理，并大量使用「待核对」）\n")
            } else {
                input.knowledgePoints.take(20).forEach { kp ->
                    append("- ").append(kp.title)
                    if (kp.explanation.isNotBlank()) append("：").append(kp.explanation.take(160))
                    if (kp.evidenceQuote.isNotBlank()) append("（证据：").append(kp.evidenceQuote.trim().take(80)).append("）")
                    append('\n')
                }
            }
            if (input.quizStems.isNotEmpty()) {
                append("\n已有自测题（可改写但不要脱离材料）：\n")
                input.quizStems.take(8).forEach { append("- ").append(it.take(120)).append('\n') }
            }
            if (input.weakPoints.isNotEmpty()) {
                append("\n学生薄弱点：").append(input.weakPoints.joinToString("、")).append('\n')
            }
            if (input.lowConfidenceNotes.isNotEmpty()) {
                append("\n低置信/需核对内容（这些务必标「待核对」，不要当成已确认事实）：\n")
                input.lowConfidenceNotes.take(8).forEach { append("- ").append(it.take(100)).append('\n') }
            }
            append("\n请按以下结构输出精修学习包（每节都只依据上面材料，可省略没有内容的小节而不是编造）：\n")
            append("1. 课程标题\n2. 资料来源摘要\n3. 核心知识结构（提纲）\n4. 重点知识点解释\n")
            append("5. 每个知识点的证据摘录（注明来源；不足写「待核对」）\n6. 易错点 / 易混点\n")
            append("7. 考前速记版\n8. 自测题（含答案与简析）\n9. 复习计划\n10. 薄弱点建议\n11. 下一步学习建议\n")
        },
    )
}
