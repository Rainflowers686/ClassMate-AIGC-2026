package com.classmate.core.transcript

import com.classmate.core.material.SpeakerLabel
import com.classmate.core.material.zhLabel
import kotlinx.serialization.Serializable

/**
 * Editable transcript foundation for Stage 5B. A user supplies subtitle/transcript TEXT (pasted, or
 * read from a local .srt/.vtt/.txt they have the right to use) and we parse it into editable
 * [TranscriptSegmentDraft]s. This is NOT real ASR and NEVER decodes audio/video bytes — the media
 * itself is only ever recorded as fileName/mimeType/sizeBytes metadata.
 *
 * Data only: no network, no credentials, no absolute local paths.
 */
@Serializable
enum class TranscriptSourceType {
    LIVE_MANUAL,
    LIVE_ASR,
    AUDIO_TRANSCRIPT,
    VIDEO_SUBTITLE,
    SRT_FILE,
    VTT_FILE,
    PASTED_TRANSCRIPT,
}

/**
 * One editable line: optional sentence-level time window, a speaker tag, and the spoken text.
 * [confidence] is only set when a recognizer returns one (e.g. system ASR); null otherwise.
 */
@Serializable
data class TranscriptSegmentDraft(
    val id: String,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val speaker: SpeakerLabel = SpeakerLabel.UNKNOWN,
    val text: String,
    val sourceLine: Int? = null,
    val confidence: Double? = null,
)

/**
 * A parsed-then-editable transcript. Only safe descriptors of the original media are kept
 * ([fileName]/[mimeType]/[sizeBytes]/[title]) — never a full local absolute path.
 */
@Serializable
data class TranscriptDraft(
    val id: String,
    val sourceType: TranscriptSourceType,
    val title: String = "",
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val segments: List<TranscriptSegmentDraft> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val timestampedCount: Int get() = segments.count { it.startMs != null }

    fun displayLabel(): String = title.ifBlank { fileName ?: TranscriptLabels.of(sourceType) }
}

/** Result of parsing a block of subtitle/transcript text: editable segments + non-fatal warnings. */
@Serializable
data class TranscriptParseResult(
    val sourceType: TranscriptSourceType,
    val segments: List<TranscriptSegmentDraft> = emptyList(),
    val warnings: List<String> = emptyList(),
)

/** Speaker label for display, or null when UNKNOWN (so callers can omit an empty "[未知]" chip). */
fun SpeakerLabel.zhLabelOrNull(): String? = if (this == SpeakerLabel.UNKNOWN) null else zhLabel()

/** Human, non-sensitive labels for each transcript source kind (used in markers, trays, summaries). */
object TranscriptLabels {
    fun of(type: TranscriptSourceType): String = when (type) {
        TranscriptSourceType.LIVE_MANUAL -> "课堂转写"
        TranscriptSourceType.LIVE_ASR -> "实时转写"
        TranscriptSourceType.AUDIO_TRANSCRIPT -> "音频转写"
        TranscriptSourceType.VIDEO_SUBTITLE -> "视频字幕"
        TranscriptSourceType.SRT_FILE -> "SRT 字幕"
        TranscriptSourceType.VTT_FILE -> "VTT 字幕"
        TranscriptSourceType.PASTED_TRANSCRIPT -> "手动转写"
    }
}

/**
 * Sentence-level clock helpers. Parsing accepts SRT (`,`) and VTT (`.`) millisecond separators and
 * `MM:SS` / `HH:MM:SS` forms; formatting always renders `HH:MM:SS`. Never throws — bad input is null.
 */
object TranscriptClock {

    private val CLOCK = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?(?:[.,](\d{1,3}))?""")

    /** Parse a single timecode to milliseconds, or null when it is not a recognizable clock. */
    fun parseMs(raw: String): Long? {
        val m = CLOCK.matchEntire(raw.trim()) ?: return null
        val g = m.groupValues
        return try {
            val (a, b, c) = Triple(g[1].toLong(), g[2].toLong(), g[3].toLongOrNull())
            val (hh, mm, ss) = if (c == null) Triple(0L, a, b) else Triple(a, b, c)
            val millis = g[4].takeIf { it.isNotEmpty() }?.padEnd(3, '0')?.toLong() ?: 0L
            (((hh * 60) + mm) * 60 + ss) * 1000 + millis
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** Format milliseconds as `HH:MM:SS` (drops the millisecond part, which markers do not need). */
    fun format(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        return "%02d:%02d:%02d".format(totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)
    }

    /** `00:01:05-00:01:12`, or just the start clock when start == end (or there is no end). */
    fun formatRange(startMs: Long, endMs: Long?): String {
        val start = format(startMs)
        return if (endMs == null || endMs == startMs) start else "$start-${format(endMs)}"
    }
}
