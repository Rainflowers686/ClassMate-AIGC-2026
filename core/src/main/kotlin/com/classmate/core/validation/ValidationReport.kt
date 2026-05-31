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

    companion object {
        val PASS = ValidationReport(true, emptyList())
        fun of(issues: List<ValidationIssue>): ValidationReport = ValidationReport(issues.isEmpty(), issues)
    }
}
