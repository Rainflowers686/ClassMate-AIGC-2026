package com.classmate.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0-8: every AI enhancement surface must label its source honestly (蓝心整理版 / 端侧模型草稿 / 本地整理版 /
 * 失败保留基础版) and must never leak provider/debug tokens to the user. This guards the source mapping and
 * the screens that render enhancement results.
 */
class EnhancementSourceHonestyTest {

    private fun read(rel: String): String =
        listOf(File(rel), File("../$rel")).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)
            ?: error("missing $rel")

    private val viewModel by lazy { read("app/src/main/java/com/classmate/app/state/AppViewModel.kt") }

    @Test
    fun sourceLabelsAreHonestChinese() {
        // The mapping turns the unified AiExecutionSource into a user-facing Chinese label.
        listOf("蓝心整理版", "端侧模型草稿", "本地整理版").forEach {
            assertTrue("enhancement source label missing: $it", viewModel.contains("\"$it\""))
        }
        // A blank/failed enhancement is honest about falling back to the base version.
        assertTrue(viewModel.contains("AI 整理失败，已保留基础版"))
    }

    @Test
    fun localTemplateIsNeverLabelledAsCloud() {
        // The CLOUD branch maps to 蓝心整理版; the local/placeholder branch must NOT map to a 蓝心 label.
        val cloudLine = "AiExecutionSource.CLOUD -> \"蓝心整理版\""
        assertTrue("cloud must map to 蓝心整理版", viewModel.contains(cloudLine))
        // The else/local branch maps to 本地整理版 (not 蓝心).
        assertTrue(viewModel.contains("else -> \"本地整理版\""))
    }

    @Test
    fun enhancementSurfacesDoNotLeakDebugTokens() {
        val surfaces = listOf(
            "app/src/main/java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt",
            "app/src/main/java/com/classmate/app/ui/screens/practice/PracticeSessionScreen.kt",
            "app/src/main/java/com/classmate/app/ui/screens/evidence/EvidenceDetailScreen.kt",
            "app/src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt",
            "app/src/main/java/com/classmate/app/ui/components/EnhancementCards.kt",
        )
        // Forbidden user-visible tokens as STRING LITERALS (field accesses like `evidence.assetId` are fine;
        // only quoted, user-rendered text would be a leak). Broad debug scanning lives in UserPageDebugLeakGuard.
        val forbidden = listOf("\"LOCAL_FALLBACK\"", "\"provider trace\"", "\"assetId\"", "\"raw id\"")
        surfaces.forEach { path ->
            val src = read(path)
            forbidden.forEach { token ->
                assertFalse("$path leaks user-visible token: $token", src.contains(token))
            }
        }
    }
}
