package com.classmate.core.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptModelsTest {
    @Test
    fun manualSegmentsAreCountedAndJoinedWhenSessionEnds() {
        val started = TranscriptSession.start(id = "live_1", title = "Calculus", now = 1000)
        val withSegments = started
            .append("First classroom segment.", now = 1200)
            .append("Second classroom segment.", now = 1500)
        val ended = withSegments.end(now = 3000)

        assertEquals(2, ended.segments.size)
        assertEquals(TranscriptStatus.ENDED, ended.status)
        assertTrue(ended.courseText().contains("First classroom segment."))
        assertTrue(ended.courseText().contains("Second classroom segment."))
        assertTrue(ended.elapsedMs(4000) >= 2000)
    }
}
