package com.classmate.core.adapter

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSegment
import com.classmate.core.model.InputSegment
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.Quiz
import com.classmate.core.model.ReviewPlanItem

/**
 * Pure-rule local provider. Replaces the v0.3.5 DemoProvider (which was
 * static replay of demo_output.json and ignored the input).
 *
 * Guarantees produced by construction — no model call, no network:
 *  1. Every result.segment_id is taken from input.segments (1:1).
 *  2. Every knowledge_point.source_segment_id is in input.segments.
 *  3. Every quiz.source_segment_id is in input.segments.
 *  4. Every quiz.related_kp_id resolves to a generated kp_id.
 *  5. Every review_plan.related_kp_ids resolves to a generated kp_id.
 *  6. Every evidence_span is a verbatim substring of the corresponding
 *     input.segment.text (picked by the SpanPicker below).
 *
 * That means ResultValidator + EvidenceValidator (strict) will pass with
 * matchRate == 1.0 on ANY non-empty input — which is the whole point: the
 * demo path is now itself a worked example of the evidence-chain claim.
 *
 * Style intentionally template-y rather than NLP-y; this is a deterministic
 * floor that the real Provider must clear.
 */
class LocalRuleProvider(
    private val maxQuizzes: Int = 5,
    private val minQuizzes: Int = 3
) : ModelProvider {

    override val name: String = "local"

    override suspend fun analyzeCourse(input: CourseAnalysisInput): CourseAnalysisResult {
        require(input.segments.isNotEmpty()) {
            "LocalRuleProvider requires at least one input segment"
        }

        val segments: List<CourseSegment> = input.segments.mapIndexed { idx, seg ->
            val kpOrdinal = idx + 1
            val kp = buildKnowledgePoint(
                ordinal = kpOrdinal,
                seg = seg,
                hotwords = input.hotwords
            )
            CourseSegment(
                segmentId = seg.segmentId,
                timeRange = seg.timeRange,
                correctedText = seg.text,
                knowledgePoints = listOf(kp),
                confusionPoints = emptyList()
            )
        }

        val knowledgePoints: List<KnowledgePoint> = segments.flatMap { it.knowledgePoints }

        // Quiz count: clamp to [minQuizzes, maxQuizzes], but never more than
        // we have knowledge points. If fewer than min, just produce as many
        // as possible (the schema's minItems=3 may be violated for a
        // 1-segment input; LocalRuleProvider's job is to stay internally
        // consistent, not to fake content).
        val quizCount = knowledgePoints.size.coerceIn(0, maxQuizzes).let {
            if (it >= minQuizzes) it else it
        }
        val quizzes: List<Quiz> = knowledgePoints.take(quizCount).mapIndexed { idx, kp ->
            buildQuiz(ordinal = idx + 1, kp = kp, inputSegment = input.segments[idx])
        }

        // Review plan: importance-desc; produce up to 5 steps, but only ones
        // that map to a real KP so the validator is satisfied.
        val planTargets = knowledgePoints.sortedByDescending { it.importance }
            .take(maxQuizzes)
        val reviewPlan: List<ReviewPlanItem> = planTargets.mapIndexed { idx, kp ->
            ReviewPlanItem(
                stepId = "rp_%03d".format(idx + 1),
                durationMinutes = 5,
                task = "复述并解释「${kp.name}」",
                relatedKpIds = listOf(kp.kpId),
                reason = "Importance ${kp.importance}/5；本地兜底建议优先巩固高重要度知识点。"
            )
        }

        return CourseAnalysisResult(
            courseTitle = input.courseTitle.ifBlank { "Untitled" },
            summary = buildSummary(input),
            segments = segments,
            quizzes = quizzes,
            reviewPlan = reviewPlan
        )
    }

    // ---------------------------------------------------------------------
    // builders
    // ---------------------------------------------------------------------

    private fun buildKnowledgePoint(
        ordinal: Int,
        seg: InputSegment,
        hotwords: List<String>
    ): KnowledgePoint {
        val span = SpanPicker.pick(seg.text, hotwords)
        val name = deriveName(span, hotwords)
        // Importance / difficulty: derive deterministically from text length
        // so the output is stable across runs but not all identical.
        val importance = ((seg.text.length / 60).coerceIn(0, 4)) + 1
        val difficulty = (((seg.text.length / 40) + ordinal) % 5).coerceIn(0, 4) + 1
        return KnowledgePoint(
            kpId = "kp_%03d".format(ordinal),
            name = name,
            importance = importance,
            difficulty = difficulty,
            sourceSegmentId = seg.segmentId,
            evidenceSpan = span,
            explanation = "本知识点来自 ${seg.segmentId}，可对照原文段落理解。"
        )
    }

    private fun buildQuiz(
        ordinal: Int,
        kp: KnowledgePoint,
        inputSegment: InputSegment
    ): Quiz {
        val correct = kp.name
        val distractors = listOf(
            "与「${kp.name}」无关的内容",
            "对「${kp.name}」的常见误解",
            "另一段落的主要观点"
        )
        return Quiz(
            quizId = "q_%03d".format(ordinal),
            question = "下列哪一项最准确地描述了「${kp.name}」？",
            options = listOf(correct) + distractors,
            answerIndex = 0,
            explanation = "正确答案对应 ${kp.kpId}，来源段落 ${kp.sourceSegmentId}。",
            sourceSegmentId = inputSegment.segmentId,
            relatedKpId = kp.kpId,
            evidenceSpan = kp.evidenceSpan
        )
    }

    private fun buildSummary(input: CourseAnalysisInput): String {
        val title = input.courseTitle.ifBlank { "本节内容" }
        val n = input.segments.size
        val hot = if (input.hotwords.isEmpty()) "" else "，重点词：${input.hotwords.take(5).joinToString("、")}"
        return "本地兜底总结：$title 共 $n 段$hot。"
    }

    private fun deriveName(span: String, hotwords: List<String>): String {
        val matchedHot = hotwords.firstOrNull { it.isNotBlank() && span.contains(it) }
        if (matchedHot != null) return matchedHot
        // Use the first ≤24 chars of the picked span as a fallback name.
        val cap = span.trim().take(24)
        return if (cap.isEmpty()) "本段要点" else cap
    }
}

/**
 * Picks a verbatim substring of segment text to use as evidence_span.
 *
 * Priority:
 *  1. The first hotword that occurs in the segment, returned verbatim.
 *  2. The first sentence (split on CJK / ASCII enders) ≥ 10 chars.
 *  3. The first 40 chars of the segment.
 *
 * The returned span is GUARANTEED to be a substring of the given text, so
 * EvidenceValidator (strict) will match.
 */
internal object SpanPicker {
    private val SENTENCE_END = Regex("[。！？!?；;]")

    fun pick(text: String, hotwords: List<String>): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        // 1) prefer the first sentence containing a hotword
        val firstHotInText = hotwords.asSequence()
            .filter { it.isNotBlank() }
            .firstOrNull { trimmed.contains(it) }
        if (firstHotInText != null) {
            val sentence = sentenceContaining(trimmed, firstHotInText)
            if (sentence.isNotBlank()) return sentence
            return firstHotInText
        }

        // 2) first sentence ≥ 10 chars
        var cursor = 0
        while (cursor < trimmed.length) {
            val m = SENTENCE_END.find(trimmed, cursor)
            val endExclusive = if (m == null) trimmed.length else m.range.last + 1
            val candidate = trimmed.substring(cursor, endExclusive).trim()
            if (candidate.length >= 10) return candidate
            if (m == null) break
            cursor = endExclusive
        }

        // 3) fallback
        return trimmed.take(40)
    }

    private fun sentenceContaining(text: String, needle: String): String {
        val idx = text.indexOf(needle)
        if (idx < 0) return ""
        var start = 0
        var end = text.length
        SENTENCE_END.findAll(text).forEach { m ->
            val pos = m.range.last + 1
            if (pos <= idx) start = pos
            if (pos > idx && end == text.length) end = pos
        }
        return text.substring(start, end).trim()
    }
}
