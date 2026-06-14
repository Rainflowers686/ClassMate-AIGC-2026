package com.classmate.core.ondevice

/**
 * The honest stand-in used until the official on-device SDK (llm-sdk-release.aar + native libraries)
 * is dropped into the build. It reports [OnDeviceLlmStatus.SDK_MISSING] and refuses to generate, so:
 *
 *  - the project still compiles and `assembleDebug` still produces an APK,
 *  - the Settings diagnostic shows a truthful "SDK 未接入" state,
 *  - the [LocalProviderChain] transparently degrades to the LocalRule safety net,
 *  - we NEVER pretend the on-device model is connected.
 *
 * When the real SDK arrives, the app module supplies a `VivoLlmManagerBridge : OnDeviceLlmProvider`
 * (wrapping LlmManager/LlmConfig) and this stand-in is simply no longer selected.
 */
class MissingOnDeviceBlueLmBridge(
    private val config: OnDeviceLlmConfig = OnDeviceLlmConfig(),
    private val reason: OnDeviceLlmStatus = OnDeviceLlmStatus.SDK_MISSING,
) : OnDeviceLlmProvider, OnDeviceMultimodalProbe {

    init {
        require(!reason.available) { "MissingOnDeviceBlueLmBridge must represent an unavailable state" }
    }

    override fun status(): OnDeviceLlmStatus = reason

    override fun diagnostic(): OnDeviceLlmDiagnostic = OnDeviceLlmDiagnostic(
        status = reason,
        sdkPresent = false,
        modelDir = config.modelPath,
        modelDirChecked = false,
        modelDirPresent = null,
        initAttempted = false,
        initSucceeded = false,
        signatureOk = null,
        initState = OnDeviceProbeState.INIT_NOT_TESTED,
        generateState = OnDeviceProbeState.GENERATE_NOT_TESTED,
        fallbackAvailable = true,
    )

    override fun generate(profile: OnDeviceLlmTaskProfile, prompt: String): OnDeviceGenerationResult =
        OnDeviceGenerationResult.Unavailable(reason)

    override fun supportsMultimodal(): Boolean = false

    override fun probeMultimodal(image: RgbImage, question: String): OnDeviceMultimodalDiagnostic =
        OnDeviceMultimodalDiagnostic(
            state = OnDeviceProbeState.MULTIMODAL_UNAVAILABLE,
            sdkSupportsMultimodalField = false,
            callVitMethodPresent = false,
            modelDir = config.modelPath,
            testImageWidth = image.width,
            testImageHeight = image.height,
            rgbByteLength = image.bytes.size,
        )
}
