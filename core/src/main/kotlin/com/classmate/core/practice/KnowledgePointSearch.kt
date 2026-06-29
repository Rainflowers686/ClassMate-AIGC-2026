package com.classmate.core.practice

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Honest external search for a single knowledge point. ClassMate does NOT integrate any 百度/必应/B站 API
 * and never renders in-app "results" — it only builds a cleaned keyword query and hands a public results
 * URL to the system browser (ACTION_VIEW). The keyword comes ONLY from a high-confidence knowledge point;
 * a low-confidence / 待复核 point returns [NeedsReview] so the UI shows a "先完善资料" hint instead of an
 * empty or misleading button.
 */
object KnowledgePointSearch {

    /** Honest, browser-only destinations. The names say what they are; the UI prefixes them with "打开". */
    val sources: List<PracticeSearchSource> = listOf(
        PracticeSearchSource("B站搜索", "https://search.bilibili.com/all?keyword="),
        PracticeSearchSource("百度搜索", "https://www.baidu.com/s?wd="),
        PracticeSearchSource("必应搜索", "https://www.bing.com/search?q="),
        PracticeSearchSource("公开课程资源", "https://www.icourses.cn/search.htm?searchText="),
    )

    sealed interface Result {
        /** High-confidence point: ready-to-open browser links built from a cleaned query. */
        data class Available(val query: String, val links: List<PracticeSearchLink>) : Result

        /** Low-confidence / 待核对 point: do not offer a search; prompt the user to complete the material first. */
        object NeedsReview : Result
    }

    fun forKnowledgePoint(courseTitle: String, knowledgePointTitle: String, highConfidence: Boolean): Result {
        if (!highConfidence) return Result.NeedsReview
        // Require a real topic — never build a search out of only the generic "讲解" keyword (no empty buttons).
        val core = PracticeSearchEngine.sanitizeQuery("$courseTitle $knowledgePointTitle")
        if (core.isBlank()) return Result.NeedsReview
        val query = PracticeSearchEngine.sanitizeQuery("$core 讲解")
        val links = sources.map { src ->
            PracticeSearchLink(src.name, query, src.urlPrefix + URLEncoder.encode(query, StandardCharsets.UTF_8.name()))
        }
        return Result.Available(query, links)
    }
}
