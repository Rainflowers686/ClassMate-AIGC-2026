package com.classmate.core.capture

import com.classmate.core.material.SpeakerLabel
import com.classmate.core.transcript.TranscriptDraft
import com.classmate.core.transcript.TranscriptSegmentDraft
import com.classmate.core.transcript.TranscriptSourceType

/**
 * Classroom Capture domain models. The transcript side REUSES the existing
 * [TranscriptDraft] / [TranscriptSegmentDraft] / [TranscriptSourceType] (core.transcript) — capture only
 * adds the ASR result shape and the image / evidence pieces, plus builders that turn provider output into
 * those existing editable drafts. Nothing here persists; persistence happens only after user confirmation.
 */

// ── ASR ────────────────────────────────────────────────────────────────────────────────────────────
/** Long-audio transcription job lifecycle (1739 task flow: create → upload → run → progress → result). */
enum class AsrJobStatus { PENDING, UPLOADING, RUNNING, SUCCESS, FAILED }

/** One recognized utterance from the ASR result (`onebest` + begin/end ms + speaker index). */
data class AsrSegment(
    val text: String,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val speakerIndex: Int? = null,
)

/** The parsed ASR result: ordered utterances + the joined transcript text. */
data class AsrTranscriptResult(
    val segments: List<AsrSegment>,
    val jobStatus: AsrJobStatus = AsrJobStatus.SUCCESS,
) {
    val fullText: String get() = segments.joinToString("\n") { it.text.trim() }.trim()

    /**
     * Turn the recognized utterances into an EDITABLE [TranscriptDraft] (not persisted). Speakers stay
     * UNKNOWN — ASR speaker indices are not trusted as identities; the user labels them later.
     */
    fun toTranscriptDraft(id: String, title: String, fileName: String?, createdAt: Long): TranscriptDraft =
        TranscriptDraft(
            id = id,
            sourceType = TranscriptSourceType.AUDIO_TRANSCRIPT,
            title = title,
            fileName = fileName,
            segments = segments.mapIndexed { i, s ->
                TranscriptSegmentDraft(
                    id = "$id-$i",
                    startMs = s.startMs,
                    endMs = s.endMs,
                    speaker = SpeakerLabel.UNKNOWN,
                    text = s.text.trim(),
                    sourceLine = i,
                )
            },
            createdAt = createdAt,
            updatedAt = createdAt,
        )
}

// ── OCR ──────────────────────────────────────────────────────────────────────────────────────────
/** Relative position of an OCR block (vivo general OCR pos=2: top_left.y / x used only for reading order). */
data class OcrTextLocation(val topLeftX: Double, val topLeftY: Double)

/** One recognized OCR text block (`words` + optional `location`). */
data class OcrTextBlock(val words: String, val location: OcrTextLocation? = null)

/** Parsed OCR result: blocks + image rotation angle. */
data class OcrResult(val blocks: List<OcrTextBlock>, val angle: Int = 0) {
    /**
     * Merge blocks into normalized reading text. When positions exist, order top→bottom then left→right;
     * otherwise keep provider order. Blocks join with newlines so structure survives into CourseAnalysis.
     */
    fun normalizedText(): String {
        val ordered = if (blocks.all { it.location != null }) {
            blocks.sortedWith(compareBy({ it.location!!.topLeftY }, { it.location!!.topLeftX }))
        } else {
            blocks
        }
        return ordered.joinToString("\n") { it.words.trim() }.trim()
    }
}

// ── Image study draft (OCR + on-device multimodal, dual-track — neither replaces the other) ──────────
/**
 * An editable image-study draft. Two independent tracks are kept side by side: [ocrNormalizedText] from
 * official OCR, and [onDeviceDraftText] from the on-device multimodal model. NEITHER replaces the other
 * (no "multimodal replaces OCR" claim). The user edits [editedText] and confirms before anything is used.
 */
data class ImageStudyDraft(
    val id: String,
    val origin: String,
    val ocrBlocks: List<OcrTextBlock> = emptyList(),
    val ocrNormalizedText: String = "",
    val ocrError: CaptureError? = null,
    val onDeviceDraftText: String = "",
    val editedText: String = "",
    val createdAt: Long = 0L,
) {
    val hasOcr: Boolean get() = ocrNormalizedText.isNotBlank()
    val hasOnDeviceDraft: Boolean get() = onDeviceDraftText.isNotBlank()

    /** The text the editor starts from: prefer OCR, fall back to the on-device draft. Always editable. */
    fun initialEditableText(): String = when {
        editedText.isNotBlank() -> editedText
        ocrNormalizedText.isNotBlank() -> ocrNormalizedText
        else -> onDeviceDraftText
    }
}

// ── Confirmed, analysis-ready payload ────────────────────────────────────────────────────────────
/** What capture is for: the user-confirmed text + source descriptor that the EXISTING CourseAnalysis consumes. */
data class ClassroomCaptureResult(
    val courseTitle: String,
    val courseText: String,
    val sourceType: TranscriptSourceType?,
    val sourceLabel: String,
    val segmentCount: Int,
)

// ── Evidence retrieval ──────────────────────────────────────────────────────────────────────────
/** A retrieved evidence segment with a relevance score (0..1) and its rank (0 = best). */
data class EvidenceCandidate(
    val segmentId: String,
    val text: String,
    val score: Double,
    val rank: Int,
)
