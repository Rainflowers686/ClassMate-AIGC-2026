package com.classmate.core.prompt

import com.classmate.core.provider.AnalysisRequest

/** A model prompt split into system (rules) and user (content) parts. */
data class Prompt(val system: String, val user: String) {
    val combined: String get() = system + "\n\n" + user
}

/**
 * Builds the prompt that constrains the model into producing an evidence-bound, JSON-only
 * analysis. Every product rule is encoded here so the contract is auditable in one place:
 *
 *  1. JSON only — no markdown, no prose.
 *  2. Distil real concepts; do NOT copy paragraphs into knowledge points.
 *  3. Merge duplicate / synonymous points.
 *  4. Every knowledge point binds a sourceSegmentId + verbatim evidenceQuotes.
 *  5. Micro-tests assess understanding/judgment/application/error-analysis/transfer —
 *     never "which line is closest to the original".
 *  6. Every question references the knowledge point(s) it tests + cites evidence.
 *  7. Every question has >=1 correct option and an explanation.
 *  8. No conclusion without evidence.
 *  9. The review plan is generated later by the app — do not output it here.
 * 10. If the rules cannot be met, output empty arrays so the app falls back — never invent.
 */
class PromptBuilder {

    fun build(request: AnalysisRequest): Prompt = Prompt(system = systemPrompt(), user = userPrompt(request))

    private fun systemPrompt(): String = """
        你是 ClassMate 的课堂理解引擎，服务大学生的课后复习。你的任务是阅读一段中文课堂文本，提炼真正的知识点并生成有学习价值的微测题。

        必须严格遵守以下规则：
        1. 只输出 JSON，符合下方 schema；不要输出 markdown、解释或多余文字。
        2. 提炼真正的「概念」，例如「级数收敛与发散」「p 级数」；不要把每一段原文直接复制成一个知识点。
        3. 合并重复或同义的知识点，保持精炼。
        4. 每个知识点必须给出 sourceSegmentId（取自输入中给定的 segment id），以及 evidenceQuotes：能在该 segment 原文中逐字找到的片段。
        5. 微测题必须考查理解，类型限定为：CONCEPT_UNDERSTANDING（概念理解）、JUDGMENT（判断）、APPLICATION（应用）、ERROR_ANALYSIS（错因辨析）、TRANSFER（迁移理解）。禁止出「以下哪一句最接近原文」这类只匹配文字的低价值题。
        6. 每道题必须用 testedKnowledgePoints 引用它所考查的知识点（用该知识点的 title），并给出 evidenceQuotes。
        7. 每道题至少有一个选项 isCorrect=true，并给出 explanation（答后讲解，含错因）。
        8. 没有原文证据支撑的结论一律不要输出。
        9. 不要输出复习计划——复习计划由系统在后续根据重要性、难度、错题与用户反馈生成。
        10. 如果信息不足或无法满足以上要求，请输出 {"knowledgePoints":[],"quizQuestions":[]}，系统会回退；不要编造内容。

        输出 JSON schema：
        {
          "knowledgePoints": [
            {
              "title": "简短概念名",
              "summary": "1-2 句面向学习者的解释",
              "sourceSegmentId": "seg_X",
              "evidenceQuotes": ["该 segment 原文中可逐字找到的片段"],
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
              "testedKnowledgePoints": ["对应知识点的 title"],
              "evidenceQuotes": ["原文可找到的片段"],
              "explanation": "答后讲解",
              "difficulty": "EASY | MEDIUM | HARD"
            }
          ]
        }
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
            append("严格按 system 中的 JSON schema 输出。")
        }
    }
}
