package com.classmate.app.exporting

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportCopyTest {
    @Test
    fun exportFormatsUseProductReadyLabels() {
        val labels = ExportFileFormat.entries.joinToString("\n") { "${it.displayName} ${it.description}" }
        listOf(
            "PDF 可打印精华版",
            "DOCX 可编辑学习文档",
            "浏览器可打开",
            "Markdown",
            "纯文本",
            "课程精华音频脚本",
        ).forEach { assertTrue("missing export label: $it", labels.contains(it)) }

        assertFalse(labels.contains("声音" + "复刻"))
        assertFalse(labels.contains("老师" + "声音"))
    }

    @Test
    fun exportCenterExplainsFormatFallbacksWithoutOverclaimingAudio() {
        val source = readFirst(
            "src/main/java/com/classmate/app/ui/components/ExportCenterCard.kt",
            "app/src/main/java/com/classmate/app/ui/components/ExportCenterCard.kt",
        )
        val strings = readFirst(
            "src/main/java/com/classmate/app/ui/i18n/Strings.kt",
            "app/src/main/java/com/classmate/app/ui/i18n/Strings.kt",
        )
        // The card keeps the draft / fallback flow copy; per-format guidance moved into the export help pack.
        listOf("生成学习报告草稿", "选择导出格式", "HTML 或 Text 兜底").forEach {
            assertTrue("missing export center copy: $it", source.contains(it) || strings.contains(it))
        }
        assertFalse(source.contains("声音" + "复刻"))
        assertFalse(source.contains("老师" + "声音克隆"))
        listOf(
            "DOCX 是真实 Word 文档",
            "PDF 适合打印",
            "HTML 适合浏览器学习",
            "TTS 未配置时先导出文本",
        ).forEach { assertTrue("missing export help copy: $it", strings.contains(it)) }
    }

    private fun readFirst(vararg paths: String): String =
        paths.map { File(it) }.first { it.exists() }.readText()
}
