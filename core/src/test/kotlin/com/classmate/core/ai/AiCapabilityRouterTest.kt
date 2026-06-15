package com.classmate.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCapabilityRouterTest {

    private val router = AiCapabilityRouter()
    private fun produced(v: String) = AiStage(AiExecutionSource.CLOUD) { StageOutcome.Produced(v) }
    private fun cloud(o: StageOutcome<String>) = AiStage(AiExecutionSource.CLOUD) { o }
    private fun onDevice(o: StageOutcome<String>) = AiStage(AiExecutionSource.ON_DEVICE) { o }

    // ---- Cloud → On-device → Manual/Placeholder ordering -----------------------------------------

    @Test fun cloudFirstSelectsCloudWhenItProduces() {
        val r = router.route(
            AiCapability.COURSE_ANALYSIS,
            listOf(cloud(StageOutcome.Produced("c")), onDevice(StageOutcome.Produced("d"))),
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) { StageOutcome.Produced("safe") },
        )
        assertEquals(AiExecutionSource.CLOUD, r.source)
        assertEquals("c", r.value)
        assertEquals(AiExecutionStatus.SUCCESS, r.status)
        assertEquals(listOf(AiExecutionSource.CLOUD), r.decision.attempted)
        assertTrue(r.decision.userConfirmationRequired)
        assertEquals("云端蓝心", r.sourceLabelZh)
    }

    @Test fun cloudConfigMissingFallsToOnDevice() {
        val r = router.route(
            AiCapability.COURSE_ANALYSIS,
            listOf(cloud(StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING)), onDevice(StageOutcome.Produced("d"))),
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) { StageOutcome.Produced("safe") },
        )
        assertEquals(AiExecutionSource.ON_DEVICE, r.source)
        assertEquals("d", r.value)
        assertEquals(listOf(AiExecutionSource.CLOUD, AiExecutionSource.ON_DEVICE), r.decision.attempted)
        assertEquals("端侧蓝心", r.sourceLabelZh)
    }

    @Test fun cloudNetworkUnavailableFallsToOnDevice() {
        val r = router.route(
            AiCapability.ASK,
            listOf(cloud(StageOutcome.Unavailable(AiExecutionStatus.NETWORK_UNAVAILABLE)), onDevice(StageOutcome.Produced("ans"))),
            terminal = AiStage(AiExecutionSource.MANUAL) { StageOutcome.Produced("manual") },
        )
        assertEquals(AiExecutionSource.ON_DEVICE, r.source)
        assertEquals("ans", r.value)
    }

    @Test fun bothFailFallsToTerminalPlaceholderWithConfirmation() {
        val r = router.route(
            AiCapability.COURSE_ANALYSIS,
            listOf(cloud(StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING)), onDevice(StageOutcome.Unavailable(AiExecutionStatus.FAILED))),
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) { StageOutcome.Produced("safe-editable") },
        )
        assertEquals(AiExecutionSource.SAFE_PLACEHOLDER, r.source)
        assertEquals("safe-editable", r.value)
        assertEquals("安全占位", r.sourceLabelZh)
        assertTrue(r.decision.userConfirmationRequired)
        assertTrue(r.decision.attempted.containsAll(listOf(AiExecutionSource.CLOUD, AiExecutionSource.ON_DEVICE, AiExecutionSource.SAFE_PLACEHOLDER)))
    }

    @Test fun noTerminalReturnsNullValueWithLastStatus() {
        val r = router.route(
            AiCapability.ASK,
            listOf(cloud(StageOutcome.Unavailable(AiExecutionStatus.NETWORK_UNAVAILABLE)), onDevice(StageOutcome.Unavailable(AiExecutionStatus.FAILED))),
        )
        assertNull(r.value)
        assertEquals(AiExecutionStatus.FAILED, r.status)
        assertEquals(AiExecutionSource.MANUAL, r.source) // ASK terminalSource = MANUAL
    }

    // ---- modes -----------------------------------------------------------------------------------

    @Test fun onDevicePreferredModeTriesOnDeviceFirst() {
        val r = router.route(
            AiCapability.COURSE_ANALYSIS,
            listOf(cloud(StageOutcome.Produced("c")), onDevice(StageOutcome.Produced("d"))),
            mode = AiExecutionMode.ON_DEVICE_PREFERRED,
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) { StageOutcome.Produced("safe") },
        )
        assertEquals(AiExecutionSource.ON_DEVICE, r.source)
        assertEquals("d", r.value)
    }

    @Test fun localOnlyModeSkipsCloud() {
        val r = router.route(
            AiCapability.ASK,
            listOf(cloud(StageOutcome.Produced("c")), onDevice(StageOutcome.Produced("d"))),
            mode = AiExecutionMode.LOCAL_ONLY,
            terminal = AiStage(AiExecutionSource.MANUAL) { StageOutcome.Produced("manual") },
        )
        assertEquals(AiExecutionSource.ON_DEVICE, r.source)
        assertFalse(r.decision.attempted.contains(AiExecutionSource.CLOUD))
    }

    // ---- policy ----------------------------------------------------------------------------------

    @Test fun confirmationPolicyMatchesMaterialsEntry() {
        listOf(
            AiCapability.COURSE_ANALYSIS, AiCapability.IMAGE_SEMANTIC_DRAFT, AiCapability.OCR_TEXT_EXTRACTION,
            AiCapability.ASR_TRANSCRIPTION, AiCapability.PRACTICE_GENERATION, AiCapability.REVIEW_PLAN,
        ).forEach { assertTrue("$it must require confirmation", AiFallbackPolicy.requiresConfirmation(it)) }
        listOf(AiCapability.ASK, AiCapability.EXPORT_REPORT, AiCapability.EVIDENCE_RETRIEVAL).forEach {
            assertFalse("$it must not require confirmation", AiFallbackPolicy.requiresConfirmation(it))
        }
    }

    // ---- capability routers ----------------------------------------------------------------------

    @Test fun courseAnalysisRouterCloudOnDevicePlaceholder() {
        val uc = RoutedCourseAnalysisUseCase<String>()
        assertEquals(AiExecutionSource.CLOUD, uc.analyze({ StageOutcome.Produced("cloud") }, { StageOutcome.Produced("od") }, { "safe" }).source)
        assertEquals(AiExecutionSource.ON_DEVICE, uc.analyze({ StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) }, { StageOutcome.Produced("od") }, { "safe" }).source)
        val placeholder = uc.analyze({ StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) }, { StageOutcome.Unavailable(AiExecutionStatus.FAILED) }, { "safe" })
        assertEquals(AiExecutionSource.SAFE_PLACEHOLDER, placeholder.source)
        assertEquals("safe", placeholder.value)
        assertTrue(placeholder.decision.userConfirmationRequired)
    }

    @Test fun askRouterFallsToHonestManualMessage() {
        val uc = RoutedAskUseCase()
        val onDevice = uc.ask({ StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) }, { StageOutcome.Produced("端侧回答") })
        assertEquals(AiExecutionSource.ON_DEVICE, onDevice.source)
        val manual = uc.ask({ StageOutcome.Unavailable(AiExecutionStatus.NETWORK_UNAVAILABLE) }, { StageOutcome.Unavailable(AiExecutionStatus.FAILED) })
        assertEquals(AiExecutionSource.MANUAL, manual.source)
        assertTrue(manual.value!!.contains("证据"))
        assertFalse(manual.decision.userConfirmationRequired) // Ask answers are read-only
    }
}
