package com.classmate.app.qa

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedTeamCredibilityGuardTest {
    @Test
    fun currentDocsDoNotOverclaimOfficialCapabilities() {
        val currentDocs = firstExisting("docs/current", "../docs/current")
        val readme = firstExisting("README.md", "../README.md")
        val docs = (listOf(readme) + currentDocs.walkTopDown().filter { it.isFile && it.extension == "md" }.toList())
            .filter { it.exists() }
        val text = docs.joinToString("\n") { it.readText(Charsets.UTF_8) }

        listOf(
            "四个官方能力已产品化",
            "已全面接入官方工具",
            "官方 Embedding 已实时用于 app",
            "录音自动转写已完成",
            "官方 TTS 已完成",
            "官方 Function Calling 已完成",
        ).forEach { banned ->
            assertFalse("Current docs must not overclaim: $banned", text.contains(banned))
        }

        assertTrue(text.contains("smoke PASS") || text.contains("network smoke"))
        assertTrue(text.contains("local fallback") || text.contains("LOCAL_FALLBACK"))
        assertTrue(text.contains("seam") || text.contains("SEAM_ONLY"))
    }

    @Test
    fun asrLongDocsUseCorePresentAppPendingStatus() {
        val report = firstExisting(
            "docs/current/asr_long_productization_report.md",
            "../docs/current/asr_long_productization_report.md",
        ).takeIf { it.exists() }?.readText(Charsets.UTF_8).orEmpty()
        assertTrue(report.contains("Core contract: PRESENT"))
        assertTrue(report.contains("App-level wiring: PARTIAL"))
        assertTrue(report.contains("Manual transcript fallback"))
        assertFalse(report.contains("schema is missing"))
        assertFalse(report.contains("HARD_BLOCKED" + "_MISSING_SCHEMA"))
    }

    @Test
    fun demoDeviceProvisionScriptChecksPresenceOnlyAndNoNetworkSmoke() {
        val script = firstExisting(
            "scripts/qa/demo_device_provision.ps1",
            "../scripts/qa/demo_device_provision.ps1",
        ).readText(Charsets.UTF_8)

        listOf(
            "CLOUD_CONFIG_PRESENT",
            "ON_DEVICE_MODEL_PRESENT",
            "STORAGE_PERMISSION_READY",
            "RECORD_AUDIO_READY",
            "CAMERA_READY",
            "APP_INSTALLED",
            "L3_DEMO_DATA_READY",
            "/sdcard/1225",
            "Test-Path -LiteralPath",
        ).forEach { assertTrue("Missing demo provisioning guard: $it", script.contains(it)) }

        assertFalse(script.contains("Get-Content"))
        assertFalse(script.contains("official_provider_smoke"))
        assertFalse(script.contains("-RunNetwork"))
        assertFalse(script.contains("Authorization", ignoreCase = true))
        assertFalse(script.contains("AppKey", ignoreCase = true))
    }

    private fun firstExisting(vararg candidates: String): File =
        candidates.map(::File).firstOrNull { it.exists() } ?: File(candidates.first())
}
