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
    val qualityReport: ExtractedTextQuality? = null,
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
                InputFileKind.PDF -> artifact(
                    fileName,
                    kind,
                    InputArtifactStatus.PARSER_PENDING,
                    "PDF 已记录为文件 artifact；原生文本解析仍为 PARSER_PENDING，请粘贴页文本或使用页面 OCR seam 继续。",
                    now,
                    bytes.size.toLong(),
                    qualityReport = ExtractedTextQuality(
                        textLength = 0,
                        nonWhitespaceCount = 0,
                        suspiciousCharRatio = 0.0,
                        replacementCharCount = 0,
                        lineCount = 0,
                        isLikelyReadable = false,
                        recommendedStatus = ExtractedTextRecommendedStatus.PARSER_PENDING,
                    ),
                )
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
        val quality = qualityFor(text)
        val status = statusForQuality(quality)
        val message = messageForQuality("DOCX", quality, "DOCX 已 best-effort 抽取文字，可进入课堂材料或题库解析。")
        return artifact(fileName, InputFileKind.DOCX, status, message, now, bytes.size.toLong(), text, quality)
    }

    private fun parsePptx(bytes: ByteArray, fileName: String, now: Long): InputArtifact {
        val slides = zipEntries(bytes)
            .filterKeys { it.startsWith("ppt/slides/slide") && it.endsWith(".xml") }
            .toSortedMap()
            .values
            .joinToString("\n")
        val text = xmlText(slides)
        val quality = qualityFor(text)
        val status = statusForQuality(quality)
        val message = messageForQuality("PPTX", quality, "PPTX 已 best-effort 抽取幻灯片文字。")
        return artifact(fileName, InputFileKind.PPTX, status, message, now, bytes.size.toLong(), text, quality)
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
        val quality = qualityFor(text)
        val status = statusForQuality(quality)
        val message = messageForQuality("XLSX", quality, "XLSX 已 best-effort 转为 CSV 题库文本。")
        return artifact(fileName, InputFileKind.XLSX, status, message, now, bytes.size.toLong(), text, quality)
    }

    fun qualityFor(text: String): ExtractedTextQuality {
        val length = text.length
        val nonWhitespace = text.count { !it.isWhitespace() }
        val replacement = text.count { it == '\uFFFD' }
        val suspicious = text.count { it.isSuspiciousExtractedChar() } + replacement
        val ratio = if (nonWhitespace == 0) 0.0 else suspicious.toDouble() / nonWhitespace
        val lines = text.lines().count { it.isNotBlank() }
        val recommended = when {
            nonWhitespace == 0 -> ExtractedTextRecommendedStatus.EMPTY_FILE
            ratio >= 0.18 || replacement >= 3 -> ExtractedTextRecommendedStatus.TEMPLATE_REQUIRED
            nonWhitespace < 12 -> ExtractedTextRecommendedStatus.PARTIAL
            else -> ExtractedTextRecommendedStatus.COMPLETE
        }
        return ExtractedTextQuality(
            textLength = length,
            nonWhitespaceCount = nonWhitespace,
            suspiciousCharRatio = ratio,
            replacementCharCount = replacement,
            lineCount = lines,
            isLikelyReadable = recommended == ExtractedTextRecommendedStatus.COMPLETE || recommended == ExtractedTextRecommendedStatus.PARTIAL,
            recommendedStatus = recommended,
        )
    }

    private fun Char.isSuspiciousExtractedChar(): Boolean {
        if (isLetterOrDigit() || isWhitespace()) return false
        if (code in 0x4E00..0x9FFF) return false
        return this !in setOf(
            '.', ',', ';', ':', '!', '?', '-', '_', '/', '\\', '(', ')', '[', ']', '{', '}',
            '"', '\'', '“', '”', '‘', '’', '。', '，', '；', '：', '！', '？', '、', '《', '》',
            '+', '=', '%', '#', '@', '&', '*',
        )
    }

    private fun statusForQuality(quality: ExtractedTextQuality): InputArtifactStatus =
        when (quality.recommendedStatus) {
            ExtractedTextRecommendedStatus.COMPLETE -> InputArtifactStatus.BEST_EFFORT
            ExtractedTextRecommendedStatus.PARTIAL -> InputArtifactStatus.BEST_EFFORT
            ExtractedTextRecommendedStatus.TEMPLATE_REQUIRED -> InputArtifactStatus.TEMPLATE_REQUIRED
            ExtractedTextRecommendedStatus.PARSER_PENDING -> InputArtifactStatus.PARSER_PENDING
            ExtractedTextRecommendedStatus.EMPTY_FILE -> InputArtifactStatus.EMPTY_FILE
            ExtractedTextRecommendedStatus.FORMAT_ERROR -> InputArtifactStatus.FORMAT_ERROR
        }

    private fun messageForQuality(kind: String, quality: ExtractedTextQuality, okMessage: String): String =
        when (quality.recommendedStatus) {
            ExtractedTextRecommendedStatus.COMPLETE -> okMessage
            ExtractedTextRecommendedStatus.PARTIAL -> "$kind 只抽取到少量文字，建议人工确认后再进入学习链路。"
            ExtractedTextRecommendedStatus.TEMPLATE_REQUIRED -> "$kind 抽取结果疑似乱码或复杂格式，请使用模板或复制为文本。"
            ExtractedTextRecommendedStatus.PARSER_PENDING -> "$kind 解析仍为 PARSER_PENDING，请使用 fallback。"
            ExtractedTextRecommendedStatus.EMPTY_FILE -> "$kind 未抽取到可用文字，请使用模板、OCR 或手动文本。"
            ExtractedTextRecommendedStatus.FORMAT_ERROR -> "$kind 格式错误，请检查文件后重试。"
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
        qualityReport: ExtractedTextQuality? = null,
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
            qualityReport = qualityReport,
        )
}
