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

enum class ImportCapabilityStatus {
    AVAILABLE,
    NEEDS_PERMISSION,
    NEEDS_CONFIG,
    UNSUPPORTED,
    EXPERIMENTAL,
}

data class ImportCapability(
    val sourceType: ImportSourceType,
    val status: ImportCapabilityStatus,
    val label: String,
    val message: String,
) {
    val available: Boolean get() = status == ImportCapabilityStatus.AVAILABLE
}

data class ImportValidationResult(
    val accepted: Boolean,
    val message: String,
    val draft: ImportDraft? = null,
)

object ImportHub {
    val capabilities: List<ImportCapability> = listOf(
        ImportCapability(ImportSourceType.PASTE_TEXT, ImportCapabilityStatus.AVAILABLE, "粘贴课堂文本", "可直接进入学习闭环。"),
        ImportCapability(ImportSourceType.TXT_FILE, ImportCapabilityStatus.AVAILABLE, "导入 .txt", "可读取本地文本并进入学习闭环。"),
        ImportCapability(ImportSourceType.MARKDOWN_FILE, ImportCapabilityStatus.AVAILABLE, "导入 .md", "可读取 Markdown 文本并进入学习闭环。"),
        ImportCapability(ImportSourceType.AUDIO_FILE, ImportCapabilityStatus.NEEDS_CONFIG, "导入音频", "需要 ASR 配置；未配置时请粘贴转写文本。"),
        ImportCapability(ImportSourceType.VIDEO_FILE, ImportCapabilityStatus.UNSUPPORTED, "导入视频", "不解析视频内嵌字幕；请手动粘贴转写或导入 SRT/VTT 字幕。"),
        ImportCapability(ImportSourceType.IMAGE_OCR, ImportCapabilityStatus.NEEDS_CONFIG, "图片 OCR", "按配置启用官方 OCR；未配置时可手动确认图片文字。"),
        ImportCapability(ImportSourceType.NETWORK_VIDEO_LINK, ImportCapabilityStatus.UNSUPPORTED, "网络视频链接", "不抓取第三方平台内容；请提供有权使用的课堂文本或字幕。"),
    )

    fun validateText(title: String, text: String, sourceType: ImportSourceType, fileName: String? = null): ImportValidationResult {
        if (sourceType !in setOf(ImportSourceType.PASTE_TEXT, ImportSourceType.TXT_FILE, ImportSourceType.MARKDOWN_FILE)) {
            return ImportValidationResult(false, placeholderMessage(sourceType))
        }
        val clean = text.trim()
        if (clean.isBlank()) return ImportValidationResult(false, "未找到可导入的课堂文本。")
        return ImportValidationResult(
            accepted = true,
            message = "已加入导入草稿。",
            draft = ImportDraft(title = title.trim(), text = clean, sourceType = sourceType, fileName = fileName),
        )
    }

    fun placeholderMessage(sourceType: ImportSourceType): String =
        capabilities.firstOrNull { it.sourceType == sourceType }?.message ?: "该导入方式暂未接入学习闭环。"
}
