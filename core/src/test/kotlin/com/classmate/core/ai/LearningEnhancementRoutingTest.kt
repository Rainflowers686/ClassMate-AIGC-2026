package com.classmate.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-6: the Adaptive AI Learning Layer must run a real second pass at high-value nodes, route Cloud →
 * On-device → local template, and label its source honestly (never a fake button, never fallback-as-蓝心).
 */
class LearningEnhancementRoutingTest {

    private val useCase = RoutedEnhancementUseCase()

    @Test
    fun cloudResultIsUsedAndLabelledCloudWhenAvailable() {
        val result = useCase.enhance(
            type = AiEnhancementType.EVIDENCE_EXPLANATION,
            cloud = { StageOutcome.Produced("蓝心生成的证据解释") },
            localTemplate = { "本地模板解释" },
        )
        assertTrue(result.isSuccess)
        assertEquals(AiExecutionSource.CLOUD, result.source)
        assertEquals("蓝心生成的证据解释", result.value?.text)
    }

    @Test
    fun fallsBackToLocalTemplateWithHonestSourceWhenModelsUnavailable() {
        val result = useCase.enhance(
            type = AiEnhancementType.POST_QUIZ_FEEDBACK,
            cloud = { StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) },
            onDevice = { StageOutcome.Unavailable(AiExecutionStatus.UNSUPPORTED_MODALITY) },
            localTemplate = { LocalEnhancementTemplates.postQuizFeedback(total = 5, correct = 2, weakTitles = listOf("受力分析")) },
        )
        assertTrue(result.isSuccess)
        // Honest: a local template result is NOT labelled as 蓝心.
        assertEquals(AiExecutionSource.SAFE_PLACEHOLDER, result.source)
        assertFalse("local output must not be tagged cloud", result.source == AiExecutionSource.CLOUD)
        assertTrue(result.value!!.text.contains("受力分析"))
    }

    @Test
    fun onDevicePreferredModeTriesOnDeviceFirst() {
        val result = useCase.enhance(
            type = AiEnhancementType.WEAKNESS_VARIANTS,
            cloud = { StageOutcome.Produced("cloud") },
            onDevice = { StageOutcome.Produced("on-device") },
            localTemplate = { "local" },
            mode = AiExecutionMode.ON_DEVICE_PREFERRED,
        )
        assertEquals(AiExecutionSource.ON_DEVICE, result.source)
        assertEquals("on-device", result.value?.text)
    }

    @Test
    fun evidenceExplanationIsGroundedAndHedgesWeakLinks() {
        val strong = LocalEnhancementTemplates.evidenceExplanation("牛顿第二定律", "F = ma 描述了力与加速度的关系", weak = false)
        val weak = LocalEnhancementTemplates.evidenceExplanation("牛顿第二定律", "F = ma 描述了力与加速度的关系", weak = true)
        assertTrue("explanation is grounded in the KP title", strong.contains("牛顿第二定律"))
        assertTrue("explanation quotes the evidence", strong.contains("F = ma"))
        assertFalse("a strong link is not hedged", strong.contains("较弱"))
        assertTrue("a weak link is hedged toward verification", weak.contains("核对"))
    }

    @Test
    fun postQuizFeedbackReflectsActualAttempt() {
        val empty = LocalEnhancementTemplates.postQuizFeedback(total = 0, correct = 0, weakTitles = emptyList())
        assertTrue("no attempt -> honest empty state", empty.contains("还没有"))
        val strong = LocalEnhancementTemplates.postQuizFeedback(total = 4, correct = 4, weakTitles = emptyList())
        assertTrue("all correct -> advance", strong.contains("进阶"))
    }
}
