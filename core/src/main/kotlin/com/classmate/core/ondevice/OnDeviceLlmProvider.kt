package com.classmate.core.ondevice

/**
 * The single abstraction over "the on-device vivo BlueLM 3B model". It mirrors the official SDK
 * lifecycle (init / generate / interrupt / release) at a level the pure-Kotlin core can own, while
 * the concrete Android implementation (LlmManager / LlmConfig over llm-sdk-release.aar) lives in the
 * app module and is supplied only when the SDK is actually bundled.
 *
 * Contract:
 *  - [generate] is BLOCKING and MUST be called off the UI thread (the app controller does this).
 *  - Implementations NEVER throw for expected failures — they return
 *    [OnDeviceGenerationResult.Unavailable] / [OnDeviceGenerationResult.Error].
 *  - Implementations NEVER log the prompt or the generated text.
 *  - When the SDK or device is unavailable, [status] is honest (not [OnDeviceLlmStatus.AVAILABLE]).
 */
interface OnDeviceLlmProvider {

    /** Current honest readiness of the on-device model. */
    fun status(): OnDeviceLlmStatus

    /** True only when the model can actually generate right now. */
    fun isAvailable(): Boolean = status().available

    /** A safe, content-free readiness snapshot for the Settings diagnostic card. */
    fun diagnostic(): OnDeviceLlmDiagnostic

    /**
     * Run an actual self-test (real bridges: init + a single fixed [question] generation) and return
     * a rich, content-free diagnostic. Blocking; call on a background thread. The default just returns
     * the lightweight [diagnostic] snapshot (used by the missing-SDK seam). Never logs the prompt or
     * the full output — only a bounded preview / safe metadata.
     */
    fun runTextProbe(question: String): OnDeviceLlmDiagnostic = diagnostic()

    /**
     * Run one generation for [profile] over the already-templated [prompt] (see
     * [OnDevicePromptTemplate]). Blocking; call on a background thread. Never throws for expected
     * failures. Never logs [prompt] or the returned text.
     */
    fun generate(profile: OnDeviceLlmTaskProfile, prompt: String): OnDeviceGenerationResult

    /** Best-effort interrupt of an in-flight generation. No-op when not generating. */
    fun interrupt() {}

    /** Release native resources. After release, [status] becomes unavailable until re-init. */
    fun release() {}

    /**
     * Update the on-device model directory (P5). Default no-op (the seam has no model). Real bridges
     * release any cached session so the next probe re-inits against the new path. We never scan or
     * enumerate this path — it is only handed to the SDK.
     */
    fun updateModelPath(path: String) {}
}
