package com.classmate.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCopyConsistencyTest {

    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    private fun read(path: String): String =
        firstExisting(path, "app/$path").readText()

    private fun ktFiles(path: String): List<File> {
        val root = firstExisting(path, "app/$path")
        return if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun userVisibleCopyDoesNotContainStaleProviderOrOverclaimPhrases() {
        val roots = listOf(
            "src/main/java/com/classmate/app/ui",
            "src/main/java/com/classmate/app/state",
            "src/main/java/com/classmate/app/exporting",
            "src/main/java/com/classmate/app/platform",
        )
        val forbidden = listOf(
            Regex("doubao", RegexOption.IGNORE_CASE),
            Regex("豆包"),
            Regex("Compatible Demo", RegexOption.IGNORE_CASE),
            Regex("演示流程"),
            Regex("当前未接入真实 OCR"),
            Regex("占位 seam"),
            Regex("音频占位"),
            Regex("视频占位"),
            Regex("网络链接占位"),
            Regex("本地智能兜底"),
            Regex("LocalRule 智能"),
            Regex("本地规则兜底"),
            Regex("多模态替代 ?OCR"),
            Regex("自动 ?OCR 完成"),
            Regex("已完成实时 ?ASR"),
            Regex("自动听课"),
            Regex("替代听脑"),
            Regex("声音" + "复刻"),
            Regex("老师" + "声音克隆"),
            Regex("同学" + "声音"),
            Regex("\\bLBS\\b"),
            Regex("\\bPOI\\b"),
            Regex("AI 生成白噪音"),
            Regex("实时生成背景音"),
        )

        val offenders = roots.flatMap(::ktFiles).mapNotNull { file ->
            val text = file.readText()
            val hit = forbidden.firstOrNull { it.containsMatchIn(text) }
            if (hit == null) null else "${file.path}: ${hit.pattern}"
        }
        assertTrue("stale or overclaim copy remains: $offenders", offenders.isEmpty())
    }

    @Test
    fun settingsModelCopyUsesCurrentSourceVocabulary() {
        val settings = read("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt")
        listOf(
            "当前模型：云端蓝心",
            "qwen3.5-plus",
            "端侧 BlueLM 3B",
            "官方 OCR",
            "ASR",
            "云端蓝心 → 端侧蓝心 → 安全占位",
            "未检测到本地 SDK 文件",
        ).forEach { assertTrue("missing settings copy: $it", settings.contains(it)) }

        assertFalse(settings.contains("doubao", ignoreCase = true))
        assertFalse(settings.contains("豆包"))
        assertFalse(settings.contains("本地智能兜底"))
        assertFalse(settings.contains("Compatible Demo", ignoreCase = true))
    }

    @Test
    fun featureSurfaceTextKeepsP0ToP2EntrancesVisible() {
        val import = read("src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt")
        val transcript = read("src/main/java/com/classmate/app/ui/screens/transcript/TranscriptImportScreen.kt")
        val course = read("src/main/java/com/classmate/app/ui/screens/course/CourseDetailScreen.kt")
        val knowledge = read("src/main/java/com/classmate/app/ui/screens/knowledge/KnowledgeTimelineScreen.kt")
        val practice = read("src/main/java/com/classmate/app/ui/screens/practice/PracticeSessionScreen.kt")
        val review = read("src/main/java/com/classmate/app/ui/screens/review/ReviewPlanScreen.kt")
        val export = read("src/main/java/com/classmate/app/ui/components/ExportCenterCard.kt")
        val settings = read("src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt")

        listOf("文本粘贴", "图片学习输入", "拍照学习输入", "官方 OCR 按配置启用").forEach {
            assertTrue("missing import entry: $it", import.contains(it))
        }
        listOf("粘贴 SRT / VTT / TXT", "TranscriptDraft").forEach {
            assertTrue("missing transcript entry: $it", transcript.contains(it))
        }
        // The ASR-config status note moved into the transcript help pack (i18n) — still present, just relocated.
        val strings = read("src/main/java/com/classmate/app/ui/i18n/Strings.kt")
        assertTrue("ASR config note present on screen or in help", transcript.contains("官方 ASR 按配置启用") || strings.contains("官方 ASR 按配置启用"))
        listOf("知识时间线", "问这节课", "专项练习", "复习计划", "导出中心").forEach {
            assertTrue("missing course entry: $it", course.contains(it) || export.contains(it))
        }
        listOf("建议追问", "加入复习").forEach {
            assertTrue("missing Ask entry: $it", knowledge.contains(it))
        }
        listOf("答案 / 解释", "证据", "下一步建议").forEach {
            assertTrue("missing practice feedback text: $it", practice.contains(it))
        }
        listOf("今日待复习", "优先级").forEach {
            assertTrue("missing review text: $it", review.contains(it))
        }
        listOf("PDF", "DOCX", "HTML", "Markdown", "Text", "课程精华音频脚本").forEach {
            assertTrue("missing export format text: $it", export.contains(it) || settings.contains(it))
        }
        listOf("TTS", "Function", "smoke", "docId", "Product-facing").forEach {
            assertTrue("missing official provider readiness copy: $it", settings.contains(it))
        }
        listOf("通用设置", "外观与主题", "AI 模型配置", "导出设置", "开发者设置").forEach {
            assertTrue("missing settings entrance: $it", settings.contains(it))
        }
        listOf("沉浸背景音", "内置", "授权循环背景音").forEach {
            assertTrue("missing ambient audio copy: $it", settings.contains(it) || read("src/main/java/com/classmate/app/ui/screens/home/HomeScreen.kt").contains(it))
        }
    }
}
