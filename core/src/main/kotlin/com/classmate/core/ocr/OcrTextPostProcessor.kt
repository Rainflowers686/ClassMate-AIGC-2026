package com.classmate.core.ocr

/**
 * Result of cleaning raw OCR text for human reading. [needsReview] is an HONEST signal: when the text
 * looks garbled it stays as-is (we never silently "fix" into nonsense) but the caller is told to ask the
 * user to compare against the original image.
 */
data class OcrCleanResult(
    val text: String,
    val needsReview: Boolean,
    val reviewHint: String = "",
)

data class OcrQualityAssessment(
    val nonWhitespaceCount: Int,
    val suspiciousCharRatio: Double,
    val replacementCharCount: Int,
    val needsReview: Boolean,
    val warning: String = "",
)

/**
 * Best-effort, conservative OCR post-processing so a photo's text is readable instead of a wall of broken
 * lines and ASCII-mangled symbols. It only does SAFE transforms: width normalization, a small set of
 * math-symbol fixes, joining obvious wrapped lines while keeping numbered items / bullets / formula lines
 * on their own line, and whitespace tidy-up. It never invents content; if the garbage ratio is high it
 * flags the text for manual review instead of pretending it is clean.
 */
object OcrTextPostProcessor {

    const val REVIEW_HINT = "识别结果可能不完整或有乱码，请对照原图修改。"
    private const val GARBAGE_THRESHOLD = 0.20

    private val symbolReplacements = listOf(
        Regex("\\s*<\\s*=\\s*") to "≤",
        Regex("\\s*>\\s*=\\s*") to "≥",
        Regex("\\s*!\\s*=\\s*") to "≠",
        Regex("\\s*-{1,2}>\\s*") to "→",
        Regex("\\+/-|\\+-(?=\\d)") to "±",
    )

    fun clean(raw: String): OcrCleanResult {
        if (raw.isBlank()) return OcrCleanResult("", needsReview = false)
        var text = raw.replace("\r\n", "\n").replace("\r", "\n")
        text = normalizeWidth(text)
        symbolReplacements.forEach { (re, rep) -> text = re.replace(text, rep) }
        text = mergeWrappedLines(text)
        text = text.lines().joinToString("\n") { it.replace(Regex("[ \\t]{2,}"), " ").trimEnd() }
        text = text.replace(Regex("\n{3,}"), "\n\n").trim()
        val quality = qualityAssessment(text)
        return OcrCleanResult(text, quality.needsReview, quality.warning)
    }

    fun qualityAssessment(text: String): OcrQualityAssessment {
        val visible = text.filterNot { it.isWhitespace() }
        if (visible.isEmpty()) {
            return OcrQualityAssessment(0, 0.0, 0, needsReview = false)
        }
        val replacement = visible.count { it == '\uFFFD' }
        val suspicious = garbageRatio(text)
        val needsReview = suspicious > GARBAGE_THRESHOLD || replacement > 0
        return OcrQualityAssessment(
            nonWhitespaceCount = visible.length,
            suspiciousCharRatio = suspicious,
            replacementCharCount = replacement,
            needsReview = needsReview,
            warning = if (needsReview) REVIEW_HINT else "",
        )
    }

    /**
     * Full-width letters/digits + ideographic space -> half-width, so mixed-width OCR reads cleanly.
     * Chinese full-width punctuation (：，。！？ etc.) is intentionally LEFT ALONE — converting it would
     * corrupt normal Chinese text.
     */
    private fun normalizeWidth(s: String): String = buildString {
        s.forEach { c ->
            append(
                when (c.code) {
                    in 0xFF10..0xFF19, in 0xFF21..0xFF3A, in 0xFF41..0xFF5A -> (c.code - 0xFEE0).toChar()
                    0x3000 -> ' '
                    else -> c
                },
            )
        }
    }

    private fun mergeWrappedLines(text: String): String {
        val result = mutableListOf<String>()
        for (line in text.lines().map { it.trim() }) {
            if (line.isEmpty()) { result.add(""); continue }
            val prev = result.lastOrNull()
            val canJoin = prev != null && prev.isNotEmpty() &&
                !isStructuralStart(line) && !looksLikeFormula(line) &&
                !endsClause(prev) && !looksLikeFormula(prev)
            if (canJoin) result[result.lastIndex] = prev + line else result.add(line)
        }
        return result.joinToString("\n")
    }

    /** Lines that must keep their own line: numbered items, bullets, circled numbers. */
    private fun isStructuralStart(line: String): Boolean =
        line.matches(Regex("^\\d+[．.、)）].*")) ||
            line.matches(Regex("^[（(]\\d+[）)].*")) ||
            (line.firstOrNull() ?: ' ') in setOf('·', '•', '-', '*', '①', '②', '③', '④', '⑤', '⑥', '⑦', '⑧', '⑨', '⑩')

    private fun looksLikeFormula(line: String): Boolean =
        line.length <= 60 && line.any { it in "=≤≥≠→√×÷±∑∫^><*/+-" }

    private fun endsClause(line: String): Boolean =
        (line.lastOrNull() ?: ' ') in "。！？.!?；;：:"

    private fun garbageRatio(text: String): Double {
        val visible = text.filterNot { it.isWhitespace() }
        if (visible.isEmpty()) return 0.0
        val ok = visible.count { c ->
            c.code in 0x4E00..0x9FFF || c.isLetterOrDigit() ||
                c in "。！？，、；：（）【】「」《》—…·•%°±×÷≤≥≠→√αβγμΔπθλσΩ.,!?;:()[]{}+-*/=<>'\"_@#＆&"
        }
        return 1.0 - ok.toDouble() / visible.length
    }
}
