package com.classmate.app.ui

import com.classmate.app.ui.theme.ThemeOption
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 9A guard: Focus is the default multi-level UI, debug detail is collapsed, the honest
 * 云端蓝心 / 端侧蓝心 / 安全占位 vocabulary holds, and no forbidden over-claim copy appears.
 */
class Stage9aFocusUiGuardTest {

    private fun firstExisting(vararg c: String): File =
        c.map { File(it) }.firstOrNull { it.exists() } ?: File(c.first())

    private fun read(rel: String): String = firstExisting("src/main/$rel", "app/src/main/$rel").readText()

    private fun ktFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun mainSources(): List<File> = buildList {
        addAll(ktFiles(firstExisting("src/main", "app/src/main")))
        addAll(ktFiles(firstExisting("../core/src/main", "core/src/main")))
    }

    // ---- theme positioning ----------------------------------------------------------------------

    @Test
    fun focusIsTheDefaultThemeAndFlowVitalityAreNot() {
        assertEquals(ThemeOption.FOCUS, ThemeOption.Default)
        assertEquals(3, ThemeOption.entries.size)
        // Focus reads as the default baseline; Flow/Vitality describe their scoped roles honestly.
        assertTrue(ThemeOption.FOCUS.tagline.contains("默认"))
        assertTrue(ThemeOption.FLOW.tagline.contains("陪学") || ThemeOption.FLOW.description.contains("局部"))
        assertTrue(ThemeOption.VITALITY.tagline.contains("预留") || ThemeOption.VITALITY.description.contains("不作为默认"))
    }

    @Test
    fun settingsExplainsThreeThemesButKeepsFocusDefault() {
        val s = read("java/com/classmate/app/ui/screens/settings/SettingsScreen.kt")
        assertTrue(s.contains("默认 Focus") || s.contains("默认 专注"))
        assertTrue(s.contains("Flow"))
        assertTrue(s.contains("Vitality"))
        assertTrue(s.contains("ThemePreviewCard"))
    }

    // ---- multi-level flow + Focus pages ---------------------------------------------------------

    @Test
    fun homeExposesImageAndPhotoInputReviewAndOnDeviceStatus() {
        val s = read("java/com/classmate/app/ui/screens/home/HomeScreen.kt")
        assertTrue(s.contains("图片学习输入"))
        assertTrue(s.contains("拍照学习输入"))
        assertTrue(s.contains("粘贴课堂文本"))
        assertTrue(s.contains("复习计划"))
        assertTrue(s.contains("端侧蓝心"))
        assertTrue(s.contains("云端蓝心 → 端侧蓝心 → 安全占位"))
    }

    @Test
    fun bottomNavHasFiveTabsIncludingMaterials() {
        val tab = read("java/com/classmate/app/state/Screen.kt")
        assertTrue(tab.contains("IMPORT(Screen.IMPORT"))
        // HOME / IMPORT / REVIEW / HISTORY / SETTINGS = 5 roots.
        listOf("HOME(", "IMPORT(", "REVIEW(", "HISTORY(", "SETTINGS(").forEach { assertTrue(tab.contains(it)) }
    }

    @Test
    fun analyzeFailureShowsStructuredSourceBreakdownNotRawLogWall() {
        val s = read("java/com/classmate/app/ui/screens/analyze/AnalyzeProgressScreen.kt")
        assertTrue(s.contains("ErrorBreakdownCard"))
        assertTrue(s.contains("云端蓝心"))
        assertTrue(s.contains("端侧蓝心"))
        assertTrue(s.contains("最终结果"))
        // Raw safe lines must be COLLAPSED into the diagnostic details card, not printed inline.
        assertTrue(s.contains("DiagnosticDetailsCard"))
        assertFalse(s.contains("diag.safeLines().forEach"))
    }

    @Test
    fun courseDetailHasSourceBadgeAndLearningEntries() {
        val s = read("java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt")
        assertTrue(s.contains("SourceBadge"))
        assertTrue(s.contains("来源"))
        assertTrue(s.contains("问这节课")) // Ask
        assertTrue(s.contains("专项练习")) // Practice
        assertTrue(s.contains("复习计划")) // Review
        assertTrue(s.contains("导出")) // Export
    }

    @Test
    fun settingsIsACapabilityCenterWithCollapsedDiagnostics() {
        val s = read("java/com/classmate/app/ui/screens/settings/SettingsScreen.kt")
        assertTrue(s.contains("官方推荐路径"))
        assertTrue(s.contains("检测候选模型目录"))
        assertTrue(s.contains("选择真实图片测试（不落库）"))
        // Debug safe lines are collapsed via the details card; no inline monospace log loops remain.
        assertTrue(s.contains("DiagnosticDetailsCard"))
        assertFalse(s.contains("report.safeLines().forEach"))
    }

    @Test
    fun diagnosticDetailsCardDefaultsToCollapsedDevTitle() {
        val s = read("java/com/classmate/app/ui/components/FocusComponents.kt")
        assertTrue(s.contains("开发诊断详情"))
        // Collapsed by default.
        assertTrue(s.contains("mutableStateOf(false)"))
    }

    // ---- honest vocabulary across all main source -----------------------------------------------

    @Test
    fun noForbiddenOrOverClaimCopyInMainSource() {
        val forbidden = listOf(
            Regex("LocalRule 可用"),
            Regex("本地规则兜底"),
            Regex("规则智能"),
            Regex("本地规则分析"),
            Regex("""LocalRule.*智能"""),
            Regex("""LocalRule.*兜底"""),
            Regex("多模态替代 ?OCR"),
            Regex("自动 ?OCR 完成"),
        )
        val offenders = mainSources().filter { f -> forbidden.any { it.containsMatchIn(f.readText()) } }
        assertTrue("Forbidden/over-claim copy remains in: $offenders", offenders.isEmpty())
    }
}
