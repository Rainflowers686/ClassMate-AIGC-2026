package com.classmate.core.adapter

/**
 * Single failure type providers raise. The [reason] is the discriminator the
 * ViewModel switches on when deciding whether to fall back to another provider.
 *
 * Keep this small — every value here turns into a UI / log surface.
 */
class ModelCallException(
    val reason: Reason,
    message: String,
    cause: Throwable? = null
) : RuntimeException(buildMessage(reason, message), cause) {

    enum class Reason {
        /** config.local.json missing or required field blank. */
        CONFIG_MISSING,
        /** HTTP transport failed OR the server returned non-2xx. */
        HTTP_ERROR,
        /** Response body did not contain extractable JSON. */
        JSON_EXTRACTION_FAILED,
        /** JSON was extractable but did not match CourseAnalysisResult shape. */
        DESERIALIZE_FAILED,
        /** ResultValidator reported fatal issues. */
        VALIDATION_FAILED,
        /** Provider is intentionally not wired (e.g. BlueLM without official contract). */
        PROVIDER_NOT_IMPLEMENTED
    }

    companion object {
        private fun buildMessage(reason: Reason, message: String): String = "[$reason] $message"
    }
}
