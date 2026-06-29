package com.classmate.core.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgePointSearchTest {

    @Test
    fun highConfidencePointBuildsBrowserLinks() {
        val r = KnowledgePointSearch.forKnowledgePoint("高等数学", "比值判别法", highConfidence = true)
        assertTrue(r is KnowledgePointSearch.Result.Available)
        val available = r as KnowledgePointSearch.Result.Available
        assertEquals(4, available.links.size)
        // honest browser destinations, every URL points at a public results page (not an app API endpoint)
        assertTrue(available.links.any { it.sourceName == "B站搜索" && it.url.startsWith("https://search.bilibili.com/") })
        assertTrue(available.links.any { it.sourceName == "百度搜索" && it.url.startsWith("https://www.baidu.com/") })
        assertTrue(available.links.any { it.sourceName == "必应搜索" && it.url.startsWith("https://www.bing.com/") })
        assertTrue(available.links.any { it.sourceName == "公开课程资源" })
        // query carries the real topic
        assertTrue(available.query.contains("比值判别法"))
    }

    @Test
    fun lowConfidenceOrFlaggedPointReturnsNeedsReview() {
        val r = KnowledgePointSearch.forKnowledgePoint("高等数学", "比值判别法", highConfidence = false)
        assertEquals(KnowledgePointSearch.Result.NeedsReview, r)
    }

    @Test
    fun blankTopicReturnsNeedsReviewNotAnEmptySearch() {
        val r = KnowledgePointSearch.forKnowledgePoint("   ", "   ", highConfidence = true)
        assertEquals(KnowledgePointSearch.Result.NeedsReview, r)
    }

    @Test
    fun queryIsCleanedOfIdsNewlinesAndLength() {
        val dirty = "高等数学\n\t比值判别法 kp_abc123 q_55 ev_9  " + "x".repeat(200)
        val clean = PracticeSearchEngine.sanitizeQuery(dirty)
        assertFalse("no newlines/tabs", clean.contains("\n") || clean.contains("\t"))
        assertFalse("no kp_ id", clean.contains("kp_abc123"))
        assertFalse("no q_ id", clean.contains("q_55"))
        assertFalse("no ev_ id", clean.contains("ev_9"))
        assertTrue("length capped", clean.length <= PracticeSearchEngine.MAX_QUERY_LENGTH)
        assertTrue("keeps the real topic", clean.contains("比值判别法"))
    }

    @Test
    fun copyNeverClaimsAnApiIntegration() {
        // The honest source names describe a browser search / public resource, never an "API".
        KnowledgePointSearch.sources.forEach { src ->
            assertFalse(src.name.contains("API"))
            assertTrue(src.name.contains("搜索") || src.name.contains("资源"))
        }
    }
}
