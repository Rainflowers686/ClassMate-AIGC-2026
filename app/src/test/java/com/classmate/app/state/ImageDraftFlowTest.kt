package com.classmate.app.state

import com.classmate.app.platform.ConfigRepository
import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.OcrImportFileMeta
import com.classmate.app.importing.OcrImportKind
import com.classmate.app.importing.OcrImportStatus
import com.classmate.core.model.ProviderKind
import com.classmate.core.ondevice.OnDeviceImageDraftResult
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8C Phase C: image/photo → editable draft → user confirmation. Multimodal output never
 * auto-writes to the knowledge base; cancel keeps the course text untouched. Also covers Phase A: the
 * analysis chain never includes LOCAL_FALLBACK.
 */
class ImageDraftFlowTest {

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-img").resolve("config.local.json").toFile()),
    )

    @Test
    fun multimodalDraftBecomesEditableTextNotManualMode() {
        val viewModel = vm()
        viewModel.beginImageDraft()
        assertTrue(viewModel.ui.imageDraftActive)

        viewModel.applyImageDraftResult(OnDeviceImageDraftResult.Draft("图片中的知识点：光合作用发生在叶绿体。"))
        assertEquals("图片中的知识点：光合作用发生在叶绿体。", viewModel.ui.imageDraftText)
        assertFalse(viewModel.ui.imageDraftManualMode)
        assertFalse(viewModel.ui.imageDraftRunning)
    }

    @Test
    fun unavailableMultimodalSwitchesToManualInputWithoutCrash() {
        val viewModel = vm()
        viewModel.applyImageDraftResult(OnDeviceImageDraftResult.Unavailable("MULTIMODAL_UNAVAILABLE"))
        assertTrue(viewModel.ui.imageDraftManualMode)
        assertTrue(viewModel.ui.imageDraftMessage.orEmpty().contains("手动"))
    }

    @Test
    fun confirmedDraftBecomesCourseTextForAnalysis() {
        val viewModel = vm()
        viewModel.beginImageDraft()
        viewModel.updateImageDraftText("板书：法拉第电磁感应定律。")
        viewModel.confirmImageDraft()

        assertEquals("板书：法拉第电磁感应定律。", viewModel.ui.courseText)
        assertFalse(viewModel.ui.imageDraftActive)
        assertEquals("", viewModel.ui.imageDraftText)
    }

    @Test
    fun confirmedDraftPublishesOcrEvidenceAndOpensEvidenceDetail() {
        val viewModel = vm()
        viewModel.beginImageDraft("拍照学习输入")
        viewModel.updateImageDraftText("板书：磁通量变化会产生感应电动势。")

        viewModel.confirmImageDraft()

        assertEquals("OCR_IMAGE", viewModel.ui.l3Pipeline.lessonSource!!.type.name)
        val evidence = viewModel.ui.l3Pipeline.evidence.firstOrNull { it.sourceType.name == "OCR_IMAGE" }
        assertNotNull(evidence)
        assertTrue(evidence!!.imageRef.isNotBlank())
        assertTrue(evidence.thumbnailRef.isNotBlank())
        viewModel.openEvidenceById(evidence.id)
        assertEquals(Screen.EVIDENCE, viewModel.currentScreen)
        assertEquals(evidence.id, viewModel.ui.selectedEvidenceId)
    }

    @Test
    fun cancelledDraftDoesNotChangeCourseTextOrLeak() {
        val viewModel = vm()
        viewModel.updateCourseText("已有课堂文本")
        viewModel.beginImageDraft()
        viewModel.updateImageDraftText("未确认的草稿内容")
        viewModel.cancelImageDraft()

        assertEquals("已有课堂文本", viewModel.ui.courseText)
        assertFalse(viewModel.ui.imageDraftActive)
        assertEquals("", viewModel.ui.imageDraftText)
    }

    @Test
    fun emptyDraftConfirmDoesNotOverwriteCourseText() {
        val viewModel = vm()
        viewModel.updateCourseText("原文")
        viewModel.beginImageDraft()
        viewModel.confirmImageDraft() // blank draft

        assertEquals("原文", viewModel.ui.courseText)
    }

    @Test
    fun courseAnalysisChainNeverIncludesLocalFallback() {
        // Phase A hard downgrade: the analysis chain is cloud-only; the rule path can't produce analysis.
        assertFalse(vm().analysisProviderOrderForTest().contains(ProviderKind.LOCAL_FALLBACK))
    }

    @Test
    fun originLabelFollowsTheDraftLifecycle() {
        val viewModel = vm()
        viewModel.beginImageDraft("拍照学习输入")
        assertEquals("拍照学习输入", viewModel.ui.imageDraftOrigin)

        viewModel.updateImageDraftText("板书：动能定理。")
        viewModel.confirmImageDraft()

        // The confirmation is honestly labelled and the editor state is fully reset.
        assertTrue(viewModel.ui.toast.orEmpty().contains("拍照学习输入"))
        assertTrue(viewModel.ui.toast.orEmpty().contains("用户确认后进入学习资料"))
        assertEquals(null, viewModel.ui.imageDraftOrigin)
        assertEquals(null, viewModel.ui.imageDraftMeta)
    }

    @Test
    fun cancelClearsOriginAndMetaToo() {
        val viewModel = vm()
        viewModel.beginImageDraft("图片学习输入")
        viewModel.updateImageDraftText("未确认内容")
        viewModel.cancelImageDraft()

        assertEquals(null, viewModel.ui.imageDraftOrigin)
        assertEquals(null, viewModel.ui.imageDraftMeta)
        assertEquals("", viewModel.ui.imageDraftText)
    }

    @Test
    fun multiImageBatchDoesNotPublishBeforeConfirmAndSkipsFailedItems() {
        val viewModel = vm()
        val batchId = viewModel.beginImageOcrBatch("图片学习输入", total = 3, now = 10L)

        viewModel.applyImageOcrBatchItem(batchDraft(batchId, 1, "第一张：牛顿第二定律。"), total = 3)
        viewModel.applyImageOcrBatchItem(batchDraft(batchId, 2, "", OcrImportStatus.FAILED, "无法读取"), total = 3)
        viewModel.applyImageOcrBatchItem(batchDraft(batchId, 3, "第三张：动量守恒。"), total = 3)

        assertEquals(null, viewModel.ui.l3Pipeline.lessonSource)
        assertTrue(viewModel.ui.imageDraftText.contains("图片1"))
        assertTrue(viewModel.ui.imageDraftText.contains("图片3"))
        assertFalse(viewModel.ui.imageDraftText.contains("图片2"))

        assertTrue(viewModel.confirmImageOcrBatch(now = 20L))

        assertEquals("OCR_IMAGE", viewModel.ui.l3Pipeline.lessonSource!!.type.name)
        assertEquals(2, viewModel.ui.l3Pipeline.evidenceAssets.count { it.type.name == "OCR_IMAGE" })
        assertFalse(viewModel.ui.imageDraftActive)
        assertTrue(viewModel.ui.ocrImports.isEmpty())
    }

    @Test
    fun startingNewImageBatchClearsPreviousBatchDrafts() {
        val viewModel = vm()
        val oldBatch = viewModel.beginImageOcrBatch("图片学习输入", total = 1, now = 100L)
        viewModel.applyImageOcrBatchItem(batchDraft(oldBatch, 1, "旧图片内容"), total = 1)

        val newBatch = viewModel.beginImageOcrBatch("图片学习输入", total = 1, now = 200L)

        assertEquals("ocr_batch_200", newBatch)
        assertTrue(viewModel.ui.ocrImports.isEmpty())
        assertEquals("", viewModel.ui.imageDraftText)
    }

    private fun batchDraft(
        batchId: String,
        page: Int,
        text: String,
        status: OcrImportStatus = OcrImportStatus.OK,
        error: String = "",
    ) = OcrImportDraft(
        id = "${batchId}_$page",
        kind = OcrImportKind.SLIDE_IMAGE,
        fileMeta = OcrImportFileMeta("image_$page.jpg", "image/jpeg", 100L, "图片$page", pageIndex = page),
        pastedText = text,
        status = status,
        errorReason = error,
        batchId = batchId,
        pageIndex = page,
        blockIndex = page,
        createdAt = 1_700_000_000_000L + page,
        updatedAt = 1_700_000_000_000L + page,
    )
}
