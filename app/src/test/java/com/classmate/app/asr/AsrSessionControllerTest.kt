package com.classmate.app.asr

import com.classmate.core.material.SpeakerLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsrSessionControllerTest {

    private val base = AsrSession()

    @Test
    fun beginUnsupportedWhenRecognizerUnavailable() {
        val s = AsrSessionController.begin(base, available = false, permissionGranted = true, now = 1_000)
        assertEquals(AsrState.UNSUPPORTED, s.state)
        assertTrue(s.segments.isEmpty())
    }

    @Test
    fun beginRequiresPermissionWhenDenied() {
        val s = AsrSessionController.begin(base, available = true, permissionGranted = false, now = 1_000)
        assertEquals(AsrState.PERMISSION_REQUIRED, s.state)
    }

    @Test
    fun beginListensWhenAvailableAndGranted() {
        val s = AsrSessionController.begin(base, available = true, permissionGranted = true, now = 1_000)
        assertEquals(AsrState.LISTENING, s.state)
        assertEquals(1_000L, s.startedAtMs)
    }

    @Test
    fun partialDoesNotBecomeSegment() {
        val listening = AsrSessionController.begin(base, true, true, 1_000)
        val s = AsrSessionController.onPartial(listening, "正在识别的临时文本")
        assertEquals("正在识别的临时文本", s.partialText)
        assertTrue(s.segments.isEmpty()) // partial never enters final segments
    }

    @Test
    fun finalResultBecomesSegmentWithSpeakerAndApproximateTime() {
        val listening = AsrSessionController.begin(base, true, true, now = 1_000)
        val withPartial = AsrSessionController.onPartial(listening, "临时")
        val s = AsrSessionController.onFinal(withPartial, "法拉第电磁感应定律", confidence = 0.9, speaker = SpeakerLabel.TEACHER, now = 4_000)

        assertEquals(1, s.segments.size)
        val seg = s.segments.first()
        assertEquals("法拉第电磁感应定律", seg.text)
        assertEquals(SpeakerLabel.TEACHER, seg.speaker)
        assertEquals(0L, seg.startMs)
        assertEquals(3_000L, seg.endMs) // approximate elapsed since start
        assertEquals(0.9, seg.confidence!!, 1e-6)
        assertEquals("", s.partialText) // partial cleared after final

        val s2 = AsrSessionController.onFinal(s, "楞次定律决定方向", confidence = null, speaker = SpeakerLabel.STUDENT, now = 7_000)
        assertEquals(2, s2.segments.size)
        assertEquals(3_000L, s2.segments[1].startMs) // monotonic, continues from previous end
        assertNull(s2.segments[1].confidence)
    }

    @Test
    fun blankFinalDoesNotAddSegment() {
        val listening = AsrSessionController.begin(base, true, true, 1_000)
        val s = AsrSessionController.onFinal(listening, "   ", null, SpeakerLabel.UNKNOWN, 2_000)
        assertTrue(s.segments.isEmpty())
    }

    @Test
    fun errorKeepsConfirmedSegments() {
        val listening = AsrSessionController.begin(base, true, true, 1_000)
        val withSeg = AsrSessionController.onFinal(listening, "已确认片段", null, SpeakerLabel.UNKNOWN, 2_000)
        val s = AsrSessionController.onError(withSeg, "系统语音识别出错")
        assertEquals(AsrState.ERROR, s.state)
        assertEquals(1, s.segments.size) // nothing lost on error
        assertEquals("系统语音识别出错", s.errorMessage)
    }

    @Test
    fun stopKeepsSegments() {
        val listening = AsrSessionController.begin(base, true, true, 1_000)
        val withSeg = AsrSessionController.onFinal(listening, "片段", null, SpeakerLabel.UNKNOWN, 2_000)
        val s = AsrSessionController.stop(withSeg, 3_000)
        assertEquals(AsrState.IDLE, s.state)
        assertEquals(1, s.segments.size)
    }

    @Test
    fun pauseThenResumeReturnsToListening() {
        val listening = AsrSessionController.begin(base, true, true, 1_000)
        val paused = AsrSessionController.pause(listening, 2_000)
        assertEquals(AsrState.PAUSED, paused.state)
        val resumed = AsrSessionController.resume(paused, 5_000)
        assertEquals(AsrState.LISTENING, resumed.state)
    }

    @Test
    fun finalWhenNotActiveIsIgnored() {
        // No segment is created if we are not listening (e.g. before begin).
        val s = AsrSessionController.onFinal(base, "text", null, SpeakerLabel.TEACHER, 1_000)
        assertTrue(s.segments.isEmpty())
        assertNotNull(s) // does not throw
    }
}
