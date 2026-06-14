package com.classmate.core.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownImportTest {

    // ---- TextDecoding -------------------------------------------------------------------------

    @Test
    fun decodesPlainUtf8Chinese() {
        val decoded = TextDecoding.decodeBestEffort("电磁感应".toByteArray(Charsets.UTF_8))
        assertEquals("电磁感应", decoded.text)
        assertEquals("UTF-8", decoded.charsetLabel)
    }

    @Test
    fun stripsUtf8Bom() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = bom + "标题".toByteArray(Charsets.UTF_8)
        val decoded = TextDecoding.decodeBestEffort(bytes)
        assertEquals("标题", decoded.text) // no stray BOM char
        assertTrue(decoded.charsetLabel.contains("BOM"))
    }

    @Test
    fun fallsBackToGb18030() {
        val original = "电磁感应与楞次定律的应用"
        val gbBytes = original.toByteArray(charset("GB18030"))
        val decoded = TextDecoding.decodeBestEffort(gbBytes)
        assertEquals(original, decoded.text)
        assertEquals("GB18030", decoded.charsetLabel)
    }

    // ---- MarkdownPlainText --------------------------------------------------------------------

    @Test
    fun normalizesHeadingsListsAndOrdered() {
        val md = "# 一级标题\n## 二级\n- 项目一\n* 项目二\n1. 第一步"
        val text = MarkdownPlainText.toPlainText(md)
        assertTrue(text.contains("一级标题"))
        assertFalse(text.contains("#"))
        assertTrue(text.contains("• 项目一"))
        assertTrue(text.contains("• 项目二"))
        assertTrue(text.contains("1. 第一步"))
    }

    @Test
    fun textifiesTablesAndDropsSeparatorRow() {
        val md = "| 概念 | 说明 |\n| --- | --- |\n| 磁通量 | 穿过面积的磁感应 |"
        val text = MarkdownPlainText.toPlainText(md)
        assertTrue(text.contains("概念"))
        assertTrue(text.contains("磁通量"))
        assertFalse(text.contains("---"))
        assertFalse(text.contains("|"))
    }

    @Test
    fun keepsCodeContentUnderMarkerAndKeepsFormulaVerbatim() {
        val md = "正文\n```kotlin\nval x = 1\n```\n公式：\$E=mc^2\$ 与 \$\$\\int_0^1 x\\,dx\$\$"
        val text = MarkdownPlainText.toPlainText(md)
        assertTrue(text.contains("[代码]"))
        assertTrue(text.contains("val x = 1"))
        assertFalse(text.contains("```"))
        assertTrue(text.contains("\$E=mc^2\$"))      // inline math preserved
        assertTrue(text.contains("\$\$\\int_0^1 x\\,dx\$\$")) // block math preserved
    }

    @Test
    fun convertsImagesAndLinks() {
        val md = "![电路图](http://x/img.png) 和 [楞次定律](https://example.com/lenz) 与 ![](a.png)"
        val text = MarkdownPlainText.toPlainText(md)
        assertTrue(text.contains("[图片: 电路图]"))
        assertTrue(text.contains("楞次定律"))
        assertFalse(text.contains("https://example.com/lenz"))
        assertTrue(text.contains("[图片]"))
    }

    @Test
    fun stripsEmphasisMarkers() {
        val text = MarkdownPlainText.toPlainText("这是**重点**和`代码`以及~~删除~~")
        assertTrue(text.contains("重点"))
        assertFalse(text.contains("**"))
        assertFalse(text.contains("`"))
        assertFalse(text.contains("~~"))
    }

    // ---- FileImportText -----------------------------------------------------------------------

    @Test
    fun markdownFileIsDecodedAndNormalized() {
        val md = "# 课堂笔记\n- 要点一\n[链接](http://x)"
        val result = FileImportText.fromBytes(md.toByteArray(Charsets.UTF_8), "lesson.md")
        assertTrue(result.accepted)
        assertTrue(result.isMarkdown)
        assertFalse(result.text.contains("#"))
        assertTrue(result.text.contains("• 要点一"))
    }

    @Test
    fun plainTxtIsNotMarkdownNormalized() {
        val result = FileImportText.fromBytes("# 这是普通文本不是标题".toByteArray(Charsets.UTF_8), "notes.txt")
        assertTrue(result.accepted)
        assertFalse(result.isMarkdown)
        assertTrue(result.text.contains("#")) // txt kept verbatim
    }

    @Test
    fun oversizeFileIsRejectedFriendly() {
        val big = ByteArray(FileImportText.MAX_BYTES + 1) { 'a'.code.toByte() }
        val result = FileImportText.fromBytes(big, "huge.md")
        assertFalse(result.accepted)
        assertTrue(result.message.contains("2MB") || result.message.contains("较大"))
    }

    @Test
    fun importedTextHasNoForbiddenTokens() {
        val md = "# 标题\n- 内容"
        val text = FileImportText.fromBytes(md.toByteArray(Charsets.UTF_8), "x.md").text
        listOf("Authorization", "Bearer", "appKey", "apiKey", "reasoning_content", "prompt", "messages").forEach {
            assertFalse(text.contains(it, ignoreCase = true))
        }
    }
}
