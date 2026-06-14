package com.classmate.app.state

import com.classmate.app.platform.ConfigRepository
import com.classmate.core.live.TranscriptStatus
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression cover for the real-device bug: after ending a Live class the "generate timeline"
 * action must be enabled whenever there is content, including text typed into the segment field
 * but not yet "added". The async analyze path itself reuses the shared CourseAnalyzer pipeline
 * (already covered elsewhere) and is not driven here (it needs a Main dispatcher).
 */
class LiveTimelineFlowTest {

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-live").resolve("config.local.json").toFile()),
    )

    @Test
    fun twoAddedSegmentsEnableTimelineGeneration() {
        val viewModel = vm()
        viewModel.updateLiveTitle("电磁感应")
        viewModel.startLiveClass()
        viewModel.updateLiveSegment("第一段：法拉第电磁感应定律。")
        viewModel.appendLiveSegment()
        viewModel.updateLiveSegment("第二段：楞次定律与感应电流方向。")
        viewModel.appendLiveSegment()
        viewModel.endLiveClass()

        assertEquals(2, viewModel.ui.liveTranscript?.segments?.size)
        assertEquals(TranscriptStatus.ENDED, viewModel.ui.liveTranscript?.status)
        assertTrue(viewModel.canGenerateLiveTimeline())
        assertTrue(viewModel.ui.courseText.contains("法拉第"))
        assertTrue(viewModel.ui.courseText.contains("楞次定律"))
    }

    @Test
    fun typedButNotAddedDraftIsFlushedOnEndSoTimelineCanGenerate() {
        val viewModel = vm()
        viewModel.startLiveClass()
        // User types into the field but never taps "添加片段", then ends the class.
        viewModel.updateLiveSegment("只是输入但没有点添加的内容。")
        viewModel.endLiveClass()

        assertEquals(1, viewModel.ui.liveTranscript?.segments?.size)
        assertEquals("", viewModel.ui.liveSegmentDraft)
        assertTrue(viewModel.canGenerateLiveTimeline())
        assertTrue(viewModel.ui.courseText.contains("只是输入"))
    }

    @Test
    fun emptyLiveCannotGenerateAndKeepsTranscriptForRetry() {
        val viewModel = vm()
        viewModel.startLiveClass()
        viewModel.endLiveClass()

        assertFalse(viewModel.canGenerateLiveTimeline())
        viewModel.analyzeLiveTranscript() // must NOT crash and must NOT start analysis
        assertFalse(viewModel.ui.liveAnalyzed)
        assertNotNull(viewModel.ui.liveTranscript) // transcript preserved for retry
        assertTrue(viewModel.ui.toast.orEmpty().contains("没有任何课堂片段"))
    }

    @Test
    fun restartingLiveClearsAnalyzedFlag() {
        val viewModel = vm()
        viewModel.startLiveClass()
        viewModel.updateLiveSegment("片段")
        viewModel.appendLiveSegment()
        viewModel.endLiveClass()
        assertTrue(viewModel.canGenerateLiveTimeline())

        viewModel.startLiveClass() // restart
        assertFalse(viewModel.ui.liveAnalyzed)
        assertEquals(TranscriptStatus.RUNNING, viewModel.ui.liveTranscript?.status)
    }
}
