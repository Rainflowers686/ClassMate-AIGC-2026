package com.classmate.app.state

import com.classmate.app.ondevice.OnDeviceLlmController
import com.classmate.app.platform.ConfigRepository
import com.classmate.app.platform.ModelConfigRepository
import com.classmate.core.model.ProviderKind
import com.classmate.core.ondevice.MissingOnDeviceBlueLmBridge
import com.classmate.core.ondevice.OnDeviceLlmStatus
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.provider.BlueLMDiagnosticStatus
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.ProviderResolver
import com.classmate.core.provider.TransportResponse
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceModelIntegrationTest {

    private fun tempConfig() = Files.createTempDirectory("cm-od").resolve("config.local.json").toFile()
    private fun tempModel() = Files.createTempDirectory("cm-od").resolve("classmate_model_config.json").toFile()

    private fun vm(
        configFile: File = tempConfig(),
        modelFile: File = tempModel(),
        transport: HttpTransport? = null,
    ): AppViewModel = AppViewModel(
        configRepository = ConfigRepository(configFile),
        modelConfigRepository = ModelConfigRepository(modelFile),
        onDeviceController = OnDeviceLlmController(MissingOnDeviceBlueLmBridge()),
        transport = transport ?: HttpTransport { _, _, _, _ -> TransportResponse(200, """{"choices":[{"message":{"content":"OK"}}]}""") },
    )

    @Test
    fun onDeviceDiagnosticIsHonestlyUnavailableWhenSdkMissing() {
        val viewModel = vm()
        val diag = viewModel.ui.onDeviceDiagnostic!!

        assertEquals(OnDeviceLlmStatus.SDK_MISSING, diag.status)
        assertFalse(diag.sdkPresent)
        assertEquals("/sdcard/1225/1.7.0.4_1225_mtk9500", diag.modelDir)
        // Local chain transparently degrades to just the rule-based safety net.
        assertEquals(listOf("安全占位"), viewModel.ui.localProviderPath)
    }

    @Test
    fun onDeviceDiagnosticReportIsContentFree() {
        val report = vm().onDeviceDiagnosticReport()
        val blob = report.safeLines().joinToString("\n")
        listOf("prompt", "appKey", "Authorization", "Bearer", "reasoning").forEach {
            assertFalse(blob.contains(it, ignoreCase = true))
        }
        assertTrue(blob.contains("ondevice_status=SDK_MISSING"))
    }

    @Test
    fun saveOfficialModelConfigPersistsAcrossRestartAndDeleteClears() {
        val configFile = tempConfig()
        val modelFile = tempModel()
        val first = vm(configFile, modelFile)
        assertFalse(first.ui.providerConfigSummary.blueLmConfigured)

        first.saveOfficialModelConfig(
            baseUrl = "https://api-ai.vivo.com.cn/v1",
            model = "qwen3.5-plus",
            appId = "fake-app-id-2026",
            appKey = "fake-app-key-for-tests",
        )
        assertTrue(first.ui.providerConfigSummary.blueLmConfigured)
        assertEquals(true, first.ui.modelConfigMasked?.credentialPresent)

        // Restart: a fresh ViewModel over the SAME persisted file is configured at init.
        val restarted = vm(configFile, modelFile)
        assertTrue(restarted.ui.providerConfigSummary.blueLmConfigured)
        assertEquals("qwen3.5-plus", restarted.activeConfigBundle().configOf(ProviderKind.BLUELM)?.model)

        // Delete: official path is unconfigured again, and the next restart stays unconfigured.
        restarted.deleteOfficialModelConfig()
        assertFalse(restarted.ui.providerConfigSummary.blueLmConfigured)
        assertFalse(vm(configFile, modelFile).ui.providerConfigSummary.blueLmConfigured)
    }

    @Test
    fun savedConfigDrivesTheSameCloudMainPathAndDiagnosticReachesTransport() {
        var transportCalls = 0
        val transport = HttpTransport { _, headers, _, _ ->
            transportCalls++
            assertEquals("fake-app-id-2026", headers["app_id"])
            assertEquals("Bearer fake-app-key-for-tests", headers["Authorization"])
            TransportResponse(200, """{"choices":[{"message":{"content":"OK"}}]}""")
        }
        val viewModel = vm(transport = transport)
        viewModel.saveOfficialModelConfig(
            baseUrl = "https://api-ai.vivo.com.cn/v1",
            model = "qwen3.5-plus",
            appId = "fake-app-id-2026",
            appKey = "fake-app-key-for-tests",
        )

        // (1) The resolver over the active bundle has BlueLM available -> main path unchanged.
        val active = viewModel.activeConfigBundle()
        val blueLm = ProviderResolver(active, PromptBuilder(), transport)
            .providersInOrder().first { it.kind == ProviderKind.BLUELM }
        assertTrue(blueLm.isAvailable())

        // (2) Official connection test reaches the transport and parses OK.
        val report = viewModel.runBlueLmConnectionDiagnostic()
        assertEquals(1, transportCalls)
        assertEquals(BlueLMDiagnosticStatus.OK, report.status)

        // (3) No secret leaked into the redacted report / logs.
        assertFalse(report.toString().contains("fake-app-key-for-tests"))
        assertTrue(viewModel.ui.logs.isEmpty())
    }

    @Test
    fun resolverOrderStillStartsWithBlueLmAndEndsWithLocalFallback() {
        val order = vm().activeConfigBundle().order
        assertEquals(ProviderKind.BLUELM, order.first())
        assertEquals(ProviderKind.LOCAL_FALLBACK, order.last())
    }
}
