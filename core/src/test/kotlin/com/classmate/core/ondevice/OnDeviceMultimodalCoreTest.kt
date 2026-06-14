package com.classmate.core.ondevice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceMultimodalCoreTest {

    @Test
    fun textTemplateIsUnchangedHumanAiShape() {
        val t = OnDevicePromptTemplate.format("用户输入")
        assertTrue(t.startsWith("[|Human|]:"))
        assertTrue(t.trimEnd().endsWith("[|AI|]:"))
        assertFalse(t.contains("<image>"))
    }

    @Test
    fun multimodalTemplateInsertsImageMarkersBeforeQuestion() {
        val t = OnDevicePromptTemplate.formatMultimodal("请用一句话描述这张图片。")
        assertEquals("[|Human|]:<im_start><image><im_end>请用一句话描述这张图片。\n[|AI|]:", t)
        // Markers appear in order, before the question, between Human and AI tags.
        assertTrue(t.indexOf("<im_start>") < t.indexOf("<image>"))
        assertTrue(t.indexOf("<image>") < t.indexOf("<im_end>"))
        assertTrue(t.indexOf("<im_end>") < t.indexOf("请用一句话"))
    }

    @Test
    fun configCarriesMultimodalAndNPredictMappedFromProfile() {
        val base = OnDeviceLlmConfig()
        assertFalse(base.multimodal)
        assertEquals("/sdcard/1225/1.7.0.4_1225_mtk9500", base.modelPath)

        val report = base.withProfile(OnDeviceLlmTaskProfile.REPORT)
        assertEquals(OnDeviceLlmTaskProfile.REPORT.maxOutputTokens, report.nPredict)
        assertEquals(0.6, report.temperature, 0.0001)

        assertTrue(base.copy(multimodal = true).multimodal)
    }

    @Test
    fun newReadinessStatesAreUnavailableExceptAvailable() {
        listOf(
            OnDeviceLlmStatus.SDK_PRESENT,
            OnDeviceLlmStatus.SDK_SIGNATURE_MISMATCH,
            OnDeviceLlmStatus.MODEL_PATH_UNKNOWN,
            OnDeviceLlmStatus.MODEL_PATH_NOT_ACCESSIBLE,
        ).forEach { assertFalse("$it must be unavailable", it.available) }
        assertTrue(OnDeviceLlmStatus.AVAILABLE.available)
    }

    @Test
    fun probeStateEnumCoversAllRequiredDiagnosticSteps() {
        val names = OnDeviceProbeState.entries.map { it.name }.toSet()
        listOf(
            "SDK_MISSING", "SDK_PRESENT", "SDK_SIGNATURE_MISMATCH",
            "MODEL_PATH_UNKNOWN", "MODEL_PATH_NOT_ACCESSIBLE",
            "INIT_NOT_TESTED", "INIT_SUCCESS", "INIT_FAILED",
            "GENERATE_NOT_TESTED", "GENERATE_SUCCESS", "GENERATE_FAILED",
            "MULTIMODAL_SUPPORTED", "MULTIMODAL_UNAVAILABLE",
            "CALL_VIT_SUCCESS", "CALL_VIT_FAILED", "FALLBACK_LOCAL_RULE",
        ).forEach { assertTrue("missing probe state: $it", it in names) }
    }

    @Test
    fun safePreviewTruncatesToEightyChars() {
        assertNull(OnDeviceLlmDiagnostic.safePreview(null))
        assertNull(OnDeviceLlmDiagnostic.safePreview("   "))
        val long = "字".repeat(200)
        val preview = OnDeviceLlmDiagnostic.safePreview(long)!!
        assertEquals(80 + 1, preview.length) // 80 chars + ellipsis
        assertTrue(preview.endsWith("…"))
        assertEquals("短文本", OnDeviceLlmDiagnostic.safePreview("短文本"))
    }

    @Test
    fun multimodalDiagnosticSafeLinesAreContentFree() {
        val diag = OnDeviceMultimodalDiagnostic(
            state = OnDeviceProbeState.CALL_VIT_SUCCESS,
            sdkSupportsMultimodalField = true,
            callVitMethodPresent = true,
            modelDir = "/sdcard/1225",
            testImageWidth = 2,
            testImageHeight = 2,
            rgbByteLength = 12,
            callVitReturnCode = 0,
            generateState = OnDeviceProbeState.GENERATE_SUCCESS,
        )
        val blob = diag.safeLines().joinToString("\n")
        assertTrue(blob.contains("call_vit_ret=0"))
        assertTrue(blob.contains("rgb_byte_length=12"))
        listOf("prompt", "appKey", "Authorization", "Bearer", "reasoning").forEach {
            assertFalse(blob.contains(it, ignoreCase = true))
        }
    }
}
