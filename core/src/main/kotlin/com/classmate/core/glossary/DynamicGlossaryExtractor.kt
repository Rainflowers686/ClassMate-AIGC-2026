package com.classmate.core.glossary

/**
 * On-device, deterministic glossary extraction. The built-in subject term packs are only a STARTER
 * pack — any course (机械 / 医学 / 法学 / 经管 / 语言 / 环境 / 材料 / 电子 / 控制 …) must be able to
 * build its own glossary from the learner's own material, and the learner must be able to pin / add
 * their own terms. Output feeds prompt context, the ASR/OCR post-processors, quiz generation, wrong
 * explanations, study-pack export and query rewrite.
 *
 * Honesty / safety: this is a heuristic over the user's OWN text. It never invents definitions, never
 * calls a network, and never logs long user text — [DynamicGlossaryTerm.examples] are short capped
 * snippets so a glossary surface can show context without dumping the source.
 */
data class DynamicGlossaryTerm(
    val term: String,
    val normalizedTerm: String,
    val aliases: List<String> = emptyList(),
    val domain: String,
    val sourceEvidenceId: String? = null,
    val confidence: Double,
    val isUserPinned: Boolean = false,
    val examples: List<String> = emptyList(),
)

/** One unit of source material with an id so an extracted term can point back at where it came from. */
data class GlossarySource(val evidenceId: String, val text: String)

object DynamicGlossaryExtractor {

    private const val MIN_CJK_LEN = 2
    private const val MAX_CJK_LEN = 6
    private const val EXAMPLE_MAX_CHARS = 48
    private const val DEFAULT_MAX = 40

    private val dictionaryConfidence = 0.85
    private val pinnedConfidence = 1.0

    /**
     * Extract a bounded glossary for [domain] from [sources].
     *  - [seedTerms] (e.g. CourseDomainDetector.seedTermsFor(domain)) are matched first with high
     *    confidence so a known domain gets clean canonical terms.
     *  - Novel ASCII tech tokens and short CJK chunks are mined by frequency for ANY domain.
     *  - [pinned] (user-added / user-pinned) always win and are returned first.
     */
    fun extract(
        domain: String,
        sources: List<GlossarySource>,
        seedTerms: List<String> = emptyList(),
        pinned: List<DynamicGlossaryTerm> = emptyList(),
        max: Int = DEFAULT_MAX,
    ): List<DynamicGlossaryTerm> {
        val out = LinkedHashMap<String, DynamicGlossaryTerm>()

        // 1) user-pinned always included, highest confidence, dedup key = normalized term.
        for (p in pinned) {
            val norm = normalize(p.term)
            if (norm.isBlank()) continue
            out[norm] = p.copy(normalizedTerm = norm, isUserPinned = true, confidence = pinnedConfidence, domain = domain)
        }

        // 2) dictionary seed pass — canonical, high confidence.
        for (seed in seedTerms) {
            val norm = normalize(seed)
            if (norm.isBlank() || out.containsKey(norm)) continue
            val hit = sources.firstOrNull { it.text.contains(seed, ignoreCase = true) } ?: continue
            out[norm] = DynamicGlossaryTerm(
                term = seed,
                normalizedTerm = norm,
                domain = domain,
                sourceEvidenceId = hit.evidenceId,
                confidence = dictionaryConfidence,
                examples = listOfNotNull(snippet(hit.text, seed)),
            )
        }

        // 3) frequency-mined candidates (ASCII tech tokens + short CJK chunks) for novel terms.
        val frequency = LinkedHashMap<String, MutableCandidate>()
        for (source in sources) {
            for (candidate in asciiTokens(source.text) + cjkChunks(source.text)) {
                val norm = normalize(candidate)
                if (norm.isBlank() || norm in STOPWORDS) continue
                val entry = frequency.getOrPut(norm) { MutableCandidate(candidate, source.evidenceId, source.text) }
                entry.count++
            }
        }
        frequency.values
            .sortedWith(compareByDescending<MutableCandidate> { it.count }.thenByDescending { it.display.length })
            .forEach { cand ->
                val norm = normalize(cand.display)
                if (out.containsKey(norm)) return@forEach
                out[norm] = DynamicGlossaryTerm(
                    term = cand.display,
                    normalizedTerm = norm,
                    domain = domain,
                    sourceEvidenceId = cand.evidenceId,
                    confidence = minOf(0.4 + 0.1 * (cand.count - 1), 0.7),
                    examples = listOfNotNull(snippet(cand.sourceText, cand.display)),
                )
            }

        return out.values
            .sortedWith(compareByDescending<DynamicGlossaryTerm> { it.isUserPinned }.thenByDescending { it.confidence })
            .take(max.coerceAtLeast(0))
    }

    private class MutableCandidate(val display: String, val evidenceId: String, val sourceText: String) {
        var count: Int = 0
    }

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")

    /** ASCII technical tokens: code identifiers, ALLCAPS, things with digits/underscore, length>=3. */
    private fun asciiTokens(text: String): List<String> =
        Regex("[A-Za-z][A-Za-z0-9_+#.]{2,}").findAll(text)
            .map { it.value.trim('.', '_') }
            .filter { token ->
                val lower = token.lowercase()
                lower.length >= 3 && lower !in ASCII_STOPWORDS &&
                    (token.any { it.isDigit() } || token.contains('_') ||
                        token.any { it.isUpperCase() } || token.length >= 4)
            }
            .toList()

    /** Short CJK chunks split on punctuation / whitespace / ASCII; kept when length is 2..6. */
    private fun cjkChunks(text: String): List<String> =
        text.split(Regex("[^\\u4e00-\\u9fa5]+"))
            .map { it.trim() }
            .filter { it.length in MIN_CJK_LEN..MAX_CJK_LEN }

    /** A short, capped context snippet containing [term] — never the whole source text. */
    private fun snippet(source: String, term: String): String? {
        val idx = source.indexOf(term, ignoreCase = true)
        if (idx < 0) return null
        val start = (idx - 12).coerceAtLeast(0)
        val end = (idx + term.length + 24).coerceAtMost(source.length)
        return source.substring(start, end).trim().replace(Regex("\\s+"), " ").take(EXAMPLE_MAX_CHARS)
    }

    private val ASCII_STOPWORDS = setOf(
        "the", "and", "for", "with", "this", "that", "you", "are", "but", "not", "can", "will",
        "from", "have", "has", "was", "were", "all", "any", "out", "use", "via", "per", "etc",
    )

    private val STOPWORDS = setOf(
        "的", "了", "和", "与", "这", "那", "我们", "可以", "因为", "所以", "一个", "进行", "通过",
        "以及", "重要", "内容", "学习", "课程", "老师", "同学", "知识", "问题", "方法", "例如", "如下",
        "部分", "什么", "如何", "需要", "包括", "并且", "但是", "或者", "这些", "那些", "本节", "本课",
    )
}
