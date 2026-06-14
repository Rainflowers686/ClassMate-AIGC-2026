package com.classmate.core.ondevice

/**
 * Optional multimodal (VIT image-encoding) capability of an on-device provider (P4). Kept separate
 * from [OnDeviceLlmProvider] because it is an experimental DIAGNOSTIC path — it does not feed the
 * course material pipeline. The app supplies the [RgbImage] (decoded from a tiny built-in test
 * bitmap); the provider performs init(multimodal=true) → callVit → generate.
 */
interface OnDeviceMultimodalProbe {

    /** Whether the loaded SDK exposes both the `multimodal` config field and the `callVit` method. */
    fun supportsMultimodal(): Boolean

    /**
     * One-shot multimodal diagnostic. Blocking — call off the UI thread. Never throws for expected
     * failures; never logs the image bytes, the prompt, or the full output. When callVit does not
     * return 0 it must stop before multimodal generate.
     */
    fun probeMultimodal(image: RgbImage, question: String): OnDeviceMultimodalDiagnostic

    /**
     * Stage 8C: full-text image understanding for the editable learning DRAFT (init(mm) → callVit →
     * generate, returning the complete text — not the ≤80-char preview). Blocking; call off the UI
     * thread. Never throws for expected failures; never logs the image/prompt/output. Default = the
     * seam is unavailable. This is image LEARNING INPUT (a draft), not OCR and not a knowledge-base write.
     */
    fun describeImage(image: RgbImage, question: String): OnDeviceGenerationResult =
        OnDeviceGenerationResult.Unavailable(OnDeviceLlmStatus.SDK_MISSING)
}

/** Outcome of turning an image into an editable learning-text draft. */
sealed interface OnDeviceImageDraftResult {
    /** Genuine on-device multimodal understanding text (still requires user confirmation to use). */
    data class Draft(val text: String) : OnDeviceImageDraftResult
    /** Unavailable → the UI shows a manual-input box. [reason] is a short content-free code. */
    data class Unavailable(val reason: String) : OnDeviceImageDraftResult
}
