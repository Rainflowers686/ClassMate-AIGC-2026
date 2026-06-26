package com.classmate.app.qa

import com.classmate.app.platform.ConfigRepository
import com.classmate.core.model.ProviderKind
import com.classmate.core.provider.ProviderError
import com.classmate.core.provider.ProviderErrorType
import com.classmate.core.validation.ProviderConfigSafetyCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM coverage of the guarantees the PowerShell smoke script exists to prove — redaction, config
 * explanation / readiness mapping, and placeholder/masked rejection — WITHOUT spawning any shell. This
 * is the CI-safe counterpart to [OfficialProviderNetworkSmokeTest] (whose shell tests are manual-QA only
 * and skip in CI). It never makes a real network request.
 */
class OfficialSmokeLogicTest {

    // ── Redaction: a provider failure never carries a response body / secret ───────────────────────
    @Test
    fun providerFailureIsStructuredAndCarriesNoBody() {
        val err = ProviderError.fromStatus(ProviderKind.BLUELM, 503, "vendor-body-with-secret-sk-do-not-leak")
        assertFalse("error must not embed the response body", err.toString().contains("sk-do-not-leak"))
        assertTrue(err.shortCode.startsWith("BLUELM:"))
        assertEquals(ProviderErrorType.HTTP_NON_2XX, err.type)
    }

    @Test
    fun networkShortCodeStaysDiagnosableButValueFree() {
        val read = ProviderError(ProviderErrorType.NETWORK, ProviderKind.BLUELM, networkSubtype = "READ")
        assertEquals("BLUELM:NETWORK:READ", read.shortCode)
    }

    // ── Config explanation / readiness mapping (the script's -ExplainConfig core) ───────────────────
    @Test
    fun genericBlueLmConfigDoesNotMakeOfficialProvidersReady() {
        val result = ConfigRepository().parseOrDefault(
            """{"provider":"bluelm","appId":"id12345","appKey":"key67890","baseUrl":"https://api-ai.vivo.com.cn/v1"}""",
            source = "unit",
        )
        assertTrue(result.summary.blueLmConfigured)
        assertFalse(result.summary.officialProviders.ocrConfigured)
        assertFalse(result.summary.officialProviders.queryRewriteConfigured)
        assertFalse(result.summary.officialProviders.anyConfigured)
    }

    @Test
    fun explicitOcrBlockMapsOnlyOcrReadyWithoutLeakingValues() {
        val result = ConfigRepository().parseOrDefault(
            """
            {
              "officialProviders": {
                "ocr": { "enabled": true, "baseUrl": "https://ocr.example.invalid", "authValue": "secret-ocr-value" }
              }
            }
            """.trimIndent(),
            source = "unit",
        )
        assertTrue(result.summary.officialProviders.ocrConfigured)
        assertFalse(result.summary.officialProviders.queryRewriteConfigured)
        assertFalse(result.summary.officialProviders.ttsConfigured)
        assertFalse("readiness summary must not leak the auth value", result.toString().contains("secret-ocr-value"))
    }

    // ── Placeholder / masked rejection (config explanation never treats them as configured) ─────────
    @Test
    fun placeholderAndMaskedSecretsAreNotReal() {
        assertFalse(ProviderConfigSafetyCheck.isRealSecret("YOUR_BLUELM_APP_KEY"))
        assertFalse(ProviderConfigSafetyCheck.isRealSecret("ab***yz"))
        assertTrue(ProviderConfigSafetyCheck.isRealSecret("real-unit-app-key-value"))
    }
}
