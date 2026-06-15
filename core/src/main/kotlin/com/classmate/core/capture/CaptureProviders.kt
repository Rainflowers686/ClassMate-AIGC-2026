package com.classmate.core.capture

/**
 * Capture provider gateway interfaces (kept out of the UI) plus the always-available LOCAL evidence
 * retriever. The Vivo HTTP implementations live in VivoCaptureProviders.kt; each returns
 * [CaptureError.ConfigMissing] when unconfigured so the manual / on-device paths keep working.
 */

// ── ASR ────────────────────────────────────────────────────────────────────────────────────────────
/** Audio in to recognized utterances. Long-audio (1739) is the primary; streaming profiles are reserved. */
interface SpeechToTextProvider {
    val isConfigured: Boolean

    /**
     * Run the long-audio transcription flow (create → slice-upload → run → poll → result) and return the
     * recognized utterances. [audioFormat] follows the doc: "pcm" for pcm, otherwise "auto". [onProgress]
     * receives 0..100 during polling so the UI can show progress without blocking the main thread.
     */
    fun transcribeLongAudio(
        audioBytes: ByteArray,
        fileName: String,
        audioFormat: String,
        onProgress: (Int) -> Unit = {},
    ): CaptureResult<AsrTranscriptResult>
}

// ── OCR ──────────────────────────────────────────────────────────────────────────────────────────
/** Image bytes to recognized text blocks (1737 general OCR). */
interface OcrProvider {
    val isConfigured: Boolean
    fun recognize(imageBytes: ByteArray): CaptureResult<OcrResult>
}

// ── Retrieval (reserved Vivo providers; local retriever below is always available) ───────────────────
/** Rerank candidate sentences against a query (2060 文本相似度). Returns one score per candidate. */
interface TextSimilarityProvider {
    val isConfigured: Boolean
    fun similarity(query: String, candidates: List<String>): CaptureResult<List<Double>>
}

/** Rewrite a question into a better retrieval query using prior Q/A (2061 查询改写). */
interface QueryRewriteProvider {
    val isConfigured: Boolean
    fun rewrite(question: String, history: List<Pair<String, String>> = emptyList()): CaptureResult<String>
}

/** Batch sentence embeddings (1734 文本向量). Reserved for course-library search / clustering. */
interface EmbeddingProvider {
    val isConfigured: Boolean
    fun embed(sentences: List<String>): CaptureResult<List<List<Double>>>
}

/** A segment offered to the retriever (decoupled from CourseSession/CourseSegment). */
data class EvidenceSegmentInput(val id: String, val text: String)

/**
 * Always-available local evidence retrieval — no network, no credentials. Scores each segment by token
 * overlap with the question (CJK characters + lowercased latin words), so Ask can pick top evidence even
 * when no official similarity API is configured. Deterministic and unit-testable.
 */
class LocalEvidenceRetriever {

    fun retrieve(question: String, segments: List<EvidenceSegmentInput>, topN: Int = 3): List<EvidenceCandidate> {
        val qTokens = tokenize(question)
        if (qTokens.isEmpty() || segments.isEmpty()) return emptyList()

        val scored = segments.mapNotNull { seg ->
            val sTokens = tokenize(seg.text)
            if (sTokens.isEmpty()) return@mapNotNull null
            val overlap = qTokens.count { it in sTokens }
            if (overlap == 0) return@mapNotNull null
            // Length-normalized overlap so long segments don't dominate; clamped to 0..1.
            val score = overlap.toDouble() / kotlin.math.sqrt(qTokens.size.toDouble() * sTokens.size.toDouble())
            seg to score.coerceIn(0.0, 1.0)
        }
        return scored
            .sortedByDescending { it.second }
            .take(topN.coerceAtLeast(0))
            .mapIndexed { rank, (seg, score) -> EvidenceCandidate(seg.id, seg.text, score, rank) }
    }

    private fun tokenize(text: String): Set<String> {
        val tokens = HashSet<String>()
        val latin = StringBuilder()
        fun flushLatin() {
            if (latin.isNotEmpty()) {
                val w = latin.toString().lowercase()
                if (w.length >= 2) tokens.add(w)
                latin.clear()
            }
        }
        text.forEach { c ->
            when {
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' -> latin.append(c)
                c.code in 0x4E00..0x9FFF -> { flushLatin(); tokens.add(c.toString()) } // CJK: per-char token
                else -> flushLatin()
            }
        }
        flushLatin()
        return tokens
    }
}
