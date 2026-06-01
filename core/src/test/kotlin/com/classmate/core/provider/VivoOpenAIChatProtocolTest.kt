package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.prompt.Prompt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VivoOpenAIChatProtocolTest {

    @Test
    fun requestBodyUsesOfficialChatCompletionsShape() {
        val body = VivoOpenAIChatRequestFactory.build(
            model = "Doubao-Seed-2.0-pro",
            prompt = Prompt(system = "system rules", user = "course prompt"),
            options = BlueLMRequestOptions(stream = false, temperature = 0.2, maxTokens = 2048),
        )
        val obj = Json.parseToJsonElement(body) as JsonObject
        val messages = obj["messages"] as JsonArray

        assertEquals("Doubao-Seed-2.0-pro", obj.str("model"))
        assertEquals(false, (obj["stream"] as JsonPrimitive).content.toBoolean())
        assertEquals(0.2, (obj["temperature"] as JsonPrimitive).content.toDouble(), 0.0001)
        assertEquals(2048, (obj["max_tokens"] as JsonPrimitive).content.toInt())
        assertEquals("system", (messages[0] as JsonObject).str("role"))
        assertEquals("user", (messages[1] as JsonObject).str("role"))
    }

    @Test
    fun qwen35PlusRequestBodyDisablesThinking() {
        val obj = requestObject("qwen3.5-plus")

        assertEquals("qwen3.5-plus", obj.str("model"))
        assertEquals(false, (obj["enable_thinking"] as JsonPrimitive).content.toBoolean())
    }

    @Test
    fun nonQwenModelsDoNotSendEnableThinking() {
        val doubao = requestObject("Doubao-Seed-2.0-pro")
        val deepSeek = requestObject("Volc-DeepSeek-V3.2")

        assertFalse(doubao.containsKey("enable_thinking"))
        assertFalse(deepSeek.containsKey("enable_thinking"))
    }

    @Test
    fun syncResponseReadsMessageContentAndOnlyReasoningMetadata() {
        val body = """
            {
              "choices": [
                {"message": {"content": "{\"knowledgePoints\":[],\"quizQuestions\":[]}", "reasoning_content": "private reasoning"}}
              ],
              "usage": {"prompt_tokens": 1, "completion_tokens": 2, "total_tokens": 3}
            }
        """.trimIndent()

        val read = VivoOpenAIChatResponseReader.readMessage(body)

        assertTrue(read.content!!.contains("knowledgePoints"))
        assertTrue(read.reasoningContentPresent)
        assertEquals("private reasoning".length, read.reasoningContentLength)
        assertFalse(read.toString().contains("private reasoning"))
    }

    @Test
    fun streamParserReadsContentReasoningMetadataAndDone() {
        val read = VivoOpenAIChatStreamParser.parseLines(
            listOf(
                """data: {"choices":[{"delta":{"content":"你","reasoning_content":"hidden-1"}}]}""",
                """data: {"choices":[{"delta":{"content":"好","reasoning_content":"hidden-2"}}]}""",
                "data: [DONE]",
            ),
        )

        assertEquals("你好", read.content)
        assertTrue(read.reasoningContentPresent)
        assertEquals("hidden-1hidden-2".length, read.reasoningContentLength)
        assertTrue(read.done)
        assertFalse(read.toString().contains("hidden-1"))
    }

    @Test
    fun providerErrorMapsVivoCodesWithoutLeakingBody() {
        assertEquals(
            ProviderErrorType.PARAM_ERROR,
            ProviderError.fromStatus(ProviderKind.BLUELM, 400, """{"code":1001,"message":"param requestId can't be empty"}""").type,
        )
        assertEquals(
            ProviderErrorType.CONTENT_BLOCKED,
            ProviderError.fromStatus(ProviderKind.BLUELM, 400, """{"code":1007,"message":"blocked"}""").type,
        )
        assertEquals(
            ProviderErrorType.MODEL_ACCESS_DENIED,
            ProviderError.fromStatus(ProviderKind.BLUELM, 403, """{"code":30001,"message":"no model access permission"}""").type,
        )
        assertEquals(
            ProviderErrorType.RATE_LIMITED,
            ProviderError.fromStatus(ProviderKind.BLUELM, 429, """{"code":30001,"message":"hit model rate limit"}""").type,
        )
        assertEquals(
            ProviderErrorType.USAGE_LIMIT,
            ProviderError.fromStatus(ProviderKind.BLUELM, 429, """{"code":2003,"message":"today usage limit"}""").type,
        )
        assertEquals(
            ProviderErrorType.APP_ID_HEADER_MISSING,
            ProviderError.fromStatus(ProviderKind.BLUELM, 401, """{"message":"app_id is required"}""").type,
        )
        assertEquals(
            ProviderErrorType.RATE_LIMITED,
            ProviderError.fromStatus(ProviderKind.BLUELM, 429, """{"message":"rate limited"}""").type,
        )
    }

    private fun requestObject(model: String): JsonObject {
        val body = VivoOpenAIChatRequestFactory.build(
            model = model,
            prompt = Prompt(system = "system rules", user = "course prompt"),
            options = BlueLMRequestOptions(stream = false, temperature = 0.1, maxTokens = 2200),
        )
        return Json.parseToJsonElement(body) as JsonObject
    }
}
