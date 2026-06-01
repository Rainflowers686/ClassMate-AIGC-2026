package com.classmate.core.prompt

import com.classmate.core.provider.AnalysisRequest

/** A model prompt split into system (rules) and user (content) parts. */
data class Prompt(val system: String, val user: String) {
    val combined: String get() = system + "\n\n" + user
}

/**
 * Builds the prompt that constrains the model into producing an evidence-bound, JSON-only
 * analysis matching the exact wire schema (see WireModels / AnalysisJsonParser / ResultValidator).
 *
 * The single most important rule — and the usual reason a real model's output fails validation —
 * is that `evidenceQuotes` MUST be copied verbatim from the provided segment text. The system
 * prompt hammers this, and [AnalysisRequest.repairHint] adds a corrective instruction on retry.
 */
class PromptBuilder {

    fun build(request: AnalysisRequest): Prompt = Prompt(system = systemPrompt(), user = userPrompt(request))

    private fun systemPrompt(): String = """
        你是 ClassMate 的课堂理解引擎，服务大学生的课后复习。阅读一段中文课堂文本，提炼真正的知识点并生成有学习价值的微测题。

        必须严格遵守以下规则：
        1. 只输出一个 JSON object（不是数组、不是多个对象）。禁止输出 markdown、代码块（```）、解释、前言或任何 JSON 之外的文字。第一个字符必须是 {，最后一个字符必须是 }。
        2. 提炼真正的「概念」，例如「级数收敛与发散」「p 级数」；不要把每一段原文直接复制成一个知识点；合并重复或同义的知识点。
        3. evidenceQuotes 中的每个片段，必须从上面对应 sourceSegmentId 的原文里【逐字符原样复制】，包括标点与空格；不得改写、翻译、概括、纠错、补全或增删任何字符。如果找不到可逐字引用的片段，就不要输出这个知识点。
        4. sourceSegmentId 必须是输入中给定的某个 segment id（例如 seg_1）。
        5. 微测题类型只能是：CONCEPT_UNDERSTANDING、JUDGMENT、APPLICATION、ERROR_ANALYSIS、TRANSFER。禁止出「以下哪一句最接近原文」这类只匹配文字的低价值题。
        6. quizQuestions 的 testedKnowledgePoints 必须使用上面 knowledgePoints 中【完全相同的 title 字符串】来引用；每道题也要给出 evidenceQuotes（同样逐字来自原文）。
        7. 每道题至少有一个选项 isCorrect=true，并给出 explanation；每个知识点和每道题都至少有 1 条 evidenceQuotes。
        8. importance 只能是 LOW/MEDIUM/HIGH/CRITICAL；difficulty 只能是 EASY/MEDIUM/HARD。没有原文证据支撑的结论一律不要输出。
        9. 不要输出复习计划——它由系统后续根据重要性、难度、错题与反馈生成。
        10. 如果信息不足或无法满足以上要求，请输出 {"knowledgePoints":[],"quizQuestions":[]}，系统会回退；不要编造内容。

        输出 JSON schema（字段名固定，类型固定）：
        {
          "knowledgePoints": [
            {
              "title": "简短概念名",
              "summary": "1-2 句面向学习者的解释",
              "sourceSegmentId": "seg_X",
              "evidenceQuotes": ["该 segment 原文中逐字复制的片段"],
              "importance": "LOW | MEDIUM | HIGH | CRITICAL",
              "difficulty": "EASY | MEDIUM | HARD",
              "tags": ["可选标签"]
            }
          ],
          "quizQuestions": [
            {
              "type": "CONCEPT_UNDERSTANDING | JUDGMENT | APPLICATION | ERROR_ANALYSIS | TRANSFER",
              "stem": "题干",
              "options": [ { "text": "选项", "isCorrect": true, "rationale": "为什么对/错" } ],
              "testedKnowledgePoints": ["对应知识点的 title（与上面完全一致）"],
              "evidenceQuotes": ["原文中逐字复制的片段"],
              "explanation": "答后讲解",
              "difficulty": "EASY | MEDIUM | HARD"
            }
          ]
        }

        合法输出示例（仅示意格式；evidenceQuotes 必须逐字来自真实 segment 原文）：
        {"knowledgePoints":[{"title":"示例概念","summary":"一句话解释","sourceSegmentId":"seg_1","evidenceQuotes":["原文中可逐字找到的片段"],"importance":"HIGH","difficulty":"MEDIUM","tags":[]}],"quizQuestions":[{"type":"CONCEPT_UNDERSTANDING","stem":"题干","options":[{"text":"正确项","isCorrect":true,"rationale":"为什么对"},{"text":"干扰项","isCorrect":false,"rationale":"为什么错"}],"testedKnowledgePoints":["示例概念"],"evidenceQuotes":["原文中可逐字找到的片段"],"explanation":"讲解","difficulty":"MEDIUM"}]}
    """.trimIndent()

    private fun userPrompt(request: AnalysisRequest): String {
        val session = request.session
        val segmentsBlock = buildString {
            for (seg in session.segments) {
                append('[').append(seg.id).append("] ").append(seg.text).append('\n')
            }
        }.trimEnd()

        return buildString {
            append("课程标题：").append(session.title.ifBlank { "未命名课程" }).append('\n')
            append("以下是课堂文本，已分段，每段前标注了 segment id：\n\n")
            append(segmentsBlock).append("\n\n")
            append("要求：最多提炼 ").append(request.maxKnowledgePoints).append(" 个知识点；")
            append("每个知识点出 ").append(request.questionsPerKnowledgePoint).append(" 道微测题。")
            append("严格按 system 中的 JSON schema 输出，只输出一个 JSON object。")
            if (request.repairHint != null) {
                append("\n\n（重要）你上一次的输出未通过校验：").append(request.repairHint)
                append("。请只输出一个合法 JSON object，不要 markdown / 代码块 / 解释；")
                append("evidenceQuotes 必须逐字复制自上方对应 segment 原文，testedKnowledgePoints 必须与 knowledgePoints 的 title 完全一致。")
            }
        }
    }
}
