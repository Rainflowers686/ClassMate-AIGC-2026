package com.classmate.app.exporting

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportDocxFlowTextTest {
    @Test
    fun exportFormatsIncludeNativeDocx() {
        assertTrue(ExportFileFormat.entries.any { it == ExportFileFormat.DOCX })
        assertTrue(ExportFileFormat.DOCX.displayName.contains("Word"))
        assertTrue(ExportFileFormat.DOCX.extension == "docx")
        assertTrue(ExportFileFormat.DOCX.description.contains("可编辑"))
    }

    @Test
    fun exportCenterUsesRefinedDraftFlowAndDocxCopy() {
        val source = read("app/src/main/java/com/classmate/app/ui/components/ExportCenterCard.kt")
        // Draft / fallback flow copy stays on the card; per-format guidance moved into the export help pack.
        listOf("生成学习报告草稿", "选择导出格式", "HTML 或 Text").forEach {
            assertTrue("missing export copy: $it", source.contains(it))
        }
        val strings = read("app/src/main/java/com/classmate/app/ui/i18n/Strings.kt")
        listOf("DOCX 是真实 Word 文档", "PDF 适合打印", "课程精华音频脚本").forEach {
            assertTrue("missing export help copy: $it", strings.contains(it))
        }
    }

    @Test
    fun settingsLearningExportIncludesDocxAndBoundaries() {
        val source = read("app/src/main/java/com/classmate/app/ui/screens/settings/SettingsScreen.kt")
        listOf(
            "Word / DOCX",
            "真实 OpenXML 文档",
            "script-only",
            "evidence、practice result、weakness、review plan、source metadata、audio script",
            "不会模拟具体人物声音",
        ).forEach { assertTrue("missing Settings export copy: $it", source.contains(it)) }
        assertFalse(source.contains("声音复刻"))
        assertFalse(source.contains("老师声音克隆"))
    }

    private fun read(path: String): String =
        listOf(File(path), File(path.removePrefix("app/"))).first { it.exists() }.readText()
}
