package com.classmate.core.ondevice

import com.classmate.core.ask.AskChatResult
import com.classmate.core.ask.AskChatSeam
import com.classmate.core.prompt.Prompt

/**
 * Adapts the on-device model to the "Ask This Lesson" [AskChatSeam], so the grounded-QA engine can
 * use the local 3B model as an intelligent fallback BELOW the cloud BlueLM and ABOVE the deterministic
 * local-evidence answer.
 *
 * Returns null (→ caller uses the next seam or the engine's LocalRule fallback) whenever the
 * on-device model is unavailable or does not produce usable text. It NEVER logs the prompt or the
 * generated answer; only the provider kind + a model label are surfaced as safe metadata.
 *
 * Blocking: the engine already runs the seam off the main thread, so this calls the (blocking)
 * provider directly.
 */
class OnDeviceAskChatSeam(
    private val provider: OnDeviceLlmProvider,
) : AskChatSeam {

    override fun chat(prompt: Prompt, repairHint: String?): AskChatResult? {
        if (!provider.isAvailable()) return null
        val user = if (repairHint.isNullOrBlank()) prompt.user else prompt.user + "\n修正提示：" + repairHint
        val templated = OnDevicePromptTemplate.format(prompt.system, user)
        return when (val result = provider.generate(OnDeviceLlmTaskProfile.ASK, templated)) {
            is OnDeviceGenerationResult.Success ->
                result.text.takeIf { it.isNotBlank() }
                    ?.let { AskChatResult(it, providerName = ON_DEVICE_PROVIDER_NAME, modelName = ON_DEVICE_MODEL_LABEL) }
            is OnDeviceGenerationResult.Unavailable -> null
            is OnDeviceGenerationResult.Error -> null
        }
    }

    companion object {
        const val ON_DEVICE_PROVIDER_NAME = "ONDEVICE_BLUELM"
        const val ON_DEVICE_MODEL_LABEL = "bluelm-3b-ondevice"
    }
}

/**
 * Chains several [AskChatSeam]s, returning the first non-null reply. Used to express the honest
 * order "cloud BlueLM → on-device BlueLM" without touching [com.classmate.core.provider.ProviderResolver].
 * When every seam declines, the engine falls back to its LocalRule evidence answer.
 */
class CompositeAskChatSeam(
    private val seams: List<AskChatSeam>,
) : AskChatSeam {
    override fun chat(prompt: Prompt, repairHint: String?): AskChatResult? {
        for (seam in seams) {
            seam.chat(prompt, repairHint)?.let { return it }
        }
        return null
    }
}
