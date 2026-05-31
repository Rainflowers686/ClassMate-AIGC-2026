package com.classmate.core.provider

import com.classmate.core.model.CourseSession
import com.classmate.core.model.Difficulty
import com.classmate.core.model.Importance
import com.classmate.core.parser.WireAnalysis
import com.classmate.core.parser.WireKnowledgePoint
import com.classmate.core.parser.WireQuizOption
import com.classmate.core.parser.WireQuizQuestion

/**
 * A deliberately BOUNDED, deterministic, offline heuristic. It is NOT language understanding
 * — it picks a concept-like sentence per segment, derives a short label, and builds simple
 * but valid, evidence-bound micro-tests. Its only job is to guarantee the app is never empty
 * when the model is unavailable. The real intelligence is BlueLM; this is the safety net.
 *
 * It emits the same [WireAnalysis] contract as the model, so the fallback path is parsed and
 * validated exactly like a real response (and is rejected if it somehow produces junk).
 */
class LocalHeuristicExtractor {

    private val conceptKeywords = listOf(
        "定义", "是指", "称为", "叫做", "记作", "收敛", "发散", "定理",
        "判别", "公式", "性质", "条件", "概念", "原理", "充要", "充分",
    )

    fun extract(
        session: CourseSession,
        maxKnowledgePoints: Int,
        questionsPerKnowledgePoint: Int,
    ): WireAnalysis {
        val kps = mutableListOf<WireKnowledgePoint>()
        val seen = mutableSetOf<String>()
        for (seg in session.segments) {
            if (kps.size >= maxKnowledgePoints) break
            val quote = pickConceptSentence(seg.text) ?: continue
            val title = deriveTitle(quote)
            if (title.length < 2) continue
            if (!seen.add(title.take(6))) continue // merge near-duplicates by leading label
            kps += WireKnowledgePoint(
                title = title,
                summary = quote.trim(),
                sourceSegmentId = seg.id,
                evidenceQuotes = listOf(quote), // exact substring -> resolves to a real span
                importance = guessImportance(quote).name,
                difficulty = guessDifficulty(quote).name,
            )
        }
        return WireAnalysis(kps, buildQuestions(kps, questionsPerKnowledgePoint))
    }

    // --- sentence handling (returns exact substrings so evidence resolves) ---

    private fun sentenceRanges(text: String): List<IntRange> {
        val delims = setOf('。', '！', '？', '；', '\n', '!', '?', ';')
        val ranges = mutableListOf<IntRange>()
        var start = 0
        for (i in text.indices) {
            if (text[i] in delims) {
                if (i >= start) ranges += start..i
                start = i + 1
            }
        }
        if (start <= text.lastIndex) ranges += start..text.lastIndex
        return ranges
    }

    private fun pickConceptSentence(text: String): String? {
        val ranges = sentenceRanges(text)
        if (ranges.isEmpty()) return null
        val scored = ranges
            .map { r -> text.substring(r.first, r.last + 1) }
            .filter { it.trim().length >= 6 }
            .map { s ->
                val score = (if (conceptKeywords.any { it in s }) 2 else 0) +
                    (if (s.trim().length in 8..140) 1 else 0)
                s to score
            }
        return scored.maxByOrNull { it.second }?.first
    }

    private fun deriveTitle(sentence: String): String {
        val s = sentence.trim().removePrefix("所谓").trim()
        Regex("(.{2,16}?)的(定义|概念|性质)").find(s)?.let { return it.groupValues[1].cleanTitle() }
        Regex("称(.{2,16}?)为").find(s)?.let { return it.groupValues[1].cleanTitle() }
        Regex("(.{2,16}?)(是指|称为|叫做|记作)").find(s)?.let { return it.groupValues[1].cleanTitle() }
        Regex("(.{2,16}?)是").find(s)?.let {
            val cand = it.groupValues[1].cleanTitle()
            if (cand.length in 2..16) return cand
        }
        val head = s.takeWhile { it != '，' && it != ',' && it != '：' && it != ':' && it != '。' }
        return head.cleanTitle()
    }

    private fun String.cleanTitle(): String = trim().trim('，', ',', '。', '、', '：', ':', ' ').take(16)

    private fun guessImportance(s: String): Importance = when {
        listOf("定理", "判别", "充要", "必", "核心").any { it in s } -> Importance.HIGH
        listOf("定义", "概念", "性质").any { it in s } -> Importance.MEDIUM
        else -> Importance.MEDIUM
    }

    private fun guessDifficulty(s: String): Difficulty = when {
        listOf("判别", "证明", "定理", "充要", "收敛半径").any { it in s } -> Difficulty.HARD
        listOf("定义", "概念", "例如", "例").any { it in s } -> Difficulty.EASY
        else -> Difficulty.MEDIUM
    }

    // --- questions: concept-discrimination MCQ (not "which line matches the original") ---

    private fun buildQuestions(kps: List<WireKnowledgePoint>, perKp: Int): List<WireQuizQuestion> {
        if (kps.isEmpty()) return emptyList()
        return kps.mapIndexed { index, kp ->
            if (kps.size >= 2) buildConceptQuestion(kps, index) else buildJudgmentQuestion(kp)
        }
    }

    private fun buildConceptQuestion(kps: List<WireKnowledgePoint>, index: Int): WireQuizQuestion {
        val kp = kps[index]
        val correct = kp.summary
        val distractors = kps.filterIndexed { i, _ -> i != index }
            .map { it.summary }
            .filter { it != correct }
            .distinct()
            .take(3)
        val optionTexts = (listOf(correct) + distractors).distinct()
        val rotated = optionTexts.rotate(index % optionTexts.size)
        val options = rotated.map { txt ->
            WireQuizOption(
                text = txt,
                isCorrect = txt == correct,
                rationale = if (txt == correct) {
                    "与「${kp.title}」的课堂表述一致。"
                } else {
                    "该表述对应的是另一个知识点，不是「${kp.title}」。"
                },
            )
        }
        return WireQuizQuestion(
            type = "CONCEPT_UNDERSTANDING",
            stem = "下列哪一项最准确地描述了「${kp.title}」？",
            options = options,
            testedKnowledgePoints = listOf(kp.title),
            evidenceQuotes = kp.evidenceQuotes,
            explanation = "「${kp.title}」对应课堂中的表述：${kp.summary}",
            difficulty = kp.difficulty,
        )
    }

    private fun buildJudgmentQuestion(kp: WireKnowledgePoint): WireQuizQuestion = WireQuizQuestion(
        type = "JUDGMENT",
        stem = "判断：下面关于「${kp.title}」的说法是否符合本节课内容？\n${kp.summary}",
        options = listOf(
            WireQuizOption("符合", true, "这正是课堂中给出的表述。"),
            WireQuizOption("不符合", false, "课堂原文支持该说法，故此项不正确。"),
        ),
        testedKnowledgePoints = listOf(kp.title),
        evidenceQuotes = kp.evidenceQuotes,
        explanation = "课堂依据：${kp.summary}",
        difficulty = kp.difficulty,
    )

    private fun List<String>.rotate(by: Int): List<String> =
        if (isEmpty()) this else drop(by) + take(by)
}
