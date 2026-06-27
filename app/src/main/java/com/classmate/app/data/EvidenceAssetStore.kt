package com.classmate.app.data

import java.io.File

data class StoredImageEvidenceAsset(
    val imageRef: String,
    val thumbnailRef: String,
    val mimeType: String,
    val sourceLabel: String,
)

data class EvidenceAssetCleanupResult(
    val deletedCount: Int = 0,
    val failedRefs: List<String> = emptyList(),
) {
    val success: Boolean get() = failedRefs.isEmpty()
}

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

    fun deleteRefs(refs: Iterable<String>): EvidenceAssetCleanupResult {
        val dir = rootDir ?: return EvidenceAssetCleanupResult()
        var deleted = 0
        val failed = mutableListOf<String>()
        refs.map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { ref ->
                val file = safeFileForRef(dir, ref) ?: return@forEach
                if (!file.exists()) return@forEach
                val ok = runCatching { file.delete() }.getOrDefault(false)
                if (ok || !file.exists()) deleted++ else failed += ref
            }
        return EvidenceAssetCleanupResult(deleted, failed)
    }

    fun deleteForSession(sessionId: String): EvidenceAssetCleanupResult {
        val dir = rootDir ?: return EvidenceAssetCleanupResult()
        if (sessionId.isBlank() || !dir.exists()) return EvidenceAssetCleanupResult()
        var deleted = 0
        val failed = mutableListOf<String>()
        dir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.contains(sessionId, ignoreCase = true) }
            .forEach { file ->
                val ok = runCatching { file.delete() }.getOrDefault(false)
                if (ok || !file.exists()) deleted++ else failed += file.name
            }
        return EvidenceAssetCleanupResult(deleted, failed)
    }

    fun cleanupOrphans(knownRefs: Set<String>, deleteUnknown: Boolean = false): EvidenceAssetCleanupResult {
        val dir = rootDir ?: return EvidenceAssetCleanupResult()
        if (!dir.exists()) return EvidenceAssetCleanupResult()
        val normalizedKnown = knownRefs.map { normalizeRef(it) }.filter { it.isNotBlank() }.toSet()
        var deleted = 0
        val failed = mutableListOf<String>()
        dir.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .filter { it.length() == 0L || (deleteUnknown && normalizeRef(it.absolutePath) !in normalizedKnown && normalizeRef(it.name) !in normalizedKnown) }
            .forEach { file ->
                val ok = runCatching { file.delete() }.getOrDefault(false)
                if (ok || !file.exists()) deleted++ else failed += file.name
            }
        return EvidenceAssetCleanupResult(deleted, failed)
    }

    companion object {
        fun disabled(): EvidenceAssetStore = EvidenceAssetStore(null)

        private fun safeFileForRef(rootDir: File, ref: String): File? {
            val normalized = normalizeRef(ref)
            if (normalized.isBlank()) return null
            val candidate = File(ref)
            val file = if (candidate.isAbsolute) candidate else File(rootDir, ref)
            val rootPath = rootDir.canonicalFile.toPath()
            val filePath = file.canonicalFile.toPath()
            return if (filePath.startsWith(rootPath)) file else null
        }

        private fun normalizeRef(ref: String): String =
            ref.trim().replace('\\', '/').lowercase()
    }
}
