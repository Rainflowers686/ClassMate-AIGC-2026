package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.sample.SampleCourses
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

    private fun fakeBlueLmConfig(requestIdQueryName: String = "request_id") = ProviderConfig(
        kind = ProviderKind.BLUELM,
        enabled = true,
        baseUrl = "https://api-ai.vivo.com.cn/v1",
        model = "Doubao-Seed-2.0-pro",
        credential = Credential.BlueLm("fake-app-id", "fake-app-key-for-tests"),
        requestIdQueryName = requestIdQueryName,
    )
}
