package com.classmate.app.state

import com.classmate.app.material.LessonMaterialAssembler
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.material.MaterialSourceType
import com.classmate.core.material.SpeakerLabel
import com.classmate.core.transcript.TranscriptSourceType
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptIntakeTest {

    private fun vm() = AppViewModel(
        configRepository = ConfigRepository(Files.createTempDirectory("cm-transcript").resolve("config.local.json").toFile()),
    )

    @Test
    fun parseEditSpeakerAndSaveToTray() {
        val viewModel = vm()
        viewModel.selectTranscriptSourceType(TranscriptSourceType.SRT_FILE)
        viewModel.updateTranscriptPaste(
            """
            1
            00:00:05,000 --> 00:00:08,000
            老师：今天讲法拉第定律

            2
            00:00:09,000 --> 00:00:12,000
            学生：怎么记忆
            """.trimIndent(),
        )
        assertTrue(viewModel.parseTranscript())
        val draft = viewModel.ui.transcriptDraft
        assertNotNull(draft)
        assertEquals(2, draft!!.segments.size)
        assertEquals(SpeakerLabel.TEACHER, draft.segments[0].speaker)

        // edit text + flip speaker on the second segment
        val secondId = draft.segments[1].id
        viewModel.editTranscriptSegmentText(secondId, "怎么记忆这个定律")
        viewModel.setTranscriptSpeaker(secondId, SpeakerLabel.TEACHER)
        assertEquals("怎么记忆这个定律", viewModel.ui.transcriptDraft!!.segments[1].text)
        assertEquals(SpeakerLabel.TEACHER, viewModel.ui.transcriptDraft!!.segments[1].speaker)

        viewModel.saveTranscriptToTray()
        assertEquals(1, viewModel.ui.transcripts.size)
        assertEquals(null, viewModel.ui.transcriptDraft)
        assertEquals(TranscriptSourceType.SRT_FILE, viewModel.ui.transcripts.single().sourceType)
    }

    @Test
    fun mergeAndDeleteSegments() {
        val viewModel = vm()
        viewModel.updateTranscriptPaste("第一句\n第二句\n第三句")
        viewModel.parseTranscript()
        val ids = viewModel.ui.transcriptDraft!!.segments.map { it.id }
        viewModel.mergeTranscriptSegmentDown(ids[0])
        assertEquals(2, viewModel.ui.transcriptDraft!!.segments.size)
        assertTrue(viewModel.ui.transcriptDraft!!.segments[0].text.contains("第一句"))
        assertTrue(viewModel.ui.transcriptDraft!!.segments[0].text.contains("第二句"))

        viewModel.deleteTranscriptSegment(viewModel.ui.transcriptDraft!!.segments.last().id)
        assertEquals(1, viewModel.ui.transcriptDraft!!.segments.size)
    }

    @Test
    fun liveSegmentDefaultsUnknownAndKeepsChosenSpeakerAndTime() {
        val viewModel = vm()
        viewModel.startLiveClass()
        viewModel.updateLiveSegment("默认说话人片段")
        viewModel.appendLiveSegment()
        assertEquals(SpeakerLabel.UNKNOWN, viewModel.ui.liveTranscript!!.segments[0].speaker)

        viewModel.setLiveSpeaker(SpeakerLabel.TEACHER)
        viewModel.updateLiveSegment("教师讲解片段")
        viewModel.appendLiveSegment()
        val seg = viewModel.ui.liveTranscript!!.segments[1]
        assertEquals(SpeakerLabel.TEACHER, seg.speaker)
        assertNotNull(seg.startMs)

        viewModel.endLiveClass()
        // endLiveClass -> bundle keeps speaker + time
        val bundle = LessonMaterialAssembler.fromLiveWithOcr(viewModel.ui.liveTranscript!!, emptyList())
        val teacher = bundle.allSegments().first { it.text.contains("教师讲解") }
        assertEquals(SpeakerLabel.TEACHER, teacher.speaker)
        assertNotNull(teacher.timeRange)
    }

    @Test
    fun savedTranscriptFusesIntoMaterialAsTranscriptSource() {
        val viewModel = vm()
        viewModel.selectTranscriptSourceType(TranscriptSourceType.AUDIO_TRANSCRIPT)
        viewModel.updateTranscriptPaste("[00:00:10] 老师：磁通量定义")
        viewModel.parseTranscript()
        viewModel.saveTranscriptToTray()

        val bundle = LessonMaterialAssembler.fromImportWithOcr(
            title = "课",
            text = "",
            importType = com.classmate.core.importing.ImportSourceType.PASTE_TEXT,
            ocrImports = emptyList(),
            transcripts = viewModel.ui.transcripts,
        )
        assertEquals(MaterialSourceType.TRANSCRIPT, bundle.sources.single().type)
        assertTrue(bundle.plainText().contains("磁通量定义"))
        assertTrue(bundle.plainText().contains("音频转写"))
    }
}
