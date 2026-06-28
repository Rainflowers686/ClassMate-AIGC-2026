package com.classmate.app.importing

import com.classmate.core.material.MaterialEvidenceRef
import com.classmate.core.material.MaterialSegment
import com.classmate.core.material.MaterialSource
import com.classmate.core.material.MaterialSourceType

enum class OcrImportKind(val displayName: String) {
    SLIDE_IMAGE("课件图片 / PPT 截图"),
    BLACKBOARD_PHOTO("板书照片"),
    PDF_PAGE("PDF / 讲义截图"),
    HANDOUT_IMAGE("讲义图片"),
}

enum class OcrImportStatus {
    PENDING,
    OK,
    FAILED,
}

/** True when an OK draft was flagged low-quality (has an errorReason note) and should be verified. */
fun OcrImportDraft.isLowQuality(): Boolean = status == OcrImportStatus.OK && errorReason.isNotBlank()

data class MergedOcrImport(
    val text: String,
    val okDrafts: List<OcrImportDraft>,
    val failedDrafts: List<OcrImportDraft>,
)

data class OcrImportFileMeta(
    val fileName: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val displayLabel: String,
    val pageIndex: Int? = null,
) {
    fun safeSummary(): String {
        val mime = mimeType?.takeIf { it.isNotBlank() } ?: "type unknown"
        val size = sizeBytes?.let { "${it}B" } ?: "size unknown"
        return "${safeDisplayLabel()} / ${safeFileName()} / $mime / $size"
    }

    fun safeDisplayLabel(): String =
        displayLabel.safeTailName().ifBlank { safeFileName() }

    private fun safeFileName(): String =
        fileName.safeTailName().ifBlank { "selected-file" }
}

data class OcrImportDraft(
    val id: String,
    val kind: OcrImportKind,
    val fileMeta: OcrImportFileMeta,
    val pastedText: String,
    val status: OcrImportStatus = OcrImportStatus.OK,
    val errorReason: String = "",
    val batchId: String = "",
    val pageIndex: Int? = null,
    val blockIndex: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

object OcrImportAssembler {
    fun toMaterialSource(draft: OcrImportDraft): MaterialSource? {
        if (draft.status != OcrImportStatus.OK) return null
        val clean = draft.pastedText.trim()
        if (clean.isBlank()) return null

        val type = materialType(draft.kind)
        val sourceId = stableSourceId(draft.id)
        val page = draft.pageIndex ?: draft.fileMeta.pageIndex ?: 1
        val block = draft.blockIndex ?: 1
        val label = sourceLabel(draft.kind)
        val segmentId = when (draft.kind) {
            OcrImportKind.BLACKBOARD_PHOTO -> "${sourceId}_b$block"
            else -> "${sourceId}_p$page"
        }
        val segment = MaterialSegment(
            id = segmentId,
            sourceType = type,
            sourceId = sourceId,
            index = block,
            text = clean,
            evidence = MaterialEvidenceRef(
                sourceType = type,
                sourceId = sourceId,
                segmentId = segmentId,
                pageId = "${sourceId}_page_$page",
                blockId = "${sourceId}_block_$block",
                quote = clean,
                sourceLabel = label,
            ),
            pageIndex = page,
            sourceLabel = label,
        )
        return MaterialSource(
            id = sourceId,
            type = type,
            title = draft.fileMeta.safeDisplayLabel().ifBlank { label },
            segments = listOf(segment),
            createdAt = draft.createdAt,
        )
    }

    fun toMaterialSources(drafts: List<OcrImportDraft>): List<MaterialSource> =
        drafts.mapNotNull(::toMaterialSource)

    fun mergeByOrder(drafts: List<OcrImportDraft>): MergedOcrImport {
        val ordered = drafts.sortedWith(
            compareBy<OcrImportDraft> { it.pageIndex ?: it.fileMeta.pageIndex ?: Int.MAX_VALUE }
                .thenBy { it.createdAt }
                .thenBy { it.id },
        )
        val ok = ordered.filter { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() }
        val failed = ordered.filter { it.status == OcrImportStatus.FAILED }
        val body = ok.joinToString("\n\n") { draft ->
            val index = draft.pageIndex ?: draft.fileMeta.pageIndex ?: (ok.indexOf(draft) + 1)
            val label = draft.fileMeta.safeDisplayLabel().ifBlank { "图片$index" }
            "【图片$index：$label】\n${draft.pastedText.trim()}"
        }
        return MergedOcrImport(body, ok, failed)
    }

    fun materialType(kind: OcrImportKind): MaterialSourceType = when (kind) {
        OcrImportKind.SLIDE_IMAGE -> MaterialSourceType.SLIDE_OCR
        OcrImportKind.HANDOUT_IMAGE -> MaterialSourceType.SLIDE_OCR
        OcrImportKind.BLACKBOARD_PHOTO -> MaterialSourceType.BLACKBOARD_OCR
        OcrImportKind.PDF_PAGE -> MaterialSourceType.PDF_OCR
    }

    fun sourceLabel(kind: OcrImportKind): String = when (kind) {
        OcrImportKind.SLIDE_IMAGE -> "课件 OCR"
        OcrImportKind.BLACKBOARD_PHOTO -> "板书 OCR"
        OcrImportKind.PDF_PAGE -> "PDF OCR"
        OcrImportKind.HANDOUT_IMAGE -> "讲义 OCR"
    }

    fun defaultDisplayLabel(kind: OcrImportKind, ordinal: Int): String = when (kind) {
        OcrImportKind.SLIDE_IMAGE -> "课件 OCR 第 $ordinal 页"
        OcrImportKind.BLACKBOARD_PHOTO -> "板书 OCR 第 $ordinal 块"
        OcrImportKind.PDF_PAGE -> "PDF OCR 第 $ordinal 页"
        OcrImportKind.HANDOUT_IMAGE -> "讲义 OCR 第 $ordinal 页"
    }

    private fun stableSourceId(id: String): String =
        "ocr__" + id.lowercase()
            .replace(Regex("[^a-z0-9_\\-]+"), "_")
            .trim('_')
            .ifBlank { "manual" }
}

private fun String.safeTailName(): String =
    trim()
        .substringAfterLast('\\')
        .substringAfterLast('/')
        .substringAfterLast("content:")
        .take(80)
