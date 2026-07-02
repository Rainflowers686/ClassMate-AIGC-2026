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
            "AI 模型配置",
            "蓝心大模型",
            "其他模型",
            "AppID",
            "AppKey",
            "API Key",
            "高级 JSON 配置",
            "保存配置",
            "删除配置",
            "恢复默认 AppID",
            "测试配置（readiness / dry-run）",
            "配置仅保存在本机",
            "2026374747",
            "云端蓝心",
            "端侧蓝心",
            "端侧 BlueLM 3B",
            "安全占位",
        ).forEach { assertTrue("missing main-config label: $it", s.contains(it)) }
    }

    @Test
    fun settingsIaV2SeparatesGeneralAndDeveloperSettings() {
        val s = source()
        listOf(
            "通用设置",
            "开发者设置",
            "外观与主题",
            "AI 模型配置",
            "隐私与权限",
            "导出设置",
            "沉浸式背景音",
            "普通用户填写 AI Key 的主入口在通用设置",
        ).forEach { assertTrue("missing Settings IA v2 copy: $it", s.contains(it)) }
    }

    @Test
    fun aiModelConfigurationPageContainsPersistentActions() {
        val s = source()
        listOf(
            "saveOfficialModelConfig",
            "deleteOfficialModelConfig",
            "saveCustomModelConfig",
            "deleteCustomModelConfig",
            "selectAiModelProviderMode",
            "JSON 格式不正确",
        ).forEach { assertTrue("missing persistent config hook: $it", s.contains(it)) }
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
        assertTrue(s.contains("图片测试"))
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
    @Test
    fun blueLmQualityModesAndOfficialProviderConfigStatusAreVisibleWithoutRawModelCopy() {
        val s = source()
        listOf(
            "蓝心大模型",
            "快速 / 均衡 / 专业",
            "专业模式开启深度思考",
            "Official OCR config",
            "Query Rewrite config",
            "Text Similarity config",
            "Embedding config",
            "TTS config",
        ).forEach { assertTrue("missing current AI status copy: $it", s.contains(it)) }
        listOf("qwen3.5-plus / DEEP_STUDY", "enable_thinking=true if supported").forEach {
            assertFalse("raw model/protocol copy should not be visible in Settings: $it", s.contains(it))
        }
    }
}
