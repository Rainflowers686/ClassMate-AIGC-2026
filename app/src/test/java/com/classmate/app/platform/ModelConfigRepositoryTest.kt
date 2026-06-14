package com.classmate.app.platform

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelConfigRepositoryTest {

    private fun tempFile() = Files.createTempDirectory("cm-model-cfg").resolve("classmate_model_config.json").toFile()

    @Test
    fun savePersistsAcrossRestartAndDeleteClearsIt() {
        val file = tempFile()
        val repo = ModelConfigRepository(file)
        assertNull(repo.load())

        assertTrue(
            repo.save(
                ModelApiProfile(
                    baseUrl = "https://api-ai.vivo.com.cn/v1",
                    model = "qwen3.5-plus",
                    appId = "fake-app-id-2026",
                    appKey = "fake-app-key-for-tests",
                ),
            ),
        )

        // Simulate an app restart: a brand-new repository over the SAME file still sees the profile.
        val reopened = ModelConfigRepository(file)
        val loaded = reopened.load()
        assertEquals("qwen3.5-plus", loaded?.model)
        assertEquals("fake-app-id-2026", loaded?.appId)
        assertTrue(reopened.hasUsableProfile())

        // Delete -> the official path is no longer configured.
        assertTrue(reopened.delete())
        assertNull(ModelConfigRepository(file).load())
        assertFalse(ModelConfigRepository(file).hasUsableProfile())
    }

    @Test
    fun maskedViewNeverExposesRawSecret() {
        val file = tempFile()
        val repo = ModelConfigRepository(file)
        repo.save(ModelApiProfile(appId = "abcd1234efgh", appKey = "sk-fake-secret-value-1234"))

        val masked = repo.masked()!!
        assertTrue(masked.credentialPresent)
        assertFalse(masked.maskedAppId.contains("abcd1234efgh"))
        assertFalse(masked.maskedAppKey.contains("sk-fake-secret-value-1234"))
        // Masked tails keep only first/last 2 chars.
        assertEquals("ab***gh", masked.maskedAppId)
        assertTrue(masked.maskedAppKey.startsWith("sk"))
        assertTrue(masked.maskedAppKey.endsWith("34"))
    }

    @Test
    fun placeholderCredentialsAreNotTreatedAsConfigured() {
        val repo = ModelConfigRepository(tempFile())
        repo.save(ModelApiProfile(appId = "YOUR_BLUELM_APP_ID", appKey = "YOUR_BLUELM_APP_KEY"))

        assertFalse(repo.hasUsableProfile())
        assertEquals("placeholder", repo.masked()?.maskedAppId)
    }

    @Test
    fun disabledRepositoryIsInertNoOp() {
        val repo = ModelConfigRepository.disabled()
        assertFalse(repo.isEnabled)
        assertNull(repo.load())
        assertNull(repo.masked())
        assertFalse(repo.save(ModelApiProfile(appId = "x", appKey = "y")))
        assertFalse(repo.delete())
    }
}
