package com.classmate.core.transcript

import com.classmate.core.material.LessonMaterialFusionEngine
import com.classmate.core.material.MaterialSourceType
import com.classmate.core.material.SpeakerLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptMaterialTest {

    private fun audioDraft() = TranscriptDraft(
        id = "transcript_1",
        sourceType = TranscriptSourceType.AUDIO_TRANSCRIPT,
        title = "录音转写",
        segments = listOf(
            TranscriptSegmentDraft(id = "seg_1", startMs = 65_000, endMs = 72_000, speaker = SpeakerLabel.TEACHER, text = "磁通量的定义。"),
            TranscriptSegmentDraft(id = "seg_2", speaker = SpeakerLabel.UNKNOWN, text = "感应电动势公式。"),
        ),
    )

    @Test
    fun draftMapsToTranscriptMaterialPreservingSpeakerAndTime() {
        val source = TranscriptMaterialAdapter.toMaterialSource(audioDraft())
        assertEquals(MaterialSourceType.TRANSCRIPT, source.type)
        assertEquals(2, source.segments.size)

        val first = source.segments[0]
        assertEquals(SpeakerLabel.TEACHER, first.speaker)
        assertNotNull(first.timeRange)
        assertEquals(65_000L, first.timeRange!!.startMs)
        assertEquals(72_000L, first.timeRange!!.endMs)
        assertEquals("音频转写", first.sourceLabel)
        assertEquals(65_000L, first.evidence.timeRange!!.startMs)
        assertEquals(first.text, first.evidence.quote) // evidence locates to transcript text
    }

    @Test
    fun plainTextMarkerCarriesClockAndSpeaker() {
        val bundle = LessonMaterialFusionEngine.fuse(
            id = "bundle_1",
            courseTitle = "电磁感应",
            sources = listOf(TranscriptMaterialAdapter.toMaterialSource(audioDraft())),
        )
        val plain = bundle.plainText()
        assertTrue(plain.contains("[音频转写 00:01:05-00:01:12 · 教师]"))
        assertTrue(plain.contains("磁通量的定义。"))
        // no-time, unknown-speaker segment still gets a label + speaker marker
        assertTrue(plain.contains("[音频转写 · 未知]"))
    }

    @Test
    fun pastedTranscriptUsesManualLabel() {
        val draft = TranscriptDraft(
            id = "t2",
            sourceType = TranscriptSourceType.PASTED_TRANSCRIPT,
            segments = listOf(TranscriptSegmentDraft(id = "s1", speaker = SpeakerLabel.STUDENT, text = "学生提问内容。")),
        )
        val bundle = LessonMaterialFusionEngine.fuse("b", "课", listOf(TranscriptMaterialAdapter.toMaterialSource(draft)))
        assertTrue(bundle.plainText().contains("[手动转写 · 学生]"))
    }

    @Test
    fun fusedTranscriptOutputHasNoForbiddenTokens() {
        val bundle = LessonMaterialFusionEngine.fuse("b", "课", listOf(TranscriptMaterialAdapter.toMaterialSource(audioDraft())))
        val plain = bundle.plainText()
        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content", "messages").forEach {
            assertFalse(plain.contains(it, ignoreCase = true))
        }
    }
}
