package com.classmate.app.platform

import com.classmate.core.model.ProviderKind
import com.classmate.core.provider.Credential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugConfigImporterTest {

    @Test
    fun placeholderConfigIsParsedButNotConfigured() {
        val json = """
            {"providers":{"bluelm":{"appId":"YOUR_BLUELM_APP_ID","appKey":"YOUR_BLUELM_APP_KEY"},"localFallback":{}}}
        """.trimIndent()
        val preview = DebugConfigImporter.inspect(json)

        assertTrue(preview.valid)
        assertFalse("placeholders must not count as configured", preview.bluelmConfigured)
        assertFalse(preview.containsRealSecret)
        assertTrue(preview.providersFound.contains("bluelm"))
    }

    @Test
    fun realCredentialsAreDetected() {
        val json = """
            {"providers":{"bluelm":{"baseUrl":"https://fake-blue-lm.test","model":"fake-blue-model","appId":"fake-app-id","appKey":"fake-app-key-for-tests"}}}
        """.trimIndent()
        val preview = DebugConfigImporter.inspect(json)

        assertTrue(preview.valid)
        assertTrue(preview.bluelmConfigured)
        assertTrue(preview.containsRealSecret)
        assertTrue(preview.providerSummaries.any { it.provider == "BLUELM" && it.credentialPresent })
        assertFalse(preview.toString().contains("fake-app-key-for-tests"))
        assertFalse(preview.toString().contains("fake-app-id"))
    }

    @Test
    fun topLevelBlueLmConfigBuildsRuntimeCredential() {
        val preview = DebugConfigImporter.inspect(topLevelFakeBlueLmJson())
        val blueLmSummary = preview.providerSummaries.first { it.provider == "BLUELM" }
        val blueLmConfig = preview.runtimeConfig?.configOf(ProviderKind.BLUELM)

        assertTrue(preview.valid)
        assertTrue(preview.bluelmConfigured)
        assertTrue(blueLmSummary.credentialPresent)
        assertEquals("https://api-ai.vivo.com.cn/v1", blueLmSummary.baseUrl)
        assertEquals("Doubao-Seed-2.0-mini", blueLmSummary.model)
        assertTrue(blueLmConfig?.credential is Credential.BlueLm)
    }

    @Test
    fun warningDoesNotBlockTopLevelDebugImport() {
        val preview = DebugConfigImporter.inspect(topLevelFakeBlueLmJson())

        assertTrue(preview.containsRealSecret)
        assertNotNull(preview.runtimeConfig)
        assertTrue(preview.providerSummaries.any { it.provider == "BLUELM" && it.credentialPresent })
    }

    @Test
    fun topLevelMissingAppKeyIsNotConfigured() {
        val preview = DebugConfigImporter.inspect(
            """
            {
              "provider": "bluelm",
              "baseUrl": "https://api-ai.vivo.com.cn/v1",
              "model": "Doubao-Seed-2.0-mini",
              "appId": "FAKE_APP_ID_2026374747"
            }
            """.trimIndent(),
        )
        val blueLmSummary = preview.providerSummaries.first { it.provider == "BLUELM" }

        assertTrue(preview.valid)
        assertFalse(preview.bluelmConfigured)
        assertFalse(blueLmSummary.credentialPresent)
    }

    @Test
    fun topLevelPreviewMasksCredentialValues() {
        val preview = DebugConfigImporter.inspect(topLevelFakeBlueLmJson())

        assertFalse(preview.toString().contains(FAKE_APP_ID))
        assertFalse(preview.toString().contains(FAKE_APP_KEY))
        assertTrue(preview.providerSummaries.any { it.maskedAppId != "absent" && it.maskedAppKey != "absent" })
    }

    @Test
    fun releaseGuardRejectsImport() {
        val preview = DebugConfigImporter.inspect(
            """{"providers":{"bluelm":{"appId":"fake-app-id","appKey":"fake-app-key-for-tests"}}}""",
            debugEnabled = false,
        )

        assertFalse(preview.valid)
        assertFalse(preview.bluelmConfigured)
        assertEquals(null, preview.runtimeConfig)
    }

    @Test
    fun invalidJsonIsReported() {
        val preview = DebugConfigImporter.inspect("definitely not json")
        assertFalse(preview.valid)
    }

    @Test
    fun blankInputIsRejected() {
        assertEquals(false, DebugConfigImporter.inspect("   ").valid)
    }

    private fun topLevelFakeBlueLmJson(): String =
        """
        {
          "provider": "bluelm",
          "baseUrl": "https://api-ai.vivo.com.cn/v1",
          "model": "Doubao-Seed-2.0-mini",
          "appId": "$FAKE_APP_ID",
          "appKey": "$FAKE_APP_KEY",
          "temperature": 0.2,
          "maxTokens": 1200,
          "stream": false,
          "requestIdQueryName": "request_id"
        }
        """.trimIndent()

    private companion object {
        const val FAKE_APP_ID = "FAKE_APP_ID_2026374747"
        const val FAKE_APP_KEY = "sk-xuanji-FAKE-ONLY-DO-NOT-USE"
    }
}
