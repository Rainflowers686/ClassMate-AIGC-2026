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
}
