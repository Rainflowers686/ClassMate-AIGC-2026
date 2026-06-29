package com.classmate.core.evidence

/** How well a stored evidence excerpt actually relates to the thing it is bound to. */
enum class EvidenceRelationLevel { STRONG, WEAK, MISSING }

/**
 * A deliberately *weak* semantic check that flags evidence whose excerpt shares no keywords at all with
 * the knowledge point / question / task it claims to support — the cheap signal of a mis-bound chain.
 *
 * It is conservative on purpose: when either side carries too little signal it returns [STRONG] rather
 * than crying wolf, so it only downgrades evidence that is clearly unrelated (zero overlap). It never
 * upgrades — a missing excerpt is decided by the caller before this runs.
 */
object EvidenceRelation {

    fun assess(excerpt: String, context: String): EvidenceRelationLevel {
        val excerptTokens = significantTokens(excerpt)
        val contextTokens = significantTokens(context)
        // Too little signal to judge -> do not flag (avoid false "weak" on real evidence).
        if (excerptTokens.size < 2 || contextTokens.size < 2) return EvidenceRelationLevel.STRONG
        val shared = excerptTokens.intersect(contextTokens)
        return if (shared.isEmpty()) EvidenceRelationLevel.WEAK else EvidenceRelationLevel.STRONG
    }

    /** ASCII words (>=2 chars) plus CJK character bigrams — robust to Chinese having no word spaces. */
    fun significantTokens(text: String): Set<String> {
        val tokens = mutableSetOf<String>()
        Regex("[a-z0-9]{2,}").findAll(text.lowercase()).forEach { tokens += it.value }
        val han = text.filter { it.code in 0x4E00..0x9FFF }
        for (i in 0 until han.length - 1) {
            tokens += han.substring(i, i + 2)
        }
        return tokens
    }
}
