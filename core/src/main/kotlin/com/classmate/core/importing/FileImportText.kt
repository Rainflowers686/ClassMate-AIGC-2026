package com.classmate.core.importing

/**
 * Turns raw imported file bytes into analysis-ready text. Centralizes the Stage 6 fix so the UI just
 * reads bytes and calls one pure, testable function:
 *   bytes -> best-effort decode (UTF-8 / BOM / GB18030) -> (.md only) Markdown normalization.
 * Friendly, non-crashing results: oversized/empty files return [accepted] = false with a message.
 */
object FileImportText {

    const val MAX_BYTES = 2_000_000 // ~2MB; larger files get a friendly "paste key parts" hint

    data class Result(
        val accepted: Boolean,
        val text: String,
        val charsetLabel: String,
        val isMarkdown: Boolean,
        val message: String,
    )

    fun fromBytes(bytes: ByteArray, fileName: String): Result {
        if (bytes.isEmpty()) return Result(false, "", "empty", false, "文件为空，请选择有内容的文本文件。")
        if (bytes.size > MAX_BYTES) {
            return Result(false, "", "", false, "文件较大（超过 2MB），请改用更小的文本或只粘贴关键段落。")
        }
        val isMarkdown = fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".markdown", ignoreCase = true)
        val decoded = TextDecoding.decodeBestEffort(bytes)
        val text = if (isMarkdown) MarkdownPlainText.toPlainText(decoded.text) else decoded.text
        val kind = if (isMarkdown) "Markdown" else "文本"
        return Result(
            accepted = text.isNotBlank(),
            text = text,
            charsetLabel = decoded.charsetLabel,
            isMarkdown = isMarkdown,
            message = if (text.isBlank()) "$kind 文件没有可用文字。" else "已读取$kind（${decoded.charsetLabel}）。",
        )
    }
}
