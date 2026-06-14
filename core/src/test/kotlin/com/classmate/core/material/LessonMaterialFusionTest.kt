package com.classmate.core.material

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonMaterialFusionTest {

    private val now = 1_700_000_000_000L

    private fun transcriptSource(): MaterialSource = MaterialSource.fromTranscript(
        id = "live_1",
        title = "电磁感应",
        segments = listOf(
            TranscriptSegment(id = "t1", index = 1, text = "法拉第电磁感应定律。", timeRange = AudioTimeRange(3200, 3450)),
            TranscriptSegment(id = "t2", index = 2, text = "楞次定律决定感应电流方向。"),
        ),
    )

    private fun slideSource(): MaterialSource = MaterialSource.fromOcr(
        ManualOcrProvider().recognizeManual(
            id = "ocr_1",
            title = "课件",
            sourceType = MaterialSourceType.SLIDE_OCR,
            pageTexts = listOf("第一页：磁通量定义。", "第二页：感应电动势公式。"),
            now = now,
        ),
    )

    private fun noteSource(): MaterialSource = MaterialSource.fromManualNote("note_1", "老师强调右手定则。", now)

    private fun bundle(): LessonMaterialBundle = LessonMaterialFusionEngine.fuse(
        id = "bundle_1",
        courseTitle = "大学物理 - 电磁感应",
        sources = listOf(transcriptSource(), slideSource(), noteSource()),
        subject = "physics",
        now = now,
    )

    @Test
    fun fusionPreservesSourceTypeAndSourceId() {
        val b = bundle()
        assertEquals(listOf("live_1", "ocr_1", "note_1"), b.sources.map { it.id }) // stable order
        assertEquals(MaterialSourceType.TRANSCRIPT, b.sources[0].type)
        assertEquals(MaterialSourceType.SLIDE_OCR, b.sources[1].type)
        assertEquals(MaterialSourceType.MANUAL_NOTE, b.sources[2].type)
        // every fused segment knows its origin
        b.allSegments().forEach { seg ->
            assertEquals(seg.sourceType, seg.evidence.sourceType)
            assertEquals(seg.sourceId, seg.evidence.sourceId)
        }
        assertEquals(b.allSegments().size, b.evidenceRefs().size)
    }

    @Test
    fun transcriptSlideOcrAndManualNoteMergeIntoPlainText() {
        val text = bundle().plainText()
        assertTrue(text.contains("法拉第电磁感应定律。"))
        assertTrue(text.contains("感应电动势公式。"))
        assertTrue(text.contains("老师强调右手定则。"))
        // safe source markers present
        assertTrue(text.contains("[转写片段 1]"))
        assertTrue(text.contains("[课件 OCR 第 2 页]"))
        assertTrue(text.contains("[手动笔记]"))
    }

    @Test
    fun speakerLabelDefaultsToUnknown() {
        val seg = bundle().sources.first().segments.first()
        assertEquals(SpeakerLabel.UNKNOWN, seg.speaker)
        assertEquals(SpeakerLabel.UNKNOWN, ManualTranscriptProvider().transcribeManual(listOf("x")).first().speaker)
    }

    @Test
    fun sentenceLevelSyncKeepsStartAndEndTime() {
        val seg = bundle().sources.first().segments.first { it.index == 1 }
        assertNotNull(seg.timeRange)
        assertEquals(3200L, seg.timeRange!!.startMs)
        assertEquals(3450L, seg.timeRange!!.endMs)
        assertEquals(3200L, seg.evidence.timeRange!!.startMs)
        // No fabricated word-level sync tokens in the manual foundation.
        assertTrue(ManualTranscriptProvider().transcribeManual(listOf("a")).first().syncTokens.isEmpty())
    }

    @Test
    fun noProviderDoesNotClaimRealAsrOrOcr() {
        assertFalse(ManualTranscriptProvider().producesRealAsr)
        assertFalse(NoOpTranscriptionProvider().producesRealAsr)
        assertFalse(ManualOcrProvider().producesRealOcr)
        assertFalse(NoOpOcrProvider().producesRealOcr)
        assertTrue(NoOpTranscriptionProvider().transcribeManual(listOf("x")).isEmpty())
        assertTrue(NoOpOcrProvider().recognizeManual("i", "t", MaterialSourceType.SLIDE_OCR, listOf("x")).pages.isEmpty())
    }

    @Test
    fun noNetworkOrVideoCrawlingClaims() {
        val notes = listOf(
            ManualTranscriptProvider().capabilityNote,
            ManualOcrProvider().capabilityNote,
            NoOpTranscriptionProvider().capabilityNote,
            NoOpOcrProvider().capabilityNote,
        )
        notes.forEach { note ->
            listOf("爬取", "抓取", "下载字幕", "crawl", "download", "已接入真实").forEach { banned ->
                assertFalse("capability note must not claim '$banned'", note.contains(banned, ignoreCase = true))
            }
        }
    }

    @Test
    fun serializedAndPlainTextContainNoForbiddenTokens() {
        val b = bundle()
        val serialized = Json.encodeToString(LessonMaterialBundle.serializer(), b)
        val combined = serialized + "\n" + b.plainText()
        listOf("Authorization", "Bearer", "appKey", "apiKey", "app_id", "reasoning_content", "messages").forEach {
            assertFalse("fused output must not contain $it", combined.contains(it, ignoreCase = true))
        }
        // round-trips back to an equal bundle
        assertEquals(b, Json.decodeFromString(LessonMaterialBundle.serializer(), serialized))
    }

    @Test
    fun emptySourcesFailSafelyWithWarning() {
        val empty = LessonMaterialFusionEngine.fuse("b", "课", emptyList())
        assertTrue(empty.sources.isEmpty())
        assertTrue(empty.plainText().isBlank())
        assertTrue(empty.fusionWarnings.isNotEmpty())
    }
}
