package com.classmate.app.state

import com.classmate.core.ai.AiEnhancementType

/**
 * State for one AI second-pass enhancement surface (study-pack polish, quiz feedback, evidence
 * explanation). Carries the generated text plus an honest source label ("蓝心整理版" / "端侧模型草稿" /
 * "本地整理版" / failure note) — never a raw provider/debug string.
 */
data class EnhancementUiState(
    val type: AiEnhancementType? = null,
    val running: Boolean = false,
    val text: String = "",
    val sourceZh: String = "",
    val failed: Boolean = false,
) {
    val hasResult: Boolean get() = text.isNotBlank() && !running
    companion object {
        fun idle(): EnhancementUiState = EnhancementUiState()
    }
}

/**
 * State for a real on-device TTS audio file generated from the 听背文稿 (P0-2). Carries the safe file name +
 * size + an honest source label ("系统 TTS 生成" / "仅生成文稿" / "TTS 不可用") — never "蓝心 TTS". The raw
 * filePath is for playback/share/delete only and is never shown verbatim in normal UI.
 */
data class TtsAudioUiState(
    val running: Boolean = false,
    val filePath: String = "",
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val failed: Boolean = false,
    val message: String = "",
    val sourceZh: String = "",
) {
    val hasFile: Boolean get() = filePath.isNotBlank() && !running
}

/**
 * The source evidence behind a quiz question, surfaced INSIDE the practice screen (P0-2) so a "根据图片"
 * question always shows its image / OCR / source — never a bare instruction with no context. Carries only
 * learner-facing fields (a local image file path + quote + label), never a raw assetId or provider trace.
 */
data class PracticeEvidenceContext(
    val evidenceId: String,
    val quote: String,
    val imagePath: String,
    val sourceLabel: String,
    val isImage: Boolean,
) {
    val hasImage: Boolean get() = imagePath.isNotBlank()
}

data class AiProcessingUiState(
    val visible: Boolean = false,
    val title: String = "",
    val steps: List<String> = emptyList(),
    val activeStep: Int = 0,
    val source: String = "",
    val fallbackMessage: String? = null,
    val canCancel: Boolean = false,
    val canRetry: Boolean = false,
    val canContinueManual: Boolean = false,
) {
    companion object {
        fun hidden(): AiProcessingUiState = AiProcessingUiState()
    }
}

