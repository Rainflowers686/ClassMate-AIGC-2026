package com.classmate.core.analysis

import com.classmate.core.model.ProviderKind
import com.classmate.core.parser.WireAnalysis
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.provider.AnalysisRequest
import com.classmate.core.provider.BlueLmSigner
import com.classmate.core.provider.Credential
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.LocalHeuristicExtractor
import com.classmate.core.provider.ProviderConfig
import com.classmate.core.provider.ProviderConfigBundle
import com.classmate.core.provider.ProviderResolver
import com.classmate.core.provider.TransportResponse
import com.classmate.core.sample.SampleCourses
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseAnalyzerTest {

    private val promptBuilder = PromptBuilder()

    @Test
    fun fallsBackToLocalWhenNothingIsConfigured() {
        val session = SampleCourses.seriesSession()
        val resolver = ProviderResolver(ProviderConfigBundle.defaults(), promptBuilder)
        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))

        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.LOCAL_FALLBACK, outcome.result.provenance.provider)
        assertTrue(outcome.result.provenance.fallbackUsed)
        assertTrue(outcome.result.knowledgePoints.isNotEmpty())
        assertTrue(outcome.result.knowledgePoints.all { it.hasEvidence })
        // The redacted trail shows BlueLM was tried first and the fallback passed.
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.status == "FAIL" })
        assertTrue(outcome.logs.any { it.provider == "LOCAL_FALLBACK" && it.validation == "PASS" })
    }

    @Test
    fun usesBlueLmAsPrimaryWhenWired() {
        val session = SampleCourses.seriesSession()
        // A guaranteed-valid wire payload, wrapped in the official OpenAI-compatible envelope.
        val wire = LocalHeuristicExtractor().extract(session, 8, 1)
        val wireJson = Json { encodeDefaults = true }.encodeToString(wire)
        val envelope = buildJsonObject {
            putJsonArray("choices") {
                addJsonObject {
                    putJsonObject("message") { put("content", wireJson) }
                }
            }
        }.toString()

        val transport = HttpTransport { _, headers, _, _ ->
            assertEquals("appid123456", headers["app_id"])
            assertEquals("Bearer appkey1234567", headers["Authorization"])
            TransportResponse(200, envelope)
        }
        val signer = BlueLmSigner { _, _, _, _ -> mapOf("X-Test-Auth" to "ok") }
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, transport, signer)

        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.BLUELM, outcome.result.provenance.provider)
        assertFalse(outcome.result.provenance.fallbackUsed)
        assertTrue(outcome.result.knowledgePoints.isNotEmpty())
        assertFalse(outcome.logs.any { it.provider == "LOCAL_FALLBACK" })
    }

    @Test
    fun fallsBackWhenPrimaryReturnsNon2xx() {
        val session = SampleCourses.seriesSession()
        val transport = HttpTransport { _, _, _, _ -> TransportResponse(503, "") }
        val signer = BlueLmSigner { _, _, _, _ -> mapOf("X-Test-Auth" to "ok") }
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, transport, signer)

        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.LOCAL_FALLBACK, outcome.result.provenance.provider)
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.errorType == "HTTP_NON_2XX" })
    }

    private fun wiredBundle() = ProviderConfigBundle(
        primary = ProviderKind.BLUELM,
        order = listOf(ProviderKind.BLUELM, ProviderKind.LOCAL_FALLBACK),
        configs = mapOf(
            ProviderKind.BLUELM to ProviderConfig(
                kind = ProviderKind.BLUELM,
                enabled = true,
                baseUrl = "https://example.test",
                model = "vivo-BlueLM-TB-Pro",
                credential = Credential.BlueLm("appid123456", "appkey1234567"),
            ),
            ProviderKind.LOCAL_FALLBACK to ProviderConfig(ProviderKind.LOCAL_FALLBACK, enabled = true),
        ),
    )
}
