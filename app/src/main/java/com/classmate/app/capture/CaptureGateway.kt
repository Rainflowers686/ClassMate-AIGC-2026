package com.classmate.app.capture

import com.classmate.app.data.AppCaptureTransport
import com.classmate.app.platform.CaptureConfigLoader
import com.classmate.app.platform.CaptureConfigStatus
import com.classmate.core.capture.CaptureProviderConfig
import com.classmate.core.capture.CaptureResult
import com.classmate.core.capture.CaptureTransport
import com.classmate.core.capture.ClassroomCaptureResult
import com.classmate.core.capture.ConfirmImageStudyDraftUseCase
import com.classmate.core.capture.ConfirmTranscriptDraftUseCase
import com.classmate.core.capture.CreateImageStudyDraftUseCase
import com.classmate.core.capture.CreateTranscriptDraftUseCase
import com.classmate.core.capture.EvidenceCandidate
import com.classmate.core.capture.ImageStudyDraft
import com.classmate.core.capture.LocalEvidenceRetriever
import com.classmate.core.capture.OcrResult
import com.classmate.core.capture.RetrieveCourseEvidenceUseCase
import com.classmate.core.capture.VivoAsrProvider
import com.classmate.core.capture.VivoOcrProvider
import com.classmate.core.capture.VivoQueryRewriteProvider
import com.classmate.core.capture.VivoTextSimilarityProvider
import com.classmate.core.model.CourseSession
import com.classmate.core.transcript.TranscriptDraft

/**
 * The single app-side seam that turns the Capture core foundation into a LIVE pipeline: it loads the
 * local credentials, wires the real [AppCaptureTransport] into the Vivo providers, and exposes a small
 * blocking API for OCR/ASR smoke + draft confirmation + evidence retrieval. When no credentials are
 * configured every call degrades to [CaptureResult.Failure]/ConfigMissing — the manual paste and
 * on-device draft paths keep working. Methods block on network; callers must run them off the main thread.
 */
class CaptureGateway(
    private val configLoader: CaptureConfigLoader = CaptureConfigLoader(),
    private val transport: CaptureTransport = AppCaptureTransport(),
    private val userId: String = java.util.UUID.randomUUID().toString().replace("-", ""),
) {
    @Volatile private var config: CaptureProviderConfig = configLoader.load()

    /** Re-read credentials (e.g. after the user updates config). */
    fun refresh() { config = configLoader.load() }

    /** Value-free status for UI ("已配置 / 未配置 / 缺少 appKey"). Never exposes credential values. */
    fun status(): CaptureConfigStatus = configLoader.status()

    val isOcrConfigured: Boolean get() = config.isConfigured
    val isAsrConfigured: Boolean get() = config.isConfigured

    private fun ocrProvider() = VivoOcrProvider(config = config, transport = transport)
    private fun asrProvider() = VivoAsrProvider(config = config, transport = transport, userId = userId)

    // ── OCR ──────────────────────────────────────────────────────────────────────────────────────
    /** Run official OCR on raw image bytes. ConfigMissing/failure returned, never thrown. */
    fun recognizeImage(imageBytes: ByteArray): CaptureResult<OcrResult> = ocrProvider().recognize(imageBytes)

    /**
     * Dual-track image draft: official OCR text + the on-device multimodal draft, side by side. OCR failure
     * (incl. ConfigMissing) leaves the on-device draft editable. Result is a DRAFT — not persisted until confirmed.
     */
    fun createImageStudyDraft(imageBytes: ByteArray, origin: String, onDeviceDraftText: String): ImageStudyDraft =
        CreateImageStudyDraftUseCase(ocrProvider()).create(imageBytes, origin, onDeviceDraftText)

    fun confirmImageDraft(draft: ImageStudyDraft, editedText: String, courseTitle: String): ClassroomCaptureResult? =
        ConfirmImageStudyDraftUseCase().confirm(draft, editedText, courseTitle)

    // ── ASR (1739 long-audio) ──────────────────────────────────────────────────────────────────────
    /**
     * Run the official long-audio transcription flow on raw audio bytes into an editable draft. ConfigMissing
     * /failure returned, never thrown. [audioFormat] = "pcm" for pcm, otherwise "auto" (doc).
     */
    fun transcribeAudio(
        audioBytes: ByteArray,
        fileName: String,
        audioFormat: String,
        title: String,
        onProgress: (Int) -> Unit = {},
    ): CaptureResult<TranscriptDraft> =
        CreateTranscriptDraftUseCase(asrProvider()).fromAudio(audioBytes, fileName, audioFormat, title, onProgress)

    /** Manual paste path — always available, no provider/credentials needed. */
    fun draftFromPastedText(text: String, title: String): TranscriptDraft =
        CreateTranscriptDraftUseCase(asrProvider()).fromPastedText(text, title)

    fun confirmTranscriptDraft(draft: TranscriptDraft, courseTitle: String): ClassroomCaptureResult? =
        ConfirmTranscriptDraftUseCase().confirm(draft, courseTitle)

    // ── Evidence retrieval (local-first; optional remote rerank when configured) ─────────────────────
    fun retrieveEvidence(question: String, session: CourseSession, topN: Int = 3): List<EvidenceCandidate> {
        val useCase = RetrieveCourseEvidenceUseCase(
            localRetriever = LocalEvidenceRetriever(),
            similarityProvider = VivoTextSimilarityProvider(config = config, transport = transport),
            queryRewriteProvider = VivoQueryRewriteProvider(config = config, transport = transport),
        )
        return useCase.retrieve(question, session, topN)
    }
}
