package com.classmate.app.material

import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.OcrImportFileMeta
import com.classmate.app.importing.OcrImportKind
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.live.TranscriptSession
import com.classmate.core.material.MaterialSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonMaterialAssemblerTest {
    private val now = 1_700_000_000_000L

    @Test
    fun pastedTextBuildsImportedTextBundleAndPlainTextHasSourceMarker() {
        val bundle = LessonMaterialAssembler.fromImport("数项级数", "这是一段粘贴的课堂文本。", ImportSourceType.PASTE_TEXT, now)

        assertEquals(MaterialSourceType.IMPORTED_TEXT, bundle.sources.single().type)
        assertTrue(bundle.plainText().contains("[导入文本]"))
        assertTrue(bundle.plainText().contains("这是一段粘贴的课堂文本。"))
    }

    @Test
    fun sampleBuildsBundleAndPreservesSubjectAndTitle() {
        val bundle = LessonMaterialAssembler.fromSample("series", "数项级数", "高等数学", "级数收敛与发散的判别。", now)

        assertEquals("数项级数", bundle.courseTitle)
        assertEquals("高等数学", bundle.subject)
        assertEquals("sample_series", bundle.sources.single().id)
        assertEquals(MaterialSourceType.IMPORTED_TEXT, bundle.sources.single().type)
    }

    @Test
    fun liveSegmentsBuildTranscriptBundleWithSegmentIds() {
        val session = TranscriptSession.start("live_1", "电磁感应", now)
            .append("法拉第电磁感应定律。", now)
            .append("楞次定律决定方向。", now)
        val bundle = LessonMaterialAssembler.fromLive(session, now)

        assertEquals(MaterialSourceType.TRANSCRIPT, bundle.sources.single().type)
        assertEquals(2, bundle.allSegments().size)
        assertEquals(session.segments.map { it.id }, bundle.allSegments().map { it.evidence.segmentId })
        assertTrue(bundle.plainText().contains("[转写片段 1]"))
    }

    @Test
    fun txtAndMarkdownSourceTypesArePreserved() {
        assertEquals(MaterialSourceType.TXT_FILE, LessonMaterialAssembler.mapImportType(ImportSourceType.TXT_FILE))
        assertEquals(MaterialSourceType.MARKDOWN_FILE, LessonMaterialAssembler.mapImportType(ImportSourceType.MARKDOWN_FILE))
        assertEquals(MaterialSourceType.TXT_FILE, LessonMaterialAssembler.fromImport("a", "x", ImportSourceType.TXT_FILE, now).sources.single().type)
        assertEquals(MaterialSourceType.MARKDOWN_FILE, LessonMaterialAssembler.fromImport("a", "x", ImportSourceType.MARKDOWN_FILE, now).sources.single().type)
        assertEquals(MaterialSourceType.SLIDE_OCR, LessonMaterialAssembler.mapImportType(ImportSourceType.IMAGE_OCR))
        assertEquals(MaterialSourceType.AUDIO_FILE, LessonMaterialAssembler.mapImportType(ImportSourceType.AUDIO_FILE))
    }

    @Test
    fun sourceSummaryLineHasCountsAndNoForbiddenTokens() {
        val bundle = LessonMaterialAssembler.fromImport("数项级数", "课堂文本一。\n课堂文本二。", ImportSourceType.PASTE_TEXT, now)
        val line = LessonMaterialAssembler.summarize(bundle).exportLine()

        assertTrue(line.contains("资料来源"))
        assertTrue(line.contains("来源数 1"))
        assertTrue(line.contains("片段数"))
        listOf(
            "Auth" + "orization",
            "Bear" + "er",
            "app" + "Key",
            "api" + "Key",
            "app" + "_id",
            "reasoning" + "_content",
            "mes" + "sages",
        ).forEach {
            assertFalse("summary must not contain $it", line.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun audioVideoSummaryFlagsAsrPlaceholderHonestly() {
        val bundle = LessonMaterialAssembler.fromImport("a", "用户粘贴的音频转写文本占位。", ImportSourceType.AUDIO_FILE, now)
        val summary = LessonMaterialAssembler.summarize(bundle)

        assertTrue(summary.hasAsrPlaceholder)
        assertFalse(summary.hasOcrPlaceholder)
    }

    @Test
    fun ocrAndPastedTextFuseIntoOnePlainTextWithCounts() {
        val slide = ocrDraft("slide_1", OcrImportKind.SLIDE_IMAGE, "p 级数：p > 1 收敛。")
        val board = ocrDraft("board_1", OcrImportKind.BLACKBOARD_PHOTO, "比值判别法：L > 1 发散。")
        val pdf = ocrDraft("pdf_1", OcrImportKind.PDF_PAGE, "法拉第定律：E = -dΦ/dt。")
        val bundle = LessonMaterialAssembler.fromImportWithOcr(
            title = "混合课堂",
            text = "课堂文本：先看核心概念。",
            importType = ImportSourceType.PASTE_TEXT,
            ocrImports = listOf(slide, board, pdf),
            now = now,
        )
        val plain = bundle.plainText()
        val summary = LessonMaterialAssembler.summarize(bundle)

        assertTrue(plain.contains("[导入文本]"))
        assertTrue(plain.contains("[课件 OCR 第 1 页]"))
        assertTrue(plain.contains("[板书 OCR 第 1 块]"))
        assertTrue(plain.contains("[PDF OCR 第 1 页]"))
        assertEquals(1, summary.slideOcrCount)
        assertEquals(1, summary.blackboardOcrCount)
        assertEquals(1, summary.pdfOcrCount)
        assertEquals(3, summary.ocrSegmentCount)
        assertTrue(summary.exportLine().contains("课件 OCR 1 段"))
        assertTrue(summary.exportLine().contains("板书 OCR 1 段"))
        assertTrue(summary.exportLine().contains("讲义/PDF OCR 1 段"))
    }

    private fun ocrDraft(id: String, kind: OcrImportKind, text: String) = OcrImportDraft(
        id = id,
        kind = kind,
        fileMeta = OcrImportFileMeta("$id.png", "image/png", 20L, id, 1),
        pastedText = text,
        pageIndex = 1,
        blockIndex = 1,
        createdAt = now,
        updatedAt = now,
    )
}
