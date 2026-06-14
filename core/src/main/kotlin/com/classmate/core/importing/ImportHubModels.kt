package com.classmate.core.importing

enum class ImportSourceType {
    PASTE_TEXT,
    TXT_FILE,
    MARKDOWN_FILE,
    AUDIO_FILE,
    VIDEO_FILE,
    IMAGE_OCR,
    NETWORK_VIDEO_LINK,
}

data class ImportDraft(
    val title: String,
    val text: String,
    val sourceType: ImportSourceType,
    val fileName: String? = null,
)

data class ImportCapability(
    val sourceType: ImportSourceType,
    val available: Boolean,
    val label: String,
    val message: String,
)

data class ImportValidationResult(
    val accepted: Boolean,
    val message: String,
    val draft: ImportDraft? = null,
)

object ImportHub {
    val capabilities: List<ImportCapability> = listOf(
        ImportCapability(ImportSourceType.PASTE_TEXT, true, "Paste classroom text", "Available now."),
        ImportCapability(ImportSourceType.TXT_FILE, true, "Import .txt", "Available now; file text becomes classroom text."),
        ImportCapability(ImportSourceType.MARKDOWN_FILE, true, "Import .md", "Available now; markdown text becomes classroom text."),
        ImportCapability(ImportSourceType.AUDIO_FILE, false, "Import audio", "Not connected yet: paste transcript text first. Later vivo long-speech ASR can plug in here."),
        ImportCapability(ImportSourceType.VIDEO_FILE, false, "Import video", "Not connected yet: paste subtitles or transcript text first. No video parsing in this build."),
        ImportCapability(ImportSourceType.IMAGE_OCR, false, "Image / OCR", "Not connected yet: paste recognized text first. Later vivo general OCR can plug in here."),
        ImportCapability(ImportSourceType.NETWORK_VIDEO_LINK, false, "Network video link", "No platform scraping. Paste classroom text or subtitles first; later compliant whitelist search can plug in here."),
    )

    fun validateText(title: String, text: String, sourceType: ImportSourceType, fileName: String? = null): ImportValidationResult {
        if (sourceType !in setOf(ImportSourceType.PASTE_TEXT, ImportSourceType.TXT_FILE, ImportSourceType.MARKDOWN_FILE)) {
            return ImportValidationResult(false, placeholderMessage(sourceType))
        }
        val clean = text.trim()
        if (clean.isBlank()) return ImportValidationResult(false, "No classroom text found.")
        return ImportValidationResult(
            accepted = true,
            message = "Import draft accepted.",
            draft = ImportDraft(title = title.trim(), text = clean, sourceType = sourceType, fileName = fileName),
        )
    }

    fun placeholderMessage(sourceType: ImportSourceType): String =
        capabilities.firstOrNull { it.sourceType == sourceType }?.message ?: "This import source is not connected yet."
}
