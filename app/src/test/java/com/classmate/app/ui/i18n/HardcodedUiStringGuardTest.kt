package com.classmate.app.ui.i18n

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for screens that have been MIGRATED to the [Strings] localization system: they must
 * read copy from `appStrings(...)` and must not re-introduce hardcoded Chinese product strings. The
 * migrated-screen list grows as more pages are localized; un-migrated screens are tracked in the round
 * report, not here (a whole-app hardcoded scan would fail until every page is migrated).
 */
class HardcodedUiStringGuardTest {

    private fun read(rel: String): String =
        listOf(File(rel), File("../$rel")).firstOrNull { it.exists() }?.readText(Charsets.UTF_8)
            ?: error("missing $rel")

    /** Screens fully migrated to appStrings(...). Each value lists Chinese product strings that must be gone. */
    private val migrated: Map<String, List<String>> = mapOf(
        "app/src/main/java/com/classmate/app/ui/screens/quiz/QuizScreen.kt" to listOf(
            "\"微测\"", "\"还没有微测题。\"", "\"上一题\"", "\"下一题\"", "\"去复习计划\"",
            "\"讲解\"", "\"考查知识点\"", "\"太难\"", "\"需要多练\"", "\"正确\"", "\"你的选择\"", "\"已选择\"",
        ),
        "app/src/main/java/com/classmate/app/ui/screens/evidence/EvidenceDetailScreen.kt" to listOf(
            "\"查看证据\"", "\"来源资料：\"", "\"原文片段\"", "\"该内容暂无可回溯的原文片段。\"",
            "\"AI 证据解释\"", "\"生成 AI 证据解释\"", "\"图片证据\"", "\"音频 / 转写证据\"",
            "\"证据资产缺失，但保留文本证据。\"", "\"相关知识点\"", "\"关联微测题\"",
            "\"证据不对\"", "\"已掌握\"", "\"来自课堂文本\"", "\"位置：\"",
        ),
        "app/src/main/java/com/classmate/app/ui/components/ExportCenterCard.kt" to listOf(
            "\"导出中心\"", "\"生成草稿\"", "\"重新生成草稿\"", "\"选择导出格式\"",
            "\"保存文件\"", "\"保存到下载目录\"", "\"分享\"", "\"内部备份\"",
            "\"导出未成功\"", "\"重试保存\"", "\"最近导出：\"",
        ),
        "app/src/main/java/com/classmate/app/ui/components/EnhancementCards.kt" to listOf(
            "\"正在调用蓝心 / 端侧模型\"", "\"AI 整理未完成\"", "\"可能原因：\"",
            "\"下一步：\"", "\"复制\"", "\"重新生成\"",
        ),
        "app/src/main/java/com/classmate/app/ui/screens/importcourse/ImportCourseScreen.kt" to listOf(
            "\"图片已保存为 evidence asset\"", "\"缩略图引用：\"", "\"端侧多模态理解中…\"",
            "\"确认加入课程\"", "\"重新选择图片\"", "\"可编辑学习文本（确认后作为课程文本）\"",
            "\"低质量 · 需核对\"", "\"识别失败：\"", "\"删除该图片\"", "\"正在识别…\"",
        ),
    )

    @Test
    fun migratedScreensUseAppStrings() {
        migrated.keys.forEach { path ->
            assertTrue("$path should read copy from appStrings(...)", read(path).contains("appStrings("))
        }
    }

    @Test
    fun migratedScreensHaveNoHardcodedChineseProductStrings() {
        migrated.forEach { (path, banned) ->
            val src = read(path)
            banned.forEach { literal ->
                assertFalse("$path still hardcodes product string: $literal", src.contains(literal))
            }
        }
    }
}
