package com.classmate.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonExtractorTest {

    @Test
    fun extractsPureJson() {
        assertEquals("""{"a":1}""", JsonExtractor.extract("""{"a":1}"""))
    }

    @Test
    fun extractsFromMarkdownFence() {
        val text = "这是模型的解释：\n```json\n{\"knowledgePoints\":[]}\n```\n谢谢"
        assertEquals("""{"knowledgePoints":[]}""", JsonExtractor.extract(text))
    }

    @Test
    fun extractsFirstBalancedObjectAmidProse() {
        val text = "好的，结果是 {\"a\":{\"b\":2}} 以上"
        assertEquals("""{"a":{"b":2}}""", JsonExtractor.extract(text))
    }

    @Test
    fun ignoresBracesInsideStrings() {
        val input = """{"note":"a } b","ok":true}"""
        assertEquals(input, JsonExtractor.extract(input))
    }

    @Test
    fun returnsNullWhenNoJson() {
        assertNull(JsonExtractor.extract("完全没有 JSON 的一段话"))
    }
}
