package com.classmate.core.provider

const val OFFICIAL_QWEN_MAX_COMPLETION_TOKENS = 65_536

/**
 * Quality presets for cloud text-generation calls used by learning tasks.
 *
 * The official large-model docs say qwen3.5-plus supports `enable_thinking` and
 * `reasoning_effort` values minimal/low/medium/high. Product "Max/Professional"
 * maps to API `high`; never send a raw `max` value. Reasoning text is never surfaced
 * or logged by response readers.
 */
enum class CloudModelQualityProfile(
    val temperature: Double,
    val topP: Double,
    val maxTokens: Int,
    val maxCompletionTokens: Int,
    val enableThinking: Boolean,
    val reasoningEffort: ReasoningEffort,
    val frequencyPenalty: Double,
    val presencePenalty: Double,
    val timeoutSeconds: Int,
    val retryCount: Int,
    val intendedTasks: Set<String>,
) {
    FAST(
        temperature = 0.20,
        topP = 0.85,
        maxTokens = 2048,
        maxCompletionTokens = 8192,
        enableThinking = false,
        reasoningEffort = ReasoningEffort.LOW,
        frequencyPenalty = 0.10,
        presencePenalty = 0.05,
        timeoutSeconds = 300,
        retryCount = 1,
        intendedTasks = setOf("FAST_UI_FEEDBACK", "short_titles", "small_status_copy"),
    ),
    BALANCED(
        temperature = 0.35,
        topP = 0.90,
        maxTokens = 4096,
        maxCompletionTokens = 32768,
        enableThinking = false,
        reasoningEffort = ReasoningEffort.MEDIUM,
        frequencyPenalty = 0.15,
        presencePenalty = 0.08,
        timeoutSeconds = 360,
        retryCount = 1,
        intendedTasks = setOf("DEFAULT_LEARNING", "normal_ask", "light_summary"),
    ),
    DEEP_STUDY(
        temperature = 0.30,
        topP = 0.90,
        maxTokens = 4096,
        maxCompletionTokens = OFFICIAL_QWEN_MAX_COMPLETION_TOKENS,
        enableThinking = true,
        reasoningEffort = ReasoningEffort.HIGH,
        frequencyPenalty = 0.20,
        presencePenalty = 0.08,
        timeoutSeconds = 600,
        retryCount = 2,
        intendedTasks = setOf(
            "COURSE_ANALYSIS",
            "ASK_WITH_EVIDENCE",
            "PRACTICE_GENERATION",
            "PRACTICE_FEEDBACK",
            "REVIEW_PLAN",
            "STUDY_REPORT",
            "COURSE_ESSENCE_SCRIPT",
            "EXPORT_REFINEMENT",
        ),
    );

    fun toRequestOptions(
        config: ProviderConfig,
        stream: Boolean = config.stream,
        maxTokensCap: Int? = null,
        featureSupport: CloudModelFeatureSupport = CloudModelFeatureSupport.QWEN_COMPATIBLE,
    ): BlueLMRequestOptions {
        val configuredMax = config.maxTokens.takeIf { it > maxTokens } ?: maxTokens
        val profileMax = maxTokensCap?.let { minOf(maxTokens, it) } ?: maxTokens
        return BlueLMRequestOptions(
            stream = stream,
            temperature = temperature,
            maxTokens = minOf(configuredMax, profileMax).coerceAtLeast(1),
            maxCompletionTokens = maxCompletionTokens,
            topP = topP,
            qualityProfile = this,
            enableThinking = enableThinking,
            reasoningEffort = reasoningEffort,
            frequencyPenalty = frequencyPenalty,
            presencePenalty = presencePenalty,
            featureSupport = featureSupport,
        )
    }

}

enum class ReasoningEffort(val wireValue: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
}

data class CloudModelFeatureSupport(
    val supportsEnableThinking: Boolean = true,
    val supportsReasoningEffort: Boolean = true,
    val supportsMaxCompletionTokens: Boolean = true,
    val supportsFrequencyPenalty: Boolean = true,
    val supportsPresencePenalty: Boolean = true,
) {
    companion object {
        val QWEN_COMPATIBLE = CloudModelFeatureSupport()
        val COMPATIBILITY_UNSUPPORTED = CloudModelFeatureSupport(
            supportsEnableThinking = false,
            supportsReasoningEffort = false,
            supportsMaxCompletionTokens = false,
            supportsFrequencyPenalty = false,
            supportsPresencePenalty = false,
        )
    }
}

enum class CloudLearningTask(val qualityProfile: CloudModelQualityProfile) {
    FAST_UI_FEEDBACK(CloudModelQualityProfile.FAST),
    DEFAULT_LEARNING(CloudModelQualityProfile.BALANCED),
    COURSE_ANALYSIS(CloudModelQualityProfile.DEEP_STUDY),
    ASK_WITH_EVIDENCE(CloudModelQualityProfile.DEEP_STUDY),
    PRACTICE_GENERATION(CloudModelQualityProfile.DEEP_STUDY),
    PRACTICE_FEEDBACK(CloudModelQualityProfile.DEEP_STUDY),
    REVIEW_PLAN(CloudModelQualityProfile.DEEP_STUDY),
    STUDY_REPORT(CloudModelQualityProfile.DEEP_STUDY),
    COURSE_ESSENCE_SCRIPT(CloudModelQualityProfile.DEEP_STUDY),
}
