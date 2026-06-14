package com.classmate.core.ondevice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceLlmTest {

    // --- A tiny fake "available" bridge so we can exercise the on-device-served path too. ---
    private class FakeAvailableBridge(
        private val reply: String = "端侧模型回答",
    ) : OnDeviceLlmProvider {
        var generateCalls = 0
        override fun status() = OnDeviceLlmStatus.AVAILABLE
        override fun diagnostic() = OnDeviceLlmDiagnostic(
            status = OnDeviceLlmStatus.AVAILABLE,
            sdkPresent = true,
            modelDir = OnDeviceLlmConfig.DEFAULT_MODEL_DIR,
            modelDirChecked = true,
            modelDirPresent = true,
            initAttempted = true,
            initSucceeded = true,
        )
        override fun generate(profile: OnDeviceLlmTaskProfile, prompt: String): OnDeviceGenerationResult {
            generateCalls++
            return OnDeviceGenerationResult.Success(reply, tokenCount = reply.length, latencyMs = 7)
        }
    }

    @Test
    fun defaultModelDirIsOfficialPreset() {
        // 2026-06-11 doc update: the versioned subdirectory is the official model root.
        assertEquals("/sdcard/1225/1.7.0.4_1225_mtk9500", OnDeviceLlmConfig.DEFAULT_MODEL_DIR)
        assertEquals("/sdcard/1225/1.7.0.4_1225_mtk9500", OnDeviceLlmConfig().modelPath)
        // The pre-update directory stays available as a legacy candidate.
        assertEquals("/sdcard/1225", OnDeviceLlmConfig.LEGACY_MODEL_DIR)
    }

    @Test
    fun promptTemplateUsesHumanAndAiTags() {
        val templated = OnDevicePromptTemplate.format("什么是 p 级数？")

        assertTrue(templated.startsWith("[|Human|]:"))
        assertTrue(templated.trimEnd().endsWith("[|AI|]:"))
        assertTrue(templated.contains("什么是 p 级数？"))
    }

    @Test
    fun promptTemplateFoldsSystemIntoHumanTurn() {
        val templated = OnDevicePromptTemplate.format(system = "你是课堂助教。", user = "解释收敛。")

        assertTrue(templated.startsWith("[|Human|]:"))
        assertTrue(templated.contains("你是课堂助教。"))
        assertTrue(templated.contains("解释收敛。"))
        // Exactly one AI marker, at the end, awaiting the model continuation.
        assertEquals(1, Regex(Regex.escape("[|AI|]:")).findAll(templated).count())
    }

    @Test
    fun missingBridgeReportsSdkMissingAndNeverGenerates() {
        val bridge = MissingOnDeviceBlueLmBridge()

        assertEquals(OnDeviceLlmStatus.SDK_MISSING, bridge.status())
        assertFalse(bridge.isAvailable())

        val result = bridge.generate(OnDeviceLlmTaskProfile.ASK, OnDevicePromptTemplate.format("hi"))
        assertTrue(result is OnDeviceGenerationResult.Unavailable)
        assertEquals(OnDeviceLlmStatus.SDK_MISSING, (result as OnDeviceGenerationResult.Unavailable).status)
    }

    @Test
    fun missingBridgeDiagnosticIsHonestAndContentFree() {
        val diag = MissingOnDeviceBlueLmBridge().diagnostic()

        assertFalse(diag.sdkPresent)
        assertFalse(diag.modelDirChecked)
        assertNull(diag.modelDirPresent)
        assertFalse(diag.initSucceeded)
        assertEquals(OnDeviceLlmConfig.DEFAULT_MODEL_DIR, diag.modelDir)

        val lines = diag.safeLines()
        assertTrue(lines.any { it == "ondevice_status=SDK_MISSING" })
        assertTrue(lines.any { it == "sdk_present=false" })
        assertTrue(lines.any { it == "reason=SDK_MISSING" })
        // Honesty: a content-free diagnostic carries no prompt/output/secret fields.
        val blob = lines.joinToString("\n")
        listOf("prompt", "appKey", "Authorization", "Bearer", "reasoning").forEach {
            assertFalse(blob.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun localChainPathIsOnDeviceThenSafetyPlaceholderWhenAvailable() {
        val chain = LocalProviderChain(FakeAvailableBridge())
        assertEquals(listOf("OnDeviceBlueLM", "SafetyPlaceholder"), chain.path())
        // User-facing Chinese: on-device is the local intelligence, rule is only a safety placeholder.
        assertEquals(listOf("端侧蓝心", "安全占位"), chain.pathZh())
    }

    @Test
    fun localChainDegradesToSafetyPlaceholderWhenSdkMissing() {
        val chain = LocalProviderChain(MissingOnDeviceBlueLmBridge())

        // Path collapses to just the safety placeholder.
        assertEquals(listOf("SafetyPlaceholder"), chain.path())
        assertEquals(listOf("安全占位"), chain.pathZh())

        val outcome = chain.resolve(OnDeviceLlmTaskProfile.FALLBACK, OnDevicePromptTemplate.format("hi"))
        assertEquals(ProviderPathNode.LOCAL_RULE, outcome.node)
        assertNull(outcome.text) // caller runs the deterministic safety placeholder
        assertEquals(OnDeviceLlmStatus.SDK_MISSING, outcome.onDeviceStatus)
    }

    @Test
    fun localChainServesOnDeviceTextWhenAvailable() {
        val bridge = FakeAvailableBridge(reply = "端侧整理建议")
        val outcome = LocalProviderChain(bridge).resolve(OnDeviceLlmTaskProfile.REPORT, "[|Human|]:x\n[|AI|]:")

        assertEquals(ProviderPathNode.ON_DEVICE_BLUELM, outcome.node)
        assertEquals("端侧整理建议", outcome.text)
        assertEquals(1, bridge.generateCalls)
    }

    @Test
    fun providerPathShortLabelsAreShortAndDistinct() {
        val labels = ProviderPathNode.entries.map { it.shortLabel }
        assertEquals(listOf("BlueLM", "OnDeviceBlueLM", "SafetyPlaceholder"), labels)
        labels.forEach { assertTrue(it.length in 1..18) }
        assertEquals(labels.size, labels.toSet().size)
        // User-facing Chinese path: 云端蓝心 → 端侧蓝心 → 安全占位.
        assertEquals(listOf("云端蓝心", "端侧蓝心", "安全占位"), ProviderPathNode.entries.map { it.displayZh })
        // Source mapping: only the two models map to a model label; everything else is the placeholder.
        assertEquals("云端蓝心", ProviderPathNode.sourceLabelZh("BLUELM"))
        assertEquals("端侧蓝心", ProviderPathNode.sourceLabelZh("ONDEVICE_BLUELM"))
        assertEquals("安全占位", ProviderPathNode.sourceLabelZh("local"))
        assertEquals("安全占位", ProviderPathNode.sourceLabelZh(null))
    }

    @Test
    fun taskProfilesUseDifferentTemperaturesAnalysisLowReportHigher() {
        val analysis = OnDeviceLlmTaskProfile.ANALYSIS
        val report = OnDeviceLlmTaskProfile.REPORT
        val fallback = OnDeviceLlmTaskProfile.FALLBACK

        assertTrue("analysis must be low-temp", analysis.temperature <= 0.3)
        assertTrue("report polish runs warmer than analysis", report.temperature > analysis.temperature)
        assertTrue("fallback stays stable", fallback.temperature <= 0.35)
        // Report allows a higher output ceiling than the terse fallback.
        assertTrue(report.maxOutputTokens > fallback.maxOutputTokens)
    }

    @Test
    fun configWithProfileAppliesSamplingButKeepsModelDir() {
        val tuned = OnDeviceLlmConfig().withProfile(OnDeviceLlmTaskProfile.ANALYSIS)

        assertEquals(0.2, tuned.temperature, 0.0001)
        assertEquals(30, tuned.topK)
        assertEquals(OnDeviceLlmConfig.DEFAULT_MODEL_DIR, tuned.modelPath) // model dir untouched
    }
}
