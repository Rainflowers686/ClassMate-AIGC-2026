package com.classmate.app.material

import com.classmate.app.importing.OcrImportAssembler
import com.classmate.app.importing.OcrImportDraft
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.live.TranscriptSession
import com.classmate.core.material.AudioTimeRange
import com.classmate.core.material.LessonMaterialBundle
import com.classmate.core.material.LessonMaterialFusionEngine
import com.classmate.core.material.MaterialSource
import com.classmate.core.material.MaterialSourceType
import com.classmate.core.material.SpeakerLabel
import com.classmate.core.material.TranscriptSegment
import com.classmate.core.transcript.TranscriptDraft
import com.classmate.core.transcript.TranscriptMaterialAdapter

/**
 * App-side adapter that turns pasted text, file imports, Live transcript, and manual OCR drafts into
 * one core LessonMaterialBundle. The existing CourseAnalyzer still receives bundle.plainText().
 */
object LessonMaterialAssembler {

    private val OCR_TYPES = setOf(
        MaterialSourceType.SLIDE_OCR,
        MaterialSourceType.BLACKBOARD_OCR,
        MaterialSourceType.PDF_OCR,
    )
    private val ASR_PLACEHOLDER_TYPES = setOf(
        MaterialSourceType.AUDIO_FILE,
        MaterialSourceType.VIDEO_FILE,
    )

    fun mapImportType(type: ImportSourceType): MaterialSourceType = when (type) {
        ImportSourceType.PASTE_TEXT -> MaterialSourceType.IMPORTED_TEXT
        ImportSourceType.TXT_FILE -> MaterialSourceType.TXT_FILE
        ImportSourceType.MARKDOWN_FILE -> MaterialSourceType.MARKDOWN_FILE
        ImportSourceType.AUDIO_FILE -> MaterialSourceType.AUDIO_FILE
        ImportSourceType.VIDEO_FILE -> MaterialSourceType.VIDEO_FILE
        ImportSourceType.IMAGE_OCR -> MaterialSourceType.SLIDE_OCR
        ImportSourceType.NETWORK_VIDEO_LINK -> MaterialSourceType.IMPORTED_TEXT
    }

    fun fromImport(title: String, text: String, importType: ImportSourceType, now: Long = 0L): LessonMaterialBundle =
        fromImportWithOcr(title, text, importType, emptyList(), now = now)

    fun fromImportWithOcr(
        title: String,
        text: String,
        importType: ImportSourceType,
        ocrImports: List<OcrImportDraft>,
        subject: String = "",
        transcripts: List<TranscriptDraft> = emptyList(),
        now: Long = 0L,
    ): LessonMaterialBundle {
        val type = mapImportType(importType)
        val textSource = MaterialSource.fromText(id = "import_$now", type = type, text = text, title = title)
            .takeIf { it.segments.isNotEmpty() }
        val sources = buildList {
            if (textSource != null) add(textSource)
            addAll(TranscriptMaterialAdapter.toMaterialSources(transcripts))
            addAll(OcrImportAssembler.toMaterialSources(ocrImports))
        }
        return LessonMaterialFusionEngine.fuse("bundle_$now", title, sources, subject = subject, now = now)
    }

    fun fromSample(sampleId: String, title: String, subject: String, body: String, now: Long = 0L): LessonMaterialBundle {
        val source = MaterialSource.fromText(id = "sample_$sampleId", type = MaterialSourceType.IMPORTED_TEXT, text = body, title = title)
        return LessonMaterialFusionEngine.fuse("bundle_sample_$sampleId", title, listOf(source), subject = subject, now = now)
    }

    fun fromLive(session: TranscriptSession, now: Long = 0L): LessonMaterialBundle =
        fromLiveWithOcr(session, emptyList(), now = now)

    fun fromLiveWithOcr(
        session: TranscriptSession,
        ocrImports: List<OcrImportDraft>,
        transcripts: List<TranscriptDraft> = emptyList(),
        now: Long = 0L,
    ): LessonMaterialBundle {
        // Preserve the Live speaker tag + approximate sentence time into the fused transcript source.
        // sourceLabel stays blank, so the analyzer marker remains the compatible "[转写片段 N]".
        val segments = session.segments.map { live ->
            TranscriptSegment(
                id = live.id,
                index = live.index,
                text = live.text,
                timeRange = live.startMs?.let { AudioTimeRange(it, live.endMs ?: it) },
                speaker = live.speaker,
            )
        }
        val source = MaterialSource.fromTranscript(id = session.id, segments = segments, title = session.title)
        val sources = buildList {
            add(source)
            addAll(TranscriptMaterialAdapter.toMaterialSources(transcripts))
            addAll(OcrImportAssembler.toMaterialSources(ocrImports))
        }
        return LessonMaterialFusionEngine.fuse("bundle_${session.id}", session.title, sources, now = now)
    }

    fun summarize(bundle: LessonMaterialBundle): MaterialSourceSummary {
        val types = bundle.sources.map { it.type }.distinct()
        val segments = bundle.allSegments()
        val slideOcrCount = segments.count { it.sourceType == MaterialSourceType.SLIDE_OCR }
        val blackboardOcrCount = segments.count { it.sourceType == MaterialSourceType.BLACKBOARD_OCR }
        val pdfOcrCount = segments.count { it.sourceType == MaterialSourceType.PDF_OCR }
        val ocrSegmentCount = segments.count { it.sourceType in OCR_TYPES }
        val transcriptSegments = segments.filter { it.sourceType == MaterialSourceType.TRANSCRIPT }
        val transcriptLabels = transcriptSegments
            .map { it.sourceLabel.ifBlank { "课堂转写" } }
            .distinct()
        return MaterialSourceSummary(
            sourceTypes = types,
            sourceCount = bundle.sources.size,
            segmentCount = segments.size,
            hasOcrPlaceholder = types.any { it in OCR_TYPES },
            hasAsrPlaceholder = types.any { it in ASR_PLACEHOLDER_TYPES },
            slideOcrCount = slideOcrCount,
            blackboardOcrCount = blackboardOcrCount,
            pdfOcrCount = pdfOcrCount,
            ocrSegmentCount = ocrSegmentCount,
            transcriptSegmentCount = transcriptSegments.size,
            timestampedSegmentCount = transcriptSegments.count { it.timeRange != null },
            teacherCount = transcriptSegments.count { it.speaker == SpeakerLabel.TEACHER },
            studentCount = transcriptSegments.count { it.speaker == SpeakerLabel.STUDENT },
            unknownSpeakerCount = transcriptSegments.count { it.speaker == SpeakerLabel.UNKNOWN },
            transcriptLabels = transcriptLabels,
        )
    }
}

data class MaterialSourceSummary(
    val sourceTypes: List<MaterialSourceType>,
    val sourceCount: Int,
    val segmentCount: Int,
    val hasOcrPlaceholder: Boolean,
    val hasAsrPlaceholder: Boolean,
    val slideOcrCount: Int = 0,
    val blackboardOcrCount: Int = 0,
    val pdfOcrCount: Int = 0,
    val ocrSegmentCount: Int = 0,
    val transcriptSegmentCount: Int = 0,
    val timestampedSegmentCount: Int = 0,
    val teacherCount: Int = 0,
    val studentCount: Int = 0,
    val unknownSpeakerCount: Int = 0,
    val transcriptLabels: List<String> = emptyList(),
) {
    fun exportLine(): String {
        val typeText = sourceTypes.joinToString("、") { it.zh() }.ifBlank { "无" }
        return buildString {
            append("资料来源：").append(typeText)
            append(" · 来源数 ").append(sourceCount)
            append(" · 片段数 ").append(segmentCount)
            if (slideOcrCount > 0) append(" · 课件 OCR ").append(slideOcrCount).append(" 段")
            if (blackboardOcrCount > 0) append(" · 板书 OCR ").append(blackboardOcrCount).append(" 段")
            if (pdfOcrCount > 0) append(" · 讲义/PDF OCR ").append(pdfOcrCount).append(" 段")
            if (ocrSegmentCount > 0) append(" · OCR 片段 ").append(ocrSegmentCount)
            if (hasAsrPlaceholder) append(" · 含音视频 ASR 占位")
        }
    }

    /** Distinct human source-type labels (incl. transcript kinds) for chips and report covers. */
    fun sourceTypeLabels(): List<String> =
        (sourceTypes.map { it.zh() } + transcriptLabels).distinct()

    /** Honest transcript-source roll-up for the export report (segment / timestamp / speaker counts). */
    fun transcriptLine(): String? {
        if (transcriptSegmentCount <= 0) return null
        val labels = transcriptLabels.joinToString("、").ifBlank { "课堂转写" }
        return buildString {
            append("转写来源：").append(labels)
            append(" · 字幕段落 ").append(transcriptSegmentCount)
            append(" · 有时间戳 ").append(timestampedSegmentCount)
            append(" · 说话人 教师").append(teacherCount)
            append("/学生").append(studentCount)
            append("/未知").append(unknownSpeakerCount)
        }
    }
}

private fun MaterialSourceType.zh(): String = when (this) {
    MaterialSourceType.TRANSCRIPT -> "课堂转写"
    MaterialSourceType.MANUAL_NOTE -> "手动笔记"
    MaterialSourceType.IMPORTED_TEXT -> "粘贴文本"
    MaterialSourceType.TXT_FILE -> "TXT 文件"
    MaterialSourceType.MARKDOWN_FILE -> "Markdown 文件"
    MaterialSourceType.AUDIO_FILE -> "音频文件"
    MaterialSourceType.VIDEO_FILE -> "视频文件"
    MaterialSourceType.SLIDE_OCR -> "课件 OCR"
    MaterialSourceType.BLACKBOARD_OCR -> "板书 OCR"
    MaterialSourceType.PDF_OCR -> "讲义/PDF OCR"
}
