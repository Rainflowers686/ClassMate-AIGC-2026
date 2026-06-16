package com.classmate.app.qa

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialProviderSmokeScriptTest {
    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    @Test
    fun smokeHarnessDefaultsToDryRunAndRedactsSensitiveTerms() {
        val script = firstExisting(
            "../scripts/qa/official_provider_smoke.ps1",
            "scripts/qa/official_provider_smoke.ps1",
        ).readText()

        assertTrue(script.contains("[switch]\$DryRun"))
        assertTrue(script.contains("[switch]\$RunNetwork"))
        assertTrue(script.contains(".codex_work\\official_provider_smoke"))
        assertTrue(script.contains("config.local.json"))
        assertTrue(script.contains("content not read"))
        assertTrue(script.contains("No network request was sent") || script.contains("Network smoke requires"))
        assertTrue(script.contains("VOICE_CLONE"))
        assertTrue(script.contains("GEO_POI"))
        assertFalse(script.contains("Auth" + "orization:"))
        assertFalse(script.contains("Bear" + "er "))
        assertFalse(script.contains("app" + "Key:"))
        assertFalse(script.contains("api" + "Key:"))
    }
}
