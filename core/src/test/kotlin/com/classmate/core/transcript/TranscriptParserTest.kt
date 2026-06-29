package com.classmate.core.transcript

import com.classmate.core.material.SpeakerLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptParserTest {

    @Test
    fun srtParsesTimestampsTextAndSpeaker() {
        val srt = """
            1
            00:01:02,300 --> 00:01:05,800
            老师：今天讲电磁感应

            2
            00:01:06,000 --> 00:01:09,000
            学生：老师这个怎么理解
            多行第二句
        """.trimIndent()

        val result = TranscriptParser.parseSrt(srt)
        assertEquals(2, result.segments.size)

        val a = result.segments[0]
        assertEquals(62_300L, a.startMs)
        assertEquals(65_800L, a.endMs)
        assertEquals(SpeakerLabel.TEACHER, a.speaker)
        assertTrue(a.text.contains("今天讲电磁感应"))
        assertFalse(a.text.contains("老师："))

        val b = result.segments[1]
        assertEquals(66_000L, b.startMs)
        assertEquals(SpeakerLabel.STUDENT, b.speaker)
        assertTrue(b.text.contains("多行第二句")) // multi-line cue merged
    }

    @Test
    fun vttParsesHeaderTimestampsAndSpeaker() {
        val vtt = """
            WEBVTT

            00:00:01.000 --> 00:00:04.000
            Teacher: today we learn induction

            00:00:05.000 --> 00:00:07.500
            Student: a question
        """.trimIndent()

        val result = TranscriptParser.parseVtt(vtt)
        assertEquals(2, result.segments.size)
        assertEquals(1_000L, result.segments[0].startMs)
        assertEquals(4_000L, result.segments[0].endMs)
        assertEquals(SpeakerLabel.TEACHER, result.segments[0].speaker)
        assertEquals(SpeakerLabel.STUDENT, result.segments[1].speaker)
        assertTrue(result.segments[0].text.contains("today we learn induction"))
    }

    @Test
    fun txtParsesLeadingTimestampAndPlainLines() {
        val txt = """
            [00:01:05] 老师：开始上课
            普通段落没有时间戳
            学生：提问
        """.trimIndent()

        val result = TranscriptParser.parse(txt, TranscriptSourceType.PASTED_TRANSCRIPT)
        assertEquals(3, result.segments.size)
        assertEquals(65_000L, result.segments[0].startMs)
        assertEquals(SpeakerLabel.TEACHER, result.segments[0].speaker)
        assertEquals("开始上课", result.segments[0].text)
        assertNull(result.segments[1].startMs)
        assertEquals(SpeakerLabel.UNKNOWN, result.segments[1].speaker)
        assertEquals(SpeakerLabel.STUDENT, result.segments[2].speaker)
    }

    @Test
    fun speakerPrefixesMapToLabels() {
        listOf("老师：讲解", "教师: 讲解", "主讲：讲解").forEach {
            assertEquals(SpeakerLabel.TEACHER, TranscriptParser.splitSpeaker(it).first)
        }
        listOf("学生：提问", "同学: 提问", "Student: ask", "STUDENT：ask").forEach {
            assertEquals(SpeakerLabel.STUDENT, TranscriptParser.splitSpeaker(it).first)
        }
        assertEquals(SpeakerLabel.UNKNOWN, TranscriptParser.splitSpeaker("没有前缀").first)
        assertEquals("讲解", TranscriptParser.splitSpeaker("老师：讲解").second)
    }

    @Test
    fun malformedCueReturnsWarningNotCrash() {
        val bad = """
            1
            xx --> yy
            还是有文本的
        """.trimIndent()

        val result = TranscriptParser.parseSrt(bad)
        assertEquals(1, result.segments.size)
        assertNull(result.segments[0].startMs) // unparseable timing -> null, text kept
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun vttWithBomHeaderAndNoteParsesCueOnly() {
        val vtt = "\uFEFFWEBVTT\n\nNOTE this is an editor note\nit should be skipped\n\n00:00:02.000 --> 00:00:03.000\nTeacher: useful cue"

        val result = TranscriptParser.parseVtt(vtt)

        assertEquals(1, result.segments.size)
        assertEquals(2_000L, result.segments.single().startMs)
        assertTrue(result.segments.single().text.contains("useful cue"))
    }

    @Test
    fun autoDetectHandlesBomWebVttAndPlainText() {
        assertEquals(TranscriptSourceType.VTT_FILE, TranscriptParser.autoDetect("\uFEFFWEBVTT\n\n00:00:01.000 --> 00:00:02.000\ntext"))
        assertEquals(TranscriptSourceType.SRT_FILE, TranscriptParser.autoDetect("1\n00:00:01,000 --> 00:00:02,000\ntext"))
        assertEquals(TranscriptSourceType.PASTED_TRANSCRIPT, TranscriptParser.autoDetect("plain transcript line"))
    }

    @Test
    fun emptyVideoSubtitleDoesNotCreateTranscriptSegments() {
        val result = TranscriptParser.parse("", TranscriptSourceType.VIDEO_SUBTITLE)

        assertTrue(result.segments.isEmpty())
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun emptyInputReturnsWarning() {
        val result = TranscriptParser.parseSrt("   \n  ")
        assertTrue(result.segments.isEmpty())
        assertTrue(result.warnings.isNotEmpty())
    }
}
