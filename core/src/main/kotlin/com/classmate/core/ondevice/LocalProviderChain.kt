package com.classmate.core.ondevice

/**
 * The displayable nodes of the full resolution path. The official cloud BlueLM is the headline
 * (competition main path); the on-device BlueLM 3B is the real LOCAL INTELLIGENCE fallback; the
 * deterministic rule extractor is now only a SAFETY PLACEHOLDER — it is never presented as an
 * intelligent capability. These labels are what the UI / StudyReport show — honest and content-free
 * (no secrets, no model file names).
 *
 * Internal short labels (English): OfficialBlueLM → OnDeviceBlueLM → SafetyPlaceholder.
 * User-facing (Chinese):           云端蓝心 → 端侧蓝心 → 安全占位.
 */
enum class ProviderPathNode(val shortLabel: String, val displayZh: String) {
    /** Official cloud BlueLM chat completions — the main path. */
    BLUELM("BlueLM", "云端蓝心"),

    /** On-device vivo BlueLM 3B — the local intelligent fallback. */
    ON_DEVICE_BLUELM("OnDeviceBlueLM", "端侧蓝心"),

    /** Deterministic rule extractor — emergency safety placeholder only, NOT an AI capability. */
    LOCAL_RULE("SafetyPlaceholder", "安全占位");

    companion object {
        /**
         * Maps a provider provenance / Ask source tag to the honest Chinese source label. Anything
         * that is not the cloud or on-device model is shown as the non-AI safety placeholder.
         */
        fun sourceLabelZh(providerName: String?): String = when (providerName?.trim()?.uppercase()) {
            "BLUELM", "OFFICIALBLUELM", "OFFICIAL_BLUELM" -> BLUELM.displayZh
            "ONDEVICE_BLUELM", "ONDEVICEBLUELM", "ON_DEVICE_BLUELM" -> ON_DEVICE_BLUELM.displayZh
            else -> LOCAL_RULE.displayZh
        }
    }
}

/** What the [LocalProviderChain] actually served, plus the model text when on-device produced it. */
data class LocalChainOutcome(
    val node: ProviderPathNode,
    /** On-device text when [node] == ON_DEVICE_BLUELM; null when the caller must run LocalRule. */
    val text: String?,
    val onDeviceStatus: OnDeviceLlmStatus,
)

/**
 * The LOCAL fallback chain: try the on-device model first, otherwise defer to the caller's LocalRule
 * path. This is deliberately small and independent of the cloud [com.classmate.core.provider.ProviderResolver]
 * so it can be wired into Ask / Report / Practice fallbacks without touching the cloud main path or
 * the resolver order.
 *
 * Honesty guarantees:
 *  - When the on-device model is unavailable, [resolve] returns [ProviderPathNode.LOCAL_RULE] with
 *    null text — the caller then produces its deterministic LocalRule output. Nothing crashes.
 *  - It never labels LocalRule output as on-device AI.
 */
class LocalProviderChain(
    private val onDevice: OnDeviceLlmProvider,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** Internal short path labels, e.g. [OnDeviceBlueLM, SafetyPlaceholder] or just [SafetyPlaceholder]. */
    fun path(): List<String> = buildList {
        if (onDevice.isAvailable()) add(ProviderPathNode.ON_DEVICE_BLUELM.shortLabel)
        add(ProviderPathNode.LOCAL_RULE.shortLabel)
    }

    /** User-facing Chinese path labels, e.g. [端侧蓝心, 安全占位] or just [安全占位]. */
    fun pathZh(): List<String> = buildList {
        if (onDevice.isAvailable()) add(ProviderPathNode.ON_DEVICE_BLUELM.displayZh)
        add(ProviderPathNode.LOCAL_RULE.displayZh)
    }

    /**
     * Attempt on-device generation for [profile] over the templated [prompt]; on any unavailable /
     * error outcome, fall through to LocalRule (null text). Never throws.
     */
    fun resolve(profile: OnDeviceLlmTaskProfile, prompt: String): LocalChainOutcome {
        if (!onDevice.isAvailable()) {
            return LocalChainOutcome(ProviderPathNode.LOCAL_RULE, null, onDevice.status())
        }
        return when (val result = onDevice.generate(profile, prompt)) {
            is OnDeviceGenerationResult.Success ->
                LocalChainOutcome(ProviderPathNode.ON_DEVICE_BLUELM, result.text, OnDeviceLlmStatus.AVAILABLE)
            is OnDeviceGenerationResult.Unavailable ->
                LocalChainOutcome(ProviderPathNode.LOCAL_RULE, null, result.status)
            is OnDeviceGenerationResult.Error ->
                // Errored mid-generation -> degrade to LocalRule rather than surfacing a failure.
                LocalChainOutcome(ProviderPathNode.LOCAL_RULE, null, onDevice.status())
        }
    }
}
