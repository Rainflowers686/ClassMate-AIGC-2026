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
    fun deepStudyProfileUsesOfficialChatQualityParamsWithoutThinkingChange() {
        val config = ProviderConfig(
            kind = ProviderKind.BLUELM,
            enabled = true,
            model = "qwen3.5-plus",
            maxTokens = 4096,
        )

        val options = CloudModelQualityProfile.DEEP_STUDY.toRequestOptions(config)

        assertEquals(false, options.stream)
        assertEquals(0.30, options.temperature, 0.0001)
        assertEquals(0.70, options.topP!!, 0.0001)
        assertEquals(4096, options.maxTokens)
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
        assertEquals(CloudModelQualityProfile.DEEP_STUDY, options.qualityProfile)
    }
}
