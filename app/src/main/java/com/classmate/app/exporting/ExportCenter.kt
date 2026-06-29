package com.classmate.app.exporting

import com.classmate.core.exporting.PolishedStudyPack
import com.classmate.core.exporting.SafeExportText
import com.classmate.core.exporting.StudyReport
import com.classmate.core.exporting.StudyReportDocxRenderer
import com.classmate.core.exporting.StudyReportRenderer
import com.classmate.core.audio.CourseEssenceAudioExporter

object ExportCenter {

    /**
     * Stage 6: the printable export path. EVERY format is rendered from one [StudyReport] so they
     * never drift, and the result is a study handout — never a UI/JSON/debug dump. Mind-map keeps its
     * own structure via [artifactFromMarkdown]; all other formats flow through here.
     */
    fun artifactFromStudyReport(
        report: StudyReport,
        format: ExportFileFormat,
        createdAt: Long = System.currentTimeMillis(),
    ): ExportArtifact {
        val safeTitle = SafeExportText.redact(report.courseTitle.ifBlank { "未命名课程" })
        val body = when (format) {
            ExportFileFormat.MARKDOWN, ExportFileFormat.MINDMAP_MARKDOWN -> StudyReportRenderer.renderMarkdown(report)
            ExportFileFormat.TEXT, ExportFileFormat.PDF -> StudyReportRenderer.renderPlainText(report)
            ExportFileFormat.HTML, ExportFileFormat.MINDMAP_HTML -> StudyReportRenderer.renderHtml(report)
            ExportFileFormat.DOCX -> StudyReportRenderer.renderPlainText(report)
            ExportFileFormat.WORD_COMPAT_HTML ->
                StudyReportRenderer.renderHtml(report, note = "Word 兼容 HTML，可用 Word/WPS 打开，不是真 .docx")
            ExportFileFormat.SLIDES_HTML -> StudyReportRenderer.renderSlidesHtml(report)
            ExportFileFormat.COURSE_ESSENCE_SCRIPT_TEXT -> CourseEssenceAudioExporter.buildScript(report).toPlainText()
        }
        val safeBody = SafeExportText.redact(body)
        val bytes = when (format) {
            ExportFileFormat.PDF -> PdfExportRenderer.render(safeTitle, safeBody)
            ExportFileFormat.DOCX -> StudyReportDocxRenderer.render(report)
            else -> safeBody.toByteArray(Charsets.UTF_8)
        }
        val fileName = ExportFileNameSanitizer.safe("ClassMate-$safeTitle-${format.fileLabel()}", format.extension)
        return ExportArtifact(
            displayName = format.displayName,
            fileName = fileName,
            mimeType = format.mimeType,
            format = format,
            bytes = bytes,
            createdAt = createdAt,
            containsSensitiveContent = ExportSafety.containsSensitiveText(safeBody),
        )
    }

    fun artifactFromMarkdown(
        courseTitle: String,
        markdown: String,
        format: ExportFileFormat,
        createdAt: Long = System.currentTimeMillis(),
    ): ExportArtifact {
        val safeTitle = SafeExportText.redact(courseTitle.ifBlank { "未命名课程" })
        val safeMarkdown = SafeExportText.redact(markdown)
        return renderMarkdownArtifact(safeTitle, safeMarkdown, format, format.fileLabel(), createdAt)
    }

    /**
     * P0-1/P0-2: render a user-initiated AI 精修学习包. EVERY format (PDF / HTML / Word / Markdown / Text)
     * comes from the SAME [PolishedStudyPack.headedMarkdown], so the polished Word can never be a different
     * version from the polished PDF. The file name carries "polished" + the course title for clarity.
     */
    fun artifactFromPolishedPack(
        pack: PolishedStudyPack,
        format: ExportFileFormat,
        createdAt: Long = System.currentTimeMillis(),
    ): ExportArtifact {
        val safeTitle = SafeExportText.redact(pack.courseTitle.ifBlank { "未命名课程" })
        val safeMarkdown = SafeExportText.redact(pack.headedMarkdown())
        return renderMarkdownArtifact(safeTitle, safeMarkdown, format, "精修学习包-polished", createdAt)
    }

    /** Shared markdown-to-format renderer: HTML is structured (not a <pre> dump) and DOCX is a real package. */
    private fun renderMarkdownArtifact(
        safeTitle: String,
        safeMarkdown: String,
        format: ExportFileFormat,
        fileLabel: String,
        createdAt: Long,
    ): ExportArtifact {
        val body = when (format) {
            ExportFileFormat.MARKDOWN -> safeMarkdown
            ExportFileFormat.HTML -> markdownToStyledHtml(safeTitle, safeMarkdown, "ClassMate 学习报告")
            ExportFileFormat.TEXT -> markdownToPlain(safeMarkdown)
            ExportFileFormat.PDF -> markdownToPlain(safeMarkdown)
            ExportFileFormat.DOCX -> markdownToPlain(safeMarkdown)
            ExportFileFormat.MINDMAP_MARKDOWN -> safeMarkdown
            ExportFileFormat.MINDMAP_HTML -> mindMapHtml(safeTitle, safeMarkdown)
            ExportFileFormat.WORD_COMPAT_HTML -> markdownToStyledHtml(safeTitle, safeMarkdown, "Word 兼容 HTML，可用 Word/WPS 打开，不是真 .docx")
            ExportFileFormat.SLIDES_HTML -> slidesHtml(safeTitle, safeMarkdown)
            ExportFileFormat.COURSE_ESSENCE_SCRIPT_TEXT -> markdownToPlain(safeMarkdown)
        }
        val bytes = when (format) {
            ExportFileFormat.PDF -> PdfExportRenderer.render(safeTitle, body)
            ExportFileFormat.DOCX -> StudyReportDocxRenderer.renderMarkdownDocument(safeTitle, safeMarkdown)
            else -> body.toByteArray(Charsets.UTF_8)
        }
        val fileName = ExportFileNameSanitizer.safe("ClassMate-$safeTitle-$fileLabel", format.extension)
        return ExportArtifact(
            displayName = format.displayName,
            fileName = fileName,
            mimeType = format.mimeType,
            format = format,
            bytes = bytes,
            createdAt = createdAt,
            containsSensitiveContent = ExportSafety.containsSensitiveText(body),
        )
    }

    private fun ExportFileFormat.fileLabel(): String = when (this) {
        ExportFileFormat.MARKDOWN -> "学习报告"
        ExportFileFormat.HTML -> "学习报告"
        ExportFileFormat.TEXT -> "学习报告"
        ExportFileFormat.PDF -> "学习报告"
        ExportFileFormat.DOCX -> "学习报告"
        ExportFileFormat.MINDMAP_MARKDOWN -> "知识结构大纲"
        ExportFileFormat.MINDMAP_HTML -> "知识结构大纲"
        ExportFileFormat.WORD_COMPAT_HTML -> "Word兼容报告"
        ExportFileFormat.SLIDES_HTML -> "演示幻灯片"
        ExportFileFormat.COURSE_ESSENCE_SCRIPT_TEXT -> "课程精华音频脚本"
    }

    private fun htmlPage(title: String, markdown: String, note: String): String =
        """
        <!doctype html>
        <html><head><meta charset="utf-8"><title>${escapeHtml(title)}</title>
        <style>body{font-family:system-ui,sans-serif;line-height:1.65;padding:24px;max-width:900px;margin:auto;} pre{white-space:pre-wrap;} .note{color:#52616b;background:#f3f6f8;padding:12px;border-radius:8px;}</style>
        </head><body><h1>${escapeHtml(title)}</h1><p class="note">${escapeHtml(note)}</p><pre>${escapeHtml(markdown)}</pre></body></html>
        """.trimIndent()

    /** A print-friendly HTML that renders markdown structure (headings / bullets) instead of a raw <pre> dump. */
    private fun markdownToStyledHtml(title: String, markdown: String, note: String): String {
        val content = StringBuilder()
        var inList = false
        fun closeList() { if (inList) { content.append("</ul>"); inList = false } }
        markdown.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> closeList()
                line.startsWith("### ") -> { closeList(); content.append("<h3>").append(inline(line.removePrefix("### "))).append("</h3>") }
                line.startsWith("## ") -> { closeList(); content.append("<h2>").append(inline(line.removePrefix("## "))).append("</h2>") }
                line.startsWith("# ") -> { closeList(); content.append("<h1>").append(inline(line.removePrefix("# "))).append("</h1>") }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    if (!inList) { content.append("<ul>"); inList = true }
                    content.append("<li>").append(inline(line.drop(2))).append("</li>")
                }
                line.startsWith("> ") -> { closeList(); content.append("<blockquote>").append(inline(line.drop(2))).append("</blockquote>") }
                else -> { closeList(); content.append("<p>").append(inline(line)).append("</p>") }
            }
        }
        closeList()
        return """
        <!doctype html>
        <html lang="zh-CN"><head><meta charset="utf-8"><title>${escapeHtml(title)}</title>
        <style>
        @page { size: A4; margin: 18mm; }
        body{font-family:"Noto Sans CJK SC","Microsoft YaHei",system-ui,sans-serif;line-height:1.7;color:#1f2933;max-width:820px;margin:auto;padding:24px;}
        h1{font-size:24px;margin:0 0 10px;} h2{font-size:19px;margin:24px 0 10px;border-left:4px solid #2563eb;padding-left:10px;} h3{font-size:16px;margin:16px 0 6px;}
        ul{margin:6px 0 12px 22px;} li{margin:3px 0;} p{margin:8px 0;} blockquote{margin:8px 0;padding:6px 12px;border-left:3px solid #cbd5e1;color:#52616b;}
        .note{color:#52616b;background:#f3f6f8;padding:10px 12px;border-radius:8px;font-size:13px;}
        </style></head><body><p class="note">${escapeHtml(note)}</p>$content</body></html>
        """.trimIndent()
    }

    /** Minimal inline markdown: escape, then de-emphasize ** and ` so they don't show as literals. */
    private fun inline(value: String): String =
        escapeHtml(value).replace("**", "").replace("`", "")

    private fun mindMapHtml(title: String, markdown: String): String =
        """
        <!doctype html>
        <html><head><meta charset="utf-8"><title>${escapeHtml(title)} 知识结构大纲</title>
        <style>body{font-family:system-ui,sans-serif;padding:24px;background:#f7fafc;} .root{font-size:24px;font-weight:700;margin-bottom:16px;} pre{white-space:pre-wrap;background:white;border:1px solid #d8e0e8;border-radius:10px;padding:16px;}</style>
        </head><body><div class="root">${escapeHtml(title)}</div><pre>${escapeHtml(markdown)}</pre></body></html>
        """.trimIndent()

    private fun slidesHtml(title: String, markdown: String): String {
        val sections = listOf(
            "封面" to title,
            "课程概览" to markdown.sectionPreview("Knowledge timeline", 900),
            "核心知识点" to markdown.sectionPreview("Knowledge points", 1200),
            "证据链" to markdown.sectionPreview("evidence", 1000),
            "微测" to markdown.sectionPreview("Micro quiz", 1000),
            "复习计划" to markdown.sectionPreview("Review plan", 900),
            "Ask This Lesson" to markdown.sectionPreview("Ask This Lesson", 900),
            "多模态来源摘要" to markdown.sectionPreview("资料来源摘要", 700),
        )
        val slides = sections.joinToString("\n") { (heading, text) ->
            "<section class=\"slide\"><h2>${escapeHtml(heading)}</h2><pre>${escapeHtml(text.ifBlank { "暂无内容" })}</pre></section>"
        }
        return """
            <!doctype html>
            <html><head><meta charset="utf-8"><title>${escapeHtml(title)} 演示幻灯片</title>
            <style>body{margin:0;background:#111827;color:#111827;font-family:system-ui,sans-serif;} .slide{box-sizing:border-box;min-height:100vh;padding:48px;background:#ffffff;border-bottom:8px solid #e5e7eb;} h2{font-size:34px;margin-top:0;} pre{white-space:pre-wrap;font-size:18px;line-height:1.55;}</style>
            </head><body>$slides</body></html>
        """.trimIndent()
    }

    private fun markdownToPlain(markdown: String): String =
        markdown
            .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
            .replace("**", "")
            .replace("`", "")

    private fun String.sectionPreview(anchor: String, max: Int): String {
        val index = indexOf(anchor, ignoreCase = true)
        return if (index >= 0) drop(index).take(max) else take(max)
    }

    private fun escapeHtml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
