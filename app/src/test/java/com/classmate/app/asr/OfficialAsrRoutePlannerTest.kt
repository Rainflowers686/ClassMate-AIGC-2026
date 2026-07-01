package com.classmate.app.asr

import com.classmate.app.platform.OfficialProviderConfigSummary
import com.classmate.app.platform.ProviderConfigSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialAsrRoutePlannerTest {
    @Test
    fun officialRealtimeConfigTakesPriorityOverSystemRecognizer() {
        val plan = OfficialAsrRoutePlanner.plan(
            ProviderConfigSummary.defaults().copy(
                officialProviders = OfficialProviderConfigSummary(
                    realtimeAsrConfigured = true,
                    asrLongConfigured = true,
                ),
            ),
            systemRecognizerAvailable = true,
        )

        assertEquals(OfficialAsrRouteKind.OFFICIAL_REALTIME, plan.primary)
        assertTrue(plan.showSystemFallbackAction)
        assertTrue(plan.detail.contains("官方实时 ASR"))
        assertTrue(plan.detail.contains("系统") == false || plan.detail.contains("fallback"))
    }

    @Test
    fun officialLongAsrIsPrimaryWhenRealtimeIsMissing() {
        val plan = OfficialAsrRoutePlanner.plan(
            ProviderConfigSummary.defaults().copy(
                officialProviders = OfficialProviderConfigSummary(asrLongConfigured = true),
            ),
            systemRecognizerAvailable = false,
        )

        assertEquals(OfficialAsrRouteKind.OFFICIAL_LONG_AFTER_RECORDING, plan.primary)
        assertTrue(plan.showOfficialLongAction)
        assertFalse(plan.showSystemFallbackAction)
        assertEquals("官方 ASR 转写录音", plan.afterRecordingAction)
    }

    @Test
    fun missingOfficialAsrDoesNotClaimRealtimeAvailability() {
        val plan = OfficialAsrRoutePlanner.plan(
            ProviderConfigSummary.defaults(),
            systemRecognizerAvailable = true,
        )

        assertEquals(OfficialAsrRouteKind.MANUAL_TRANSCRIPT, plan.primary)
        assertFalse(plan.showOfficialLongAction)
        assertTrue(plan.showManualTranscriptAction)
        assertTrue(plan.showSystemFallbackAction)
        assertTrue(plan.headline.contains("官方 ASR 未配置"))
        assertFalse(plan.detail.contains("实时转写可用"))
    }
}
