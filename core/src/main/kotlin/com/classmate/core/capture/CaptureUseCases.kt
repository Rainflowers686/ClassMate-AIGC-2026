package com.classmate.core.capture

import com.classmate.core.model.CourseSession
import com.classmate.core.transcript.TranscriptDraft
import com.classmate.core.transcript.TranscriptLabels
import com.classmate.core.transcript.TranscriptSegmentDraft
import com.classmate.core.transcript.TranscriptSourceType

/**
 * Classroom Capture use cases. The contract that keeps the knowledge base clean: CREATE produces an
 * in-memory editable DRAFT only; CONFIRM is the single step that yields a [ClassroomCaptureResult] — the
 * analysis-ready payload the caller feeds into the EXISTING CourseAnalysis. An unconfirmed draft never
 * yields a ClassroomCaptureResult, so it can never reach persistence/analysis.
 */

// ── Transcript / ASR ───────────────────────────────────────────────────────────────────────────────
class CreateTranscriptDraftUseCase(
    private val asrProvider: SpeechToTextProvider,
    private val idGen: () -> String = { java.util.UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    /** ASR path: recognize long audio into an editable draft. ConfigMissing/failure is returned, not thrown —
     *  the caller falls back to manual paste. */
    fun fromAudio(
        audioBytes: ByteArray,
        fileName: String,
        audioFormat: String,
        title: String,
        onProgress: (Int) -> Unit = {},
    ): CaptureResult<TranscriptDraft> {
        val id = idGen()
        return when (val r = asrProvider.transcribeLongAudio(audioBytes, fileName, audioFormat, onProgress)) {
            is CaptureResult.Success -> CaptureResult.Success(r.value.toTranscriptDraft(id, title, fileName, clock()))
            is CaptureResult.Failure -> r
        }
    }

    /** Manual paste path: always available, no provider needed. Splits non-blank lines into editable segments. */
    fun fromPastedText(text: String, title: String): TranscriptDraft {
        val id = idGen()
        val now = clock()
        val segments = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.mapIndexed { i, line ->
            TranscriptSegmentDraft(id = "$id-$i", text = line, sourceLine = i)
        }
        return TranscriptDraft(
            id = id,
            sourceType = TranscriptSourceType.PASTED_TRANSCRIPT,
            title = title,
            segments = segments,
            createdAt = now,
            updatedAt = now,
        )
    }
}

class ConfirmTranscriptDraftUseCase {
    /**
     * Confirm an (edited) transcript draft into the analysis-ready payload. Empty drafts are rejected so
     * nothing blank reaches analysis. This is the only path that produces a persistable result.
     */
    fun confirm(draft: TranscriptDraft, courseTitle: String = draft.title): ClassroomCaptureResult? {
        val text = draft.segments.joinToString("\n") { it.text.trim() }.trim()
        if (text.isEmpty()) return null
        return ClassroomCaptureResult(
            courseTitle = courseTitle.ifBlank { draft.displayLabel() },
            courseText = text,
            sourceType = draft.sourceType,
            sourceLabel = TranscriptLabels.of(draft.sourceType),
            segmentCount = draft.segments.size,
        )
    }
}

// ── Image study (OCR + on-device multimodal, dual-track) ─────────────────────────────────────────────
class CreateImageStudyDraftUseCase(
    private val ocrProvider: OcrProvider,
    private val idGen: () -> String = { java.util.UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    /**
     * Build a dual-track image draft: run official OCR AND keep the on-device multimodal draft. If OCR is
     * unconfigured or fails, the on-device draft stays editable — neither track replaces the other.
     */
    fun create(imageBytes: ByteArray, origin: String, onDeviceDraftText: String): ImageStudyDraft {
        val id = idGen()
        return when (val r = ocrProvider.recognize(imageBytes)) {
            is CaptureResult.Success -> ImageStudyDraft(
                id = id, origin = origin,
                ocrBlocks = r.value.blocks,
                ocrNormalizedText = r.value.normalizedText(),
                onDeviceDraftText = onDeviceDraftText,
                createdAt = clock(),
            )
            is CaptureResult.Failure -> ImageStudyDraft(
                id = id, origin = origin,
                ocrError = r.failure.error,
                onDeviceDraftText = onDeviceDraftText,
                createdAt = clock(),
            )
        }
    }
}

class ConfirmImageStudyDraftUseCase {
    /** Confirm the user-edited image text into the analysis-ready payload. Blank is rejected. */
    fun confirm(draft: ImageStudyDraft, editedText: String, courseTitle: String): ClassroomCaptureResult? {
        val text = editedText.trim()
        if (text.isEmpty()) return null
        return ClassroomCaptureResult(
            courseTitle = courseTitle.ifBlank { draft.origin },
            courseText = text,
            sourceType = null,
            sourceLabel = if (draft.hasOcr) "图片文字识别" else "端侧多模态草稿",
            segmentCount = draft.ocrBlocks.size.coerceAtLeast(1),
        )
    }
}

// ── Evidence retrieval foundation ────────────────────────────────────────────────────────────────
class RetrieveCourseEvidenceUseCase(
    private val localRetriever: LocalEvidenceRetriever = LocalEvidenceRetriever(),
    private val similarityProvider: TextSimilarityProvider? = null,
    private val queryRewriteProvider: QueryRewriteProvider? = null,
) {
    /**
     * Retrieve the top evidence segments for [question]. Always uses the local retriever first (works with
     * no credentials). If the official query-rewrite/similarity providers are CONFIGURED, they enhance the
     * result; if they are missing or fail, retrieval silently falls back to the local result.
     */
    fun retrieve(question: String, segments: List<EvidenceSegmentInput>, topN: Int = 3): List<EvidenceCandidate> {
        if (question.isBlank() || segments.isEmpty()) return emptyList()

        // Optional query rewrite (fallback to the original question on missing/failure).
        val effectiveQuery = queryRewriteProvider
            ?.takeIf { it.isConfigured }
            ?.let { (it.rewrite(question) as? CaptureResult.Success)?.value }
            ?.takeIf { it.isNotBlank() }
            ?: question

        val local = localRetriever.retrieve(effectiveQuery, segments, maxOf(topN, 5))
        if (local.isEmpty()) return emptyList()

        // Optional remote rerank over the local shortlist (fallback to local order on missing/failure).
        val sim = similarityProvider?.takeIf { it.isConfigured }
        if (sim != null) {
            val texts = local.map { it.text }
            val scores = (sim.similarity(effectiveQuery, texts) as? CaptureResult.Success)?.value
            if (scores != null && scores.size == texts.size) {
                return local.mapIndexed { i, c -> c.copy(score = scores[i]) }
                    .sortedByDescending { it.score }
                    .take(topN)
                    .mapIndexed { rank, c -> c.copy(rank = rank) }
            }
        }
        return local.take(topN)
    }

    /** Convenience: retrieve directly over a [CourseSession]'s segments. */
    fun retrieve(question: String, session: CourseSession, topN: Int = 3): List<EvidenceCandidate> =
        retrieve(question, session.segments.map { EvidenceSegmentInput(it.id, it.text) }, topN)
}
