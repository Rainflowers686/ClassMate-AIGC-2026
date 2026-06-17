package com.classmate.core.provider

/**
 * Quality presets for cloud text-generation calls used by learning tasks.
 *
 * qwen3.5-plus still keeps the compatibility guard in [VivoOpenAIChatRequestFactory]:
 * enable_thinking=false. These profiles improve output quality through documented chat
 * parameters only, without enabling hidden reasoning output.
 */
enum class CloudModelQualityProfile(
    val temperature: Double,
    val topP: Double,
    val maxTokens: Int,
) {
    FAST(
        temperature = 0.15,
        topP = 0.60,
        maxTokens = 1200,
    ),
    BALANCED(
        temperature = 0.25,
        topP = 0.70,
        maxTokens = 2200,
    ),
    DEEP_STUDY(
        temperature = 0.30,
        topP = 0.70,
        maxTokens = 4096,
    );

    fun toRequestOptions(
        config: ProviderConfig,
        stream: Boolean = config.stream,
        maxTokensCap: Int? = null,
    ): BlueLMRequestOptions {
        val configuredMax = config.maxTokens.takeIf { it > 0 } ?: maxTokens
        val profileMax = maxTokensCap?.let { minOf(maxTokens, it) } ?: maxTokens
        return BlueLMRequestOptions(
            stream = stream,
            temperature = temperature,
            maxTokens = minOf(configuredMax, profileMax).coerceAtLeast(1),
            topP = topP,
            qualityProfile = this,
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
