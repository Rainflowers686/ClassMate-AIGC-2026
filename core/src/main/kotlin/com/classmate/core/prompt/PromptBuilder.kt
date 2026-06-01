package com.classmate.core.prompt

import com.classmate.core.provider.AnalysisRequest

/** A model prompt split into system (rules) and user (content) parts. */
data class Prompt(val system: String, val user: String) {
    val combined: String get() = system + "\n\n" + user
}

/**
 * Builds the compact BlueLM draft prompt. The model no longer emits the app's final internal
 * schema; it emits a small ClassMateLlmDraftV1 object and local deterministic code assembles
 * ids, references, evidence spans, and UI-ready defaults.
 */
class PromptBuilder {

    fun build(request: AnalysisRequest): Prompt = Prompt(system = systemPrompt(), user = userPrompt(request))

    private fun systemPrompt(): String = """
        You are ClassMate's classroom-understanding engine. Return one compact JSON object only.
        No markdown, no code fence, no explanation before or after JSON.

        Output schema: ClassMateLlmDraftV1
        {
          "courseTitle": "...",
          "knowledgePoints": [
            {
              "title": "...",
              "conceptType": "definition|formula|method|example|warning",
              "importance": "core|important|normal",
              "difficulty": "intro|medium|advanced",
              "segmentIndex": 1,
              "evidenceQuote": "short verbatim quote copied from the source segment",
              "explanation": "one sentence"
            }
          ],
          "quizItems": [
            {
              "knowledgePointTitle": "must exactly equal one title above",
              "type": "single_choice|judge",
              "question": "...",
              "options": ["...", "...", "...", "..."],
              "correctIndex": 0,
              "explanation": "...",
              "evidenceQuote": "short verbatim quote copied from source text"
            }
          ]
        }

        Rules:
        1. Produce 5-8 knowledgePoints and 5-8 quizItems unless the source is very short.
        2. evidenceQuote must be copied verbatim from the original source text. Do not paraphrase.
        3. segmentIndex must be one of the provided segmentIndex values.
        4. Do not use generic classroom filler as titles: greetings, lesson-start phrases,
           first/last transition words, or "this lesson" labels.
        5. Keep every explanation short. Prefer formulas, definitions, methods, examples, and warnings.
        6. If evidence cannot be copied exactly, omit that item.
        7. Return JSON object only. The first character must be { and the last must be }.
    """.trimIndent()

    private fun userPrompt(request: AnalysisRequest): String {
        val session = request.session
        val segmentsBlock = buildString {
            for (seg in session.segments) {
                append("[segmentIndex=").append(seg.index).append("] ").append(seg.text).append('\n')
            }
        }.trimEnd()

        return buildString {
            append("courseTitle: ").append(session.title.ifBlank { "Untitled course" }).append('\n')
            append("source segments:\n").append(segmentsBlock).append("\n\n")
            append("Return ClassMateLlmDraftV1 JSON only. ")
            append("knowledgePoints max=").append(request.maxKnowledgePoints).append(", ")
            append("quizItems target=5-8.")
            if (request.repairHint != null) {
                append("\nRepair instruction: ").append(request.repairHint)
                append(". Return the same compact JSON schema only, shorter than before.")
            }
        }
    }
}
