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

