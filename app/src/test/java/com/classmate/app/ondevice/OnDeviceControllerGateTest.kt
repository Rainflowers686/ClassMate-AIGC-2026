package com.classmate.app.ondevice

import com.classmate.core.ondevice.MissingOnDeviceBlueLmBridge
import com.classmate.core.ondevice.OnDeviceErrorExplain
import com.classmate.core.ondevice.OnDeviceGenerationResult
import com.classmate.core.ondevice.OnDeviceLlmDiagnostic
import com.classmate.core.ondevice.OnDeviceLlmProvider
import com.classmate.core.ondevice.OnDeviceLlmStatus
import com.classmate.core.ondevice.OnDeviceLlmTaskProfile
import com.classmate.core.ondevice.OnDeviceMultimodalDiagnostic
import com.classmate.core.ondevice.OnDeviceMultimodalProbe
import com.classmate.core.ondevice.OnDeviceProbeState
import com.classmate.core.ondevice.RgbImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the Stage 8A-2.2 permission / text-init gate never lets a native init/callVit run when it
 * shouldn't — this is the fix for the -2105 churn and the multimodal crash. The gate decisions are
 * synchronous, so no coroutines are needed.
 */
class OnDeviceControllerGateTest {

    /** A spy that records native entry points and reports SDK present (so gating applies). */
    private class SpyProvider(
        @Volatile var available: Boolean = false,
        private val sdkPresent: Boolean = true,
    ) : OnDeviceLlmProvider, OnDeviceMultimodalProbe {
        var runTextProbeCalls = 0
        var probeMultimodalCalls = 0
        var generateCalls = 0

        override fun status(): OnDeviceLlmStatus =
            if (available) OnDeviceLlmStatus.AVAILABLE else OnDeviceLlmStatus.SDK_PRESENT

        override fun diagnostic(): OnDeviceLlmDiagnostic =
            OnDeviceLlmDiagnostic(status = status(), sdkPresent = sdkPresent, modelDir = "/sdcard/1225")

        override fun runTextProbe(question: String): OnDeviceLlmDiagnostic {
            runTextProbeCalls++
            available = true // a successful init makes the model available
            return diagnostic().copy(
                status = OnDeviceLlmStatus.AVAILABLE,
                initState = OnDeviceProbeState.INIT_SUCCESS,
                generateState = OnDeviceProbeState.GENERATE_SUCCESS,
            )
        }

        override fun generate(profile: OnDeviceLlmTaskProfile, prompt: String): OnDeviceGenerationResult {
            generateCalls++
            return OnDeviceGenerationResult.Success("ok", 1, 1)
        }

        override fun supportsMultimodal(): Boolean = true

        override fun probeMultimodal(image: RgbImage, question: String): OnDeviceMultimodalDiagnostic {
            probeMultimodalCalls++
            return OnDeviceMultimodalDiagnostic(
                state = OnDeviceProbeState.CALL_VIT_SUCCESS,
                sdkSupportsMultimodalField = true,
                callVitMethodPresent = true,
                modelDir = "/sdcard/1225",
                testImageWidth = image.width,
                testImageHeight = image.height,
                rgbByteLength = image.bytes.size,
                callVitReturnCode = 0,
                generateState = OnDeviceProbeState.GENERATE_SUCCESS,
            )
        }
    }

    @Test
    fun textInitIsBlockedAndNativeUntouchedWhenAllFilesAccessDenied() {
        val spy = SpyProvider()
        val controller = OnDeviceLlmController(spy)

        val report = controller.runTextProbeBlocking(allFilesGranted = false)

        assertEquals("native init must NOT be called", 0, spy.runTextProbeCalls)
        assertEquals(OnDeviceErrorExplain.ALL_FILES_ACCESS_REQUIRED, report.errorCode)
        assertFalse(controller.isTextProbeAllowed(false))
    }

    @Test
    fun textInitRunsWhenAllFilesAccessGranted() {
        val spy = SpyProvider()
        val controller = OnDeviceLlmController(spy)

        val report = controller.runTextProbeBlocking(allFilesGranted = true)

        assertEquals(1, spy.runTextProbeCalls)
        assertEquals(OnDeviceLlmStatus.AVAILABLE, report.status)
    }

    @Test
    fun multimodalNeverCallsNativeWhenAllFilesAccessDenied() {
        val spy = SpyProvider()
        val controller = OnDeviceLlmController(spy)

        val report = controller.runMultimodalBlocking(allFilesGranted = false)

        assertEquals("native callVit must NOT be called", 0, spy.probeMultimodalCalls)
        assertEquals(OnDeviceProbeState.MULTIMODAL_UNAVAILABLE, report.state)
        assertEquals(OnDeviceErrorExplain.ALL_FILES_ACCESS_REQUIRED, report.errorCode)
    }

    @Test
    fun multimodalBlockedUntilTextInitSucceededEvenWithAccess() {
        val spy = SpyProvider(available = false) // text init not yet successful
        val controller = OnDeviceLlmController(spy)

        val report = controller.runMultimodalBlocking(allFilesGranted = true)

        assertEquals("must not crash into native before a successful text init", 0, spy.probeMultimodalCalls)
        assertEquals(OnDeviceErrorExplain.TEXT_INIT_REQUIRED, report.errorCode)
    }

    @Test
    fun multimodalRunsAfterGrantAndSuccessfulTextInit() {
        val spy = SpyProvider()
        val controller = OnDeviceLlmController(spy)

        controller.runTextProbeBlocking(allFilesGranted = true) // makes the model available
        val report = controller.runMultimodalBlocking(allFilesGranted = true)

        assertEquals(1, spy.probeMultimodalCalls)
        assertEquals(OnDeviceProbeState.CALL_VIT_SUCCESS, report.state)
    }

    @Test
    fun missingSdkNeedsNoAllFilesGateAndStaysHonest() {
        val controller = OnDeviceLlmController(MissingOnDeviceBlueLmBridge())

        // No native present -> no gate required; the probe is a safe no-op returning SDK_MISSING.
        assertTrue(controller.isTextProbeAllowed(allFilesGranted = false))
        assertEquals(OnDeviceLlmStatus.SDK_MISSING, controller.runTextProbeBlocking(false).status)
        assertEquals(
            OnDeviceProbeState.MULTIMODAL_UNAVAILABLE,
            controller.runMultimodalBlocking(false).state,
        )
    }
}
