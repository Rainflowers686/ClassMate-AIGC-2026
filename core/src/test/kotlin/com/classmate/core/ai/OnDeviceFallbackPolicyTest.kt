package com.classmate.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-6: a device WITHOUT the on-device 3B model (cloud-real-machine / other phones lacking the AAR + model
 * files) must still get a full learning loop. On-device is a secondary enhancement only: the unified router
 * goes Cloud (蓝心, HTTP, available on all devices) → On-device → Local, so a missing on-device stage is
 * transparently degraded and the result is honestly sourced — never a crash, never a fake "端侧已就绪".
 */
class OnDeviceFallbackPolicyTest {

    private val router = AiCapabilityRouter()

    private fun stage(source: AiExecutionSource, produces: String?) = AiStage<String>(source) {
        if (produces != null) StageOutcome.Produced(produces) else StageOutcome.Unavailable(AiExecutionStatus.UNSUPPORTED_MODALITY)
    }

    @Test
    fun withoutOnDeviceCloudStillServesEverywhere() {
        // On-device unavailable (no model on this device) but cloud configured: cloud serves.
        val result = router.route(
            capability = AiCapability.COURSE_ANALYSIS,
            stages = listOf(
                stage(AiExecutionSource.CLOUD, "云端蓝心结果"),
                stage(AiExecutionSource.ON_DEVICE, null),
            ),
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) { StageOutcome.Produced("本地基础整理") },
        )
        assertEquals(AiExecutionSource.CLOUD, result.source)
        assertTrue(result.isSuccess)
    }

    @Test
    fun withoutCloudOrOnDeviceLocalRuleTakesOver() {
        // Offline + no on-device: the terminal local-rule stage produces a real (non-empty) fallback.
        val result = router.route(
            capability = AiCapability.COURSE_ANALYSIS,
            stages = listOf(
                stage(AiExecutionSource.CLOUD, null),
                stage(AiExecutionSource.ON_DEVICE, null),
            ),
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) { StageOutcome.Produced("本地基础整理") },
        )
        assertEquals(AiExecutionSource.SAFE_PLACEHOLDER, result.source)
        assertEquals("本地基础整理", result.value)
        // The honest source label is never 蓝心 for a local fallback.
        assertTrue(result.sourceLabelZh == AiExecutionSource.SAFE_PLACEHOLDER.displayZh)
    }
}
