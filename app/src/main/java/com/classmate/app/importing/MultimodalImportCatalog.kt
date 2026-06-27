package com.classmate.app.importing

import com.classmate.core.importing.ImportCapabilityStatus

enum class MultimodalEntryId {
    PASTE_TEXT,
    TXT_MD,
    AUDIO,
    VIDEO,
    SLIDE_IMAGE,
    BLACKBOARD_PHOTO,
    PDF_HANDOUT,
    NETWORK_VIDEO_LINK,
}

data class MultimodalImportEntry(
    val id: MultimodalEntryId,
    val title: String,
    val status: String,
    val detail: String,
    val availableNow: Boolean,
    val opensPicker: Boolean,
    val mimeTypes: List<String> = emptyList(),
    val requiresNetwork: Boolean = false,
    val capabilityStatus: ImportCapabilityStatus = when (id) {
        MultimodalEntryId.PASTE_TEXT,
        MultimodalEntryId.TXT_MD -> ImportCapabilityStatus.AVAILABLE
        MultimodalEntryId.AUDIO,
        MultimodalEntryId.SLIDE_IMAGE,
        MultimodalEntryId.BLACKBOARD_PHOTO,
        MultimodalEntryId.PDF_HANDOUT -> ImportCapabilityStatus.NEEDS_CONFIG
        MultimodalEntryId.VIDEO,
        MultimodalEntryId.NETWORK_VIDEO_LINK -> ImportCapabilityStatus.UNSUPPORTED
    },
)

data class SelectedLocalFileMetadata(
    val entryTitle: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long?,
) {
    fun summary(): String {
        val size = sizeBytes?.let { "${it}B" } ?: "size unknown"
        val type = mimeType.ifBlank { "type unknown" }
        return "$entryTitle: $fileName, $type, $size. 当前只记录元数据，不解析、不上传。"
    }
}

object MultimodalImportCatalog {
    val entries: List<MultimodalImportEntry> = listOf(
        MultimodalImportEntry(
            id = MultimodalEntryId.PASTE_TEXT,
            title = "粘贴课堂文本",
            status = "已支持",
            detail = "直接粘贴课堂转写、笔记或讲义文本，进入现有分析流程。",
            availableNow = true,
            opensPicker = false,
        ),
        MultimodalImportEntry(
            id = MultimodalEntryId.TXT_MD,
            title = ".txt / .md 文件",
            status = "已支持",
            detail = "读取用户本地文本文件内容，不上传，不解析音视频。",
            availableNow = true,
            opensPicker = false,
            mimeTypes = listOf("text/plain", "text/markdown", "text/*"),
        ),
        MultimodalImportEntry(
            id = MultimodalEntryId.AUDIO,
            title = "本地音频",
            status = "官方 ASR 按配置",
            detail = "可选择音频并尝试官方长语音转写；未配置或失败时可粘贴转写文本，不上传到第三方平台。",
            availableNow = true,
            opensPicker = true,
            mimeTypes = listOf("audio/*"),
        ),
        MultimodalImportEntry(
            id = MultimodalEntryId.VIDEO,
            title = "本地视频",
            status = "下一阶段音频/字幕/OCR",
            detail = "当前不解析视频、不抽帧、不上传；仅允许用户本地文件，后续做合规处理。",
            availableNow = false,
            opensPicker = true,
            mimeTypes = listOf("video/*"),
        ),
        MultimodalImportEntry(
            id = MultimodalEntryId.SLIDE_IMAGE,
            title = "PPT / 课件图片",
            status = "官方 OCR 按配置",
            detail = "图片/拍照路径可尝试官方 OCR；未配置或失败时可粘贴识别文字，并保留端侧蓝心可编辑草稿。",
            availableNow = true,
            opensPicker = true,
            mimeTypes = listOf("image/*", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        ),
        MultimodalImportEntry(
            id = MultimodalEntryId.BLACKBOARD_PHOTO,
            title = "板书照片",
            status = "官方 OCR 按配置",
            detail = "可记录图片元数据并尝试识别；未配置时粘贴文字作为板书 OCR 证据来源，不上传图片。",
            availableNow = true,
            opensPicker = true,
            mimeTypes = listOf("image/*"),
        ),
        MultimodalImportEntry(
            id = MultimodalEntryId.PDF_HANDOUT,
            title = "PDF / 讲义截图",
            status = "官方 OCR 按配置",
            detail = "可记录 PDF/截图元数据并粘贴讲义 OCR 文本；未配置官方 OCR 时仍可手动确认后进入资料篮。",
            availableNow = true,
            opensPicker = true,
            mimeTypes = listOf("application/pdf", "image/*"),
        ),
        MultimodalImportEntry(
            id = MultimodalEntryId.NETWORK_VIDEO_LINK,
            title = "网络视频链接",
            status = "不做爬取",
            detail = "不抓取第三方平台内容；仅允许用户提供自己有权使用的课堂文本或字幕。",
            availableNow = false,
            opensPicker = false,
            requiresNetwork = false,
        ),
    )

    fun byId(id: MultimodalEntryId): MultimodalImportEntry = entries.first { it.id == id }
}
