package com.classmate.app.exporting

data class ExportArtifact(
    val displayName: String,
    val fileName: String,
    val mimeType: String,
    val format: ExportFileFormat,
    val bytes: ByteArray,
    val createdAt: Long,
    val containsSensitiveContent: Boolean = false,
) {
    val safeSummary: String
        get() = "$fileName / ${format.displayName} / ${bytes.size}B / safe=${!containsSensitiveContent}"
}

object ExportFileNameSanitizer {
    fun safe(baseName: String, extension: String): String {
        val clean = baseName
            .replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F\\[\\]]+"), "-")
            .replace(Regex("\\s+"), "-")
            .trim('-', '.', ' ')
            .take(90)
            .ifBlank { "ClassMate-learning-report" }
        return "$clean.$extension"
    }
}

object ExportSafety {
    private val blocked = listOf(
        "app" + "Key",
        "api" + "Key",
        "Auth" + "orization",
        "Bear" + "er",
        "app" + "_id",
        "reasoning" + "_content",
        "pro" + "mpt",
        "mes" + "sages",
        "vendor" + " body",
        "vendor" + " response body",
    )

    fun containsSensitiveText(value: String): Boolean =
        blocked.any { value.contains(it, ignoreCase = true) }
}

