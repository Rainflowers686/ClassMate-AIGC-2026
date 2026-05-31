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
                  "timeoutMs": 12345
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
        assertTrue(bluelm?.credential is Credential.BlueLm)
        assertTrue(result.summary.blueLmConfigured)
        assertFalse(result.toString().contains("fake-app-key-for-tests"))
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
