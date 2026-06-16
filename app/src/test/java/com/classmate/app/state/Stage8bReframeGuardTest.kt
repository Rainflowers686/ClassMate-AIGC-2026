package com.classmate.app.state

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8B guard: the deterministic rule path is only a SAFETY PLACEHOLDER and the on-device BlueLM 3B
 * is the local intelligence. User-visible main source must not advertise "LocalRule" / "本地规则兜底"
 * as a capability, and the provider path must read 云端蓝心 → 端侧蓝心 → 安全占位.
 */
class Stage8bReframeGuardTest {

    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    private fun ktFiles(root: File): List<File> =
        if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun mainSources(): List<File> = buildList {
        addAll(ktFiles(firstExisting("src/main", "app/src/main")))
        addAll(ktFiles(firstExisting("../core/src/main", "core/src/main")))
    }

    private fun settingsSource(): String =
        firstExisting(
            "src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt",
            "app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt",
        ).readText()

    @Test
    fun mainSourceNeverAdvertisesRuleFallbackAsACapability() {
        val forbidden = listOf(
            Regex("LocalRule 可用"),
            Regex("本地规则兜底"),
            Regex("规则智能"),
            Regex("LocalRule.*智能"),
            Regex("LocalRule.*兜底"),
        )
        val offenders = mainSources().filter { f ->
            val t = f.readText()
            forbidden.any { it.containsMatchIn(t) }
        }
        assertTrue("Files still advertise the rule path as a capability: $offenders", offenders.isEmpty())
    }

    @Test
    fun providerPathUsesCloudOnDeviceSafetyPlaceholderLabels() {
        val s = settingsSource()
        assertTrue(s.contains("云端蓝心"))
        assertTrue(s.contains("端侧蓝心"))
        assertTrue(s.contains("安全占位"))
    }

    @Test
    fun settingsTreatsOnDeviceAsOnDeviceModelPathAndRuleAsSafetyPlaceholderOnly() {
        val s = settingsSource()
        // On-device is the explicit fallback model path.
        assertTrue(s.contains("端侧 BlueLM 3B 是云端不可用时的端侧模型路径"))
        // The rule path is described only as a safety placeholder, never an intelligent capability.
        assertFalse(s.contains("本地规则兜底"))
        assertFalse(s.contains("可用（LocalRule）"))
    }

    @Test
    fun settingsDoesNotPresentDeepSeekOrCompatibleAsCompetitionMainPath() {
        val s = settingsSource()
        listOf("DeepSeek", "Compatible Demo", "Compatible demo").forEach {
            assertFalse("forbidden competition main-path term: $it", s.contains(it, ignoreCase = true))
        }
    }
}
