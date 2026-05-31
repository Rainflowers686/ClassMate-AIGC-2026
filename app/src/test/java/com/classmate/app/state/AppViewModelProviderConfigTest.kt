package com.classmate.app.state

import com.classmate.app.platform.ConfigRepository
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppViewModelProviderConfigTest {

    @Test
    fun missingLocalConfigDoesNotReportBlueLmReady() {
        val missing = Files.createTempDirectory("classmate-vm-missing").resolve("config.local.json").toFile()
        val viewModel = AppViewModel(configRepository = ConfigRepository(missing))

        assertFalse(viewModel.ui.providerConfigSummary.blueLmConfigured)
        assertFalse(viewModel.ui.providerConfigSummary.primaryReady)
        assertTrue(viewModel.ui.providerConfigSummary.localFallbackEnabled)
    }

    @Test
    fun debugImportUpdatesConfigAndMarksPrimaryReadyWhenTransportExists() {
        val missing = Files.createTempDirectory("classmate-vm-debug").resolve("config.local.json").toFile()
        val viewModel = AppViewModel(configRepository = ConfigRepository(missing))
        val preview = viewModel.importDebugProviderConfig(
            """
            {
              "providers": {
                "bluelm": {
                  "enabled": true,
                  "baseUrl": "https://fake-blue-lm.test",
                  "model": "fake-blue-model",
                  "appId": "fake-app-id",
                  "appKey": "fake-app-key-for-tests"
                },
                "localFallback": { "enabled": true }
              }
            }
            """.trimIndent(),
        )

        assertTrue(preview.valid)
        assertTrue(viewModel.ui.providerConfigSummary.blueLmConfigured)
        assertTrue(viewModel.ui.providerConfigSummary.primaryReady)
        assertTrue(viewModel.ui.providerConfigSummary.localFallbackEnabled)
    }

    @Test
    fun topLevelDebugImportUpdatesActiveProviderSummaryWithBlueLmCredential() {
        val missing = Files.createTempDirectory("classmate-vm-top-level").resolve("config.local.json").toFile()
        val viewModel = AppViewModel(configRepository = ConfigRepository(missing))
        val preview = viewModel.importDebugProviderConfig(
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
        )
        val blueLmSummary = viewModel.ui.providerConfigSummary.providers.first { it.provider == "BLUELM" }

        assertTrue(preview.valid)
        assertTrue(viewModel.ui.providerConfigSummary.blueLmConfigured)
        assertTrue(viewModel.ui.providerConfigSummary.primaryReady)
        assertTrue(blueLmSummary.credentialPresent)
        assertTrue(blueLmSummary.maskedAppId != "absent")
        assertTrue(blueLmSummary.maskedAppKey != "absent")
        assertTrue(viewModel.ui.toast?.contains("未配置") != true)
    }
}
