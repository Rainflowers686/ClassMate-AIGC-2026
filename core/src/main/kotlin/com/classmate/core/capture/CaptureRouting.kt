package com.classmate.core.capture

import com.classmate.core.ai.AiCapability
import com.classmate.core.ai.AiCapabilityResult
import com.classmate.core.ai.AiCapabilityRouter
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ai.AiExecutionStatus
import com.classmate.core.ai.AiStage
import com.classmate.core.ai.StageOutcome

/** Map a capture error to the router's content-free status (so the route decision stays honest + log-safe). */
fun CaptureError.toAiStatus(): AiExecutionStatus = when (this) {
    CaptureError.ConfigMissing -> AiExecutionStatus.CONFIG_MISSING
    CaptureError.NetworkUnavailable -> AiExecutionStatus.NETWORK_UNAVAILABLE
    CaptureError.UnsupportedFormat -> AiExecutionStatus.UNSUPPORTED_MODALITY
    CaptureError.AudioTooLong, CaptureError.InvalidAudio -> AiExecutionStatus.UNSUPPORTED_MODALITY
    else -> AiExecutionStatus.FAILED
}

/**
 * Routes the image-study capture through Cloud → On-device → Manual:
 *   - CLOUD: official OCR text (1737) → ImageStudyDraft.ocrNormalizedText.
 *   - ON_DEVICE: the on-device multimodal SEMANTIC DRAFT (端侧蓝心) — a parallel track, NOT an OCR replacement.
 *   - MANUAL: an always-editable draft when neither track produced text.
 *
 * The draft is dual-track (OCR text and the on-device draft coexist); the router only decides which SOURCE
 * is the headline. The result requires user confirmation before it can enter course materials.
 */
class RoutedImageStudyDraftUseCase(
    private val ocrProvider: OcrProvider,
    private val router: AiCapabilityRouter = AiCapabilityRouter(),
    private val idGen: () -> String = { java.util.UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun create(imageBytes: ByteArray, origin: String, onDeviceDraftText: String): AiCapabilityResult<ImageStudyDraft> {
        val id = idGen()
        val ocr = ocrProvider.recognize(imageBytes)
        val ocrText = (ocr as? CaptureResult.Success)?.value?.normalizedText().orEmpty()
        val draft = when (ocr) {
            is CaptureResult.Success -> ImageStudyDraft(
                id = id, origin = origin,
                ocrBlocks = ocr.value.blocks,
                ocrNormalizedText = ocrText,
                onDeviceDraftText = onDeviceDraftText,
                createdAt = clock(),
            )
            is CaptureResult.Failure -> ImageStudyDraft(
                id = id, origin = origin,
                ocrError = ocr.failure.error,
                onDeviceDraftText = onDeviceDraftText,
                createdAt = clock(),
            )
        }
        val cloudStatus = (ocr as? CaptureResult.Failure)?.failure?.error?.toAiStatus() ?: AiExecutionStatus.SUCCESS

        return router.route(
            capability = AiCapability.OCR_TEXT_EXTRACTION,
            stages = listOf(
                AiStage(AiExecutionSource.CLOUD) {
                    if (ocrText.isNotBlank()) StageOutcome.Produced(draft) else StageOutcome.Unavailable(cloudStatus)
                },
                AiStage(AiExecutionSource.ON_DEVICE) {
                    if (onDeviceDraftText.isNotBlank()) StageOutcome.Produced(draft) else StageOutcome.Unavailable(AiExecutionStatus.UNSUPPORTED_MODALITY)
                },
            ),
            terminal = AiStage(AiExecutionSource.MANUAL) { StageOutcome.Produced(draft) },
        )
    }
}
