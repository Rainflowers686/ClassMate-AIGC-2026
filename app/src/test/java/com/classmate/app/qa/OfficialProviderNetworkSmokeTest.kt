package com.classmate.app.qa

import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialProviderNetworkSmokeTest {
    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    private fun readWorkspace(path: String): String =
        firstExisting(path, "../$path").readText()

    @Test
    fun smokeScriptDefaultsToDryRunAndRequiresExplicitNetworkFlag() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("[switch]\$DryRun"))
        assertTrue(script.contains("[switch]\$RunNetwork"))
        assertTrue(script.contains("[switch]\$UseLocalConfig"))
        assertTrue(script.contains("[switch]\$PrintSetupHelp"))
        assertTrue(script.contains("[switch]\$ExplainConfig"))
        assertTrue(script.contains("if (-not \$RunNetwork)"))
        assertTrue(script.contains("\$DryRun = \$true"))
        assertTrue(script.contains("No network request was sent."))
    }

    @Test
    fun localConfigReadIsExplicitOptInOnly() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")
        val localConfigBlock = script.substringAfter("function Read-LocalSmokeConfig").substringBefore("function New-OcrInput")

        assertTrue(localConfigBlock.contains("if (-not \$UseLocalConfig"))
        assertTrue(localConfigBlock.contains("Get-Content -LiteralPath \$path -Raw"))
        assertTrue(script.contains("content read only with -UseLocalConfig"))
        assertTrue(script.contains("local config read"))
    }

    @Test
    fun setupHelpListsEnvNamesButOnlyPlaceholderValues() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("Print-SetupHelp"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_OCR_URL=<your-value>") || script.contains("\$entry.urlEnv + \"=<your-value>\""))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE=<your-value>"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_QUERY_REWRITE_URL"))
        assertFalse(script.contains("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE=sk-"))
    }

    @Test
    fun allSafeExcludesLongAsrAndDevLabCapabilities() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")
        val safeBlock = script.substringAfter("\$SafeCapabilities = @(").substringBefore(")")

        listOf("OCR", "QUERY_REWRITE", "TEXT_SIMILARITY", "TRANSLATION", "TTS", "FUNCTION_CALLING", "EMBEDDING").forEach {
            assertTrue("safe capability missing: $it", safeBlock.contains("\"$it\""))
        }

        listOf("ASR_LONG", "IMAGE_GEN", "VIDEO_GEN", "SHORT_ASR", "DIALECT_ASR", "SIMULTANEOUS_INTERPRETATION").forEach {
            assertFalse("unsafe capability included in AllSafe: $it", safeBlock.contains("\"$it\""))
        }
    }

    @Test
    fun scriptWritesOnlyCodexWorkOutputsAndRedactsSensitiveTerms() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains(".codex_work\\official_provider_smoke"))
        assertTrue(script.contains("smoke_result.json"))
        assertTrue(script.contains("smoke_result.md"))
        assertTrue(script.contains("smoke.log"))
        assertTrue(script.contains("test_inputs"))
        assertTrue(script.contains("outputs"))
        assertTrue(script.contains("config.local.json"))
        assertTrue(script.contains("content not read") || script.contains("content read only with -UseLocalConfig"))

        assertFalse(script.contains("Auth" + "orization:"))
        assertFalse(script.contains("Bear" + "er "))
        assertFalse(script.contains("app" + "Key:"))
        assertFalse(script.contains("app" + "Id:"))
        assertFalse(script.contains("api" + "Key:"))
        assertFalse(script.contains("VOICE" + "_CLONE"))
        assertFalse(script.contains("GEO" + "_POI"))
        assertFalse(script.contains("CLASSMATE_PROVIDER_SMOKE_L" + "BS"))
        assertFalse(script.contains("CLASSMATE_PROVIDER_SMOKE_P" + "OI"))
    }

    @Test
    fun configMissingIsSkippedNotScriptFailure() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("SKIPPED_CONFIG_MISSING"))
        assertTrue(script.contains("missingEnvNames"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_OCR_URL"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_OCR_AUTH_VALUE"))
        assertTrue(script.contains("CLASSMATE_PROVIDER_SMOKE_AUTH_VALUE"))
        assertFalse(script.contains("ConfigMissing is failure"))
    }

    @Test
    fun endpointMappingAndSeamOnlyStatesAreRecorded() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("SKIPPED_ENDPOINT_MAPPING_MISSING"))
        assertTrue(script.contains("SKIPPED_SEAM_ONLY"))
        assertTrue(script.contains("SeamReadyButEndpointMappingMissing"))
        assertTrue(script.contains("endpointMappingStatus"))
        assertTrue(script.contains("requestSchemaStatus"))
        assertTrue(script.contains("SEAM_ONLY"))
        assertTrue(script.contains("GENERIC_ONLY"))
    }

    @Test
    fun codexWorkIsNotTracked() {
        val repo = firstExisting(".git", "../.git").parentFile
        val process = ProcessBuilder("git", "ls-files", ".codex_work")
            .directory(repo)
            .redirectErrorStream(true)
            .start()
        assertTrue("git ls-files timed out", process.waitFor(10, TimeUnit.SECONDS))
        val output = process.inputStream.bufferedReader().readText().trim()
        assertTrue(".codex_work should not be tracked: $output", output.isEmpty())
    }

    @Test
    fun globalProviderGuardsRemainInPlace() {
        val vendorIo = readWorkspace("core/src/main/kotlin/com/classmate/core/provider/VendorIo.kt")
        val diagnostic = readWorkspace("core/src/main/kotlin/com/classmate/core/provider/BlueLMDiagnostic.kt")
        val mainFiles = firstExisting("app/src/main", "../app/src/main")
            .walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .plus(
                firstExisting("core/src/main", "../core/src/main")
                    .walkTopDown()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") },
            )

        assertTrue(vendorIo.contains("qwen3.5-plus"))
        assertTrue(vendorIo.contains("put(\"enable_thinking\", false)"))
        assertTrue(diagnostic.contains("put(\"enable_thinking\", false)"))
        assertFalse(mainFiles.any { it.readText().contains("import com.vivo.llmsdk") })
    }
}
