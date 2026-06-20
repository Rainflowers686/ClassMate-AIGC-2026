package com.classmate.app.l3

import com.classmate.core.importing.FileImportText
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.zip.ZipInputStream

enum class InputFileKind {
    TXT,
    MARKDOWN,
    CSV,
    DOCX,
    XLSX,
    PPTX,
    PDF,
    IMAGE,
    AUDIO,
    VIDEO,
    UNKNOWN,
}

enum class InputArtifactStatus {
    READY,
    BEST_EFFORT,
    ARTIFACT_ONLY,
    PARSER_PENDING,
    TEMPLATE_REQUIRED,
    UNSUPPORTED_FORMAT,
    EMPTY_FILE,
    FORMAT_ERROR,
    READ_FAILED,
    OCR_READY_SEAM,
    CAMERA_PENDING,
    ASR_NOT_CONFIGURED,
    PENDING_ASR_CONFIG,
}

data class InputArtifact(
    val id: String,
    val fileName: String,
    val kind: InputFileKind,
    val status: InputArtifactStatus,
    val extractedText: String = "",
    val message: String,
    val createdAt: Long,
    val sizeBytes: Long? = null,
)

object InputSuperhub {
    fun parseFile(
        bytes: ByteArray,
        fileName: String,
        mimeType: String = "",
        now: Long = System.currentTimeMillis(),
    ): InputArtifact {
        val kind = detectKind(fileName, mimeType)
        if (bytes.isEmpty()) {
            return artifact(fileName, kind, InputArtifactStatus.EMPTY_FILE, "文件为空。", now, bytes.size.toLong())
        }
        return runCatching {
            when (kind) {
                InputFileKind.TXT, InputFileKind.MARKDOWN, InputFileKind.CSV -> parseTextLike(bytes, fileName, kind, now)
                InputFileKind.DOCX -> parseDocx(bytes, fileName, now)
                InputFileKind.XLSX -> parseXlsx(bytes, fileName, now)
                InputFileKind.PPTX -> parsePptx(bytes, fileName, now)
                InputFileKind.PDF -> artifact(fileName, kind, InputArtifactStatus.PARSER_PENDING, "PDF 已记录为文件 artifact；请粘贴可用文字或 OCR 文本继续。", now, bytes.size.toLong())
                InputFileKind.IMAGE -> artifact(fileName, kind, InputArtifactStatus.OCR_READY_SEAM, "图片已记录；官方 OCR 按配置启用，未配置时可粘贴识别文字。", now, bytes.size.toLong())
                InputFileKind.AUDIO -> artifact(fileName, kind, InputArtifactStatus.ASR_NOT_CONFIGURED, "音频已记录；ASR Long 未配置时使用手动转写 fallback。", now, bytes.size.toLong())
                InputFileKind.VIDEO -> artifact(fileName, kind, InputArtifactStatus.ARTIFACT_ONLY, "视频已记录为 artifact；当前不抽帧、不爬取、不上传。", now, bytes.size.toLong())
                InputFileKind.UNKNOWN -> artifact(fileName, kind, InputArtifactStatus.UNSUPPORTED_FORMAT, "暂不支持该文件类型，请转为 TXT/MD/CSV 或粘贴文本。", now, bytes.size.toLong())
            }
        }.getOrElse {
            artifact(fileName, kind, InputArtifactStatus.READ_FAILED, "文件读取失败，请改用文本粘贴或模板导入。", now, bytes.size.toLong())
        }
    }

    fun detectKind(fileName: String, mimeType: String = ""): InputFileKind {
        val name = fileName.lowercase(Locale.US)
        val mime = mimeType.lowercase(Locale.US)
        return when {
            name.endsWith(".txt") || mime == "text/plain" -> InputFileKind.TXT
            name.endsWith(".md") || name.endsWith(".markdown") || mime.contains("markdown") -> InputFileKind.MARKDOWN
            name.endsWith(".csv") || mime.contains("csv") -> InputFileKind.CSV
            name.endsWith(".docx") || mime.contains("wordprocessingml") -> InputFileKind.DOCX
            name.endsWith(".xlsx") || mime.contains("spreadsheetml") -> InputFileKind.XLSX
            name.endsWith(".pptx") || mime.contains("presentationml") -> InputFileKind.PPTX
            name.endsWith(".pdf") || mime == "application/pdf" -> InputFileKind.PDF
            mime.startsWith("image/") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") -> InputFileKind.IMAGE
            mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".wav") -> InputFileKind.AUDIO
            mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mov") -> InputFileKind.VIDEO
            else -> InputFileKind.UNKNOWN
        }
    }

    private fun parseTextLike(bytes: ByteArray, fileName: String, kind: InputFileKind, now: Long): InputArtifact {
        val result = FileImportText.fromBytes(bytes, fileName)
        val status = if (result.accepted) InputArtifactStatus.READY else InputArtifactStatus.FORMAT_ERROR
        return artifact(fileName, kind, status, result.message, now, bytes.size.toLong(), result.text)
    }

    private fun parseDocx(bytes: ByteArray, fileName: String, now: Long): InputArtifact {
        val document = zipEntries(bytes)
            .filterKeys { it == "word/document.xml" || it.startsWith("word/header") || it.startsWith("word/footer") }
            .values
            .joinToString("\n")
        val text = xmlText(document)
        val status = if (text.isBlank()) InputArtifactStatus.TEMPLATE_REQUIRED else InputArtifactStatus.BEST_EFFORT
        val message = if (text.isBlank()) "DOCX 未抽取到文字，请使用题库模板或复制为文本。" else "DOCX 已 best-effort 抽取文字，可进入课堂材料或题库解析。"
        return artifact(fileName, InputFileKind.DOCX, status, message, now, bytes.size.toLong(), text)
    }

    private fun parsePptx(bytes: ByteArray, fileName: String, now: Long): InputArtifact {
        val slides = zipEntries(bytes)
            .filterKeys { it.startsWith("ppt/slides/slide") && it.endsWith(".xml") }
            .toSortedMap()
            .values
            .joinToString("\n")
        val text = xmlText(slides)
        val status = if (text.isBlank()) InputArtifactStatus.PARSER_PENDING else InputArtifactStatus.BEST_EFFORT
        val message = if (text.isBlank()) "PPTX 已记录；复杂课件解析仍为 PARSER_PENDING。" else "PPTX 已 best-effort 抽取幻灯片文字。"
        return artifact(fileName, InputFileKind.PPTX, status, message, now, bytes.size.toLong(), text)
    }

    private fun parseXlsx(bytes: ByteArray, fileName: String, now: Long): InputArtifact {
        val entries = zipEntries(bytes)
        val shared = Regex("""<t[^>]*>(.*?)</t>""").findAll(entries["xl/sharedStrings.xml"].orEmpty())
            .map { xmlUnescape(it.groupValues[1]) }
            .toList()
        val rows = entries
            .filterKeys { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .toSortedMap()
            .values
            .flatMap { sheetRows(it, shared) }
        val text = rows.joinToString("\n") { it.joinToString(",") { cell -> cell.replace(",", "，") } }
        val status = if (text.isBlank()) InputArtifactStatus.TEMPLATE_REQUIRED else InputArtifactStatus.BEST_EFFORT
        val message = if (text.isBlank()) "XLSX 未读取到题库表格，请使用 stem,a,b,c,d,answer,explanation 模板。" else "XLSX 已 best-effort 转为 CSV 题库文本。"
        return artifact(fileName, InputFileKind.XLSX, status, message, now, bytes.size.toLong(), text)
    }

    private fun sheetRows(xml: String, shared: List<String>): List<List<String>> =
        Regex("""<row[^>]*>(.*?)</row>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { row ->
            Regex("""<c([^>]*)>(.*?)</c>""", RegexOption.DOT_MATCHES_ALL).findAll(row.groupValues[1]).map { cell ->
                val attrs = cell.groupValues[1]
                val body = cell.groupValues[2]
                val raw = Regex("""<v[^>]*>(.*?)</v>""").find(body)?.groupValues?.getOrNull(1).orEmpty()
                if (attrs.contains("""t="s"""")) {
                    shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
                } else {
                    xmlUnescape(raw)
                }
            }.toList()
        }.filter { it.any(String::isNotBlank) }.toList()

    private fun zipEntries(bytes: ByteArray): Map<String, String> {
        val out = linkedMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    out[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        return out
    }

    private fun xmlText(xml: String): String =
        Regex("""<[^>]+>""")
            .replace(xml.replace(Regex("""</(w:p|a:p|p|row)>"""), "\n"), " ")
            .split(Regex("""\s+"""))
            .joinToString(" ")
            .let(::xmlUnescape)
            .trim()

    private fun xmlUnescape(value: String): String =
        value.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")

    private fun artifact(
        fileName: String,
        kind: InputFileKind,
        status: InputArtifactStatus,
        message: String,
        now: Long,
        sizeBytes: Long? = null,
        text: String = "",
    ): InputArtifact =
        InputArtifact(
            id = "input_${now}_${fileName.hashCode().toUInt()}",
            fileName = fileName.ifBlank { "unnamed" },
            kind = kind,
            status = status,
            extractedText = text.trim(),
            message = message,
            createdAt = now,
            sizeBytes = sizeBytes,
        )
}
