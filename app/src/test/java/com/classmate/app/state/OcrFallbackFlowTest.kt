package com.classmate.app.state

import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.OcrImportFileMeta
import com.classmate.app.importing.OcrImportKind
import com.classmate.app.importing.OcrImportStatus
import com.classmate.app.platform.ConfigRepository
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-1: OCR being unavailable / unconfigured on the device must never make an imported image a dead end.
 * A failed segment is editable, a manual edit makes it usable, one failed image never blocks the others,
 * and an all-empty batch never fabricates a course.
 */
class OcrFallbackFlowTest {

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-ocr").resolve("config.local.json").toFile()),
        historyStore = InMemoryHistoryStore(emptyList()),
        exportStore = InMemoryExportStore(),
    )

    private fun failed(id: String, batch: String, page: Int, reason: String = "OCR 未配置，请在下方手动输入图片中的文字。") = OcrImportDraft(
        id = id,
        kind = OcrImportKind.SLIDE_IMAGE,
        fileMeta = OcrImportFileMeta(fileName = "img_$page.jpg", displayLabel = "图片$page", pageIndex = page),
        pastedText = "",
        status = OcrImportStatus.FAILED,
        errorReason = reason,
        batchId = batch,
        pageIndex = page,
        blockIndex = page,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun ok(id: String, batch: String, page: Int, text: String) = OcrImportDraft(
        id = id,
        kind = OcrImportKind.SLIDE_IMAGE,
        fileMeta = OcrImportFileMeta(fileName = "img_$page.jpg", displayLabel = "图片$page", pageIndex = page),
        pastedText = text,
        status = OcrImportStatus.OK,
        batchId = batch,
        pageIndex = page,
        blockIndex = page,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun aFailedImageIsRecoverableByManualEditAndStillCreatesTheCourse() {
        val viewModel = vm()
        val batch = viewModel.beginImageOcrBatch("图片学习输入", 2)
        viewModel.applyImageOcrBatchItem(failed("${batch}_1", batch, 1), total = 2)
        viewModel.applyImageOcrBatchItem(ok("${batch}_2", batch, 2, "级数收敛的比值判别法定义。"), total = 2)

        // The failed image is editable -> typing flips it to OK (the manual fallback).
        viewModel.updateOcrImportText("${batch}_1", "根值判别法的定义与适用范围。")
        assertTrue(viewModel.ui.ocrImports.first { it.id == "${batch}_1" }.status == OcrImportStatus.OK)

        // Confirm succeeds and the course text carries BOTH images' content -> OCR participates in analysis.
        assertTrue(viewModel.confirmImageOcrBatch())
        assertTrue(viewModel.ui.courseText.contains("比值判别法"))
        assertTrue(viewModel.ui.courseText.contains("根值判别法"))
    }

    @Test
    fun oneFailedImageNeverBlocksTheSuccessfulOne() {
        val viewModel = vm()
        val batch = viewModel.beginImageOcrBatch("图片学习输入", 2)
        viewModel.applyImageOcrBatchItem(failed("${batch}_1", batch, 1), total = 2)
        viewModel.applyImageOcrBatchItem(ok("${batch}_2", batch, 2, "电磁感应的楞次定律方向判断。"), total = 2)
        // Even with image 1 still failed, the successful image 2 lets the user create the course.
        assertTrue(viewModel.confirmImageOcrBatch())
        assertTrue(viewModel.ui.courseText.contains("楞次定律"))
    }

    @Test
    fun anAllEmptyBatchNeverFabricatesACourse() {
        val viewModel = vm()
        val batch = viewModel.beginImageOcrBatch("图片学习输入", 1)
        viewModel.applyImageOcrBatchItem(failed("${batch}_1", batch, 1), total = 1)
        // No usable text anywhere -> blocked, no fake course / knowledge points.
        assertFalse(viewModel.confirmImageOcrBatch())
        assertTrue(viewModel.ui.courseText.isBlank())
    }
}
