package com.classmate.core.provider

import com.classmate.core.prompt.Prompt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import org.junit.Assert.assertEquals
import org.junit.Test

class VivoRequestFactoryTest {

    @Test
    fun qwenRequestBodyEnablesThinkingForDeepStudyWhenSupported() {
        val body = VivoOpenAIChatRequestFactory.build(
            model = "qwen3.5-plus",
            prompt = Prompt("system rules", "user content"),
            options = CloudModelQualityProfile.DEEP_STUDY.toRequestOptions(
                config = ProviderConfigBundle.defaults().configOf(com.classmate.core.model.ProviderKind.BLUELM)!!,
            ),
        )
        val obj = Json.parseToJsonElement(body) as JsonObject
        assertEquals(true, (obj["enable_thinking"] as? JsonPrimitive)?.booleanOrNull)
        assertEquals("high", (obj["reasoning_effort"] as JsonPrimitive).content)
        assertEquals(65_536, (obj["max_completion_tokens"] as JsonPrimitive).content.toInt())
        assertEquals("qwen3.5-plus", (obj["model"] as JsonPrimitive).content)
    }

    @Test
    fun qwenFastAndBalancedRequestBodiesDisableThinking() {
        val config = ProviderConfigBundle.defaults().configOf(com.classmate.core.model.ProviderKind.BLUELM)!!

        val fast = Json.parseToJsonElement(
            VivoOpenAIChatRequestFactory.build(
                model = "qwen3.5-plus",
                prompt = Prompt("system rules", "user content"),
                options = CloudModelQualityProfile.FAST.toRequestOptions(config),
            ),
        ) as JsonObject
        val balanced = Json.parseToJsonElement(
            VivoOpenAIChatRequestFactory.build(
                model = "qwen3.5-plus",
                prompt = Prompt("system rules", "user content"),
                options = CloudModelQualityProfile.BALANCED.toRequestOptions(config),
            ),
        ) as JsonObject

        assertEquals(false, (fast["enable_thinking"] as? JsonPrimitive)?.booleanOrNull)
        assertEquals("low", (fast["reasoning_effort"] as JsonPrimitive).content)
        assertEquals(8192, (fast["max_completion_tokens"] as JsonPrimitive).content.toInt())
        assertEquals(false, (balanced["enable_thinking"] as? JsonPrimitive)?.booleanOrNull)
        assertEquals("medium", (balanced["reasoning_effort"] as JsonPrimitive).content)
        assertEquals(32768, (balanced["max_completion_tokens"] as JsonPrimitive).content.toInt())
    }
}
