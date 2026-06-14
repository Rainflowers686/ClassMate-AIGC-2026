package com.classmate.core.ondevice

/**
 * The outcome of one on-device generation. Mirrors the shape of
 * [com.classmate.core.provider.ProviderResult]: on [Success], [text] is the model output and is
 * handed to the parser/validator pipeline but is NEVER logged. [Unavailable] and [Error] carry only
 * safe, enumerated metadata — never the prompt, the output, or vendor-internal debug/reasoning.
 */
sealed interface OnDeviceGenerationResult {

    /** Model produced text. [text] must never be logged; only [tokenCount]/[latencyMs] are safe. */
    data class Success(
        val text: String,
        val tokenCount: Int,
        val latencyMs: Long,
    ) : OnDeviceGenerationResult

    /** The model could not run (SDK missing, device unsupported, init failed, ...). */
    data class Unavailable(val status: OnDeviceLlmStatus) : OnDeviceGenerationResult

    /**
     * The model ran but failed. [code] is a short SDK error code (e.g. onError code), [safeMessage]
     * is a redacted, content-free description — never the raw SDK message which could echo content.
     */
    data class Error(val code: String, val safeMessage: String) : OnDeviceGenerationResult
}
