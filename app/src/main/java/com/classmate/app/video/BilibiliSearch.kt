package com.classmate.app.video

import com.classmate.core.practice.PracticeSearchLink
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object BilibiliSearch {
    private const val PREFIX = "https://search.bilibili.com/all?keyword="
    private const val FALLBACK_QUERY = "课程讲解"

    fun linkFor(keyword: String?, courseTitle: String? = null): PracticeSearchLink {
        val query = listOfNotNull(courseTitle, keyword)
            .joinToString(" ")
            .replace(Regex("(?i)Authorization|Bearer|appKey|apiKey|config\\.local\\.json|prompt|messages"), "")
            .trim()
            .ifBlank { FALLBACK_QUERY }
        return PracticeSearchLink(
            sourceName = "B站搜索",
            query = query,
            url = PREFIX + URLEncoder.encode(query, StandardCharsets.UTF_8.name()),
        )
    }
}
