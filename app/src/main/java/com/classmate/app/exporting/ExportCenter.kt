package com.classmate.app.exporting

import com.classmate.core.exporting.SafeExportText
import com.classmate.core.exporting.StudyReport
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
            ExportFileFormat.WORD_COMPAT_HTML ->
                StudyReportRenderer.renderHtml(report, note = "Word 兼容 HTML，可用 Word/WPS 打开，不是真 .docx")
            ExportFileFormat.SLIDES_HTML -> StudyReportRenderer.renderSlidesHtml(report)
            ExportFileFormat.COURSE_ESSENCE_SCRIPT_TEXT -> CourseEssenceAudioExporter.buildScript(report).toPlainText()
        }
        val safeBody = SafeExportText.redact(body)
        val bytes = if (format == ExportFileFormat.PDF) {
            PdfExportRenderer.render(safeTitle, safeBody)
        } else {
            safeBody.toByteArray(Charsets.UTF_8)
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
        val body = when (format) {
            ExportFileFormat.MARKDOWN -> safeMarkdown
            ExportFileFormat.HTML -> htmlPage(safeTitle, safeMarkdown, "ClassMate 学习报告")
            ExportFileFormat.TEXT -> markdownToPlain(safeMarkdown)
            ExportFileFormat.PDF -> markdownToPlain(safeMarkdown)
            ExportFileFormat.MINDMAP_MARKDOWN -> safeMarkdown
            ExportFileFormat.MINDMAP_HTML -> mindMapHtml(safeTitle, safeMarkdown)
            ExportFileFormat.WORD_COMPAT_HTML -> htmlPage(safeTitle, safeMarkdown, "Word 兼容 HTML，可用 Word/WPS 打开，不是真 .docx")
            ExportFileFormat.SLIDES_HTML -> slidesHtml(safeTitle, safeMarkdown)
            ExportFileFormat.COURSE_ESSENCE_SCRIPT_TEXT -> markdownToPlain(safeMarkdown)
        }
        val bytes = if (format == ExportFileFormat.PDF) {
            PdfExportRenderer.render(safeTitle, body)
        } else {
            body.toByteArray(Charsets.UTF_8)
        }
        val fileName = ExportFileNameSanitizer.safe("ClassMate-$safeTitle-${format.fileLabel()}", format.extension)
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
        ExportFileFormat.MINDMAP_MARKDOWN -> "思维导图"
        ExportFileFormat.MINDMAP_HTML -> "思维导图"
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

    private fun mindMapHtml(title: String, markdown: String): String =
        """
        <!doctype html>
        <html><head><meta charset="utf-8"><title>${escapeHtml(title)} 思维导图</title>
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
