package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueLMProviderTest {

    @Test
    fun fakeTransportAndSignerCanReturnAssistantText() {
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { url, headers, body, _ ->
                assertTrue(url.contains("__official_bluelm_path_pending__"))
                assertTrue(headers.containsKey("X-Test-Auth"))
                assertTrue(body.isNotBlank())
                TransportResponse(200, """{"data":{"content":"{\"knowledgePoints\":[],\"quizQuestions\":[]}"}}""")
            },
            signer = BlueLmSigner { _, _, _, _ -> mapOf("X-Test-Auth" to "ok") },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Success)
        result as ProviderResult.Success
        assertTrue(result.rawModelText.contains("knowledgePoints"))
    }

    @Test
    fun non2xxDropsVendorBodyFromFailure() {
        val sensitiveVendorBody = "vendor-body-with-prompt-course-and-fake-app-key-for-tests"
        val provider = BlueLMProvider(
            config = fakeBlueLmConfig(),
            promptBuilder = PromptBuilder(),
            transport = HttpTransport { _, _, _, _ -> TransportResponse(503, sensitiveVendorBody) },
            signer = BlueLmSigner { _, _, _, _ -> mapOf("X-Test-Auth" to "ok") },
        )

        val result = provider.generate(AnalysisRequest(SampleCourses.seriesSession()))

        assertTrue(result is ProviderResult.Failure)
        assertFalse(result.toString().contains(sensitiveVendorBody))
        assertTrue(result.toString().contains("HTTP_NON_2XX"))
    }

    @Test
    fun unconfiguredProviderIsNotAvailable() {
        val provider = BlueLMProvider(
            config = ProviderConfigBundle.defaults().configOf(ProviderKind.BLUELM)!!,
            promptBuilder = PromptBuilder(),
        )

        assertFalse(provider.isAvailable())
    }

    private fun fakeBlueLmConfig() = ProviderConfig(
        kind = ProviderKind.BLUELM,
        enabled = true,
        baseUrl = "https://fake-blue-lm.test",
        model = "fake-blue-model",
        credential = Credential.BlueLm("fake-app-id", "fake-app-key-for-tests"),
    )
}
