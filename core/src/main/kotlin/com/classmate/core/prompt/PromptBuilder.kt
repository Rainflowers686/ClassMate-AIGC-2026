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
        1. The number of knowledgePoints is decided BY THE MATERIAL, never a fixed quota. Thin material →
           1-2 points; normal → 3-6; rich/long → more (up to the max). NEVER pad to reach a number and
           NEVER invent a point to "fill" the list. Quality over quantity.
        2. If the source is too thin or low-quality to yield reliable points, return as few as 0-1 points.
           Returning fewer real points is correct; fabricating is forbidden.
        3. Every knowledgePoint MUST carry a verbatim evidenceQuote copied exactly from the source segment.
           Do not paraphrase. If you cannot copy real evidence for a point, omit that point entirely.
        4. segmentIndex must be one of the provided segmentIndex values.
        5. Do not summarize non-learning content: greetings, lesson-start/end phrases, transition words,
           "this lesson" labels, or organisational chatter. These are NOT knowledge points.
        6. Keep every explanation short. Prefer formulas, definitions, methods, examples, and warnings.
        7. quizItems: one per knowledgePoint that supports a real question; fewer is fine. Each quiz needs a
           verbatim evidenceQuote. Do not write questions without supporting evidence.
        8. Return JSON object only. The first character must be { and the last must be }.
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
            append("knowledgePoints: as many as the material truly supports, max=").append(request.maxKnowledgePoints)
            append(" (fewer is correct when the material is thin; do not pad). ")
            append("quizItems: one per knowledge point that has real supporting evidence.")
            if (request.repairHint != null) {
                append("\nRepair instruction: ").append(request.repairHint)
                append(". Return the same compact JSON schema only, shorter than before.")
            }
        }
    }
}
