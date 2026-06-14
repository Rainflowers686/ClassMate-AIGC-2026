package com.classmate.core.ondevice

/**
 * A safe, content-free snapshot of the on-device model's readiness, shown in the Settings
 * "端侧模型诊断" card. It carries ONLY enumerated/boolean fields and the official preset directory
 * label — never a user file listing, never prompt/output text, never SDK reasoning/debug.
 *
 * We deliberately do not scan the device file system here: [modelDirChecked] stays false unless a
 * real SDK bridge performed a bounded existence check (which needs no broad storage permission and
 * never enumerates files).
 */
data class OnDeviceLlmDiagnostic(
    val status: OnDeviceLlmStatus,
    val sdkPresent: Boolean,
    val modelDir: String,
    /** Whether a bounded model-dir existence check was performed at all (false for the seam). */
    val modelDirChecked: Boolean = false,
    /** Result of that check; null when [modelDirChecked] is false (we never guessed/scanned). */
    val modelDirPresent: Boolean? = null,
    val initAttempted: Boolean = false,
    val initSucceeded: Boolean = false,
    val backendLabel: String = DEFAULT_BACKEND_LABEL,
    val latencyMs: Long? = null,
    // --- Stage 8A-2 real-bridge probe fields (all optional / content-free) ---
    /** True when the reflected SDK signature matched; false on SDK_SIGNATURE_MISMATCH; null untested. */
    val signatureOk: Boolean? = null,
    val initState: OnDeviceProbeState = OnDeviceProbeState.INIT_NOT_TESTED,
    val generateState: OnDeviceProbeState = OnDeviceProbeState.GENERATE_NOT_TESTED,
    /** A short (≤[MAX_PREVIEW] chars) redacted preview of the generated text. Never the full output. */
    val outputPreview: String? = null,
    /** Short SDK/onError code; never a raw message or stack trace. */
    val errorCode: String? = null,
    val fallbackAvailable: Boolean = true,
) {
    /** Short k=v lines for the redacted diagnostic UI. No content, no file listings, no secrets. */
    fun safeLines(): List<String> = buildList {
        add("ondevice_status=${status.name}")
        add("sdk_present=$sdkPresent")
        add("model_dir=$modelDir")
        add("model_dir_checked=$modelDirChecked")
        if (modelDirChecked) add("model_dir_present=$modelDirPresent")
        signatureOk?.let { add("signature_ok=$it") }
        add("init_state=${initState.name}")
        add("generate_state=${generateState.name}")
        add("init_attempted=$initAttempted")
        if (initAttempted) add("init_succeeded=$initSucceeded")
        add("backend=$backendLabel")
        latencyMs?.let { add("latency_ms=$it") }
        errorCode?.let { add("error_code=$it") }
        add("fallback_available=$fallbackAvailable")
        if (!status.available) add("reason=${status.reasonCode}")
    }

    companion object {
        const val DEFAULT_BACKEND_LABEL = "vivo-bluelm-3b-ondevice"
        const val MAX_PREVIEW = 80

        /** Truncate any model text to a safe preview length for display/logging. */
        fun safePreview(text: String?, max: Int = MAX_PREVIEW): String? =
            text?.takeIf { it.isNotBlank() }?.trim()?.let { if (it.length <= max) it else it.take(max) + "…" }
    }
}

/**
 * A safe, content-free snapshot of the on-device MULTIMODAL (VIT) probe (P4). It records whether the
 * SDK exposes the multimodal field + callVit method, the bounded RGB conversion stats of a tiny built
 * -in test image, the callVit return code, and the multimodal generate step — never the image, the
 * prompt, or the full output.
 *
 * This is an experimental DIAGNOSTIC only — it does NOT wire image understanding into the course
 * material pipeline.
 */
data class OnDeviceMultimodalDiagnostic(
    val state: OnDeviceProbeState,
    val sdkSupportsMultimodalField: Boolean,
    val callVitMethodPresent: Boolean,
    val modelDir: String,
    val testImageWidth: Int,
    val testImageHeight: Int,
    val rgbByteLength: Int,
    val callVitReturnCode: Int? = null,
    val generateState: OnDeviceProbeState = OnDeviceProbeState.GENERATE_NOT_TESTED,
    val outputPreview: String? = null,
    val errorCode: String? = null,
    val backendLabel: String = OnDeviceLlmDiagnostic.DEFAULT_BACKEND_LABEL,
) {
    fun safeLines(): List<String> = buildList {
        add("multimodal_state=${state.name}")
        add("sdk_multimodal_field=$sdkSupportsMultimodalField")
        add("call_vit_method_present=$callVitMethodPresent")
        add("model_dir=$modelDir")
        add("test_image=${testImageWidth}x$testImageHeight")
        add("rgb_byte_length=$rgbByteLength")
        callVitReturnCode?.let { add("call_vit_ret=$it") }
        add("generate_state=${generateState.name}")
        errorCode?.let { add("error_code=$it") }
    }
}
