package com.classmate.core.transcript

import com.classmate.core.material.SpeakerLabel

/**
 * Lightweight, offline parser for user-supplied subtitle/transcript TEXT. It NEVER touches a network
 * and NEVER decodes audio/video — it only reads text the user pasted or read from a local
 * .srt/.vtt/.txt they have the right to use. Malformed input degrades to warnings, never a crash.
 */
object TranscriptParser {

    // Finds the first clock token inside a side of a "-->" line (tolerates trailing cue settings).
    private val CLOCK_FIND = Regex("""\d{1,2}:\d{2}(?::\d{2})?(?:[.,]\d{1,3})?""")

    // Optional leading timecode for plain transcript lines, e.g. "[00:01:05] 老师：..." or "01:05 ...".
    private val LEADING_TIME = Regex("""^\s*\[?\s*(\d{1,2}:\d{2}(?::\d{2})?(?:[.,]\d{1,3})?)\s*]?\s*""")

    private val TEACHER_PREFIX = Regex("""^\s*(老师|教师|主讲|讲师|teacher)\s*[:：]\s*""", RegexOption.IGNORE_CASE)
    private val STUDENT_PREFIX = Regex("""^\s*(学生|同学|学员|student)\s*[:：]\s*""", RegexOption.IGNORE_CASE)

    /** Dispatch by the user's chosen source type; SRT/VTT use cue parsing, others fall back to text. */
    fun parse(text: String, sourceType: TranscriptSourceType): TranscriptParseResult = when (sourceType) {
        TranscriptSourceType.SRT_FILE, TranscriptSourceType.VTT_FILE -> parseCues(text, sourceType)
        else -> if (text.contains("-->")) parseCues(text, sourceType) else parseTxt(text, sourceType)
    }

    fun parseSrt(text: String): TranscriptParseResult = parseCues(text, TranscriptSourceType.SRT_FILE)

    fun parseVtt(text: String): TranscriptParseResult = parseCues(text, TranscriptSourceType.VTT_FILE)

    /** Best-effort source-type guess for a pasted blob (the UI still lets the user override it). */
    fun autoDetect(text: String): TranscriptSourceType {
        val head = text.trimStart()
        return when {
            head.startsWith("WEBVTT") -> TranscriptSourceType.VTT_FILE
            text.contains("-->") -> TranscriptSourceType.SRT_FILE
            else -> TranscriptSourceType.PASTED_TRANSCRIPT
        }
    }

    /** SRT / VTT cue blocks: `idx\n start --> end\n text...`, blank-line separated. */
    private fun parseCues(raw: String, sourceType: TranscriptSourceType): TranscriptParseResult {
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isBlank()) {
            return TranscriptParseResult(sourceType, emptyList(), listOf("转写/字幕内容为空。"))
        }
        val blocks = normalized.split(Regex("\n[ \t]*\n")).map { it.trim() }.filter { it.isNotEmpty() }
        val segments = ArrayList<TranscriptSegmentDraft>()
        val warnings = ArrayList<String>()
        var malformedTiming = false
        var plainKept = false

        blocks.forEach { block ->
            val lines = block.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) return@forEach
            val first = lines.first()
            if (first.equals("WEBVTT", true) || first.startsWith("WEBVTT")) return@forEach // header
            if (first.startsWith("NOTE")) return@forEach // VTT comment block

            val timingIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingIndex < 0) {
                // No timecode in this block: keep its text rather than dropping it.
                val (speaker, body) = splitSpeaker(lines.joinToString(" "))
                if (body.isNotBlank()) {
                    plainKept = true
                    segments += segment(segments.size + 1, null, null, speaker, body, lines.size)
                }
                return@forEach
            }

            val sides = lines[timingIndex].split("-->")
            val startMs = firstClockMs(sides.getOrElse(0) { "" })
            val endMs = firstClockMs(sides.getOrElse(1) { "" })
            if (startMs == null) malformedTiming = true

            val body = lines.filterIndexed { i, _ -> i > timingIndex }.joinToString(" ").trim()
            if (body.isBlank()) return@forEach
            val (speaker, clean) = splitSpeaker(body)
            segments += segment(segments.size + 1, startMs, endMs, speaker, clean, timingIndex + 1)
        }

        if (malformedTiming) warnings += "部分字幕块的时间轴无法解析，已保留文本但缺少时间戳。"
        if (plainKept) warnings += "部分字幕块缺少时间轴，已按纯文本片段保留。"
        if (segments.isEmpty()) warnings += "未解析出任何字幕段落，请检查 SRT/VTT 格式。"
        return TranscriptParseResult(sourceType, segments, warnings)
    }

    /** Plain transcript: each non-blank line is a segment; an optional leading clock becomes startMs. */
    private fun parseTxt(raw: String, sourceType: TranscriptSourceType): TranscriptParseResult {
        val normalized = raw.replace("\r\n", "\n").replace("\r", "\n")
        if (normalized.isBlank()) {
            return TranscriptParseResult(sourceType, emptyList(), listOf("转写内容为空。"))
        }
        val segments = ArrayList<TranscriptSegmentDraft>()
        normalized.split("\n").forEachIndexed { lineNo, lineRaw ->
            val line = lineRaw.trim()
            if (line.isEmpty()) return@forEachIndexed
            var rest = line
            var startMs: Long? = null
            LEADING_TIME.find(line)?.let { m ->
                startMs = TranscriptClock.parseMs(m.groupValues[1])
                if (startMs != null) rest = line.substring(m.range.last + 1)
            }
            val (speaker, body) = splitSpeaker(rest)
            if (body.isNotBlank()) {
                segments += segment(segments.size + 1, startMs, null, speaker, body, lineNo + 1)
            }
        }
        val warnings = if (segments.isEmpty()) listOf("未解析出任何转写段落。") else emptyList()
        return TranscriptParseResult(sourceType, segments, warnings)
    }

    /** Strip a leading speaker prefix (老师 / 教师 / 学生 / 同学 / Teacher / Student) → label + text. */
    fun splitSpeaker(text: String): Pair<SpeakerLabel, String> {
        TEACHER_PREFIX.find(text)?.let { return SpeakerLabel.TEACHER to text.substring(it.range.last + 1).trim() }
        STUDENT_PREFIX.find(text)?.let { return SpeakerLabel.STUDENT to text.substring(it.range.last + 1).trim() }
        return SpeakerLabel.UNKNOWN to text.trim()
    }

    private fun firstClockMs(side: String): Long? =
        CLOCK_FIND.find(side)?.value?.let { TranscriptClock.parseMs(it) }

    private fun segment(index: Int, startMs: Long?, endMs: Long?, speaker: SpeakerLabel, text: String, sourceLine: Int) =
        TranscriptSegmentDraft(
            id = "seg_$index",
            startMs = startMs,
            endMs = endMs,
            speaker = speaker,
            text = text,
            sourceLine = sourceLine,
        )
}
