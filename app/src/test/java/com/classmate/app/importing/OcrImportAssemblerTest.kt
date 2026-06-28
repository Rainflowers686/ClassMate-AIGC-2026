package com.classmate.app.importing

import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.evidence.EvidenceResolver
import com.classmate.core.material.MaterialSourceType
import com.classmate.core.material.safeSourceMarker
import com.classmate.core.model.SourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrImportAssemblerTest {
    private val now = 1_700_000_000_000L

    @Test
    fun slideOcrDraftMapsToSlideSource() {
        val source = OcrImportAssembler.toMaterialSource(draft(OcrImportKind.SLIDE_IMAGE, "级数判别法"))!!

        assertEquals(MaterialSourceType.SLIDE_OCR, source.type)
        assertEquals("课件 OCR", source.segments.single().sourceLabel)
        assertEquals("课件 OCR", source.segments.single().evidence.sourceLabel)
        assertTrue(safeSourceMarker(source.segments.single()).contains("[课件 OCR 第 1 页]"))
    }

    @Test
    fun blackboardOcrDraftMapsToBlackboardSource() {
        val source = OcrImportAssembler.toMaterialSource(draft(OcrImportKind.BLACKBOARD_PHOTO, "比值判别法例题"))!!

        assertEquals(MaterialSourceType.BLACKBOARD_OCR, source.type)
        assertTrue(safeSourceMarker(source.segments.single()).contains("[板书 OCR 第 1 块]"))
    }

    @Test
    fun pdfAndHandoutDraftsMapToDesignedSources() {
        val pdf = OcrImportAssembler.toMaterialSource(draft(OcrImportKind.PDF_PAGE, "法拉第定律讲义"))!!
        val handout = OcrImportAssembler.toMaterialSource(draft(OcrImportKind.HANDOUT_IMAGE, "讲义截图"))!!

        assertEquals(MaterialSourceType.PDF_OCR, pdf.type)
        assertEquals(MaterialSourceType.SLIDE_OCR, handout.type)
        assertEquals("讲义 OCR", handout.segments.single().sourceLabel)
    }

    @Test
    fun blankOcrTextDoesNotCreateSource() {
        assertNull(OcrImportAssembler.toMaterialSource(draft(OcrImportKind.SLIDE_IMAGE, "")))
    }

    @Test
    fun failedOcrTextDoesNotCreateSource() {
        val failed = draft(OcrImportKind.SLIDE_IMAGE, "should not enter material").copy(
            status = OcrImportStatus.FAILED,
            errorReason = "failed",
        )

        assertNull(OcrImportAssembler.toMaterialSource(failed))
    }

    @Test
    fun mergeByOrderKeepsImageSectionsAndSkipsFailedDrafts() {
        val third = draft(OcrImportKind.SLIDE_IMAGE, "第三张内容").copy(
            id = "draft_3",
            fileMeta = OcrImportFileMeta("third.png", "image/png", 100L, "第三张", pageIndex = 3),
            pageIndex = 3,
        )
        val first = draft(OcrImportKind.SLIDE_IMAGE, "第一张内容").copy(
            id = "draft_1",
            fileMeta = OcrImportFileMeta("first.png", "image/png", 100L, "第一张", pageIndex = 1),
            pageIndex = 1,
        )
        val failed = draft(OcrImportKind.SLIDE_IMAGE, "").copy(
            id = "draft_2",
            fileMeta = OcrImportFileMeta("second.png", "image/png", 100L, "第二张", pageIndex = 2),
            status = OcrImportStatus.FAILED,
            errorReason = "无法读取",
            pageIndex = 2,
        )

        val merged = OcrImportAssembler.mergeByOrder(listOf(third, failed, first))

        assertTrue(merged.text.indexOf("图片1") < merged.text.indexOf("图片3"))
        assertTrue(merged.text.contains("第一张内容"))
        assertTrue(merged.text.contains("第三张内容"))
        assertFalse(merged.text.contains("第二张"))
        assertEquals(2, merged.okDrafts.size)
        assertEquals(1, merged.failedDrafts.size)
    }

    @Test
    fun ocrQuoteIsLocatableInAnalyzerRawText() {
        val source = OcrImportAssembler.toMaterialSource(draft(OcrImportKind.SLIDE_IMAGE, "p 级数：p > 1 收敛，p <= 1 发散"))!!
        val rawText = "${safeSourceMarker(source.segments.single())}\n${source.segments.single().text}"
        val session = CourseSegmenter.buildSession("s1", "高等数学", rawText, now, SourceKind.PASTED_TEXT)
        val span = EvidenceResolver().resolveAnywhere(session, "p > 1 收敛")

        assertNotNull(span)
        assertTrue(span!!.quote.contains("p > 1"))
    }

    @Test
    fun metadataSummaryDoesNotContainPathOrSensitiveMarkers() {
        val meta = OcrImportFileMeta(
            fileName = "C:\\Users\\Rain\\secret\\slide-01.png",
            mimeType = "image/png",
            sizeBytes = 2048L,
            displayLabel = "/Users/Rain/secret/课件 OCR 第 1 页",
            pageIndex = 1,
        )
        val summary = meta.safeSummary()

        assertFalse(summary.contains("C:\\"))
        assertFalse(summary.contains("/Users/"))
        assertTrue(summary.contains("slide-01.png"))
        assertTrue(summary.contains("课件 OCR 第 1 页"))
        listOf(
            "Auth" + "orization",
            "Bear" + "er",
            "app" + "Key",
            "api" + "Key",
            "app" + "_id",
            "reasoning" + "_content",
            "pro" + "mpt",
            "mes" + "sages",
        ).forEach {
            assertFalse(summary.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun isLowQualityFlagsOkDraftsWithAReviewNote() {
        // P0-4: an OK draft carrying an errorReason note is "low quality, needs review"; a clean OK draft
        // and a failed draft are not "low quality".
        val clean = draft(OcrImportKind.SLIDE_IMAGE, "清晰文本")
        assertFalse(clean.isLowQuality())
        assertTrue(clean.copy(errorReason = "识别置信度偏低").isLowQuality())
        assertFalse(clean.copy(status = OcrImportStatus.FAILED, errorReason = "无法识别").isLowQuality())
    }

    private fun draft(kind: OcrImportKind, text: String) = OcrImportDraft(
        id = "draft_1",
        kind = kind,
        fileMeta = OcrImportFileMeta("source.png", "image/png", 100L, "source", pageIndex = 1),
        pastedText = text,
        pageIndex = 1,
        blockIndex = 1,
        createdAt = now,
        updatedAt = now,
    )
}
