package com.classmate.app.state

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8D Phase 8: no user-visible source mislabels remain, and the analyze-failure UI surfaces the
 * structured cloud/on-device/final-source breakdown (not just BLUELM:CONFIG_MISSING). Settings shows
 * the on-device model as a cross-app local AI layer with an offline-mode check.
 */
class Stage8dDiagnosticsGuardTest {

    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    private fun ktFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun mainSources(): List<File> = buildList {
        addAll(ktFiles(firstExisting("src/main", "app/src/main")))
        addAll(ktFiles(firstExisting("../core/src/main", "core/src/main")))
    }

    private fun read(vararg candidates: String): String = firstExisting(*candidates).readText()

    @Test
    fun noUserVisibleRuleOrLocalFallbackMislabels() {
        val forbidden = listOf(
            Regex("LocalRule 可用"),
            Regex("本地规则兜底"),
            Regex("规则智能"),
            Regex("本地规则分析"),
            Regex("""LocalRule.*兜底"""),
            Regex("""LocalRule.*智能"""),
            // The Pill/label that used to show the raw "Local fallback" string in a quoted UI literal.
            Regex(""""Local fallback""""),
        )
        val offenders = mainSources().filter { f -> forbidden.any { it.containsMatchIn(f.readText()) } }
        assertTrue("User-visible mislabels remain in: $offenders", offenders.isEmpty())
    }

    @Test
    fun analyzeScreenSurfacesStructuredSourceDiagnostics() {
        // Failure message is built from cloud / 端侧蓝心 / 端侧结果 / 最终结果.
        val vm = read(
            "src/main/java/com/classmate/app/state/AppViewModel.kt",
            "app/src/main/java/com/classmate/app/state/AppViewModel.kt",
        )
        assertTrue(vm.contains("云端蓝心："))
        assertTrue(vm.contains("端侧蓝心："))
        assertTrue(vm.contains("端侧结果："))
    }

    @Test
    fun settingsShowsOnDeviceAsCrossAppLayerWithOfflineCheck() {
        val s = read(
            "src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt",
            "app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt",
        )
        assertTrue(s.contains("端侧本地智能层"))
        assertTrue(s.contains("端侧独立模式检查"))
        assertTrue(s.contains("端侧课程分析"))
        assertTrue(s.contains("云端蓝心 → 端侧蓝心 → 安全占位"))
    }
}
