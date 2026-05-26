package com.classmate.core.evidence

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult

/**
 * Verifies the evidence chain described in spec §11.
 *
 * Three checks, run in order:
 *   1. Every source_segment_id resolves to a real input segment.
 *   2. Every quiz.related_kp_id resolves to a real knowledge point.
 *   3. Every evidence_span appears in the corresponding segment's text.
 *
 * The validator does NOT raise on mismatch — it reports. The UI is meant to
 * downgrade affected highlights gracefully (spec §11.2 item 4 / §14.3); a
 * hard error here would defeat the demo-fallback story.
 *
 * Match strategy: substring containment after whitespace normalization. The
 * spec doesn't mandate semantic match; the rule is "as much verbatim as
 * possible". v0.3 may want fuzzy diff but the probe stays simple.
 */
object EvidenceValidator {

    fun validate(
        input: CourseAnalysisInput,
        result: CourseAnalysisResult
    ): EvidenceValidationResult {
        val inputSegmentIds = input.segments.map { it.segment_id }.toSet()
        val resultSegmentTextById: Map<String, String> =
            result.segments.associate { it.segment_id to it.corrected_text }
        val inputSegmentTextById: Map<String, String> =
            input.segments.associate { it.segment_id to it.text }
        val kpIds = result.segments
            .flatMap { it.knowledge_points }
            .map { it.kp_id }
            .toSet()

        val missingKpSegmentRefs = mutableListOf<String>()
        val missingQuizSegmentRefs = mutableListOf<String>()
        val missingRelatedKpRefs = mutableListOf<String>()
        val spanMismatches = mutableListOf<EvidenceSpanMismatch>()

        var spansChecked = 0
        var spansMatched = 0

        // (1) + (3) for knowledge points
        result.segments.forEach { seg ->
            seg.knowledge_points.forEach { kp ->
                if (kp.source_segment_id !in inputSegmentIds) {
                    missingKpSegmentRefs += "kp ${kp.kp_id} -> ${kp.source_segment_id}"
                }
                spansChecked++
                if (spanMatches(kp.evidence_span, kp.source_segment_id, inputSegmentTextById, resultSegmentTextById)) {
                    spansMatched++
                } else {
                    spanMismatches += EvidenceSpanMismatch(
                        ownerKind = "knowledge_point",
                        ownerId = kp.kp_id,
                        segmentId = kp.source_segment_id,
                        span = kp.evidence_span
                    )
                }
            }
        }

        // (1) + (2) + (3) for quizzes
        result.quizzes.forEach { quiz ->
            if (quiz.source_segment_id !in inputSegmentIds) {
                missingQuizSegmentRefs += "quiz ${quiz.quiz_id} -> ${quiz.source_segment_id}"
            }
            if (quiz.related_kp_id !in kpIds) {
                missingRelatedKpRefs += "quiz ${quiz.quiz_id} -> ${quiz.related_kp_id}"
            }
            spansChecked++
            if (spanMatches(quiz.evidence_span, quiz.source_segment_id, inputSegmentTextById, resultSegmentTextById)) {
                spansMatched++
            } else {
                spanMismatches += EvidenceSpanMismatch(
                    ownerKind = "quiz",
                    ownerId = quiz.quiz_id,
                    segmentId = quiz.source_segment_id,
                    span = quiz.evidence_span
                )
            }
        }

        val matchRate = if (spansChecked == 0) 1.0 else spansMatched.toDouble() / spansChecked

        return EvidenceValidationResult(
            schemaPassed = missingKpSegmentRefs.isEmpty()
                && missingQuizSegmentRefs.isEmpty()
                && missingRelatedKpRefs.isEmpty(),
            missingKpSegmentRefs = missingKpSegmentRefs,
            missingQuizSegmentRefs = missingQuizSegmentRefs,
            missingRelatedKpRefs = missingRelatedKpRefs,
            spanMismatches = spanMismatches,
            evidenceMatchRate = matchRate
        )
    }

    /**
     * A span matches if it appears in EITHER the original input text or the
     * model's corrected_text. We allow corrected_text because the model is
     * permitted to fix ASR errors — spec §7.2 — and its own quote is then
     * the better reference. Whitespace is normalized to single spaces.
     */
    private fun spanMatches(
        span: String,
        segmentId: String,
        inputTextById: Map<String, String>,
        resultTextById: Map<String, String>
    ): Boolean {
        if (span.isBlank()) return false
        val needle = normalize(span)
        val haystacks = listOfNotNull(inputTextById[segmentId], resultTextById[segmentId])
            .map(::normalize)
        return haystacks.any { it.contains(needle) }
    }

    private fun normalize(s: String): String =
        s.replace(Regex("\\s+"), " ").trim()
}

data class EvidenceValidationResult(
    val schemaPassed: Boolean,
    val missingKpSegmentRefs: List<String>,
    val missingQuizSegmentRefs: List<String>,
    val missingRelatedKpRefs: List<String>,
    val spanMismatches: List<EvidenceSpanMismatch>,
    val evidenceMatchRate: Double
)

data class EvidenceSpanMismatch(
    val ownerKind: String, // "knowledge_point" | "quiz"
    val ownerId: String,
    val segmentId: String,
    val span: String
)
