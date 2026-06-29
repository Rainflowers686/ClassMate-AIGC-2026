package com.classmate.app.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BilibiliSearchTest {
    @Test
    fun keywordBuildsSearchUrl() {
        val link = BilibiliSearch.linkFor(keyword = "导数 极限", courseTitle = "高数")

        assertEquals("B站搜索", link.sourceName)
        assertTrue(link.url.startsWith("https://search.bilibili.com/all?keyword="))
        assertTrue(link.url.contains("%E9%AB%98%E6%95%B0"))
        assertTrue(link.url.contains("%E5%AF%BC%E6%95%B0"))
    }

    @Test
    fun emptyKeywordUsesSafeFallbackAndDoesNotOverclaimRecommendation() {
        val link = BilibiliSearch.linkFor(keyword = "", courseTitle = "")

        assertEquals("课程讲解", link.query)
        assertTrue(link.url.endsWith("%E8%AF%BE%E7%A8%8B%E8%AE%B2%E8%A7%A3"))
        assertFalse(link.sourceName.contains("AI 精准推荐"))
        assertFalse(link.url.contains("Authorization", ignoreCase = true))
    }
}
