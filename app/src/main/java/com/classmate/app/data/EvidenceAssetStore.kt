package com.classmate.app.data

import java.io.File

data class StoredImageEvidenceAsset(
    val imageRef: String,
    val thumbnailRef: String,
    val mimeType: String,
    val sourceLabel: String,
)

class EvidenceAssetStore(private val rootDir: File? = null) {
    fun saveImage(
        bytes: ByteArray,
        sourceLabel: String,
        mimeType: String = "image/jpeg",
        now: Long = System.currentTimeMillis(),
    ): StoredImageEvidenceAsset {
        val safeLabel = sourceLabel.ifBlank { "OCR image" }
        val normalizedMime = mimeType.ifBlank { "image/jpeg" }
        val extension = when {
            normalizedMime.contains("png", ignoreCase = true) -> "png"
            normalizedMime.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val fallbackRef = "image_asset_$now.$extension"
        val fallbackThumb = "image_asset_${now}_thumb.$extension"
        val dir = rootDir ?: return StoredImageEvidenceAsset(fallbackRef, fallbackThumb, normalizedMime, safeLabel)
        if (bytes.isEmpty()) return StoredImageEvidenceAsset(fallbackRef, fallbackThumb, normalizedMime, safeLabel)

        return runCatching {
            dir.mkdirs()
            val image = File(dir, fallbackRef)
            val thumb = File(dir, fallbackThumb)
            image.writeBytes(bytes)
            thumb.writeBytes(bytes.copyOfRange(0, minOf(bytes.size, 4_096)))
            StoredImageEvidenceAsset(
                imageRef = image.absolutePath,
                thumbnailRef = thumb.absolutePath,
                mimeType = normalizedMime,
                sourceLabel = safeLabel,
            )
        }.getOrElse {
            StoredImageEvidenceAsset(fallbackRef, fallbackThumb, normalizedMime, safeLabel)
        }
    }

    companion object {
        fun disabled(): EvidenceAssetStore = EvidenceAssetStore(null)
    }
}
