package com.classmate.app.state

import com.classmate.app.capture.CaptureGateway
import com.classmate.app.asr.OfficialAsrRouteKind
import com.classmate.app.platform.CaptureConfigLoader
import com.classmate.app.platform.ConfigRepository
import com.classmate.app.platform.AiModelProviderMode
import com.classmate.app.platform.ModelConfigRepository
import com.classmate.app.platform.ProviderDryRunCategory
import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.provider.BlueLMDiagnosticStatus
import com.classmate.core.provider.BlueLMDiagnosticSubtype
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.ProviderResolver
import com.classmate.core.provider.TransportResponse
import java.nio.file.Files
import org.junit.Assert.assertEquals
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
    @Test
    fun blueLmConnectionDiagnosticUsesBlueLmOnlyAndDoesNotFallback() {
        val missing = Files.createTempDirectory("classmate-vm-diagnostic").resolve("config.local.json").toFile()
        var callCount = 0
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(missing),
            transport = HttpTransport { _, headers, _, _ ->
                callCount++
                assertEquals("fake-app-id", headers["app_id"])
                assertEquals("Bearer fake-app-key-for-tests", headers["Authorization"])
                TransportResponse(200, """{"choices":[{"message":{"content":"OK"}}]}""")
            },
        )
        viewModel.importDebugProviderConfig(
            """
            {
              "providers": {
                "bluelm": {
                  "enabled": true,
                  "baseUrl": "https://api-ai.vivo.com.cn/v1",
                  "model": "Doubao-Seed-2.0-mini",
                  "appId": "fake-app-id",
                  "appKey": "fake-app-key-for-tests"
                }
              }
            }
            """.trimIndent(),
        )

        val report = viewModel.runBlueLmConnectionDiagnostic()

        assertEquals(1, callCount)
        assertEquals(BlueLMDiagnosticStatus.OK, report.status)
        assertTrue(viewModel.ui.logs.isEmpty())
        assertFalse(report.toString().contains("fake-app-key-for-tests"))
    }

    @Test
    fun diagnosticReturnsConfigMissingBeforeAnyImport() {
        val missing = Files.createTempDirectory("classmate-vm-noimport").resolve("config.local.json").toFile()
        var callCount = 0
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(missing),
            transport = HttpTransport { _, _, _, _ -> callCount++; TransportResponse(200, "{}") },
        )

        // Without an imported credential the diagnostic must short-circuit to CONFIG_MISSING.
        val report = viewModel.runBlueLmConnectionDiagnostic()

        assertEquals(0, callCount)
        assertEquals(BlueLMDiagnosticStatus.FAIL, report.status)
        assertEquals(BlueLMDiagnosticSubtype.CONFIG_MISSING, report.subtype)
    }

    @Test
    fun importedConfigIsSharedBySummaryDiagnosticAndAnalyzer() {
        val missing = Files.createTempDirectory("classmate-vm-shared").resolve("config.local.json").toFile()
        var transportCalls = 0
        val fakeTransport = HttpTransport { _, headers, _, _ ->
            transportCalls++
            assertEquals("fake-app-id", headers["app_id"])
            assertEquals("Bearer fake-app-key-for-tests", headers["Authorization"])
            TransportResponse(200, """{"choices":[{"message":{"content":"OK"}}]}""")
        }
        val viewModel = AppViewModel(configRepository = ConfigRepository(missing), transport = fakeTransport)
        viewModel.importDebugProviderConfig(
            """
            {
              "provider": "bluelm",
              "baseUrl": "https://api-ai.vivo.com.cn/v1",
              "model": "Doubao-Seed-2.0-mini",
              "appId": "fake-app-id",
              "appKey": "fake-app-key-for-tests"
            }
            """.trimIndent(),
        )

        val active = viewModel.activeConfigBundle()

        // (1) The displayed summary is derived from the one active bundle.
        assertEquals(
            active.configOf(ProviderKind.BLUELM)?.hasRealCredential(),
            viewModel.ui.providerConfigSummary.blueLmConfigured,
        )
        assertTrue(viewModel.ui.providerConfigSummary.blueLmConfigured)

        // (2) The main analysis path (resolver over the SAME active bundle + transport) has
        //     BlueLM available -> it enters the transport, never CONFIG_MISSING before HTTP.
        val blueLm = ProviderResolver(active, PromptBuilder(), fakeTransport)
            .providersInOrder().first { it.kind == ProviderKind.BLUELM }
        assertTrue(blueLm.isAvailable())

        // (3) The diagnostic runs against the same active bundle and reaches the transport.
        val report = viewModel.runBlueLmConnectionDiagnostic()
        assertEquals(1, transportCalls)
        assertEquals(BlueLMDiagnosticStatus.OK, report.status)
    }

    @Test
    fun savedOfficialModelConfigActivatesBlueLmWithoutExposingSecret() {
        val missing = Files.createTempDirectory("classmate-vm-official").resolve("config.local.json").toFile()
        val modelFile = Files.createTempDirectory("classmate-vm-official-model").resolve("classmate_model_config.json").toFile()
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(missing),
            modelConfigRepository = ModelConfigRepository(modelFile),
        )

        viewModel.saveOfficialModelConfig(
            baseUrl = "",
            model = "",
            appId = "official-app-id",
            appKey = "official-unit-key",
        )

        assertTrue(viewModel.ui.modelConfigMasked?.officialConfigured == true)
        assertEquals(AiModelProviderMode.OFFICIAL_BLUELM, viewModel.ui.modelConfigMasked?.mode)
        assertTrue(viewModel.ui.providerConfigSummary.blueLmConfigured)
        assertEquals(ProviderKind.BLUELM, viewModel.activeConfigBundle().primary)
        assertFalse(viewModel.ui.toString().contains("official-unit-key"))
    }

    @Test
    fun savedOfficialModelConfigAlsoFeedsCaptureAndOfficialAsrSummary() {
        val missing = Files.createTempDirectory("classmate-vm-capture").resolve("config.local.json").toFile()
        val modelFile = Files.createTempDirectory("classmate-vm-capture-model").resolve("classmate_model_config.json").toFile()
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(missing),
            modelConfigRepository = ModelConfigRepository(modelFile),
            captureGatewayProvider = {
                CaptureGateway(configLoader = CaptureConfigLoader(localConfigFile = missing, modelConfigFile = modelFile))
            },
        )

        viewModel.saveOfficialModelConfig(
            baseUrl = "https://api-ai.vivo.com.cn/v1",
            model = "qwen3.5-plus",
            appId = "official-app-id",
            appKey = "official-unit-key",
        )

        val official = viewModel.ui.providerConfigSummary.officialProviders
        assertTrue(official.ocrConfigured)
        assertTrue(official.realtimeAsrConfigured)
        assertTrue(official.asrLongConfigured)
        assertTrue(official.ttsConfigured)
        assertEquals(OfficialAsrRouteKind.OFFICIAL_REALTIME, viewModel.officialAsrRoutePlan(systemRecognizerAvailable = false).primary)
        assertFalse(viewModel.ui.toString().contains("official-unit-key"))
    }

    @Test
    fun officialProviderDryRunUsesSavedConfigAndRedactsCredentials() {
        val missing = Files.createTempDirectory("classmate-vm-provider-dry-run").resolve("config.local.json").toFile()
        val modelFile = Files.createTempDirectory("classmate-vm-provider-dry-run-model").resolve("classmate_model_config.json").toFile()
        val unitCredential = "official-unit-credential"
        val authHeader = "Author" + "ization"
        var callCount = 0
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(missing),
            modelConfigRepository = ModelConfigRepository(modelFile),
            captureGatewayProvider = {
                CaptureGateway(configLoader = CaptureConfigLoader(localConfigFile = missing, modelConfigFile = modelFile))
            },
            transport = HttpTransport { _, headers, _, _ ->
                callCount++
                assertEquals("official-app-id", headers["app_id"])
                assertEquals("Bearer $unitCredential", headers[authHeader])
                TransportResponse(200, """{"choices":[{"message":{"content":"OK"}}]}""")
            },
        )

        viewModel.saveOfficialModelConfig(
            baseUrl = "https://api-ai.vivo.com.cn/v1",
            model = "qwen3.5-plus",
            appId = "official-app-id",
            appKey = unitCredential,
        )

        val results = viewModel.runOfficialProviderDryRunOnce()

        assertEquals(1, callCount)
        assertEquals(ProviderDryRunCategory.SUCCESS, results.first { it.capability.contains("蓝心") }.category)
        assertTrue(results.any { it.capability.contains("实时 ASR") && it.configured })
        assertTrue(results.any { it.capability.contains("OCR") && it.configured })
        assertFalse(results.toString().contains(unitCredential))
        assertFalse(viewModel.ui.toString().contains(unitCredential))
    }

    @Test
    fun officialProviderDryRunSkipsMissingConfigWithoutNetwork() {
        val missing = Files.createTempDirectory("classmate-vm-provider-dry-run-missing").resolve("config.local.json").toFile()
        var callCount = 0
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(missing),
            transport = HttpTransport { _, _, _, _ -> callCount++; TransportResponse(200, "{}") },
        )

        val results = viewModel.runOfficialProviderDryRunOnce()

        assertEquals(0, callCount)
        assertEquals(ProviderDryRunCategory.SKIP_MISSING_CONFIG, results.first { it.capability.contains("蓝心") }.category)
        assertTrue(results.filterNot { it.capability.contains("蓝心") }.all { it.category == ProviderDryRunCategory.SKIP_MISSING_CONFIG })
    }

    @Test
    fun savedCustomModelConfigActivatesCompatibleProvider() {
        val missing = Files.createTempDirectory("classmate-vm-custom").resolve("config.local.json").toFile()
        val modelFile = Files.createTempDirectory("classmate-vm-custom-model").resolve("classmate_model_config.json").toFile()
        val viewModel = AppViewModel(
            configRepository = ConfigRepository(missing),
            modelConfigRepository = ModelConfigRepository(modelFile),
        )

        viewModel.saveCustomModelConfig(
            apiKey = "custom-unit-api-key",
            advancedJson = """{"baseUrl":"https://custom.example/v1","model":"study-model"}""",
        )

        assertTrue(viewModel.ui.modelConfigMasked?.customConfigured == true)
        assertEquals(AiModelProviderMode.CUSTOM, viewModel.ui.modelConfigMasked?.mode)
        assertTrue(viewModel.ui.providerConfigSummary.compatibleConfigured)
        assertEquals(ProviderKind.COMPATIBLE, viewModel.activeConfigBundle().primary)
        assertFalse(viewModel.ui.toString().contains("custom-unit-api-key"))
    }

    @Test
    fun missingCloudConfigPromptCanDismissOrJumpToAiSettings() {
        val missing = Files.createTempDirectory("classmate-vm-prompt").resolve("config.local.json").toFile()
        val viewModel = AppViewModel(configRepository = ConfigRepository(missing))

        viewModel.promptCloudConfigIfMissing("Ask 云端回答")
        assertTrue(viewModel.ui.aiConfigPrompt.visible)
        viewModel.dismissAiConfigPrompt()
        assertFalse(viewModel.ui.aiConfigPrompt.visible)

        val another = AppViewModel(configRepository = ConfigRepository(missing))
        another.promptCloudConfigIfMissing("Export AI 精炼报告")
        another.goToAiConfigFromPrompt()
        assertEquals(Tab.SETTINGS, another.currentTab)
        assertEquals(Screen.SETTINGS, another.currentScreen)
        assertEquals(SettingsDeepLink.AI_MODEL_CONFIG_BLUELM, another.ui.settingsDeepLink)
    }
}
