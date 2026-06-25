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
                    appKey = "unit-test-official-key",
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
        repo.save(ModelApiProfile(appId = "abcd1234efgh", appKey = "unit-test-secret-value-1234"))

        val masked = repo.masked()!!
        assertTrue(masked.credentialPresent)
        assertFalse(masked.maskedAppId.contains("abcd1234efgh"))
        assertFalse(masked.maskedAppKey.contains("unit-test-secret-value-1234"))
        // Masked tails keep only first/last 2 chars.
        assertEquals("ab***gh", masked.maskedAppId)
        assertTrue(masked.maskedAppKey.startsWith("un"))
        assertTrue(masked.maskedAppKey.endsWith("34"))
    }

    @Test
    fun officialConfigUsesDefaultCompetitionAppId() {
        assertEquals("2026374747", ModelApiProfile.DEFAULT_APP_ID)
    }

    @Test
    fun customConfigPersistsSeparatelyAndCanBeDeleted() {
        val file = tempFile()
        val repo = ModelConfigRepository(file)

        assertTrue(repo.saveOfficial("https://api-ai.vivo.com.cn/v1", "qwen3.5-plus", "official-app-id", "official-unit-key"))
        assertTrue(repo.saveCustom("custom-unit-api-key", """{"baseUrl":"https://custom.example/v1","model":"study-model"}"""))

        val reopened = ModelConfigRepository(file)
        val profile = reopened.load()!!
        assertEquals(AiModelProviderMode.CUSTOM, profile.mode)
        assertTrue(profile.officialConfigured())
        assertTrue(profile.customConfigured())
        assertEquals("https://custom.example/v1", profile.customBaseUrl("fallback"))
        assertEquals("study-model", profile.customModel("fallback"))

        assertTrue(reopened.deleteCustom())
        val afterDelete = ModelConfigRepository(file).load()!!
        assertEquals(AiModelProviderMode.OFFICIAL_BLUELM, afterDelete.mode)
        assertTrue(afterDelete.officialConfigured())
        assertFalse(afterDelete.customConfigured())
    }

    @Test
    fun invalidCustomJsonIsRejectedWithoutSavingSecret() {
        val file = tempFile()
        val repo = ModelConfigRepository(file)

        assertFalse(repo.saveCustom("custom-unit-api-key", """{"baseUrl":"""))
        assertNull(repo.load())
    }

    @Test
    fun placeholderCredentialsAreNotTreatedAsConfigured() {
        val repo = ModelConfigRepository(tempFile())
        repo.save(ModelApiProfile(appId = "YOUR_BLUELM_APP_ID", appKey = "YOUR_BLUELM_APP_KEY"))

        assertFalse(repo.hasUsableProfile())
        assertEquals("placeholder", repo.masked()?.maskedAppId)
    }

    @Test
    fun saveOfficialRejectsMaskedKeyAndKeepsExistingCredential() {
        val file = tempFile()
        val repo = ModelConfigRepository(file)
        // First save a real credential.
        assertTrue(repo.saveOfficial("https://api-ai.vivo.com.cn/v1", "qwen3.5-plus", "official-app-id", "real-official-key"))
        assertTrue(repo.load()!!.officialConfigured())

        // A re-save with a UI-masked AppKey must be rejected (return false) and NOT overwrite the key.
        assertFalse(repo.saveOfficial("https://api-ai.vivo.com.cn/v1", "qwen3.5-plus", "official-app-id", "re***ey"))
        val after = ModelConfigRepository(file).load()!!
        assertEquals("real-official-key", after.appKey)
        assertTrue(after.officialConfigured())
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
