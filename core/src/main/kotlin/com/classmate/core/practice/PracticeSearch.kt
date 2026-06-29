package com.classmate.core.practice

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** A named external search destination (a public search/results page — never an in-app crawler). */
data class PracticeSearchSource(val name: String, val urlPrefix: String)

/** A ready-to-open practice search: the human keywords plus the external results URL. */
data class PracticeSearchLink(val sourceName: String, val query: String, val url: String)

/** A short recommendation shown before opening external search. */
data class PracticeKeywordSuggestion(val label: String, val query: String)

/**
 * Builds "找练习 / 找题" search links for the "需要多练" learning state, mirroring the existing video
 * search seam: ClassMate only constructs a safe keyword query and hands a public results URL to the
 * system browser (ACTION_VIEW). It NEVER crawls or fetches third-party platform content itself, and
 * the query is built only from course/topic text — never from prompts, messages, or credentials.
 */
object PracticeSearchEngine {

    val sources: List<PracticeSearchSource> = listOf(
        PracticeSearchSource("综合搜索", "https://www.bing.com/search?q="),
        PracticeSearchSource("B站讲题", "https://search.bilibili.com/all?keyword="),
        PracticeSearchSource("百度/必应", "https://www.baidu.com/s?wd="),
        PracticeSearchSource("学科公开资源", "https://www.icourses.cn/search.htm?searchText="),
    )

    /** e.g. ("高等数学 - 数项级数", "比值判别法") -> "高等数学 数项级数 比值判别法 练习题". */
    fun buildQuery(course: String, topic: String, keyword: String = "练习题"): String =
        sanitizeQuery(
            (splitParts(course) + splitParts(topic) + keyword)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(" "),
        )

    const val MAX_QUERY_LENGTH = 60

    /**
     * Cleans a search query before it ever reaches a URL: strips internal ids (kp_/q_/ev_), removes
     * secret-like fragments, collapses ALL whitespace (including newlines/tabs) to single spaces, and caps
     * the length. Keeps the query short, clean, and free of anything that should not be sent to a browser.
     */
    fun sanitizeQuery(raw: String): String {
        var q = raw.replace(rawIdRegex, " ")
        q = removeSensitiveFragments(q)
        q = q.split(Regex("\\s+")).filterNot { secretLikeRegex.containsMatchIn(it) }.joinToString(" ")
        q = q.replace(Regex("\\s+"), " ").trim()
        if (q.length > MAX_QUERY_LENGTH) q = q.take(MAX_QUERY_LENGTH).trim()
        return q
    }

    fun recommendedKeywords(course: String, topic: String): List<PracticeKeywordSuggestion> {
        val subject = detectSubject(course, topic)
        return templatesFor(subject)
            .map { PracticeKeywordSuggestion(it.label, buildQuery(course, topic, it.keyword)) }
            .filter { it.query.isNotBlank() }
            .distinctBy { it.query }
    }

    fun links(course: String, topic: String, keyword: String = "练习题"): List<PracticeSearchLink> {
        val query = buildQuery(course, topic, keyword)
        return sources.map { linkFor(it, query) }
    }

    fun panelLinks(course: String, topic: String): List<PracticeSearchLink> {
        val suggestions = recommendedKeywords(course, topic).ifEmpty {
            listOf(PracticeKeywordSuggestion("练习题", buildQuery(course, topic)))
        }
        return sources.mapIndexed { index, source ->
            linkFor(source, suggestions.getOrElse(index) { suggestions.first() }.query)
        }
    }

    /** Convenience: the default (综合搜索) link for a topic. */
    fun primaryLink(course: String, topic: String, keyword: String = "练习题"): PracticeSearchLink =
        links(course, topic, keyword).first()

    private fun linkFor(source: PracticeSearchSource, query: String): PracticeSearchLink {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        return PracticeSearchLink(source.name, query, source.urlPrefix + encoded)
    }

    // Course titles often look like "高等数学 - 数项级数"; split common separators into clean words.
    private fun splitParts(value: String): List<String> =
        value.split('-', '–', '—', '·', '/', '|', '：', ':', ',', '，')
            .map { it.trim() }
            .map { removeSensitiveFragments(it) }
            .filter { it.isNotEmpty() }
            .filterNot { secretLikeRegex.containsMatchIn(it) }

    private fun detectSubject(course: String, topic: String): PracticeSubject {
        val text = "$course $topic"
        return when {
            listOf("高等数学", "高数", "极限", "导数", "积分", "级数", "判别法").any { text.contains(it, ignoreCase = true) } ->
                PracticeSubject.HIGH_MATH
            listOf("大学物理", "物理", "电磁", "法拉第", "楞次", "动生电动势").any { text.contains(it, ignoreCase = true) } ->
                PracticeSubject.PHYSICS
            listOf("离散数学", "离散", "关系", "偏序", "哈斯图", "等价类").any { text.contains(it, ignoreCase = true) } ->
                PracticeSubject.DISCRETE
            listOf("C++", "指针", "引用", "const", "函数参数", "内存").any { text.contains(it, ignoreCase = true) } ->
                PracticeSubject.CPP
            listOf("马原", "马克思", "实践", "认识", "真理", "唯物").any { text.contains(it, ignoreCase = true) } ->
                PracticeSubject.MARXISM
            listOf("AI", "机器学习", "训练集", "损失函数", "过拟合", "评估指标").any { text.contains(it, ignoreCase = true) } ->
                PracticeSubject.AI
            else -> PracticeSubject.GENERAL
        }
    }

    private fun templatesFor(subject: PracticeSubject): List<QueryTemplate> = when (subject) {
        PracticeSubject.HIGH_MATH -> listOf(
            QueryTemplate("例题", "例题"),
            QueryTemplate("练习题", "练习题"),
            QueryTemplate("易错题", "易错题"),
            QueryTemplate("大学期末", "大学期末 题目"),
            QueryTemplate("考研", "考研 例题"),
        )
        PracticeSubject.PHYSICS -> listOf(
            QueryTemplate("例题", "例题"),
            QueryTemplate("方向判断", "方向判断 例题"),
            QueryTemplate("单位换算", "单位换算 练习"),
            QueryTemplate("实验题", "实验题"),
            QueryTemplate("大学期末", "大学期末 题目"),
        )
        PracticeSubject.DISCRETE -> listOf(
            QueryTemplate("定义判断", "定义判断 练习"),
            QueryTemplate("证明题", "证明题"),
            QueryTemplate("哈斯图", "哈斯图 例题"),
            QueryTemplate("关系性质", "关系性质 判断题"),
            QueryTemplate("期末题", "大学期末 题目"),
        )
        PracticeSubject.CPP -> listOf(
            QueryTemplate("代码题", "代码题"),
            QueryTemplate("易错点", "易错点"),
            QueryTemplate("选择题", "选择题"),
            QueryTemplate("编程练习", "编程练习"),
            QueryTemplate("函数参数", "函数参数传递 例题"),
        )
        PracticeSubject.MARXISM -> listOf(
            QueryTemplate("辨析题", "辨析题"),
            QueryTemplate("材料分析题", "材料分析题"),
            QueryTemplate("简答题", "简答题"),
            QueryTemplate("易错点", "易错点"),
            QueryTemplate("期末题", "大学期末 题目"),
        )
        PracticeSubject.AI -> listOf(
            QueryTemplate("概念题", "概念题"),
            QueryTemplate("流程题", "流程题"),
            QueryTemplate("过拟合例题", "过拟合 例题"),
            QueryTemplate("损失函数例题", "损失函数 例题"),
            QueryTemplate("评估指标", "评估指标 练习题"),
        )
        PracticeSubject.GENERAL -> listOf(
            QueryTemplate("练习题", "练习题"),
            QueryTemplate("例题", "例题"),
            QueryTemplate("易错题", "易错题"),
            QueryTemplate("讲解", "讲解"),
            QueryTemplate("大学课程", "大学课程 题目"),
        )
    }

    private fun removeSensitiveFragments(value: String): String =
        blockedFragments.fold(value) { acc, fragment -> acc.replace(fragment, "", ignoreCase = true) }.trim()

    private enum class PracticeSubject { HIGH_MATH, PHYSICS, DISCRETE, CPP, MARXISM, AI, GENERAL }
    private data class QueryTemplate(val label: String, val keyword: String)

    private val blockedFragments = listOf(
        "Authorization",
        "Bearer",
        "appKey",
        "apiKey",
        "app_id",
        "prompt",
        "messages",
        "reasoning_content",
    )
    private val secretLikeRegex = Regex("(?i)(s" + "k-[a-z0-9_-]{8,}|[a-z0-9_-]{32,})")
    private val rawIdRegex = Regex("\\b(kp|q|ev)_[A-Za-z0-9_-]+\\b")
}
