package com.classmate.app.ui.screens.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the competition main config page (Settings) wording: 云端蓝心 first, on-device +
 * safety placeholder fallback shown honestly, and NO Compatible/DeepSeek/external-model enhancement as
 * main UI copy. The neutral custom-API affordance stays debug-only.
 */
class SettingsModelConfigTextTest {

    private fun source(): String =
        listOf(
            File("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
            File("app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt"),
        ).first { it.exists() }.readText()

    @Test
    fun mainConfigPageUsesOfficialFirstLabels() {
        val s = source()
        listOf(
            "模型 API 管理",
            "当前模型：云端蓝心",
            "qwen3.5-plus",
            "云端蓝心",
            "端侧蓝心",
            "端侧 BlueLM 3B",
            "安全占位",
            "端侧模型诊断",
            "保存配置",
            "测试连接",
            "删除配置",
            "Provider path",
        ).forEach { assertTrue("missing main-config label: $it", s.contains(it)) }
    }

    @Test
    fun mainConfigPageDoesNotShowCompatibleOrDeepSeekEnhancement() {
        val s = source()
        listOf("Compatible Demo", "Compatible demo", "DeepSeek", "外部模型增强", "兼容增强").forEach {
            assertFalse("forbidden competition main-UI copy present: $it", s.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun providerPathExposesHonestShortLabels() {
        val s = source()
        // Short, honest path nodes used by the StudyReport / Settings provider path.
        assertTrue(s.contains("BlueLM"))
        assertTrue(s.contains("LocalRule") || s.contains("localProviderPath"))
        // The local rule fallback is never dressed up as on-device AI.
        assertFalse(s.contains("本地规则兜底 AI"))
    }

    @Test
    fun stage8eModelPathDetectionAndRealImageDiagnosticArePresent() {
        val s = source()
        // P0-3: official path recommendation + bounded candidate detection + one-tap switch.
        assertTrue(s.contains("官方推荐路径"))
        assertTrue(s.contains("检测候选模型目录"))
        assertTrue(s.contains("建议切换后重试"))
        assertTrue(s.contains("切换到推荐路径"))
        assertTrue(s.contains("不扫描全盘"))
        // P0-4: real-image multimodal diagnostic entry (diagnostic only, never persisted).
        assertTrue(s.contains("选择真实图片测试（不落库）"))
        // Forbidden over-claims stay out.
        assertFalse(s.contains("自动 OCR 完成"))
        assertFalse(s.contains("多模态替代 OCR"))
    }

    @Test
    fun p2LearningAndRetrievalSettingsAreVisibleWithoutKeys() {
        val s = source()
        listOf(
            "检索增强",
            "查询改写",
            "文本相似度",
            "文本向量",
            "默认题目难度",
            "easy / medium / hard",
            "Word / DOCX",
            "bilingual notes",
            "source metadata",
        ).forEach { assertTrue("missing P2 settings copy: $it", s.contains(it)) }
        assertFalse(s.contains("完整密钥"))
    }
}
