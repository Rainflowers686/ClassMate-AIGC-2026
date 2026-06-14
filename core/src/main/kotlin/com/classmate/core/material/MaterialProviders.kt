package com.classmate.core.material

/**
 * Provider abstractions for transcription and OCR. This foundation ships ONLY manual and no-op
 * implementations: there is no real ASR, no real OCR, no audio/video decoding, and no network or
 * platform crawling. Every provider here truthfully reports `producesReal* = false`, so the UI can
 * never present an unimplemented capability as done.
 */

/** Turns audio/manual input into transcript segments. Real ASR is a future, injected implementation. */
interface TranscriptionProvider {
    val providerId: String
    val supportsRealtime: Boolean
    /** True only for an actual ASR backend. Manual/no-op providers MUST return false. */
    val producesRealAsr: Boolean
    /** Honest, user-facing capability note — never claims unfinished ASR is done. */
    val capabilityNote: String

    /** Build transcript segments from already-typed manual lines (no audio is read in this foundation). */
    fun transcribeManual(lines: List<String>, speaker: SpeakerLabel = SpeakerLabel.UNKNOWN, now: Long = 0L): List<TranscriptSegment>
}

/** Turns slide/board/PDF images into an OCR document. Real OCR is a future, injected implementation. */
interface OcrProvider {
    val providerId: String
    /** True only for an actual OCR backend. Manual/no-op providers MUST return false. */
    val producesRealOcr: Boolean
    /** Honest, user-facing capability note — never claims unfinished OCR is done. */
    val capabilityNote: String

    /** Build an OCR document from text the user pasted from their own images (no image is decoded). */
    fun recognizeManual(id: String, title: String, sourceType: MaterialSourceType, pageTexts: List<String>, now: Long = 0L): OcrDocument
}

/** The abstracted version of today's Live manual transcript: user-typed lines, honestly labelled. */
class ManualTranscriptProvider : TranscriptionProvider {
    override val providerId = "manual_transcript"
    override val supportsRealtime = false
    override val producesRealAsr = false
    override val capabilityNote = "手动转写：用户输入的课堂片段，未接入真实 ASR，不录音、不联网取音频。"

    override fun transcribeManual(lines: List<String>, speaker: SpeakerLabel, now: Long): List<TranscriptSegment> =
        lines.map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { i, text ->
                TranscriptSegment(
                    id = "manual_${now}_${i + 1}",
                    index = i + 1,
                    text = text,
                    speaker = speaker, // defaults to UNKNOWN; never inferred as voiceprint identity
                )
            }
}

/** OCR fallback: the user pastes text recognised from their own images; clearly a manual fallback. */
class ManualOcrProvider : OcrProvider {
    override val providerId = "manual_ocr"
    override val producesRealOcr = false
    override val capabilityNote = "手动 OCR 兜底：用户自行粘贴图片识别结果，未启用真实 OCR，不解析图片、不联网。"

    override fun recognizeManual(id: String, title: String, sourceType: MaterialSourceType, pageTexts: List<String>, now: Long): OcrDocument {
        val pages = pageTexts.map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { i, text ->
                OcrPage(
                    id = "${id}_p${i + 1}",
                    pageIndex = i + 1,
                    blocks = listOf(OcrBlock(id = "${id}_p${i + 1}_b1", text = text, role = "body")),
                )
            }
        return OcrDocument(id = id, sourceType = sourceType, title = title, pages = pages, provider = providerId, createdAt = now)
    }
}

/** Not-connected transcription provider: returns nothing and says so. Useful before ASR is wired. */
class NoOpTranscriptionProvider : TranscriptionProvider {
    override val providerId = "noop_transcript"
    override val supportsRealtime = false
    override val producesRealAsr = false
    override val capabilityNote = "ASR 暂未接入：当前不进行任何语音识别。"

    override fun transcribeManual(lines: List<String>, speaker: SpeakerLabel, now: Long): List<TranscriptSegment> = emptyList()
}

/** Not-connected OCR provider: returns an empty document and says so. Useful before OCR is wired. */
class NoOpOcrProvider : OcrProvider {
    override val providerId = "noop_ocr"
    override val producesRealOcr = false
    override val capabilityNote = "OCR 暂未接入：当前不进行任何图片文字识别。"

    override fun recognizeManual(id: String, title: String, sourceType: MaterialSourceType, pageTexts: List<String>, now: Long): OcrDocument =
        OcrDocument(id = id, sourceType = sourceType, title = title, pages = emptyList(), provider = providerId, createdAt = now)
}
