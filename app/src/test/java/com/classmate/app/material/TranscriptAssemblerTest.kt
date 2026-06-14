package com.classmate.app.material

import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.OcrImportFileMeta
import com.classmate.app.importing.OcrImportKind
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.material.SpeakerLabel
import com.classmate.core.transcript.TranscriptDraft
import com.classmate.core.transcript.TranscriptSegmentDraft
import com.classmate.core.transcript.TranscriptSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptAssemblerTest {
    private val now = 1_700_000_000_000L

    private fun transcriptDraft() = TranscriptDraft(
        id = "transcript_1",
        sourceType = TranscriptSourceType.SRT_FILE,
        title = "字幕",
        segments = listOf(
            TranscriptSegmentDraft(id = "s1", startMs = 5_000, endMs = 8_000, speaker = SpeakerLabel.TEACHER, text = "法拉第定律。"),
            TranscriptSegmentDraft(id = "s2", speaker = SpeakerLabel.STUDENT, text = "学生提问。"),
        ),
    )

    @Test
    fun transcriptOcrAndTextFuseWithCorrectSourceSummary() {
        val ocr = OcrImportDraft(
            id = "slide_1",
            kind = OcrImportKind.SLIDE_IMAGE,
            fileMeta = OcrImportFileMeta("slide.png", "image/png", 20L, "课件", 1),
            pastedText = "p 级数收敛。",
            pageIndex = 1,
            blockIndex = 1,
            createdAt = now,
            updatedAt = now,
        )
        val bundle = LessonMaterialAssembler.fromImportWithOcr(
            title = "混合课堂",
            text = "课堂文本核心概念。",
            importType = ImportSourceType.PASTE_TEXT,
            ocrImports = listOf(ocr),
            transcripts = listOf(transcriptDraft()),
            now = now,
        )
        val summary = LessonMaterialAssembler.summarize(bundle)
        val plain = bundle.plainText()

        assertTrue(plain.contains("[导入文本]"))
        assertTrue(plain.contains("[课件 OCR 第 1 页]"))
        assertTrue(plain.contains("[SRT 字幕 00:00:05-00:00:08 · 教师]"))
        assertEquals(2, summary.transcriptSegmentCount)
        assertEquals(1, summary.timestampedSegmentCount)
        assertEquals(1, summary.teacherCount)
        assertEquals(1, summary.studentCount)
    }

    @Test
    fun transcriptExportLineHasSegmentTimestampAndSpeakerCounts() {
        val bundle = LessonMaterialAssembler.fromImportWithOcr(
            title = "课",
            text = "",
            importType = ImportSourceType.PASTE_TEXT,
            ocrImports = emptyList(),
            transcripts = listOf(transcriptDraft()),
            now = now,
        )
        val summary = LessonMaterialAssembler.summarize(bundle)
        val line = summary.transcriptLine()
        assertNotNull(line)
        assertTrue(line!!.contains("SRT 字幕"))
        assertTrue(line.contains("字幕段落 2"))
        assertTrue(line.contains("有时间戳 1"))
        assertTrue(line.contains("教师1"))
        assertTrue(line.contains("学生1"))

        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content", "messages").forEach {
            assertFalse(line.contains(it, ignoreCase = true))
            assertFalse(summary.exportLine().contains(it, ignoreCase = true))
        }
    }

    @Test
    fun noTranscriptMeansNoTranscriptLine() {
        val bundle = LessonMaterialAssembler.fromImport("课", "只有文本。", ImportSourceType.PASTE_TEXT, now)
        assertEquals(0, LessonMaterialAssembler.summarize(bundle).transcriptSegmentCount)
        assertEquals(null, LessonMaterialAssembler.summarize(bundle).transcriptLine())
    }
}
