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
 * v0.4 visual QA pass:
 *  - KP names are now derived from the picked sentence (trimmed +
 *    leading-filler stripped), not just the hotword. Three demo segments
 *    that all contain "泰勒公式" no longer collapse to three KPs named
 *    "泰勒公式".
 *  - Review-plan tasks rotate across five distinct templates so the same
 *    course doesn't render as N copies of "复述并解释 …".
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

        val quizCount = knowledgePoints.size.coerceIn(0, maxQuizzes)
        val quizzes: List<Quiz> = knowledgePoints.take(quizCount).mapIndexed { idx, kp ->
            buildQuiz(ordinal = idx + 1, kp = kp, inputSegment = input.segments[idx])
        }

        val planTargets = knowledgePoints.sortedByDescending { it.importance }
            .take(maxQuizzes)
        val reviewPlan: List<ReviewPlanItem> = planTargets.mapIndexed { idx, kp ->
            ReviewPlanItem(
                stepId = "rp_%03d".format(idx + 1),
                durationMinutes = 5,
                task = reviewTaskFor(idx, kp),
                relatedKpIds = listOf(kp.kpId),
                reason = reviewReasonFor(idx, kp)
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
        val importance = ((seg.text.length / 60).coerceIn(0, 4)) + 1
        val difficulty = (((seg.text.length / 40) + ordinal) % 5).coerceIn(0, 4) + 1
        return KnowledgePoint(
            kpId = "kp_%03d".format(ordinal),
            name = name,
            importance = importance,
            difficulty = difficulty,
            sourceSegmentId = seg.segmentId,
            evidenceSpan = span,
            explanation = explanationFor(seg)
        )
    }

    private fun buildQuiz(
        ordinal: Int,
        kp: KnowledgePoint,
        inputSegment: InputSegment
    ): Quiz {
        val correct = kp.name
        val distractors = listOf(
            "与本段主题无关的说法",
            "对「${kp.name}」的常见误解",
            "另一段课程里出现的次要观点"
        )
        return Quiz(
            quizId = "q_%03d".format(ordinal),
            question = "在这一段课程中，下列哪一项最贴近原文表述？",
            options = listOf(correct) + distractors,
            answerIndex = 0,
            explanation = "这是「${kp.name}」对应的原话；可在下方原文依据中对照。",
            sourceSegmentId = inputSegment.segmentId,
            relatedKpId = kp.kpId,
            evidenceSpan = kp.evidenceSpan
        )
    }

    private fun buildSummary(input: CourseAnalysisInput): String {
        val title = input.courseTitle.ifBlank { "本节内容" }
        val n = input.segments.size
        val hot = if (input.hotwords.isEmpty())
            ""
        else
            "；重点词：${input.hotwords.take(5).joinToString("、")}"
        return "已从《$title》共 $n 段课程中提取知识点$hot。所有知识点和题目均直接对应到原文段落。"
    }

    /**
     * Friendly explanation copy for a knowledge point — references the
     * segment as "第 N 段（时间段）" instead of leaking the raw `seg_xxx`
     * identifier so users see something readable.
     */
    private fun explanationFor(seg: InputSegment): String {
        val human = humanSegmentLabel(seg.segmentId)
        return "本知识点来自$human（${seg.timeRange}），可对照下方的原文依据快速回顾。"
    }

    private fun humanSegmentLabel(segmentId: String): String {
        val digits = segmentId.trim().removePrefix("seg_").trimStart('0')
        val n = digits.toIntOrNull()
        return if (n != null && n > 0) "第 $n 段" else segmentId
    }

    /**
     * Derive a short, sentence-style KP name from the picked span. Keeps
     * neighbouring KPs distinct even when they share a hotword.
     */
    private fun deriveName(span: String, hotwords: List<String>): String {
        var s = span.trim()
        if (s.isEmpty()) {
            // last resort: pick a hotword or generic
            return hotwords.firstOrNull { it.isNotBlank() } ?: "本段要点"
        }
        // strip leading conversational fillers
        for (lead in LEADING_FILLERS) {
            if (s.startsWith(lead)) {
                s = s.removePrefix(lead).trimStart('，', '、', ' ')
            }
        }
        // strip trailing CJK punctuation so the title doesn't end mid-sentence
        s = s.trimEnd('。', '！', '？', '!', '?', '；', ';', '，', ',')
        // cap to a comfortable mobile title length
        if (s.length > 28) s = s.take(27) + "…"
        return s.ifBlank { hotwords.firstOrNull { it.isNotBlank() } ?: "本段要点" }
    }

    private fun reviewTaskFor(stepIdx: Int, kp: KnowledgePoint): String {
        val seg = humanSegmentLabel(kp.sourceSegmentId)
        return when (stepIdx % REVIEW_TASK_TEMPLATES.size) {
            0 -> "复述「${kp.name}」的核心结论"
            1 -> "用自己的话解释「${kp.name}」为什么成立"
            2 -> "举一个与「${kp.name}」相关的实际例子"
            3 -> "回到$seg 的原文，验证「${kp.name}」"
            else -> "围绕「${kp.name}」给自己出一道判断题"
        }
    }

    private fun reviewReasonFor(stepIdx: Int, kp: KnowledgePoint): String {
        val seg = humanSegmentLabel(kp.sourceSegmentId)
        return when {
            kp.importance >= 4 -> "这是本节的高重要度知识点（${kp.importance}/5），建议优先巩固。"
            kp.difficulty >= 4 -> "这条知识点偏难（${kp.difficulty}/5），多角度复述能加深理解。"
            stepIdx == 0 -> "这是本节排序最靠前的知识点，先从它开始。"
            else -> "建议结合 $seg 的原文一并回顾，避免遗漏细节。"
        }
    }

    companion object {
        private val LEADING_FILLERS = listOf(
            "今天我们分别看", "今天我们来看", "今天我们看", "今天我们",
            "接下来我们", "接下来", "我们来看", "我们看", "我们", "下面",
            "首先", "其次", "然后", "另外", "因此", "所以",
            "当我们", "当", "如果", "假设", "事实上",
        )
        private val REVIEW_TASK_TEMPLATES = listOf(0, 1, 2, 3, 4) // 5 templates
    }
}

/**
 * Picks a verbatim substring of segment text to use as evidence_span.
 *
 * Priority:
 *  1. The first sentence containing a hotword, returned verbatim.
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

        val firstHotInText = hotwords.asSequence()
            .filter { it.isNotBlank() }
            .firstOrNull { trimmed.contains(it) }
        if (firstHotInText != null) {
            val sentence = sentenceContaining(trimmed, firstHotInText)
            if (sentence.isNotBlank()) return sentence
            return firstHotInText
        }

        var cursor = 0
        while (cursor < trimmed.length) {
            val m = SENTENCE_END.find(trimmed, cursor)
            val endExclusive = if (m == null) trimmed.length else m.range.last + 1
            val candidate = trimmed.substring(cursor, endExclusive).trim()
            if (candidate.length >= 10) return candidate
            if (m == null) break
            cursor = endExclusive
        }

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
