package com.classmate.app.qa

import java.io.File
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
        assertTrue(script.contains("if (-not \$RunNetwork)"))
        assertTrue(script.contains("\$DryRun = \$true"))
        assertTrue(script.contains("No network request was sent."))
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
        assertTrue(script.contains("content not read"))

        assertFalse(script.contains("Auth" + "orization:"))
        assertFalse(script.contains("Bear" + "er "))
        assertFalse(script.contains("app" + "Key:"))
        assertFalse(script.contains("app" + "Id:"))
        assertFalse(script.contains("api" + "Key:"))
        assertFalse(script.contains("VOICE" + "_CLONE"))
        assertFalse(script.contains("L" + "BS"))
        assertFalse(script.contains("P" + "OI"))
    }

    @Test
    fun configMissingIsSkippedNotScriptFailure() {
        val script = readWorkspace("scripts/qa/official_provider_smoke.ps1")

        assertTrue(script.contains("SKIPPED_CONFIG_MISSING"))
        assertTrue(script.contains("Endpoint or auth environment variables are missing."))
        assertFalse(script.contains("ConfigMissing is failure"))
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
