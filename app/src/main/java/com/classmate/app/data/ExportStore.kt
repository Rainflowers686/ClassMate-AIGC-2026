package com.classmate.app.data

import com.classmate.core.exporting.ExportDocument
import java.io.File

enum class ExportActionStatus {
    SAVED_AS,
    SAVED_DOWNLOADS,
    SHARED,
    INTERNAL_ONLY,
    FAILED,
    CANCELED,
}

data class ExportReceipt(
    val fileName: String,
    val mimeType: String,
    val length: Int,
    val pathSummary: String,
    val format: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastAction: ExportActionStatus = ExportActionStatus.INTERNAL_ONLY,
    val message: String = "",
)

data class ExportCleanupResult(
    val deletedCount: Int = 0,
    val failedFiles: List<String> = emptyList(),
) {
    val success: Boolean get() = failedFiles.isEmpty()
}

interface ExportStore {
    fun save(document: ExportDocument): ExportReceipt
    fun deleteForCourse(courseTitles: Set<String>, sessionIds: Set<String>): ExportCleanupResult =
        ExportCleanupResult()
}

class InMemoryExportStore : ExportStore {
    val saved = mutableListOf<ExportDocument>()

    override fun save(document: ExportDocument): ExportReceipt {
        saved += document
        return ExportReceipt(document.fileName, document.mimeType, document.length, "memory/${document.fileName}")
    }

    override fun deleteForCourse(courseTitles: Set<String>, sessionIds: Set<String>): ExportCleanupResult {
        val before = saved.size
        saved.removeAll { document -> matchesCourse(document.fileName, courseTitles, sessionIds) }
        return ExportCleanupResult(deletedCount = before - saved.size)
    }
}

class FileExportStore(private val directory: File) : ExportStore {
    override fun save(document: ExportDocument): ExportReceipt {
        if (!directory.exists()) directory.mkdirs()
        File(directory, document.fileName).writeText(document.content, Charsets.UTF_8)
        return ExportReceipt(document.fileName, document.mimeType, document.length, "exports/${document.fileName}")
    }

    override fun deleteForCourse(courseTitles: Set<String>, sessionIds: Set<String>): ExportCleanupResult {
        if (!directory.exists()) return ExportCleanupResult()
        var deleted = 0
        val failed = mutableListOf<String>()
        directory.listFiles()
            .orEmpty()
            .filter { it.isFile && matchesCourse(it.name, courseTitles, sessionIds) }
            .forEach { file ->
                val ok = runCatching { file.delete() }.getOrDefault(false)
                if (ok || !file.exists()) deleted++ else failed += file.name
            }
        return ExportCleanupResult(deleted, failed)
    }
}

private fun matchesCourse(fileName: String, courseTitles: Set<String>, sessionIds: Set<String>): Boolean {
    val name = normalize(fileName)
    val titleTokens = courseTitles
        .flatMap { title -> setOf(normalize(title), normalize(title).replace(" ", "-"), normalize(title).replace(" ", "_")) }
        .filter { it.length >= 2 }
    val sessionTokens = sessionIds.map(::normalize).filter { it.length >= 2 }
    return (titleTokens + sessionTokens).any { token -> token.isNotBlank() && name.contains(token) }
}

private fun normalize(value: String): String =
    value.trim().replace('\\', '/').lowercase()
