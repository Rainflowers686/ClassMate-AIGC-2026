package com.classmate.core.evidence

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult

/**
 * Verifies the evidence chain described in spec §11.
 *
 * Checks performed:
 *   1. Every sourceSegmentId resolves to a real INPUT segment (truth source).
 *   2. Every quiz.relatedKpId resolves to a real knowledge point.
 *   3. Every evidenceSpan match — measured TWO ways:
 *      - strict: span appears in the original input segment text;
 *      - lenient: span appears in input OR the model's correctedText.
 *
 * The strict rate is the honest "is the model quoting real source" number.
 * The lenient rate tolerates ASR-fix rewrites. The UI surfaces both so a
 * reviewer can never see "structure invalid + 100% match" again.
 *
 * The validator does NOT raise on mismatch — it reports. The UI is meant to
 * downgrade affected highlights gracefully (spec §11.2 item 4 / §14.3); a
 * hard error here would defeat the demo-fallback story.
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
        var spansStrictMatched = 0
        var spansLenientMatched = 0

        result.segments.forEach { seg ->
            seg.knowledgePoints.forEach { kp ->
                if (kp.sourceSegmentId !in inputSegmentIds) {
                    missingKpSegmentRefs += "kp ${kp.kpId} -> ${kp.sourceSegmentId}"
                }
                spansChecked++
                val strict = spanInInputOnly(kp.evidenceSpan, kp.sourceSegmentId, inputSegmentTextById)
                val lenient = strict ||
                    spanInResult(kp.evidenceSpan, kp.sourceSegmentId, resultSegmentTextById)
                if (strict) spansStrictMatched++
                if (lenient) spansLenientMatched++
                if (!lenient) {
                    spanMismatches += EvidenceSpanMismatch(
                        ownerKind = "knowledge_point",
                        ownerId = kp.kpId,
                        segmentId = kp.sourceSegmentId,
                        span = kp.evidenceSpan
                    )
                }
            }
        }

        result.quizzes.forEach { quiz ->
            if (quiz.sourceSegmentId !in inputSegmentIds) {
                missingQuizSegmentRefs += "quiz ${quiz.quizId} -> ${quiz.sourceSegmentId}"
            }
            if (quiz.relatedKpId !in kpIds) {
                missingRelatedKpRefs += "quiz ${quiz.quizId} -> ${quiz.relatedKpId}"
            }
            spansChecked++
            val strict = spanInInputOnly(quiz.evidenceSpan, quiz.sourceSegmentId, inputSegmentTextById)
            val lenient = strict ||
                spanInResult(quiz.evidenceSpan, quiz.sourceSegmentId, resultSegmentTextById)
            if (strict) spansStrictMatched++
            if (lenient) spansLenientMatched++
            if (!lenient) {
                spanMismatches += EvidenceSpanMismatch(
                    ownerKind = "quiz",
                    ownerId = quiz.quizId,
                    segmentId = quiz.sourceSegmentId,
                    span = quiz.evidenceSpan
                )
            }
        }

        val strictRate = if (spansChecked == 0) 1.0 else spansStrictMatched.toDouble() / spansChecked
        val lenientRate = if (spansChecked == 0) 1.0 else spansLenientMatched.toDouble() / spansChecked

        return EvidenceValidationResult(
            schemaPassed = missingKpSegmentRefs.isEmpty()
                && missingQuizSegmentRefs.isEmpty()
                && missingRelatedKpRefs.isEmpty(),
            missingKpSegmentRefs = missingKpSegmentRefs,
            missingQuizSegmentRefs = missingQuizSegmentRefs,
            missingRelatedKpRefs = missingRelatedKpRefs,
            spanMismatches = spanMismatches,
            strictEvidenceMatchRate = strictRate,
            lenientEvidenceMatchRate = lenientRate
        )
    }

    private fun spanInInputOnly(
        span: String,
        segmentId: String,
        inputTextById: Map<String, String>
    ): Boolean {
        if (span.isBlank()) return false
        val haystack = inputTextById[segmentId] ?: return false
        return normalize(haystack).contains(normalize(span))
    }

    private fun spanInResult(
        span: String,
        segmentId: String,
        resultTextById: Map<String, String>
    ): Boolean {
        if (span.isBlank()) return false
        val haystack = resultTextById[segmentId] ?: return false
        return normalize(haystack).contains(normalize(span))
    }

    private fun normalize(s: String): String =
        s.replace(Regex("\\s+"), " ").trim()
}
