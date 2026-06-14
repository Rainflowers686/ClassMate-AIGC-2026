package com.classmate.app.ondevice

import com.classmate.core.ondevice.OnDeviceGenerationResult
import com.classmate.core.ondevice.OnDeviceLlmConfig
import com.classmate.core.ondevice.OnDeviceLlmStatus
import com.classmate.core.ondevice.OnDeviceLlmTaskProfile
import com.classmate.core.ondevice.OnDeviceProbeState
import com.classmate.core.ondevice.OnDevicePromptTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealVivoOnDeviceLlmBridgeTest {

    private val fakeManager = "com.classmate.app.ondevice.fakesdk.FakeLlmManager"
    private val fakeConfig = "com.classmate.app.ondevice.fakesdk.FakeLlmConfig"
    private val fakeCallback = "com.classmate.app.ondevice.fakesdk.FakeTokenCallback"

    private fun okLoad() = VivoSdkReflection.load(fakeManager, fakeConfig, fakeCallback)

    private fun bridge(modelPath: String = "/sdcard/1225", timeoutMs: Long = 2_000) =
        RealVivoOnDeviceLlmBridge(
            initialConfig = OnDeviceLlmConfig(modelPath = modelPath),
            loadResult = okLoad(),
            generateTimeoutMs = timeoutMs,
        )

    @Test
    fun textProbeInitsAndGeneratesViaReflectionWithNoArgOnComplete() {
        val b = bridge()
        val diag = b.runTextProbe("用一句话解释什么是学习复习计划。")

        assertEquals(OnDeviceLlmStatus.AVAILABLE, diag.status)
        assertEquals(OnDeviceProbeState.INIT_SUCCESS, diag.initState)
        // Reaching GENERATE_SUCCESS proves the dynamic-Proxy NO-ARG onComplete() fired (else timeout).
        assertEquals(OnDeviceProbeState.GENERATE_SUCCESS, diag.generateState)
        assertEquals("端侧回答", diag.outputPreview)
        assertTrue(b.isAvailable())
        assertTrue(diag.signatureOk == true)
    }

    @Test
    fun generateReturnsFullTextToTheChain() {
        val b = bridge()
        val result = b.generate(OnDeviceLlmTaskProfile.ASK, OnDevicePromptTemplate.format("hi"))
        assertTrue(result is OnDeviceGenerationResult.Success)
        val success = result as OnDeviceGenerationResult.Success
        assertEquals("端侧回答", success.text) // full text, not the 80-char preview
        assertEquals(2, success.tokenCount)
    }

    @Test
    fun initFailureIsReportedAndFallbackStaysAvailable() {
        val b = bridge(modelPath = "/sdcard/INIT_FAIL")
        val diag = b.runTextProbe("q")
        assertEquals(OnDeviceLlmStatus.INIT_FAILED, diag.status)
        assertEquals(OnDeviceProbeState.INIT_FAILED, diag.initState)
        assertEquals(OnDeviceProbeState.GENERATE_NOT_TESTED, diag.generateState)
        assertTrue(diag.fallbackAvailable)
        assertFalse(b.isAvailable())
    }

    @Test
    fun generateOnErrorSurfacesSafeCodeNotMessage() {
        val b = bridge(modelPath = "/sdcard/GEN_ERROR")
        val result = b.generate(OnDeviceLlmTaskProfile.ASK, OnDevicePromptTemplate.format("hi"))
        assertTrue(result is OnDeviceGenerationResult.Error)
        val err = result as OnDeviceGenerationResult.Error
        assertEquals("ONDEVICE_42", err.code)
        assertFalse(err.safeMessage.contains("boom")) // raw SDK message never surfaced
    }

    @Test
    fun generateTimeoutInterruptsAndReportsTimeout() {
        val b = bridge(modelPath = "/sdcard/HANG", timeoutMs = 120)
        val result = b.generate(OnDeviceLlmTaskProfile.ASK, OnDevicePromptTemplate.format("hi"))
        assertTrue(result is OnDeviceGenerationResult.Error)
        assertEquals("TIMEOUT", (result as OnDeviceGenerationResult.Error).code)
    }

    @Test
    fun missingSdkIsHonestAndNeverGenerates() {
        val b = RealVivoOnDeviceLlmBridge(loadResult = VivoSdkReflection.LoadResult.Missing)
        assertEquals(OnDeviceLlmStatus.SDK_MISSING, b.status())
        assertFalse(b.isAvailable())
        assertFalse(b.supportsMultimodal())
        assertTrue(
            b.generate(OnDeviceLlmTaskProfile.ASK, "x") is OnDeviceGenerationResult.Unavailable,
        )
    }

    @Test
    fun signatureMismatchIsReportedNotCrashed() {
        val b = RealVivoOnDeviceLlmBridge(
            loadResult = VivoSdkReflection.LoadResult.SignatureMismatch("onComplete arity"),
        )
        assertEquals(OnDeviceLlmStatus.SDK_SIGNATURE_MISMATCH, b.status())
        assertEquals(false, b.diagnostic().signatureOk)
    }

    @Test
    fun multimodalProbeRunsCallVitThenGenerate() {
        val b = bridge()
        val image = BitmapToRgb.diagnosticTestImage()
        val diag = b.probeMultimodal(image, "请用一句话描述这张图片。")

        assertTrue(diag.sdkSupportsMultimodalField)
        assertTrue(diag.callVitMethodPresent)
        assertEquals(0, diag.callVitReturnCode) // int return path is representable
        assertEquals(OnDeviceProbeState.CALL_VIT_SUCCESS, diag.state)
        assertEquals(OnDeviceProbeState.GENERATE_SUCCESS, diag.generateState)
        assertEquals(12, diag.rgbByteLength) // 2x2x3
        assertEquals("端侧回答", diag.outputPreview)
    }

    @Test
    fun multimodalStopsWhenCallVitFails() {
        val b = bridge(modelPath = "/sdcard/VIT_FAIL")
        val diag = b.probeMultimodal(BitmapToRgb.diagnosticTestImage(), "q")
        assertEquals(OnDeviceProbeState.CALL_VIT_FAILED, diag.state)
        assertEquals(-7, diag.callVitReturnCode)
        // Vendor contract: do NOT run multimodal generate after a callVit failure.
        assertEquals(OnDeviceProbeState.GENERATE_NOT_TESTED, diag.generateState)
        assertNull(diag.outputPreview)
    }

    @Test
    fun bitmapToRgbDiagnosticImageIsTwoByTwoTwelveBytes() {
        val image = BitmapToRgb.diagnosticTestImage()
        assertEquals(2, image.width)
        assertEquals(2, image.height)
        assertEquals(12, image.bytes.size)
        // First pixel is red -> R=0xFF, G=0, B=0
        assertEquals(0xFF.toByte(), image.bytes[0])
        assertEquals(0x00.toByte(), image.bytes[1])
        assertEquals(0x00.toByte(), image.bytes[2])
    }

    @Test
    fun describeImageReturnsFullTextOnSuccess() {
        val b = bridge()
        val result = b.describeImage(BitmapToRgb.diagnosticTestImage(), "请描述这张图片中的学习内容。")
        assertTrue(result is OnDeviceGenerationResult.Success)
        assertEquals("端侧回答", (result as OnDeviceGenerationResult.Success).text)
    }

    @Test
    fun describeImageMissingSdkIsUnavailableNotCrash() {
        val b = RealVivoOnDeviceLlmBridge(loadResult = VivoSdkReflection.LoadResult.Missing)
        assertTrue(b.describeImage(BitmapToRgb.diagnosticTestImage(), "x") is OnDeviceGenerationResult.Unavailable)
    }

    @Test
    fun describeImageStopsWhenCallVitFails() {
        val b = bridge(modelPath = "/sdcard/VIT_FAIL")
        val result = b.describeImage(BitmapToRgb.diagnosticTestImage(), "x")
        assertTrue(result is OnDeviceGenerationResult.Error)
    }
}
