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
    fun qwenRequestBodyDisablesThinkingAtTopLevel() {
        val body = VivoOpenAIChatRequestFactory.build(
            model = "qwen3.5-plus",
            prompt = Prompt("system rules", "user content"),
            options = BlueLMRequestOptions(stream = false, temperature = 0.1, maxTokens = 2200),
        )
        val obj = Json.parseToJsonElement(body) as JsonObject
        // The qwen3.5-plus request body MUST carry top-level enable_thinking=false.
        assertEquals(false, (obj["enable_thinking"] as? JsonPrimitive)?.booleanOrNull)
        assertEquals("qwen3.5-plus", (obj["model"] as JsonPrimitive).content)
    }
}
