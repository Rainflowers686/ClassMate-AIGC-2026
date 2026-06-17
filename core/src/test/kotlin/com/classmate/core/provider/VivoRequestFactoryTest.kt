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
}
