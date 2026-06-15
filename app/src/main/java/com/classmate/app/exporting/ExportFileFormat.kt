package com.classmate.app.exporting

enum class ExportFileFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val description: String,
) {
    MARKDOWN("Markdown", "md", "text/markdown; charset=utf-8", "结构化学习报告"),
    HTML("HTML", "html", "text/html; charset=utf-8", "浏览器可打开的富文本报告"),
    TEXT("纯文本", "txt", "text/plain; charset=utf-8", "适合复制和快速查看"),
    PDF("PDF 报告", "pdf", "application/pdf", "基础 PDF，排版简单可靠"),
    MINDMAP_MARKDOWN("思维导图 Markdown", "md", "text/markdown; charset=utf-8", "缩进列表形式的思维导图"),
    MINDMAP_HTML("思维导图 HTML", "html", "text/html; charset=utf-8", "浏览器可打开的思维导图"),
    WORD_COMPAT_HTML("Word 兼容 HTML", "html", "text/html; charset=utf-8", "可用 Word/WPS 打开，不是真 .docx"),
    SLIDES_HTML("演示幻灯片 HTML", "html", "text/html; charset=utf-8", "演示幻灯片 HTML，不是真 .pptx"),
    COURSE_ESSENCE_SCRIPT_TEXT("课程精华音频脚本", "txt", "text/plain; charset=utf-8", "TTS 未配置时可导出的可朗读复习稿"),
}
