package com.classmate.app.l3

import java.io.File

data class RecordingFileCleanupResult(
    val deletedCount: Int = 0,
    val failedFiles: List<String> = emptyList(),
) {
    val success: Boolean get() = failedFiles.isEmpty()
}

/**
 * App-private recording file lifecycle helper. It never touches user-exported files and only deletes
 * files under the injected recording directory (or explicit record paths that resolve inside it).
 */
class RecordingFileManager(private val directory: File? = null) {

    fun isUsable(record: ClassroomRecordingRecord): Boolean {
        if (record.fileSizeBytes <= 0L) return false
        val dir = directory ?: return true
        val file = fileForRecord(dir, record) ?: return false
        return file.exists() && file.length() > 0L
    }

    fun deleteForRecords(records: Collection<ClassroomRecordingRecord>): RecordingFileCleanupResult {
        val dir = directory ?: return RecordingFileCleanupResult()
        var deleted = 0
        val failed = mutableListOf<String>()
        records.mapNotNull { fileForRecord(dir, it) }
            .distinctBy { it.canonicalOrAbsolute() }
            .forEach { file ->
                if (!file.exists()) return@forEach
                val ok = runCatching { file.delete() }.getOrDefault(false)
                if (ok || !file.exists()) deleted++ else failed += file.name
            }
        return RecordingFileCleanupResult(deleted, failed)
    }

    fun deleteForSession(sessionId: String): RecordingFileCleanupResult {
        val dir = directory ?: return RecordingFileCleanupResult()
        if (sessionId.isBlank() || !dir.exists()) return RecordingFileCleanupResult()
        var deleted = 0
        val failed = mutableListOf<String>()
        dir.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.contains(sessionId, ignoreCase = true) }
            .forEach { file ->
                val ok = runCatching { file.delete() }.getOrDefault(false)
                if (ok || !file.exists()) deleted++ else failed += file.name
            }
        return RecordingFileCleanupResult(deleted, failed)
    }

    fun cleanupOrphans(knownFileNames: Set<String>, deleteUnknown: Boolean = false): RecordingFileCleanupResult {
        val dir = directory ?: return RecordingFileCleanupResult()
        if (!dir.exists()) return RecordingFileCleanupResult()
        val known = knownFileNames.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        var deleted = 0
        val failed = mutableListOf<String>()
        dir.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .filter { it.length() == 0L || (deleteUnknown && it.name.lowercase() !in known) }
            .forEach { file ->
                val ok = runCatching { file.delete() }.getOrDefault(false)
                if (ok || !file.exists()) deleted++ else failed += file.name
            }
        return RecordingFileCleanupResult(deleted, failed)
    }

    companion object {
        fun disabled(): RecordingFileManager = RecordingFileManager(null)

        private fun fileForRecord(dir: File, record: ClassroomRecordingRecord): File? {
            val ref = record.artifactPath?.takeIf { it.isNotBlank() } ?: record.artifactFileName?.takeIf { it.isNotBlank() }
            if (ref.isNullOrBlank()) return null
            val candidate = File(ref)
            val file = if (candidate.isAbsolute) candidate else File(dir, ref)
            val rootPath = dir.canonicalFile.toPath()
            val filePath = file.canonicalFile.toPath()
            return if (filePath.startsWith(rootPath)) file else null
        }

        private fun File.canonicalOrAbsolute(): String =
            runCatching { canonicalPath }.getOrDefault(absolutePath)
    }
}
