package com.classmate.core.evidence

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult

/**
 * Verifies the evidence chain described in spec §11.
 *
 * Three checks, run in order:
 *   1. Every sourceSegmentId resolves to a real input segment.
 *   2. Every quiz.relatedKpId resolves to a real knowledge point.
 *   3. Every evidenceSpan appears in the corresponding segment's text.
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
        val inputSegmentIds = input.segments.map { it.segmentId }.toSet()
        val inputSegmentTextById: Map<String, String> =
            input.segments.associate { it.segmentId to it.text }
        val resultSegmentTextById: Map<String, String> =
            result.segments.associate { it.segmentId to it.correctedText }
        val kpIds = result.segments
            .flatMap { it.knowledgePoints }
            .map { it.kpId }
            .toSet()

        val missingKpSegmentRefs = mutableListOf<String>()
        val missingQuizSegmentRefs = mutableListOf<String>()
        val missingRelatedKpRefs = mutableListOf<String>()
        val spanMismatches = mutableListOf<EvidenceSpanMismatch>()

        var spansChecked = 0
        var spansMatched = 0

        // (1) + (3) for knowledge points
        result.segments.forEach { seg ->
            seg.knowledgePoints.forEach { kp ->
                if (kp.sourceSegmentId !in inputSegmentIds) {
                    missingKpSegmentRefs += "kp ${kp.kpId} -> ${kp.sourceSegmentId}"
                }
                spansChecked++
                if (spanMatches(kp.evidenceSpan, kp.sourceSegmentId, inputSegmentTextById, resultSegmentTextById)) {
                    spansMatched++
                } else {
                    spanMismatches += EvidenceSpanMismatch(
                        ownerKind = "knowledge_point",
                        ownerId = kp.kpId,
                        segmentId = kp.sourceSegmentId,
                        span = kp.evidenceSpan
                    )
                }
            }
        }

        // (1) + (2) + (3) for quizzes
        result.quizzes.forEach { quiz ->
            if (quiz.sourceSegmentId !in inputSegmentIds) {
                missingQuizSegmentRefs += "quiz ${quiz.quizId} -> ${quiz.sourceSegmentId}"
            }
            if (quiz.relatedKpId !in kpIds) {
                missingRelatedKpRefs += "quiz ${quiz.quizId} -> ${quiz.relatedKpId}"
            }
            spansChecked++
            if (spanMatches(quiz.evidenceSpan, quiz.sourceSegmentId, inputSegmentTextById, resultSegmentTextById)) {
                spansMatched++
            } else {
                spanMismatches += EvidenceSpanMismatch(
                    ownerKind = "quiz",
                    ownerId = quiz.quizId,
                    segmentId = quiz.sourceSegmentId,
                    span = quiz.evidenceSpan
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
     * model's correctedText. correctedText is allowed because the model may
     * have fixed ASR errors (spec §7.2) and its own quote is then the better
     * reference. Whitespace is normalized to single spaces.
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
