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

    @Test
    fun markdownFencedJsonStillValidatesAsBlueLm() {
        val session = SampleCourses.seriesSession()
        val content = "```json\n" + validWireJson(session) + "\n```"
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, HttpTransport { _, _, _, _ -> TransportResponse(200, envelope(content)) })

        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.BLUELM, outcome.result.provenance.provider)
        assertFalse(outcome.result.provenance.fallbackUsed)
    }

    @Test
    fun jsonWithSurroundingProseStillValidatesAsBlueLm() {
        val session = SampleCourses.seriesSession()
        val content = "好的，这是分析结果：\n" + validWireJson(session) + "\n以上就是全部内容。"
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, HttpTransport { _, _, _, _ -> TransportResponse(200, envelope(content)) })

        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.BLUELM, outcome.result.provenance.provider)
        assertFalse(outcome.result.provenance.fallbackUsed)
    }

    @Test
    fun naturalLanguageFailsAsJsonParseFailedAndFallsBack() {
        val session = SampleCourses.seriesSession()
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, HttpTransport { _, _, _, _ -> TransportResponse(200, envelope("我无法把这段课堂文本解析为 JSON。")) })

        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.LOCAL_FALLBACK, outcome.result.provenance.provider)
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.validationErrorType == "JSON_PARSE_FAILED" })
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.jsonExtracted == false })
    }

    @Test
    fun brokenReferenceFailsAsReferenceBrokenAndFallsBack() {
        val session = SampleCourses.seriesSession()
        val brokenRef =
            """{"knowledgePoints":[{"title":"级数的定义","summary":"无穷级数的概念","sourceSegmentId":"seg_1","evidenceQuotes":["称为无穷级数"],"importance":"HIGH","difficulty":"EASY","tags":[]}],"quizQuestions":[{"type":"CONCEPT_UNDERSTANDING","stem":"下列说法正确的是？","options":[{"text":"无穷级数是数列各项之和","isCorrect":true,"rationale":"对"},{"text":"无穷级数就是数列本身","isCorrect":false,"rationale":"错"}],"testedKnowledgePoints":["完全不存在的知识点"],"evidenceQuotes":["称为无穷级数"],"explanation":"讲解","difficulty":"EASY"}]}"""
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, HttpTransport { _, _, _, _ -> TransportResponse(200, envelope(brokenRef)) })

        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.LOCAL_FALLBACK, outcome.result.provenance.provider)
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.validationErrorType == "REFERENCE_BROKEN" })
    }

    @Test
    fun unlocatableEvidenceFailsAsEvidenceMismatchAndFallsBack() {
        val session = SampleCourses.seriesSession()
        val mismatch =
            """{"knowledgePoints":[{"title":"级数的定义","summary":"概念","sourceSegmentId":"seg_1","evidenceQuotes":["这句话根本不在任何原文段落里面出现过"],"importance":"HIGH","difficulty":"EASY","tags":[]}],"quizQuestions":[{"type":"JUDGMENT","stem":"判断：级数是数列各项之和","options":[{"text":"正确","isCorrect":true,"rationale":"对"},{"text":"错误","isCorrect":false,"rationale":"错"}],"testedKnowledgePoints":["级数的定义"],"evidenceQuotes":["称为无穷级数"],"explanation":"讲解","difficulty":"EASY"}]}"""
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, HttpTransport { _, _, _, _ -> TransportResponse(200, envelope(mismatch)) })

        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.LOCAL_FALLBACK, outcome.result.provenance.provider)
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.validationErrorType == "EVIDENCE_MISMATCH" })
    }

    @Test
    fun repairRetrySucceedsOnSecondAttemptWithoutFallback() {
        val session = SampleCourses.seriesSession()
        var call = 0
        val transport = HttpTransport { _, _, _, _ ->
            call++
            val content = if (call == 1) "抱歉，我先用自然语言回答这道题。" else validWireJson(session)
            TransportResponse(200, envelope(content))
        }
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, transport)

        val outcome = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
        assertEquals(2, call) // first bad, one repair retry
        assertTrue(outcome is AnalysisOutcome.Success)
        outcome as AnalysisOutcome.Success
        assertEquals(ProviderKind.BLUELM, outcome.result.provenance.provider)
        assertFalse(outcome.result.provenance.fallbackUsed)
        assertFalse(outcome.logs.any { it.provider == "LOCAL_FALLBACK" })
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.validation == "FAIL" })
        assertTrue(outcome.logs.any { it.provider == "BLUELM" && it.validation == "PASS" })
    }

    @Test
    fun redactedLogsNeverLeakSecretsBodyOrReasoning() {
        val session = SampleCourses.seriesSession()
        val secretReasoning = "REASONING_SHOULD_NEVER_APPEAR"
        val body = buildJsonObject {
            putJsonArray("choices") {
                addJsonObject {
                    putJsonObject("message") {
                        put("content", validWireJson(session))
                        put("reasoning_content", secretReasoning)
                    }
                }
            }
        }.toString()
        val transport = HttpTransport { _, headers, _, _ ->
            assertEquals("Bearer appkey1234567", headers["Authorization"]) // the secret only lives here
            TransportResponse(200, body)
        }
        val resolver = ProviderResolver(wiredBundle(), promptBuilder, transport)

        val rendered = CourseAnalyzer(resolver).analyze(AnalysisRequest(session))
            .logs.joinToString("\n") { it.format() }
        assertFalse(rendered.contains("appkey1234567"))
        assertFalse(rendered.contains("Authorization"))
        assertFalse(rendered.contains("Bearer"))
        assertFalse(rendered.contains(secretReasoning))
        assertFalse(rendered.contains("称为无穷级数")) // no course text / evidence body in logs
    }

    private fun envelope(content: String): String =
        buildJsonObject {
            putJsonArray("choices") { addJsonObject { putJsonObject("message") { put("content", content) } } }
        }.toString()

    private fun validWireJson(session: com.classmate.core.model.CourseSession): String =
        Json { encodeDefaults = true }.encodeToString(LocalHeuristicExtractor().extract(session, 8, 1))

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
