package com.classmate.app.platform

import com.classmate.core.model.ProviderKind
import com.classmate.core.provider.Credential
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigRepositoryTest {

    @Test
    fun readsFakeLocalConfigIntoProviderBundle() {
        val file = Files.createTempFile("classmate-config", ".json").toFile()
        file.writeText(
            """
            {
              "activeProvider": "bluelm",
              "providers": {
                "bluelm": {
                  "enabled": true,
                  "baseUrl": "https://fake-blue-lm.test",
                  "model": "fake-blue-model",
                  "appId": "fake-app-id",
                  "appKey": "fake-app-key-for-tests",
                  "timeoutMs": 12345,
                  "temperature": 0.2,
                  "maxTokens": 2048,
                  "requestIdQueryName": "requestId"
                },
                "localFallback": { "enabled": true }
              },
              "resolver": { "order": ["bluelm", "localFallback"] }
            }
            """.trimIndent(),
        )

        val result = ConfigRepository(file).loadLocalOrDefault()
        val bluelm = result.bundle.configOf(ProviderKind.BLUELM)

        assertEquals(null, result.error)
        assertEquals("https://fake-blue-lm.test", bluelm?.baseUrl)
        assertEquals("fake-blue-model", bluelm?.model)
        assertEquals(0.2, bluelm?.temperature ?: 0.0, 0.0001)
        assertEquals(2048, bluelm?.maxTokens)
        assertEquals("requestId", bluelm?.requestIdQueryName)
        assertTrue(bluelm?.credential is Credential.BlueLm)
        assertTrue(result.summary.blueLmConfigured)
        assertFalse(result.toString().contains("fake-app-key-for-tests"))
    }

    @Test
    fun parsesTopLevelBlueLmConfigIntoProviderBundle() {
        val result = ConfigRepository().parseOrDefault(
            """
            {
              "provider": "bluelm",
              "baseUrl": "https://api-ai.vivo.com.cn/v1",
              "model": "Doubao-Seed-2.0-mini",
              "appId": "FAKE_APP_ID_2026374747",
              "appKey": "sk-xuanji-FAKE-ONLY-DO-NOT-USE",
              "temperature": 0.2,
              "maxTokens": 1200,
              "stream": false,
              "requestIdQueryName": "request_id"
            }
            """.trimIndent(),
            source = "debug import",
        )
        val bluelm = result.bundle.configOf(ProviderKind.BLUELM)

        assertEquals(null, result.error)
        assertEquals(ProviderKind.BLUELM, result.bundle.primary)
        assertEquals("https://api-ai.vivo.com.cn/v1", bluelm?.baseUrl)
        assertEquals("Doubao-Seed-2.0-mini", bluelm?.model)
        assertEquals(0.2, bluelm?.temperature ?: 0.0, 0.0001)
        assertEquals(1200, bluelm?.maxTokens)
        assertTrue(bluelm?.credential is Credential.BlueLm)
        assertTrue(result.summary.blueLmConfigured)
        assertTrue(result.summary.providers.any { it.provider == "BLUELM" && it.credentialPresent })
        assertFalse(result.toString().contains("sk-xuanji-FAKE-ONLY-DO-NOT-USE"))
    }

    @Test
    fun parsesOfficialProviderSchemaPresenceWithoutPromotingTopLevelBlueLm() {
        val result = ConfigRepository().parseOrDefault(
            """
            {
              "provider": "bluelm",
              "baseUrl": "https://api-ai.vivo.com.cn/v1",
              "model": "qwen3.5-plus",
              "appId": "FAKE_APP_ID_2026374747",
              "appKey": "sk-xuanji-FAKE-ONLY-DO-NOT-USE",
              "officialProviders": {
                "ocr": {
                  "enabled": true,
                  "baseUrl": "https://official-ocr.example.invalid",
                  "authValue": "secret-ocr-auth"
                },
                "queryRewrite": {
                  "enabled": true,
                  "baseUrl": "https://official-query.example.invalid",
                  "authValue": "secret-query-auth"
                },
                "tts": {
                  "enabled": false,
                  "baseUrl": "https://official-tts.example.invalid",
                  "authValue": "secret-tts-auth"
                }
              }
            }
            """.trimIndent(),
            source = "debug import",
        )

        assertTrue(result.summary.blueLmConfigured)
        assertTrue(result.summary.officialProviders.ocrConfigured)
        assertTrue(result.summary.officialProviders.queryRewriteConfigured)
        assertFalse(result.summary.officialProviders.ttsConfigured)
        assertFalse(result.summary.officialProviders.textSimilarityConfigured)
        assertFalse(result.toString().contains("secret-ocr-auth"))
        assertFalse(result.toString().contains("official-ocr.example.invalid"))
    }

    @Test
    fun missingLocalConfigFallsBackSafely() {
        val missing = Files.createTempDirectory("classmate-missing").resolve("config.local.json").toFile()
        val result = ConfigRepository(missing).loadLocalOrDefault()

        assertEquals("CONFIG_NOT_FOUND", result.error?.code)
        assertFalse(result.summary.blueLmConfigured)
        assertTrue(result.summary.localFallbackEnabled)
    }
}
