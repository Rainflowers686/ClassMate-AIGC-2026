package com.classmate.app.state

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.model.ProviderKind
import com.classmate.core.provider.CompatibleDiagnosticStatus
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.LearnerProfile
import com.classmate.core.provider.TransportResponse
import com.classmate.core.sample.SampleCourses
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAndHistoryTest {

    private fun missingFile() = Files.createTempDirectory("cm-profile").resolve("config.local.json").toFile()
    private fun vm(transport: HttpTransport? = null) =
        if (transport == null) AppViewModel(configRepository = ConfigRepository(missingFile()))
        else AppViewModel(configRepository = ConfigRepository(missingFile()), transport = transport)

    @Test
    fun officialBlueLmImportSetsProfilePrimaryAndModel() {
        val viewModel = vm()
        viewModel.importDebugProviderConfig(
            """{"profile":"official_bluelm","provider":"bluelm","baseUrl":"https://api-ai.vivo.com.cn/v1","model":"qwen3.5-plus","appId":"fake-app-id","appKey":"fake-app-key-for-tests"}""",
        )
        val bundle = viewModel.activeConfigBundle()
        assertEquals(LearnerProfile.OFFICIAL_BLUELM, bundle.profile)
        assertEquals(ProviderKind.BLUELM, bundle.primary)
        assertEquals("qwen3.5-plus", bundle.configOf(ProviderKind.BLUELM)?.model)
        assertEquals(listOf(ProviderKind.BLUELM, ProviderKind.LOCAL_FALLBACK), bundle.order)
        // Home mode chip reflects the effective serving provider.
        assertEquals("官方 BlueLM", viewModel.ui.providerConfigSummary.modeLabel)
    }

    @Test
    fun demoCompatibleImportConfiguresCompatibleAndMasksKey() {
        val viewModel = vm()
        val preview = viewModel.importDebugProviderConfig(
            """{"profile":"demo_compatible","provider":"compatible","baseUrl":"https://compat.test/v1","model":"deepseek-v4pro","apiKey":"sk-demo-1234567"}""",
        )
        val bundle = viewModel.activeConfigBundle()
        assertEquals(LearnerProfile.DEMO_COMPATIBLE, bundle.profile)
        assertEquals(ProviderKind.COMPATIBLE, bundle.primary)
        // Compatible is primary; BlueLM stays a secondary; local fallback is last.
        assertEquals(listOf(ProviderKind.COMPATIBLE, ProviderKind.BLUELM, ProviderKind.LOCAL_FALLBACK), bundle.order)
        assertEquals("自有模型", viewModel.ui.providerConfigSummary.modeLabel)
        assertTrue(bundle.configOf(ProviderKind.COMPATIBLE)?.hasRealCredential() == true)
        assertTrue(viewModel.ui.providerConfigSummary.compatibleConfigured)
        assertTrue(preview.providerSummaries.any { it.provider == "COMPATIBLE" && it.credentialPresent && it.maskedAppKey != "absent" })
        // never the full key
        assertFalse(preview.toString().contains("sk-demo-1234567"))
        assertFalse(viewModel.ui.providerConfigSummary.toString().contains("sk-demo-1234567"))
    }

    @Test
    fun localOnlyImportUsesOnlyLocalFallback() {
        val viewModel = vm()
        viewModel.importDebugProviderConfig("""{"profile":"local_only","provider":"local"}""")
        val bundle = viewModel.activeConfigBundle()
        assertEquals(LearnerProfile.LOCAL_ONLY, bundle.profile)
        assertEquals(listOf(ProviderKind.LOCAL_FALLBACK), bundle.order)
        // local_only never reaches a network provider, and the chip says so.
        assertFalse(bundle.order.contains(ProviderKind.BLUELM))
        assertFalse(bundle.order.contains(ProviderKind.COMPATIBLE))
        assertEquals("安全占位", viewModel.ui.providerConfigSummary.modeLabel)
    }

    @Test
    fun compatibleDiagnosticUsesBearerNotAppIdAndDoesNotLeakKey() {
        var headersSeen: Map<String, String> = emptyMap()
        val transport = HttpTransport { _, headers, _, _ ->
            headersSeen = headers
            TransportResponse(200, """{"choices":[{"message":{"content":"OK"}}]}""")
        }
        val viewModel = vm(transport)
        viewModel.importDebugProviderConfig(
            """{"profile":"demo_compatible","provider":"compatible","baseUrl":"https://compat.test/v1","model":"deepseek-v4pro","apiKey":"sk-demo-1234567"}""",
        )
        val report = viewModel.runCompatibleConnectionDiagnostic()

        assertEquals(CompatibleDiagnosticStatus.OK, report.status)
        assertEquals("Bearer sk-demo-1234567", headersSeen["Authorization"])
        assertFalse(headersSeen.containsKey("app_id")) // compatible is NOT BlueLM
        val rendered = report.safeLines().joinToString("\n")
        assertFalse(rendered.contains("sk-demo-1234567"))
        assertFalse(rendered.contains("Authorization"))
        assertFalse(rendered.contains("Bearer"))
        assertTrue(rendered.contains("content_preview=OK"))
    }

    @Test
    fun initialTabIsHomeAndSelectTabSwitchesRoot() {
        val viewModel = vm()
        assertEquals(Tab.HOME, viewModel.currentTab)
        assertEquals(Screen.HOME, viewModel.currentScreen)
        viewModel.selectTab(Tab.REVIEW)
        assertEquals(Tab.REVIEW, viewModel.currentTab)
        assertEquals(Screen.REVIEW, viewModel.currentScreen)
        viewModel.selectTab(Tab.HISTORY)
        assertEquals(Screen.HISTORY, viewModel.currentScreen)
    }

    @Test
    fun historyLoadsFromStoreAndDeleteIsPure() {
        val store = InMemoryHistoryStore(listOf(sampleRecord("a"), sampleRecord("b")))
        val viewModel = AppViewModel(configRepository = ConfigRepository(missingFile()), historyStore = store)
        assertEquals(2, viewModel.ui.history.size)
        viewModel.deleteHistory("a")
        assertEquals(1, viewModel.ui.history.size)
        assertEquals("b", viewModel.ui.history.first().id)
        // deleting history never wipes the active provider config
        assertEquals(LearnerProfile.OFFICIAL_BLUELM, viewModel.activeConfigBundle().profile)
    }

    private fun sampleRecord(id: String): HistoryRecord {
        val session = SampleCourses.seriesSession()
        val result = SampleCourses.seriesAnalysis()
        return HistoryRecord(
            id = id, title = "示例", createdAtEpochMs = 0L,
            providerName = "BLUELM", profileLabel = "官方 BlueLM", model = "qwen3.5-plus",
            knowledgePointCount = result.knowledgePoints.size, quizCount = result.quizQuestions.size,
            fallbackUsed = false, validationStatus = "PASS", session = session, result = result,
        )
    }
}
