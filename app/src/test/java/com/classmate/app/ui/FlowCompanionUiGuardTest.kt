package com.classmate.app.ui

import com.classmate.app.ui.theme.ThemeOption
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FlowCompanion guard: Flow is implemented as ONE immersive companion page (reverse-engineered from
 * docs/design_refs/classmate_flow.html), NOT as a global theme reskin of the task pages. Proves:
 *   1. the Flow design source exists and the companion layer documents it;
 *   2. the companion layer (FlowCompanionUi.kt) is a real immersive layout — light-field backdrop +
 *      breathing ring (radialGradient + drawArc + rememberInfiniteTransition), not a dark ProductUi;
 *   3. the companion screen renders breathing timer + sound scene + knowledge cache + session footer
 *      with honest audio/ASR wording;
 *   4. Flow is NOT applied to Home / Import / Course / History / Settings (no companion backdrop, no
 *      global theme-default change) — only a restrained entry navigates to it;
 *   5. honesty holds: no forbidden over-claim copy, no WebView, no direct vivo SDK import, qwen guard.
 */
class FlowCompanionUiGuardTest {

    private fun firstExisting(vararg c: String): File =
        c.map { File(it) }.firstOrNull { it.exists() } ?: File(c.first())

    private fun read(rel: String): String = firstExisting("src/main/$rel", "app/src/main/$rel").readText()

    private fun ktFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun mainSources(): List<File> = buildList {
        addAll(ktFiles(firstExisting("src/main", "app/src/main")))
        addAll(ktFiles(firstExisting("../core/src/main", "core/src/main")))
    }

    private fun companionUi(): String = read("java/com/classmate/app/ui/flow/FlowCompanionUi.kt")
    private fun companionScreen(): String = read("java/com/classmate/app/ui/screens/live/LiveCompanionScreen.kt")
    private fun screen(name: String): String = read("java/com/classmate/app/ui/screens/$name")

    // ---- 1. design source referenced --------------------------------------------------------------

    @Test
    fun flowDesignSourceExistsAndIsReferenced() {
        val ref = firstExisting("docs/design_refs/classmate_flow.html", "../docs/design_refs/classmate_flow.html")
        assertTrue("classmate_flow.html design source must exist", ref.exists())
        assertTrue("FlowCompanionUi must document its classmate_flow.html origin", companionUi().contains("classmate_flow.html"))
    }

    // ---- 2. the companion layer is a real immersive layout (not a dark ProductUi) ------------------

    @Test
    fun companionLayerExistsWithRealDepthAndMotion() {
        val s = companionUi()
        listOf(
            "fun FlowCompanionBackdrop", "fun FlowBreathingTimer", "fun FlowControlCluster",
            "fun FlowSoundSceneCard", "fun FlowKnowledgeCacheCard", "fun FlowSessionFooter",
            "fun FlowScenePicker", "fun FlowSessionTopBar", "fun FlowMiniPlayer",
        ).forEach { assertTrue("FlowCompanionUi missing: $it", s.contains(it)) }
        // Real light-field backdrop + breathing ring, not a flat dark fill.
        assertTrue(s.contains("radialGradient"))
        assertTrue(s.contains("rememberInfiniteTransition"))
        assertTrue(s.contains("drawArc"))
        assertTrue(s.contains("drawBehind"))
        // Scene-anchored visuals (multiple ambient scenes), not one global tint.
        assertTrue(s.contains("flowCompScenes"))
        // It is NOT the Stage 10 ProductUi reskinned.
        assertTrue("companion must not be built on ProductCanvas", !s.contains("ProductCanvas"))
    }

    // ---- 3. the companion screen has the required regions + honest wording -------------------------

    @Test
    fun companionScreenHasTimerSceneCacheFooterAndHonestWording() {
        val s = companionScreen()
        assertTrue(s.contains("FlowCompanionBackdrop"))
        assertTrue(s.contains("FlowBreathingTimer"))
        assertTrue(s.contains("FlowSoundSceneCard"))
        assertTrue(s.contains("FlowKnowledgeCacheCard"))
        assertTrue(s.contains("FlowSessionFooter"))
        assertTrue(s.contains("FlowScenePicker"))
        // Honest companion / audio / transcript wording.
        val honest = companionUi() + s
        assertTrue(honest.contains("陪学模拟演示"))
        assertTrue(honest.contains("声音场景预览"))
        assertTrue(honest.contains("不录音"))
        assertTrue(honest.contains("转写能力预留"))
    }

    // ---- 4. Flow is NOT a global theme / not applied to the task pages -----------------------------

    @Test
    fun flowIsNotAppliedGloballyToTaskPages() {
        // None of the task pages may adopt the companion backdrop (no dark reskin / no Flow layout).
        listOf(
            "home/HomeScreen.kt", "importcourse/ImportCourseScreen.kt", "course/CourseDetailScreen.kt",
            "history/HistoryScreen.kt", "settings/SettingsScreen.kt",
        ).forEach { rel ->
            val s = screen(rel)
            assertTrue("$rel must not adopt FlowCompanionBackdrop", !s.contains("FlowCompanionBackdrop"))
            assertTrue("$rel must not adopt FlowBreathingTimer", !s.contains("FlowBreathingTimer"))
            // The task pages keep their Stage 10 product shell.
        }
        // Focus stays the global default; Flow is not promoted.
        assertEquals(ThemeOption.FOCUS, ThemeOption.Default)
    }

    @Test
    fun restrainedEntryNavigatesToCompanion() {
        // A small entry exists (Home + Course) that routes to the companion (Screen.LIVE).
        assertTrue(screen("home/HomeScreen.kt").contains("Screen.LIVE"))
        assertTrue(screen("course/CourseDetailScreen.kt").contains("Screen.LIVE"))
        assertTrue(companionUi().contains("心流") || companionScreen().contains("心流学习") || screen("home/HomeScreen.kt").contains("心流学习"))
    }

    // ---- 5. honesty + safety ----------------------------------------------------------------------

    @Test
    fun noForbiddenCopyNoWebViewNoVivoImport() {
        val forbidden = listOf(
            Regex("LocalRule 可用"), Regex("本地规则兜底"), Regex("LocalRule 智能"), Regex("LocalRule 兜底"),
            Regex("本地规则分析"), Regex("端侧结果 LOCAL_FALLBACK"), Regex("多模态替代 ?OCR"),
            Regex("自动 ?OCR 完成"), Regex("DeepSeek 复赛主路径"), Regex("Compatible 复赛主路径"),
            Regex("已完成实时 ?ASR"), Regex("自动听课"), Regex("替代听脑"),
            Regex("已接入白噪音真实播放"), Regex("已接入真实混音"),
        )
        val sources = mainSources()
        val offenders = sources.filter { f -> forbidden.any { it.containsMatchIn(f.readText()) } }
        assertTrue("Forbidden copy remains in: $offenders", offenders.isEmpty())
        assertTrue("WebView must not be used", sources.none { it.readText().contains("WebView") })
        assertTrue("Direct vivo SDK import must not appear", sources.none { it.readText().contains("import com.vivo.llmsdk") })
    }

    @Test
    fun qwenThinkingGuardHolds() {
        assertTrue(mainSources().any { it.readText().contains("enable_thinking") })
    }
}
