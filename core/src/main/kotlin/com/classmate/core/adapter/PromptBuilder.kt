package com.classmate.core.adapter

import com.classmate.core.model.CourseAnalysisInput

/**
 * Assembles the prompt sent to a real model provider.
 *
 * In v0.2.5 this is unused (the demo provider ignores its input). It lives in
 * the tree so v0.3's BlueLM / Compatible providers have a concrete shape to
 * wire against — and so reviewers can read the rules without grepping prose.
 *
 * The system rules string is intentionally inlined here rather than read from
 * an asset, to keep the core module free of Android dependencies. The repo
 * also carries a readable copy in `prompts/00_system_rules.md` — keep both in
 * sync until v0.3 introduces an asset-loading shim.
 */
object PromptBuilder {

    /**
     * Hard constraints, restated from spec §10.2. The model MUST be told these
     * out loud, every call — drift here is what causes hallucinated knowledge
     * points and broken evidence links.
     */
    const val SYSTEM_RULES: String = """
You are ClassMate's course-structure analysis model.

Hard constraints:
1. Only emit knowledge_points grounded in the input segments. Do not invent.
2. Every knowledge_point MUST set source_segment_id to an existing input segment_id.
3. Every quiz MUST set source_segment_id AND related_kp_id, both referencing existing items.
4. evidence_span SHOULD be copied verbatim from the corresponding segment text.
5. Output MUST be valid JSON. No Markdown. No prose around the JSON.
6. importance and difficulty are integers in [1, 5].
7. Generate 3-5 quizzes.
8. review_plan covers ONLY this session; do not claim long-term scheduling.

Output schema is the CourseAnalysisResult contract supplied by the caller.
"""

    fun build(input: CourseAnalysisInput): String = buildString {
        appendLine(SYSTEM_RULES.trim())
        appendLine()
        appendLine("Course title: ${input.courseTitle}")
        appendLine("Hotwords: ${input.hotwords.joinToString(", ")}")
        appendLine("Segments:")
        input.segments.forEach { seg ->
            appendLine("- segment_id=${seg.segmentId} time_range=${seg.timeRange}")
            appendLine("  text: ${seg.text}")
        }
        appendLine()
        appendLine("Return JSON matching CourseAnalysisResult.")
    }
}
