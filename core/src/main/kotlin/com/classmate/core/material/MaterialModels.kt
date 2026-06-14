package com.classmate.core.material

import kotlinx.serialization.Serializable

/**
 * Foundation data models for the next-stage "multimodal lesson material" pipeline. Several input
 * kinds (live/ASR transcript, uploaded audio/video, slide/board/PDF OCR, manual notes, pasted text,
 * .txt/.md) are fused into a single [LessonMaterialBundle] that the EXISTING CourseAnalyzer can read
 * as plain classroom text — while every fused unit keeps a [MaterialEvidenceRef] back to its origin.
 *
 * This file is data only. It does NOT connect real ASR/OCR, does NOT touch the BlueLM/Compatible/
 * LocalFallback path, and carries no credentials, prompts, or vendor payloads.
 */
@Serializable
enum class MaterialSourceType {
    TRANSCRIPT,
    MANUAL_NOTE,
    IMPORTED_TEXT,
    TXT_FILE,
    MARKDOWN_FILE,
    AUDIO_FILE,
    VIDEO_FILE,
    SLIDE_OCR,
    BLACKBOARD_OCR,
    PDF_OCR,
}

/** Who spoke a segment. Defaults to [UNKNOWN]; simple rules must never be called speaker recognition. */
@Serializable
enum class SpeakerLabel { TEACHER, STUDENT, UNKNOWN }

/** A time window inside an audio/video media. Only present when a real media timeline exists. */
@Serializable
data class AudioTimeRange(
    val startMs: Long,
    val endMs: Long,
    val sourceMediaId: String? = null,
)

/**
 * Word/char-level sync unit. ONLY produced when an ASR service returns real token timestamps —
 * never fabricated by averaging. Empty by default in this manual-only foundation.
 */
@Serializable
data class SyncToken(
    val token: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Double? = null,
    val sourceSegmentId: String,
)

/** One transcribed line (from manual entry today; from ASR later). */
@Serializable
data class TranscriptSegment(
    val id: String,
    val index: Int,
    val text: String,
    val timeRange: AudioTimeRange? = null,
    val speaker: SpeakerLabel = SpeakerLabel.UNKNOWN,
    val confidence: Double? = null,
    val syncTokens: List<SyncToken> = emptyList(),
)

/** One OCR text block on a page. [role] is title/body/formula/caption/table/unknown. */
@Serializable
data class OcrBlock(
    val id: String,
    val text: String,
    val role: String = "unknown",
    val confidence: Double? = null,
)

/** A structured slide, when the OCR source is presentation slides. */
@Serializable
data class SlideFrame(
    val slideNumber: Int,
    val title: String = "",
    val bullets: List<String> = emptyList(),
    val figureCaptions: List<String> = emptyList(),
    val speakerNote: String = "",
)

/** One page of an OCR document. [imageRef] is an opaque local handle, never a credential/path leak. */
@Serializable
data class OcrPage(
    val id: String,
    val pageIndex: Int,
    val imageRef: String? = null,
    val blocks: List<OcrBlock> = emptyList(),
    val slideFrame: SlideFrame? = null,
) {
    fun pageText(): String = blocks.joinToString("\n") { it.text }
}

/** A whole OCR input (a set of slide images, board photos, or PDF screenshots). */
@Serializable
data class OcrDocument(
    val id: String,
    val sourceType: MaterialSourceType,
    val title: String = "",
    val pages: List<OcrPage> = emptyList(),
    val provider: String = "manual",
    val createdAt: Long = 0L,
)

/** A course term for glossary-aware cleanup (no model call; user/course supplied). */
@Serializable
data class CourseTerm(
    val term: String,
    val aliases: List<String> = emptyList(),
    val subject: String = "",
    val definition: String = "",
    val examples: List<String> = emptyList(),
    val priority: Int = 0,
    val source: String = "",
)

/** A subject/course glossary. */
@Serializable
data class TermGlossary(
    val subject: String = "",
    val terms: List<CourseTerm> = emptyList(),
    val version: Int = 1,
    val selectedByUser: Boolean = false,
    val generatedFromCourse: Boolean = false,
)

/**
 * A pointer from a fused unit back to where it came from: transcript segment + audio time, OCR page
 * + block, or a note/imported-text id. This is what lets knowledge-point evidence later resolve to a
 * lecture moment, a slide page, a board block, or a note — not just a text offset.
 */
@Serializable
data class MaterialEvidenceRef(
    val sourceType: MaterialSourceType,
    val sourceId: String,
    val segmentId: String? = null,
    val pageId: String? = null,
    val blockId: String? = null,
    val timeRange: AudioTimeRange? = null,
    val quote: String = "",
    val confidence: Double? = null,
    val sourceLabel: String = "",
)

/** One uniform fused text unit inside a [MaterialSource], carrying its origin [evidence]. */
@Serializable
data class MaterialSegment(
    val id: String,
    val sourceType: MaterialSourceType,
    val sourceId: String,
    val index: Int,
    val text: String,
    val evidence: MaterialEvidenceRef,
    val speaker: SpeakerLabel = SpeakerLabel.UNKNOWN,
    val timeRange: AudioTimeRange? = null,
    val pageIndex: Int? = null,
    val sourceLabel: String = "",
)

/**
 * One input source (a transcript session, an OCR document, a note, pasted text, …) normalized to an
 * ordered list of [MaterialSegment]. The builder helpers convert the richer typed inputs into this
 * uniform shape so fusion can stay a small, stable, pure function.
 */
@Serializable
data class MaterialSource(
    val id: String,
    val type: MaterialSourceType,
    val title: String = "",
    val segments: List<MaterialSegment> = emptyList(),
    val createdAt: Long = 0L,
) {
    companion object {
        private fun evidence(
            type: MaterialSourceType,
            sourceId: String,
            quote: String,
            segmentId: String? = null,
            pageId: String? = null,
            blockId: String? = null,
            timeRange: AudioTimeRange? = null,
            confidence: Double? = null,
        ) = MaterialEvidenceRef(type, sourceId, segmentId, pageId, blockId, timeRange, quote, confidence)

        /** Pasted text / .txt / .md / a single manual note → one source with one segment. */
        fun fromText(
            id: String,
            type: MaterialSourceType,
            text: String,
            title: String = "",
            createdAt: Long = 0L,
        ): MaterialSource {
            val clean = text.trim()
            val seg = MaterialSegment(
                id = "${id}_1",
                sourceType = type,
                sourceId = id,
                index = 1,
                text = clean,
                evidence = evidence(type, id, clean, segmentId = "${id}_1"),
            )
            return MaterialSource(id, type, title, if (clean.isBlank()) emptyList() else listOf(seg), createdAt)
        }

        fun fromManualNote(id: String, text: String, createdAt: Long = 0L): MaterialSource =
            fromText(id, MaterialSourceType.MANUAL_NOTE, text, title = "", createdAt = createdAt)

        /** A transcript session (manual today, ASR later) → one segment per line, keeping time + speaker. */
        fun fromTranscript(
            id: String,
            segments: List<TranscriptSegment>,
            title: String = "",
            type: MaterialSourceType = MaterialSourceType.TRANSCRIPT,
            createdAt: Long = 0L,
        ): MaterialSource {
            val mapped = segments
                .filter { it.text.isNotBlank() }
                .mapIndexed { i, t ->
                    MaterialSegment(
                        id = "${id}_${i + 1}",
                        sourceType = type,
                        sourceId = id,
                        index = i + 1,
                        text = t.text.trim(),
                        evidence = evidence(type, id, t.text.trim(), segmentId = t.id, timeRange = t.timeRange, confidence = t.confidence),
                        speaker = t.speaker,
                        timeRange = t.timeRange,
                    )
                }
            return MaterialSource(id, type, title, mapped, createdAt)
        }

        /** An OCR document → one segment per page (concatenated blocks), keeping page + first block ids. */
        fun fromOcr(doc: OcrDocument): MaterialSource {
            val mapped = doc.pages
                .filter { it.pageText().isNotBlank() }
                .mapIndexed { i, page ->
                    val text = page.pageText().trim()
                    MaterialSegment(
                        id = "${doc.id}_p${page.pageIndex}",
                        sourceType = doc.sourceType,
                        sourceId = doc.id,
                        index = i + 1,
                        text = text,
                        evidence = evidence(
                            doc.sourceType, doc.id, text,
                            pageId = page.id,
                            blockId = page.blocks.firstOrNull()?.id,
                        ),
                        pageIndex = page.pageIndex,
                    )
                }
            return MaterialSource(doc.id, doc.sourceType, doc.title, mapped, doc.createdAt)
        }
    }
}

/**
 * The fused, analysis-ready material for one lesson. [plainText] is what the existing CourseAnalyzer
 * consumes; [evidenceRefs] is the parallel origin index. Contains ONLY learning data — no secrets,
 * prompts, or vendor bodies.
 */
@Serializable
data class LessonMaterialBundle(
    val id: String,
    val courseTitle: String,
    val subject: String = "",
    val createdAt: Long = 0L,
    val sources: List<MaterialSource> = emptyList(),
    val glossary: TermGlossary = TermGlossary(),
    val fusionWarnings: List<String> = emptyList(),
) {
    /** Every fused segment in stable order (source order, then segment index). */
    fun allSegments(): List<MaterialSegment> = sources.flatMap { it.segments }

    /** Origin pointers for every fused unit, in the same stable order. */
    fun evidenceRefs(): List<MaterialEvidenceRef> = allSegments().map { it.evidence }

    /**
     * Classroom text for the existing CourseAnalyzer. Each unit is prefixed with a safe, human source
     * marker (e.g. "[转写片段 1]", "[课件 OCR 第 2 页]", "[手动笔记]"). Markers carry only kind/index —
     * never credentials, prompts, or raw vendor output.
     */
    fun plainText(): String =
        allSegments().joinToString("\n\n") { "${safeSourceMarker(it)}\n${it.text}" }
}

/** Human, non-sensitive source marker for a fused segment. */
fun sourceMarker(segment: MaterialSegment): String = when (segment.sourceType) {
    MaterialSourceType.TRANSCRIPT -> "[转写片段 ${segment.index}]"
    MaterialSourceType.AUDIO_FILE -> "[音频转写片段 ${segment.index}]"
    MaterialSourceType.VIDEO_FILE -> "[视频转写片段 ${segment.index}]"
    MaterialSourceType.SLIDE_OCR -> "[课件 OCR 第 ${segment.pageIndex ?: segment.index} 页]"
    MaterialSourceType.BLACKBOARD_OCR -> "[板书 OCR ${segment.index}]"
    MaterialSourceType.PDF_OCR -> "[讲义 OCR 第 ${segment.pageIndex ?: segment.index} 页]"
    MaterialSourceType.MANUAL_NOTE -> "[手动笔记]"
    MaterialSourceType.IMPORTED_TEXT -> "[导入文本]"
    MaterialSourceType.TXT_FILE -> "[导入 .txt]"
    MaterialSourceType.MARKDOWN_FILE -> "[导入 .md]"
}

/** Current readable marker used by analysis/export. Kept separate to avoid rewriting old mojibake. */
fun safeSourceMarker(segment: MaterialSegment): String = when (segment.sourceType) {
    MaterialSourceType.TRANSCRIPT -> transcriptMarker(segment, "转写片段")
    MaterialSourceType.AUDIO_FILE -> transcriptMarker(segment, "音频转写片段")
    MaterialSourceType.VIDEO_FILE -> transcriptMarker(segment, "视频转写片段")
    MaterialSourceType.SLIDE_OCR -> "[${segment.sourceLabel.ifBlank { "课件 OCR" }} 第 ${segment.pageIndex ?: segment.index} 页]"
    MaterialSourceType.BLACKBOARD_OCR -> "[${segment.sourceLabel.ifBlank { "板书 OCR" }} 第 ${segment.index} 块]"
    MaterialSourceType.PDF_OCR -> "[${segment.sourceLabel.ifBlank { "PDF OCR" }} 第 ${segment.pageIndex ?: segment.index} 页]"
    MaterialSourceType.MANUAL_NOTE -> "[手动笔记]"
    MaterialSourceType.IMPORTED_TEXT -> "[导入文本]"
    MaterialSourceType.TXT_FILE -> "[导入 .txt]"
    MaterialSourceType.MARKDOWN_FILE -> "[导入 .md]"
}

/**
 * Transcript-family marker. Backward compatible: with no [MaterialSegment.sourceLabel] it stays
 * "[转写片段 N]" (so existing Live/fusion output is unchanged). When a parsed transcript draft sets a
 * sourceLabel it becomes "[音频转写 00:01:05-00:01:12 · 教师]" / "[手动转写 · 学生]" — carrying only the
 * human label, optional sentence clock, and speaker tag; never credentials, prompts, or vendor output.
 */
private fun transcriptMarker(segment: MaterialSegment, fallbackLabel: String): String {
    if (segment.sourceLabel.isBlank()) return "[$fallbackLabel ${segment.index}]"
    val parts = buildList {
        add(segment.sourceLabel)
        segment.timeRange?.let { add(clockRange(it.startMs, it.endMs)) }
        add("· ${segment.speaker.zhLabel()}")
    }
    return "[" + parts.joinToString(" ") + "]"
}

private fun clockRange(startMs: Long, endMs: Long): String {
    val start = clock(startMs)
    return if (endMs == startMs) start else "$start-${clock(endMs)}"
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

/** Chinese label for a speaker tag, shared by markers, transcript editor, and export summaries. */
fun SpeakerLabel.zhLabel(): String = when (this) {
    SpeakerLabel.TEACHER -> "教师"
    SpeakerLabel.STUDENT -> "学生"
    SpeakerLabel.UNKNOWN -> "未知"
}
