package com.classmate.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** P0 acceptance A + B: CourseAnalysis main-chain route decision + Ask source mapping. */
class CourseAnalysisRoutingTest {

    // ---- CourseAnalysis route (A) ----------------------------------------------------------------

    @Test fun cloudSuccessSelectsCloud() {
        val r = CourseAnalysisRouting.decide(cloudSucceeded = true, cloudStatusCode = "OK", onDeviceAttempted = false, onDeviceAccepted = false)
        assertEquals(AiExecutionSource.CLOUD, r.source)
        assertEquals("云端蓝心", CourseAnalysisRouting.finalSourceZh(r.source))
        assertTrue(r.decision.userConfirmationRequired)
    }

    @Test fun cloudConfigMissingFallsToOnDevice() {
        val r = CourseAnalysisRouting.decide(cloudSucceeded = false, cloudStatusCode = "BLUELM:CONFIG_MISSING", onDeviceAttempted = true, onDeviceAccepted = true)
        assertEquals(AiExecutionSource.ON_DEVICE, r.source)
        assertEquals("端侧蓝心", CourseAnalysisRouting.finalSourceZh(r.source))
        assertEquals(listOf(AiExecutionSource.CLOUD, AiExecutionSource.ON_DEVICE), r.decision.attempted)
    }

    @Test fun cloudNetworkFailureFallsToOnDevice() {
        val r = CourseAnalysisRouting.decide(cloudSucceeded = false, cloudStatusCode = "BLUELM:SOCKET_TIMEOUT", onDeviceAttempted = true, onDeviceAccepted = true)
        assertEquals(AiExecutionSource.ON_DEVICE, r.source)
    }

    @Test fun cloudAndOnDeviceFailSelectSafePlaceholderWithNoValue() {
        val r = CourseAnalysisRouting.decide(cloudSucceeded = false, cloudStatusCode = "BLUELM:HTTP_NON_2XX", onDeviceAttempted = true, onDeviceAccepted = false)
        assertEquals(AiExecutionSource.SAFE_PLACEHOLDER, r.source)
        assertEquals("安全占位", CourseAnalysisRouting.finalSourceZh(r.source))
        assertNull(r.value) // no fabricated analysis to persist
        assertTrue(r.decision.userConfirmationRequired)
    }

    @Test fun cloudStatusMappingIsHonest() {
        assertEquals(AiExecutionStatus.CONFIG_MISSING, CourseAnalysisRouting.cloudStatusToAi("BLUELM:CONFIG_MISSING:401"))
        assertEquals(AiExecutionStatus.NETWORK_UNAVAILABLE, CourseAnalysisRouting.cloudStatusToAi("BLUELM:NETWORK"))
        assertEquals(AiExecutionStatus.FAILED, CourseAnalysisRouting.cloudStatusToAi("BLUELM:HTTP_NON_2XX:500"))
    }

    // ---- Ask source mapping (B) ------------------------------------------------------------------

    @Test fun askSourceMapsProviderToUnifiedVocabulary() {
        assertEquals(AiExecutionSource.CLOUD, AskRouting.sourceOf("BLUELM"))
        assertEquals(AiExecutionSource.ON_DEVICE, AskRouting.sourceOf("ONDEVICE_BLUELM"))
        assertEquals(AiExecutionSource.SAFE_PLACEHOLDER, AskRouting.sourceOf("LOCAL_RULE"))
        assertEquals("云端蓝心", AskRouting.sourceOf("BLUELM").displayZh)
        assertTrue(AskRouting.servedByModel("BLUELM"))
        assertTrue(AskRouting.servedByModel("ONDEVICE_BLUELM"))
        assertTrue(!AskRouting.servedByModel("LOCAL_RULE"))
    }
}
