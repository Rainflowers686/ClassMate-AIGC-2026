package com.classmate.core.transcript

import com.classmate.core.material.AudioTimeRange
import com.classmate.core.material.MaterialEvidenceRef
import com.classmate.core.material.MaterialSegment
import com.classmate.core.material.MaterialSource
import com.classmate.core.material.MaterialSourceType

/**
 * Bridges an editable [TranscriptDraft] into the EXISTING [MaterialSource] shape so transcripts flow
 * through the same fusion → CourseAnalyzer → evidence path as pasted text and OCR. Every kind maps to
 * [MaterialSourceType.TRANSCRIPT] (so it is never mislabeled as an unparsed "ASR placeholder") and is
 * distinguished by a human [MaterialSegment.sourceLabel] (音频转写 / 视频字幕 / SRT 字幕 / …). Sentence
 * time and speaker are preserved on each segment so the analyzer marker and evidence stay grounded.
 */
object TranscriptMaterialAdapter {

    fun toMaterialSource(draft: TranscriptDraft): MaterialSource {
        val label = TranscriptLabels.of(draft.sourceType)
        val segments = draft.segments
            .filter { it.text.isNotBlank() }
            .mapIndexed { i, seg ->
                val index = i + 1
                val segId = "${draft.id}_$index"
                val timeRange = seg.startMs?.let { AudioTimeRange(it, seg.endMs ?: it) }
                val text = seg.text.trim()
                MaterialSegment(
                    id = segId,
                    sourceType = MaterialSourceType.TRANSCRIPT,
                    sourceId = draft.id,
                    index = index,
                    text = text,
                    evidence = MaterialEvidenceRef(
                        sourceType = MaterialSourceType.TRANSCRIPT,
                        sourceId = draft.id,
                        segmentId = segId,
                        timeRange = timeRange,
                        quote = text,
                        sourceLabel = label,
                    ),
                    speaker = seg.speaker,
                    timeRange = timeRange,
                    sourceLabel = label,
                )
            }
        return MaterialSource(
            id = draft.id,
            type = MaterialSourceType.TRANSCRIPT,
            title = draft.displayLabel(),
            segments = segments,
            createdAt = draft.createdAt,
        )
    }

    fun toMaterialSources(drafts: List<TranscriptDraft>): List<MaterialSource> =
        drafts.map(::toMaterialSource)
}
