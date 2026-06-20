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
            "全面接入官方工具",
            "已全面接入官方工具",
            "已完成官方 Query Rewrite 实时调用",
            "官方 Embedding 已实时用于 app",
            "已完成官方 Embedding 实时调用",
            "已完成官方 Text Similarity 实时调用",
            "录音自动转写已完成",
            "实时 ASR 已完成",
            "官方 TTS 已完成",
            "官方 Translation 已完成",
            "官方 Function Calling 已完成",
            "PDF 全量解析已完成",
            "Word/Excel/PPT 完美解析",
            "端侧模型所有设备可用",
        ).forEach { banned ->
            assertFalse("Current docs must not overclaim: $banned", text.contains(banned))
        }

        assertTrue(text.contains("smoke PASS") || text.contains("network smoke"))
        assertTrue(text.contains("local fallback") || text.contains("LOCAL_FALLBACK"))
        assertTrue(text.contains("seam") || text.contains("SEAM_ONLY"))
    }

    @Test
    fun finalSyncDocsExistAndRecordValidationPendingStatus() {
        listOf(
            "docs/current/project_current_status_v1_8.md",
            "docs/current/official_runtime_injection_v1_7.md",
            "docs/current/redteam_v2_official_runtime_review.md",
            "docs/current/claude_v3_review_handoff.md",
        ).forEach { path ->
            assertTrue("Missing final sync doc: $path", firstExisting(path, "../$path").exists())
        }

        val matrix = firstExisting(
            "docs/current/official_tool_productization_matrix.md",
            "../docs/current/official_tool_productization_matrix.md",
        ).readText(Charsets.UTF_8)
        assertTrue(matrix.contains("OFFICIAL_RUNTIME_READY / VALIDATION_PENDING"))
        assertTrue(matrix.contains("OfficialRuntimeGatewayFactory.production()"))
        assertTrue(matrix.contains("VivoQueryRewriteProvider -> VivoQueryRewriteLearningProvider"))
        assertTrue(matrix.contains("VivoEmbeddingProvider -> VivoEmbeddingLearningProvider"))
        assertTrue(matrix.contains("VivoTextSimilarityProvider -> VivoTextSimilarityLearningProvider"))
    }

    @Test
    fun productionRuntimeGatewayUsesFactoryInsteadOfConfigMissingOnlyDefault() {
        val viewModel = firstExisting(
            "app/src/main/java/com/classmate/app/state/AppViewModel.kt",
            "../app/src/main/java/com/classmate/app/state/AppViewModel.kt",
        ).readText(Charsets.UTF_8)
        val factory = firstExisting(
            "app/src/main/java/com/classmate/app/l3/OfficialRuntimeGatewayFactory.kt",
            "../app/src/main/java/com/classmate/app/l3/OfficialRuntimeGatewayFactory.kt",
        ).readText(Charsets.UTF_8)
        val captureProvider = firstExisting(
            "core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt",
            "../core/src/main/kotlin/com/classmate/core/capture/VivoCaptureProviders.kt",
        ).readText(Charsets.UTF_8)

        assertTrue(viewModel.contains("OfficialRuntimeGatewayFactory.production()"))
        assertFalse(viewModel.contains("OfficialRuntimeGateway = ProviderBackedOfficialRuntimeGateway()"))
        assertTrue(factory.contains("VivoQueryRewriteLearningProvider"))
        assertTrue(factory.contains("VivoEmbeddingLearningProvider"))
        assertTrue(factory.contains("VivoTextSimilarityLearningProvider"))
        assertTrue(captureProvider.contains("\"prompts\""))
        assertFalse(captureProvider.contains("buildJson { put(\"query\", question) }"))
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
        assertFalse(report.contains("schema missing", ignoreCase = true))
        assertFalse(report.contains("HARD_BLOCKED" + "_MISSING_SCHEMA"))
    }

    @Test
    fun demoAndCloudValidationDocsKeepGoNoGoAndNoAutomaticAsrClaim() {
        val demo = firstExisting(
            "docs/current/demo_script_l3_pipeline.md",
            "../docs/current/demo_script_l3_pipeline.md",
        ).readText(Charsets.UTF_8)
        val cloud = firstExisting(
            "docs/current/cloud_device_validation_plan.md",
            "../docs/current/cloud_device_validation_plan.md",
        ).readText(Charsets.UTF_8)

        assertTrue(cloud.contains("GO/NO-GO"))
        assertTrue(cloud.contains("CLOUD_CONFIG_PRESENT"))
        assertTrue(cloud.contains("Query Rewrite"))
        assertTrue(cloud.contains("Embedding"))
        assertTrue(cloud.contains("Text Similarity"))
        assertTrue(demo.contains("Final Route"))
        assertTrue(demo.contains("Risk / fallback"))
        assertFalse(demo.contains("automatic ASR completed", ignoreCase = true))
        assertFalse(demo.contains("录音自动转写已完成"))
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
