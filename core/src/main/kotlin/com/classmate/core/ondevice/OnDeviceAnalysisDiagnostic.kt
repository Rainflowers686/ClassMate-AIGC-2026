package com.classmate.core.ondevice

/**
 * Stage 8D-2 — a content-free, honest breakdown of one on-device CourseAnalysis attempt. It exists so
 * the UI never collapses a generate timeout / bad-JSON / validation failure into a generic
 * "model unavailable". No prompt or model output is carried; only states/booleans/codes.
 */
data class OnDeviceAnalysisDiagnostic(
    val sdkPresent: Boolean,
    val modelDir: String,
    val allFilesAccess: Boolean,
    val modelFilesReady: Boolean,
    /** True when a text init/generate has already succeeded — a positive availability signal. */
    val textGenerationLastSuccess: Boolean,
    val courseAnalysisAttempted: Boolean,
    /** SUCCESS / FAILED / TIMEOUT / NOT_RUN. */
    val generateState: String,
    /** Reject code (INVALID_JSON / VALIDATION_FAILED / GENERATE_FAILED / ...); null when accepted. */
    val rejectReason: String?,
    /** 云端蓝心 / 端侧蓝心 / 安全占位 (set by the caller). */
    val finalSource: String,
    /** Stage 8D-3: bounded JSON-shape facts (from [OnDeviceJsonDiagnostic.safeLines]) on rejection. */
    val extraLines: List<String> = emptyList(),
) {
    fun safeLines(): List<String> = listOf(
        "sdk_present=$sdkPresent",
        "model_dir=$modelDir",
        "all_files_access=$allFilesAccess",
        "model_files_ready=$modelFilesReady",
        "text_generation_last_success=$textGenerationLastSuccess",
        "course_analysis_attempted=$courseAnalysisAttempted",
        "course_analysis_generate_state=$generateState",
        "course_analysis_reject_reason=${rejectReason ?: "-"}",
        "course_analysis_final_source=$finalSource",
    ) + extraLines
}

/** The on-device CourseAnalysis outcome plus its honest diagnostic, returned together to the caller. */
data class OnDeviceAnalysisRun(
    val outcome: OnDeviceCourseAnalysis.Outcome,
    val diagnostic: OnDeviceAnalysisDiagnostic,
)
