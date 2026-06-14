package com.classmate.app.asr

import com.classmate.core.transcript.TranscriptDraft
import com.classmate.core.transcript.TranscriptSourceType

/**
 * Maps confirmed ASR segments into a [TranscriptDraft] of kind [TranscriptSourceType.LIVE_ASR], which
 * then flows through the EXISTING transcript → MaterialSource → CourseAnalyzer path (no second
 * analyzer). The LIVE_ASR label produces the analyzer marker "[实时转写 00:01:05-00:01:12 · 教师]".
 */
object AsrTranscriptMapper {

    fun toDraft(session: AsrSession, title: String, now: Long, id: String): TranscriptDraft =
        TranscriptDraft(
            id = id,
            sourceType = TranscriptSourceType.LIVE_ASR,
            title = title.ifBlank { "实时转写" },
            segments = session.segments.filter { it.text.isNotBlank() },
            createdAt = now,
            updatedAt = now,
        )
}
