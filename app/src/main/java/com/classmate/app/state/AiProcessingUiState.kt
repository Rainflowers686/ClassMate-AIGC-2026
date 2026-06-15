package com.classmate.app.state

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

