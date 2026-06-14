package com.classmate.core.practice

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSearchTest {

    @Test
    fun buildQueryProducesReadableKeywords() {
        val q = PracticeSearchEngine.buildQuery("高等数学 - 数项级数", "比值判别法")
        assertTrue(q.contains("高等数学"))
        assertTrue(q.contains("数项级数"))
        assertTrue(q.contains("比值判别法"))
        assertTrue(q.endsWith("练习题"))
    }

    @Test
    fun linksAreHttpsResultPagesWithEncodedQuery() {
        val links = PracticeSearchEngine.links("大学物理", "楞次定律")
        assertTrue(links.isNotEmpty())
        links.forEach { link ->
            assertTrue(link.url.startsWith("https://"))
            assertTrue(link.url.contains("%")) // query is URL-encoded
            assertTrue(link.sourceName.isNotBlank())
        }
        assertTrue(links.any { it.sourceName.contains("B站") })
    }

    @Test
    fun subjectSpecificKeywordsCoverCommonPracticeNeeds() {
        val math = PracticeSearchEngine.recommendedKeywords("高等数学", "比值判别法").joinToString(" ") { it.query }
        assertTrue(math.contains("例题"))
        assertTrue(math.contains("易错题"))
        assertTrue(math.contains("考研") || math.contains("大学期末"))

        val physics = PracticeSearchEngine.recommendedKeywords("大学物理", "楞次定律").joinToString(" ") { it.query }
        assertTrue(physics.contains("方向判断"))
        assertTrue(physics.contains("实验题") || physics.contains("单位换算"))

        val discrete = PracticeSearchEngine.recommendedKeywords("离散数学", "偏序关系").joinToString(" ") { it.query }
        assertTrue(discrete.contains("定义判断"))
        assertTrue(discrete.contains("证明题") || discrete.contains("哈斯图"))

        val cpp = PracticeSearchEngine.recommendedKeywords("C++", "指针与引用").joinToString(" ") { it.query }
        assertTrue(cpp.contains("代码题"))
        assertTrue(cpp.contains("编程练习") || cpp.contains("选择题"))

        val marxism = PracticeSearchEngine.recommendedKeywords("马原", "实践与认识").joinToString(" ") { it.query }
        assertTrue(marxism.contains("辨析题"))
        assertTrue(marxism.contains("材料分析题"))

        val ai = PracticeSearchEngine.recommendedKeywords("AI/机器学习", "过拟合").joinToString(" ") { it.query }
        assertTrue(ai.contains("概念题"))
        assertTrue(ai.contains("过拟合"))
    }

    @Test
    fun panelLinksUseExternalSearchDestinationsOnly() {
        val links = PracticeSearchEngine.panelLinks("离散数学", "哈斯图")
        assertTrue(links.map { it.sourceName }.containsAll(listOf("综合搜索", "B站讲题", "百度/必应", "学科公开资源")))
        links.forEach { link ->
            assertTrue(link.url.startsWith("https://"))
            assertFalse(link.url.contains("localhost", ignoreCase = true))
            assertFalse(link.url.contains("127.0.0.1"))
        }
    }

    @Test
    fun queryAndLinksContainNoSecretsOrPromptTokens() {
        val links = PracticeSearchEngine.links("C++ Authorization appKey", "指针 prompt messages apiKey")
        val combined = links.joinToString(" ") { it.query + " " + it.url }
        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content", "prompt", "messages").forEach {
            assertFalse(combined.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun practiceSearchEngineDoesNotContainHttpCrawlerCode() {
        val sourceFile = listOf(
            File("src/main/kotlin/com/classmate/core/practice/PracticeSearch.kt"),
            File("core/src/main/kotlin/com/classmate/core/practice/PracticeSearch.kt"),
        ).first { it.exists() }
        val source = sourceFile.readText()
        listOf("HttpURLConnection", "OkHttp", "java.net.http", ".execute()", ".newCall(").forEach {
            assertFalse(source.contains(it))
        }
    }
}
