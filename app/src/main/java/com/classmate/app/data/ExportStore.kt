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

interface ExportStore {
    fun save(document: ExportDocument): ExportReceipt
}

class InMemoryExportStore : ExportStore {
    val saved = mutableListOf<ExportDocument>()

    override fun save(document: ExportDocument): ExportReceipt {
        saved += document
        return ExportReceipt(document.fileName, document.mimeType, document.length, "memory/${document.fileName}")
    }
}

class FileExportStore(private val directory: File) : ExportStore {
    override fun save(document: ExportDocument): ExportReceipt {
        if (!directory.exists()) directory.mkdirs()
        File(directory, document.fileName).writeText(document.content, Charsets.UTF_8)
        return ExportReceipt(document.fileName, document.mimeType, document.length, "exports/${document.fileName}")
    }
}
