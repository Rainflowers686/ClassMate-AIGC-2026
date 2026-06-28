package com.classmate.core.ai

import com.classmate.core.prompt.Prompt

/**
 * Builds the model [Prompt] for each enhancement scenario from REAL learning data. Pure + testable.
 * Every prompt instructs the model to answer in Chinese, stay grounded in the supplied material, and not
 * invent facts — the same anti-fabrication contract the analyzer/Ask flows use. No ids, no debug tokens.
 */
object EnhancementPromptBuilder {

    private const val GROUND_RULE =
        "只能依据下面提供的课程内容作答，不要编造课程之外的事实；用简洁中文输出，不要输出任何编号 id、调试信息或英文标记。"

    fun studyPackHandout(courseTitle: String, points: List<EnhancementPoint>): Prompt = Prompt(
        system = "你是学习材料整理助手，把课程整理成可直接复习/打印的讲义版。$GROUND_RULE",
        user = buildString {
            append("课程：$courseTitle\n请整理为讲义版，包含：核心概念、结构化知识点、例子、易错点、证据引用、复习建议。\n\n知识点与证据：\n")
            append(pointsBlock(points))
        },
    )

    fun examCramSheet(courseTitle: String, points: List<EnhancementPoint>): Prompt = Prompt(
        system = "你是考前复习助手，把课程压缩成考前速记版。$GROUND_RULE",
        user = buildString {
            append("课程：$courseTitle\n请整理为考前速记版，包含：高频知识点、公式/定义、易混淆点、3～5 条考前提示。\n\n知识点与证据：\n")
            append(pointsBlock(points))
        },
    )

    fun quizFeedback(total: Int, correct: Int, wrongSummaries: List<String>, weakTitles: List<String>): Prompt = Prompt(
        system = "你是学习反馈助手，根据本次答题结果给出针对性、可执行的中文反馈。$GROUND_RULE",
        user = buildString {
            append("本次共 $total 题，答对 $correct 题。\n")
            if (wrongSummaries.isNotEmpty()) {
                append("错题（题目 / 你的选择 / 正确答案 / 考查知识点）：\n")
                wrongSummaries.take(8).forEach { append("- $it\n") }
            } else {
                append("没有错题。\n")
            }
            if (weakTitles.isNotEmpty()) append("当前薄弱知识点：${weakTitles.joinToString("、")}\n")
            append("请输出：本次表现总结、错因分析、对应薄弱知识点、下一步复习建议、是否需要补基础或可以加难。")
        },
    )

    fun weaknessRemediation(knowledgePointTitle: String, reason: String, missCount: Int): Prompt = Prompt(
        system = "你是薄弱点强化助手，为一个被反复答错的知识点设计循序渐进的强化方案。$GROUND_RULE",
        user = buildString {
            append("薄弱知识点：$knowledgePointTitle\n")
            if (reason.isNotBlank()) append("薄弱原因：$reason\n")
            if (missCount > 0) append("已答错次数：$missCount\n")
            append("请输出：① 用一句话复述定义 ② 2～3 道由易到难的练习方向（不直接给出完整题目，给练习重点） ③ 掌握后如何进阶。")
        },
    )

    fun evidenceExplanation(knowledgePointTitle: String, evidenceQuote: String, questionStem: String?, weak: Boolean): Prompt = Prompt(
        system = "你是证据解读助手，解释一段原文如何支持某个知识点。$GROUND_RULE" +
            if (weak) "这条证据关联较弱，请保守表述并提示需要核对，不要给出确定结论。" else "",
        user = buildString {
            append("知识点：$knowledgePointTitle\n证据原文：「${evidenceQuote.trim()}」\n")
            if (!questionStem.isNullOrBlank()) append("相关题目：$questionStem\n")
            append("请说明：这条证据支持哪个知识点、为什么支持、与题目的关系；若关联弱请明确提示「证据待核对」。")
        },
    )

    private fun pointsBlock(points: List<EnhancementPoint>): String =
        points.take(16).joinToString("\n") { p ->
            buildString {
                append("- ${p.title}")
                if (p.summary.isNotBlank()) append("：${p.summary}")
                if (p.evidenceQuote.isNotBlank()) append("（证据：${p.evidenceQuote.trim().take(60)}）")
            }
        }
}
