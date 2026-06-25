package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.sample.SampleCourses
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueLMProviderTest {

    @Test
    fun fakeTransportAndFakeKeyCanReturnAssistantText() {
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { url, headers, body, _ ->
                assertTrue(url.contains("/v1/chat/completions?request_id=req-123"))
                assertEquals("Bearer fake-app-key-for-tests", headers["Authorization"])
                assertEquals("fake-app-id", headers["app_id"])
                assertEquals("application/json; charset=utf-8", headers["Content-Type"])
                assertTrue(body.isNotBlank())
                TransportResponse(200, """{"choices":[{"message":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}]}""")
            },
            requestIdFactory = { "req-123" },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Success)
        result as ProviderResult.Success
        assertTrue(result.rawModelText.contains("knowledgePoints"))
    }

    @Test
    fun analysisPathUsesAnalysisProfileAndLongReadTimeout() {
        var capturedProfile: HttpRequestProfile? = null
        var capturedTimeouts: HttpTimeouts? = null
        val transport = object : HttpTransport {
            override fun postJson(
                url: String,
                headers: Map<String, String>,
                body: String,
                timeoutMs: Long,
            ): TransportResponse = error("BlueLMProvider must use the profiled transport call")

            override fun postJson(
                url: String,
                headers: Map<String, String>,
                body: String,
                profile: HttpRequestProfile,
                timeouts: HttpTimeouts,
            ): TransportResponse {
                capturedProfile = profile
                capturedTimeouts = timeouts
                assertEquals("Bearer fake-app-key-for-tests", headers["Authorization"])
                assertEquals("fake-app-id", headers["app_id"])
                return TransportResponse(200, """{"choices":[{"message":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}]}""")
            }
        }
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = transport,
            requestIdFactory = { "req-profile" },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Success)
        assertEquals(HttpRequestProfile.ANALYSIS, capturedProfile)
        assertEquals(15_000L, capturedTimeouts?.connectTimeoutMs)
        assertEquals(120_000L, capturedTimeouts?.readTimeoutMs)
    }

    @Test
    fun qwenAnalysisRequestUsesDeepStudyThinkingWithImportedModel() {
        var capturedProfile: HttpRequestProfile? = null
        val transport = object : HttpTransport {
            override fun postJson(
                url: String,
                headers: Map<String, String>,
                body: String,
                timeoutMs: Long,
            ): TransportResponse = error("BlueLMProvider must use the profiled transport call")

            override fun postJson(
                url: String,
                headers: Map<String, String>,
                body: String,
                profile: HttpRequestProfile,
                timeouts: HttpTimeouts,
            ): TransportResponse {
                capturedProfile = profile
                assertTrue(url.contains("/v1/chat/completions?request_id=qwen-req"))
                assertEquals("Bearer fake-app-key-for-tests", headers["Authorization"])
                assertEquals("fake-app-id", headers["app_id"])
                val root = Json.parseToJsonElement(body) as JsonObject
                assertEquals("qwen3.5-plus", (root["model"] as JsonPrimitive).content)
                assertEquals(true, (root["enable_thinking"] as JsonPrimitive).content.toBoolean())
                assertEquals("high", (root["reasoning_effort"] as JsonPrimitive).content)
                assertEquals(4096, (root["max_tokens"] as JsonPrimitive).content.toInt())
                assertEquals(65_536, (root["max_completion_tokens"] as JsonPrimitive).content.toInt())
                assertEquals(false, (root["stream"] as JsonPrimitive).content.toBoolean())
                return TransportResponse(200, """{"choices":[{"message":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}]}""")
            }
        }
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig().copy(model = "qwen3.5-plus", maxTokens = 2200, temperature = 0.1),
            promptBuilder = PromptBuilder(),
            transport = transport,
            requestIdFactory = { "qwen-req" },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Success)
        assertEquals(HttpRequestProfile.ANALYSIS, capturedProfile)
    }

    @Test
    fun defaultsToRequestIdUnderscoreAndRetriesCamelCaseOnDocumented1001() {
        val urls = mutableListOf<String>()
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { url, _, _, _ ->
                urls += url
                if (url.contains("request_id=")) {
                    TransportResponse(400, """{"code":1001,"message":"param requestId can't be empty"}""")
                } else {
                    TransportResponse(200, """{"choices":[{"message":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}]}""")
                }
            },
            requestIdFactory = { "retry-req" },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Success)
        assertTrue(urls[0].contains("request_id=retry-req"))
        assertTrue(urls[1].contains("requestId=retry-req"))
    }

    @Test
    fun configurableRequestIdQueryNameSkipsFallback() {
        val urls = mutableListOf<String>()
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(requestIdQueryName = "requestId"),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { url, _, _, _ ->
                urls += url
                TransportResponse(200, """{"choices":[{"message":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}]}""")
            },
            requestIdFactory = { "camel-req" },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Success)
        assertEquals(1, urls.size)
        assertTrue(urls.single().contains("requestId=camel-req"))
    }

    @Test
    fun non2xxDropsVendorBodyFromFailure() {
        val sensitiveVendorBody = "vendor-body-with-prompt-course-and-fake-app-key-for-tests"
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { _, _, _, _ -> TransportResponse(503, sensitiveVendorBody) },
            requestIdFactory = { "req-503" },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Failure)
        assertFalse(result.toString().contains(sensitiveVendorBody))
        assertTrue(result.toString().contains("HTTP_NON_2XX"))
    }

    @Test
    fun missingAppIdHeaderResponseMapsToSafeAuthError() {
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { _, _, _, _ ->
                TransportResponse(401, """{"message":"app_id is required"}""")
            },
            requestIdFactory = { "req-auth" },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Failure)
        result as ProviderResult.Failure
        assertEquals(ProviderErrorType.APP_ID_HEADER_MISSING, result.error.type)
        assertFalse(result.toString().contains("app_id is required"))
    }

    @Test
    fun unconfiguredProviderIsNotAvailable() {
        val provider = BlueLMProvider(
            config = ProviderConfigBundle.defaults().configOf(ProviderKind.BLUELM)!!,
            promptBuilder = PromptBuilder(),
        )

        assertFalse(provider.isAvailable())
    }

    @Test
    fun missingAppIdProviderIsNotAvailable() {
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig().copy(credential = Credential.BlueLm("", "fake-app-key-for-tests")),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { _, _, _, _ -> TransportResponse(200, "{}") },
        )

        assertFalse(provider.isAvailable())
    }

    @Test
    fun slowReadIsRetriedWithBackoffThenSucceeds() {
        var calls = 0
        val sleeps = mutableListOf<Long>()
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = object : HttpTransport {
                override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): TransportResponse =
                    error("BlueLMProvider must use the profiled transport call")

                override fun postJson(url: String, headers: Map<String, String>, body: String, profile: HttpRequestProfile, timeouts: HttpTimeouts): TransportResponse {
                    calls++
                    if (calls == 1) throw TransportDiagnosticException(BlueLMDiagnosticStage.READ, BlueLMDiagnosticSubtype.IO)
                    return TransportResponse(200, """{"choices":[{"message":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}]}""")
                }
            },
            requestIdFactory = { "req-read" },
            sleeper = { sleeps += it },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession(), intensity = AnalysisIntensity.STANDARD))

        assertTrue(result is ProviderResult.Success)
        assertEquals(2, calls) // STANDARD allows 1 READ retry
        assertEquals(listOf(1_000L), sleeps) // exponential backoff, first step 1s
    }

    @Test
    fun slowReadRetriesAreBoundedThenReturnDiagnosableFailure() {
        var calls = 0
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = object : HttpTransport {
                override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): TransportResponse =
                    error("BlueLMProvider must use the profiled transport call")

                override fun postJson(url: String, headers: Map<String, String>, body: String, profile: HttpRequestProfile, timeouts: HttpTimeouts): TransportResponse {
                    calls++
                    throw TransportDiagnosticException(BlueLMDiagnosticStage.READ, BlueLMDiagnosticSubtype.IO)
                }
            },
            requestIdFactory = { "req-read-fail" },
            sleeper = {},
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession(), intensity = AnalysisIntensity.DEEP))

        assertTrue(result is ProviderResult.Failure)
        result as ProviderResult.Failure
        assertEquals(3, calls) // DEEP allows 2 READ retries → 3 attempts total
        assertEquals("BLUELM:NETWORK:READ", result.error.shortCode)
    }

    @Test
    fun non2xxIsNotRetriedEvenWhenIntensityAllowsReadRetries() {
        var calls = 0
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { _, _, _, _ -> calls++; TransportResponse(503, "busy") },
            requestIdFactory = { "req-503" },
            sleeper = {},
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession(), intensity = AnalysisIntensity.DEEP))

        assertTrue(result is ProviderResult.Failure)
        assertEquals(1, calls) // HTTP_NON_2XX is not a transient READ wobble → no retry
    }

    @Test
    fun degradedRetryRequestIsShorterThanTheFirstAttempt() {
        val bodies = mutableListOf<String>()
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = object : HttpTransport {
                override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): TransportResponse =
                    error("BlueLMProvider must use the profiled transport call")

                override fun postJson(url: String, headers: Map<String, String>, body: String, profile: HttpRequestProfile, timeouts: HttpTimeouts): TransportResponse {
                    bodies += body
                    if (bodies.size == 1) throw TransportDiagnosticException(BlueLMDiagnosticStage.READ, BlueLMDiagnosticSubtype.IO)
                    return TransportResponse(200, """{"choices":[{"message":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}]}""")
                }
            },
            requestIdFactory = { "req-degrade" },
            sleeper = {},
        )

        provider.generate(AnalysisRequest(SampleCourses.seriesSession(), intensity = AnalysisIntensity.STANDARD))

        assertEquals(2, bodies.size)
        fun maxTokensOf(body: String): Int = Regex("\"max_tokens\":\\s*(\\d+)").find(body)!!.groupValues[1].toInt()
        assertTrue("degraded retry must request fewer output tokens", maxTokensOf(bodies[1]) < maxTokensOf(bodies[0]))
    }

    @Test
    fun intensityDrivesTheHttpReadTimeout() {
        var captured: HttpTimeouts? = null
        val transport = object : HttpTransport {
            override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): TransportResponse =
                error("BlueLMProvider must use the profiled transport call")

            override fun postJson(url: String, headers: Map<String, String>, body: String, profile: HttpRequestProfile, timeouts: HttpTimeouts): TransportResponse {
                captured = timeouts
                return TransportResponse(200, """{"choices":[{"message":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}]}""")
            }
        }
        val provider = BlueLMProvider(config = fakeBlueLmConfig(), promptBuilder = PromptBuilder(), transport = transport, requestIdFactory = { "req-fast" })

        provider.generate(AnalysisRequest(SampleCourses.seriesSession(), intensity = AnalysisIntensity.FAST))

        assertEquals(45_000L, captured?.readTimeoutMs)
    }

    private fun fakeBlueLmConfig(requestIdQueryName: String = "request_id") = ProviderConfig(
        kind = ProviderKind.BLUELM,
        enabled = true,
        baseUrl = "https://api-ai.vivo.com.cn/v1",
        model = "Doubao-Seed-2.0-pro",
        credential = Credential.BlueLm("fake-app-id", "fake-app-key-for-tests"),
        requestIdQueryName = requestIdQueryName,
    )
}
