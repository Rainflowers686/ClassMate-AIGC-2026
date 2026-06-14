package com.classmate.core.live

import com.classmate.core.material.SpeakerLabel

enum class TranscriptSource {
    MANUAL,
    SIMULATED,
    ASR_FUTURE,
}

enum class TranscriptStatus {
    IDLE,
    RUNNING,
    PAUSED,
    ENDED,
}

data class TranscriptSegment(
    val id: String,
    val index: Int,
    val text: String,
    val source: TranscriptSource,
    val createdAt: Long,
    // Speaker tag + approximate sentence time (ms since class start). Manual today; ASR-ready later.
    // Time is wall-clock elapsed at the moment the segment was added — never derived from audio bytes.
    val speaker: SpeakerLabel = SpeakerLabel.UNKNOWN,
    val startMs: Long? = null,
    val endMs: Long? = null,
)

data class TranscriptSession(
    val id: String,
    val title: String,
    val source: TranscriptSource = TranscriptSource.MANUAL,
    val status: TranscriptStatus = TranscriptStatus.IDLE,
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val accumulatedElapsedMs: Long = 0L,
    val segments: List<TranscriptSegment> = emptyList(),
) {
    fun elapsedMs(now: Long): Long =
        accumulatedElapsedMs + if (status == TranscriptStatus.RUNNING) (now - startedAt).coerceAtLeast(0L) else 0L

    fun append(
        text: String,
        now: Long,
        source: TranscriptSource = this.source,
        speaker: SpeakerLabel = SpeakerLabel.UNKNOWN,
    ): TranscriptSession {
        val clean = text.trim()
        if (clean.isBlank()) return this
        val start = elapsedMs(now)
        return copy(
            segments = segments + TranscriptSegment(
                id = "transcript_${id}_${segments.size + 1}",
                index = segments.size + 1,
                text = clean,
                source = source,
                createdAt = now,
                speaker = speaker,
                startMs = start,
                endMs = null,
            ),
        )
    }

    /** Speaker-tag distribution across the session's segments (teacher / student / unknown counts). */
    fun speakerCounts(): Map<SpeakerLabel, Int> =
        segments.groupingBy { it.speaker }.eachCount()

    fun end(now: Long): TranscriptSession =
        copy(
            status = TranscriptStatus.ENDED,
            endedAt = now,
            accumulatedElapsedMs = elapsedMs(now),
        )

    fun courseText(): String =
        segments.sortedBy { it.index }.joinToString("\n") { it.text }

    companion object {
        fun start(id: String, title: String, now: Long, source: TranscriptSource = TranscriptSource.MANUAL): TranscriptSession =
            TranscriptSession(
                id = id,
                title = title.trim(),
                source = source,
                status = TranscriptStatus.RUNNING,
                startedAt = now,
            )
    }
}

object TranscriptController {
    fun pause(session: TranscriptSession, now: Long): TranscriptSession =
        if (session.status == TranscriptStatus.RUNNING) {
            session.copy(status = TranscriptStatus.PAUSED, accumulatedElapsedMs = session.elapsedMs(now))
        } else {
            session
        }

    fun resume(session: TranscriptSession, now: Long): TranscriptSession =
        if (session.status == TranscriptStatus.PAUSED) {
            session.copy(status = TranscriptStatus.RUNNING, startedAt = now)
        } else {
            session
        }
}
