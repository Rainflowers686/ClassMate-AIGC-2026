package com.classmate.core.video

import com.classmate.core.learning.ReviewTask
import com.classmate.core.model.KnowledgePoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class VideoRecommendation(
    val id: String,
    val knowledgePointId: String,
    val title: String,
    val source: String,
    val searchUrl: String,
    val reason: String,
    val triggeredBy: String,
)

data class VideoSource(
    val name: String,
    val searchUrlPrefix: String,
)

object VideoRecommendationEngine {
    val whitelist: List<VideoSource> = listOf(
        VideoSource("\u0042\u7ad9\u77e5\u8bc6\u533a", "https://search.bilibili.com/all?keyword="),
        VideoSource("\u53ef\u6c57\u5b66\u9662", "https://www.khanacademy.org/search?page_search_query="),
        VideoSource("Crash Course", "https://www.youtube.com/results?search_query=Crash%20Course%20"),
        VideoSource("\u56fd\u5bb6\u4e2d\u5c0f\u5b66\u667a\u6167\u6559\u80b2\u5e73\u53f0", "https://basic.smartedu.cn/syncClassroom?keyword="),
        VideoSource("\u9ad8\u6821\u516c\u5f00\u8bfe", "https://www.icourses.cn/search.htm?searchText="),
    )

    fun recommendationsForTask(task: ReviewTask, maxPerKnowledgePoint: Int = 2): List<VideoRecommendation> {
        val trigger = triggerFor(task) ?: return emptyList()
        return buildRecommendations(
            knowledgePointId = task.knowledgePointId,
            title = task.title,
            reason = when (trigger) {
                "wrong_answer" -> "Wrong answers reached the review threshold."
                "need_example" -> "The learner asked for more examples."
                "priority_high" -> "This item is currently high priority."
                else -> "The learner requested an external explanation."
            },
            triggeredBy = trigger,
            maxPerKnowledgePoint = maxPerKnowledgePoint,
        )
    }

    fun recommendationsForKnowledgePoint(
        knowledgePoint: KnowledgePoint,
        userRequested: Boolean,
        maxPerKnowledgePoint: Int = 2,
    ): List<VideoRecommendation> {
        if (!userRequested) return emptyList()
        return buildRecommendations(
            knowledgePointId = knowledgePoint.id,
            title = knowledgePoint.title,
            reason = "The learner explicitly requested a video explanation.",
            triggeredBy = "user_request",
            maxPerKnowledgePoint = maxPerKnowledgePoint,
        )
    }

    private fun triggerFor(task: ReviewTask): String? = when {
        task.counters.wrongAnswer >= 2 -> "wrong_answer"
        task.counters.needExample >= 1 -> "need_example"
        task.priority >= HIGH_PRIORITY -> "priority_high"
        else -> null
    }

    private fun buildRecommendations(
        knowledgePointId: String,
        title: String,
        reason: String,
        triggeredBy: String,
        maxPerKnowledgePoint: Int,
    ): List<VideoRecommendation> {
        val encoded = encodeSearch(title)
        return whitelist.take(maxPerKnowledgePoint.coerceIn(1, 2)).mapIndexed { index, source ->
            VideoRecommendation(
                id = "video_${knowledgePointId}_${index + 1}",
                knowledgePointId = knowledgePointId,
                title = title,
                source = source.name,
                searchUrl = source.searchUrlPrefix + encoded,
                reason = reason,
                triggeredBy = triggeredBy,
            )
        }
    }

    internal fun encodeSearch(value: String): String =
        URLEncoder.encode(value.trim(), StandardCharsets.UTF_8.name())

    private const val HIGH_PRIORITY = 8
}
