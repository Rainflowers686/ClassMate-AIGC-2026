package com.classmate.core.validation

/** One thing wrong with an analysis. [code] is a short, stable, log-safe tag. */
data class ValidationIssue(
    val code: String,
    val message: String,
    val targetId: String? = null,
)

/** The outcome of validating a [com.classmate.core.model.CourseAnalysisResult]. */
data class ValidationReport(
    val ok: Boolean,
    val issues: List<ValidationIssue>,
) {
    val errorCount: Int get() = issues.size

    /**
     * A short, log-safe classification of *why* validation failed (null when ok). Used as the
     * `validation_error_type` debug field. Ordered most-specific-first.
     */
    val validationErrorType: String?
        get() {
            if (ok) return null
            val codes = issues.map { it.code }
            return when {
                codes.any { it.startsWith("EVIDENCE_") || it == "KP_NO_EVIDENCE" || it == "Q_NO_EVIDENCE" } -> "EVIDENCE_MISMATCH"
                codes.any { it == "Q_KP_UNKNOWN" || it == "Q_NO_KP" || it == "KP_RELATED_UNKNOWN" } -> "REFERENCE_BROKEN"
                codes.any { it in SCHEMA_CODES } -> "SCHEMA_MISSING_FIELD"
                else -> "VALIDATION_ERROR"
            }
        }

    companion object {
        val PASS = ValidationReport(true, emptyList())
        fun of(issues: List<ValidationIssue>): ValidationReport = ValidationReport(issues.isEmpty(), issues)

        private val SCHEMA_CODES = setOf(
            "EMPTY_ANALYSIS", "DUPLICATE_KP_ID", "KP_SEGMENT_MISSING", "Q_NO_OPTIONS", "Q_NO_CORRECT",
        )
    }
}
