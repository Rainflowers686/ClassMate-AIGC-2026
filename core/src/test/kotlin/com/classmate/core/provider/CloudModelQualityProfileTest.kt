package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CloudModelQualityProfileTest {

    @Test
    fun importantLearningTasksUseDeepStudyProfile() {
        listOf(
            CloudLearningTask.COURSE_ANALYSIS,
            CloudLearningTask.ASK_WITH_EVIDENCE,
            CloudLearningTask.PRACTICE_GENERATION,
            CloudLearningTask.PRACTICE_FEEDBACK,
            CloudLearningTask.REVIEW_PLAN,
            CloudLearningTask.STUDY_REPORT,
            CloudLearningTask.COURSE_ESSENCE_SCRIPT,
        ).forEach { task ->
            assertEquals("task should not use FAST: $task", CloudModelQualityProfile.DEEP_STUDY, task.qualityProfile)
        }
    }

    @Test
    fun deepStudyProfileUsesOfficialChatQualityParamsWithThinkingEnabled() {
        val config = ProviderConfig(
            kind = ProviderKind.BLUELM,
            enabled = true,
            model = "qwen3.5-plus",
            maxTokens = 1200,
        )

        val options = CloudModelQualityProfile.DEEP_STUDY.toRequestOptions(config)

        assertEquals(false, options.stream)
        assertEquals(0.30, options.temperature, 0.0001)
        assertEquals(0.90, options.topP!!, 0.0001)
        assertEquals(4096, options.maxTokens)
        assertEquals(65_536, options.maxCompletionTokens)
        assertEquals(true, options.enableThinking)
        assertEquals(ReasoningEffort.HIGH, options.reasoningEffort)
        assertEquals(0.20, options.frequencyPenalty ?: 0.0, 0.0001)
        assertEquals(0.08, options.presencePenalty ?: 0.0, 0.0001)
        assertEquals(600, CloudModelQualityProfile.DEEP_STUDY.timeoutSeconds)
        assertEquals(2, CloudModelQualityProfile.DEEP_STUDY.retryCount)
        assertEquals(CloudModelQualityProfile.DEEP_STUDY, options.qualityProfile)
    }

    @Test
    fun repairOrCallSiteCapsCanReduceMaxTokensWithoutChangingProfile() {
        val config = ProviderConfig(
            kind = ProviderKind.BLUELM,
            enabled = true,
            model = "qwen3.5-plus",
            maxTokens = 4096,
        )

        val options = CloudModelQualityProfile.DEEP_STUDY.toRequestOptions(config, maxTokensCap = 1600)

        assertEquals(1600, options.maxTokens)
        assertEquals(65_536, options.maxCompletionTokens)
        assertEquals(CloudModelQualityProfile.DEEP_STUDY, options.qualityProfile)
    }

    @Test
    fun fastBalancedAndProfessionalMapToOfficialQwenThinkingModes() {
        assertEquals(false, CloudModelQualityProfile.FAST.enableThinking)
        assertEquals(ReasoningEffort.LOW, CloudModelQualityProfile.FAST.reasoningEffort)
        assertEquals(300, CloudModelQualityProfile.FAST.timeoutSeconds)

        assertEquals(false, CloudModelQualityProfile.BALANCED.enableThinking)
        assertEquals(ReasoningEffort.MEDIUM, CloudModelQualityProfile.BALANCED.reasoningEffort)
        assertEquals(32768, CloudModelQualityProfile.BALANCED.maxCompletionTokens)
        assertEquals(360, CloudModelQualityProfile.BALANCED.timeoutSeconds)

        assertEquals(true, CloudModelQualityProfile.DEEP_STUDY.enableThinking)
        assertEquals(ReasoningEffort.HIGH, CloudModelQualityProfile.DEEP_STUDY.reasoningEffort)
        assertEquals(600, CloudModelQualityProfile.DEEP_STUDY.timeoutSeconds)
    }
}
