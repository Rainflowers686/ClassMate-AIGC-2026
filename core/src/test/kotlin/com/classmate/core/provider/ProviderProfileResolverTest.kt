package com.classmate.core.provider

import com.classmate.core.analysis.AnalysisOutcome
import com.classmate.core.analysis.CourseAnalyzer
import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.sample.SampleCourses
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the resolver ordering implied by each [LearnerProfile]. */
class ProviderProfileResolverTest {

    private val promptBuilder = PromptBuilder()
    private val session = SampleCourses.seriesSession()
    private val realBlueLm = Credential.BlueLm("appid123456", "appkey1234567")
    private val realApiKey = Credential.ApiKey("sk-compat-1234567")

    @Test
    fun officialBlueLmFallsBackToLocalWhenBlueLmFails() {
        var calls = 0
        val transport = HttpTransport { _, _, _, _ -> calls++; TransportResponse(503, "") }
        val bundle = ProviderConfigBundle.forProfile(
            LearnerProfile.OFFICIAL_BLUELM,
            configs(blueLm = realBlueLm), // BlueLM configured but the server 503s
        )
        val outcome = analyze(bundle, transport) as AnalysisOutcome.Success
        assertEquals(ProviderKind.LOCAL_FALLBACK, outcome.result.provenance.provider)
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.status == "FAIL" })
        assertFalse(outcome.logs.any { it.provider == "COMPATIBLE" }) // compatible not in official order
    }

    @Test
    fun demoCompatibleSuccessNeverCallsBlueLmOrLocal() {
        var blueLmCalls = 0
        val transport = HttpTransport { _, headers, _, _ ->
            if (headers.containsKey("app_id")) { blueLmCalls++; TransportResponse(503, "") } else okEnvelope()
        }
        val bundle = ProviderConfigBundle.forProfile(
            LearnerProfile.DEMO_COMPATIBLE,
            configs(blueLm = realBlueLm, compatible = realApiKey),
        )
        val outcome = analyze(bundle, transport) as AnalysisOutcome.Success
        assertEquals(ProviderKind.COMPATIBLE, outcome.result.provenance.provider)
        assertFalse(outcome.result.provenance.fallbackUsed)
        assertEquals(0, blueLmCalls)
        assertFalse(outcome.logs.any { it.provider == "BLUELM" || it.provider == "LOCAL_FALLBACK" })
    }

    @Test
    fun demoCompatibleFailWithBlueLmConfiguredUsesBlueLm() {
        val transport = HttpTransport { _, headers, _, _ ->
            if (headers.containsKey("app_id")) okEnvelope() else TransportResponse(503, "")
        }
        val bundle = ProviderConfigBundle.forProfile(
            LearnerProfile.DEMO_COMPATIBLE,
            configs(blueLm = realBlueLm, compatible = realApiKey),
        )
        val outcome = analyze(bundle, transport) as AnalysisOutcome.Success
        assertEquals(ProviderKind.BLUELM, outcome.result.provenance.provider)
        assertTrue(outcome.logs.any { it.provider == "COMPATIBLE" && it.status == "FAIL" })
    }

    @Test
    fun demoCompatibleFailWithBlueLmAbsentFallsBackToLocal() {
        val transport = HttpTransport { _, _, _, _ -> TransportResponse(503, "") }
        val bundle = ProviderConfigBundle.forProfile(
            LearnerProfile.DEMO_COMPATIBLE,
            configs(blueLm = Credential.None, compatible = realApiKey), // BlueLM not configured
        )
        val outcome = analyze(bundle, transport) as AnalysisOutcome.Success
        assertEquals(ProviderKind.LOCAL_FALLBACK, outcome.result.provenance.provider)
        assertTrue(outcome.logs.any { it.provider == "COMPATIBLE" && it.status == "FAIL" })
    }

    @Test
    fun localOnlyNeverTouchesTheNetwork() {
        var calls = 0
        val transport = HttpTransport { _, _, _, _ -> calls++; TransportResponse(200, "") }
        val bundle = ProviderConfigBundle.forProfile(LearnerProfile.LOCAL_ONLY, configs(blueLm = realBlueLm, compatible = realApiKey))
        val outcome = analyze(bundle, transport) as AnalysisOutcome.Success
        assertEquals(0, calls)
        assertEquals(ProviderKind.LOCAL_FALLBACK, outcome.result.provenance.provider)
        assertFalse(outcome.logs.any { it.provider == "BLUELM" || it.provider == "COMPATIBLE" })
    }

    // --- helpers ---

    private fun analyze(bundle: ProviderConfigBundle, transport: HttpTransport): AnalysisOutcome =
        CourseAnalyzer(ProviderResolver(bundle, promptBuilder, transport)).analyze(AnalysisRequest(session))

    private fun configs(
        blueLm: Credential = Credential.None,
        compatible: Credential = Credential.None,
    ): Map<ProviderKind, ProviderConfig> = mapOf(
        ProviderKind.BLUELM to ProviderConfig(ProviderKind.BLUELM, enabled = true, baseUrl = "https://bluelm.test", model = "qwen3.5-plus", credential = blueLm),
        ProviderKind.COMPATIBLE to ProviderConfig(ProviderKind.COMPATIBLE, enabled = true, baseUrl = "https://compat.test", model = "deepseek-v4pro", credential = compatible),
        ProviderKind.LOCAL_FALLBACK to ProviderConfig(ProviderKind.LOCAL_FALLBACK, enabled = true),
    )

    private fun okEnvelope(): TransportResponse {
        val wireJson = Json { encodeDefaults = true }.encodeToString(LocalHeuristicExtractor().extract(session, 8, 1))
        val body = buildJsonObject {
            putJsonArray("choices") { addJsonObject { putJsonObject("message") { put("content", wireJson) } } }
        }.toString()
        return TransportResponse(200, body)
    }
}
