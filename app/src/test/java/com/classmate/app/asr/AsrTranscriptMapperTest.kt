package com.classmate.app.asr

import com.classmate.core.material.LessonMaterialFusionEngine
import com.classmate.core.material.MaterialSourceType
import com.classmate.core.material.SpeakerLabel
import com.classmate.core.transcript.TranscriptMaterialAdapter
import com.classmate.core.transcript.TranscriptSourceType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsrTranscriptMapperTest {

    private fun sessionWithSegments(): AsrSession {
        var s = AsrSessionController.begin(AsrSession(), available = true, permissionGranted = true, now = 1_000)
        s = AsrSessionController.onFinal(s, "磁通量的定义", confidence = 0.8, speaker = SpeakerLabel.TEACHER, now = 4_000)
        s = AsrSessionController.onFinal(s, "学生提问", confidence = null, speaker = SpeakerLabel.STUDENT, now = 7_000)
        return s
    }

    @Test
    fun toDraftProducesLiveAsrTranscriptWithNoAudioPath() {
        val draft = AsrTranscriptMapper.toDraft(sessionWithSegments(), title = "电磁感应", now = 10_000, id = "asr_1")
        assertEquals(TranscriptSourceType.LIVE_ASR, draft.sourceType)
        assertEquals(2, draft.segments.size)
        assertNull(draft.fileName)  // no raw audio file path is ever stored
        assertNull(draft.mimeType)
        assertNull(draft.sizeBytes)
    }

    @Test
    fun asrTranscriptFusesIntoMaterialWithRealtimeMarker() {
        val draft = AsrTranscriptMapper.toDraft(sessionWithSegments(), title = "电磁感应", now = 10_000, id = "asr_1")
        val bundle = LessonMaterialFusionEngine.fuse("b", "电磁感应", listOf(TranscriptMaterialAdapter.toMaterialSource(draft)))

        assertEquals(MaterialSourceType.TRANSCRIPT, bundle.sources.single().type)
        val plain = bundle.plainText()
        assertTrue(plain.contains("[实时转写 00:00:00-00:00:03 · 教师]"))
        assertTrue(plain.contains("磁通量的定义"))
        assertTrue(plain.contains("[实时转写 00:00:03-00:00:06 · 学生]"))
    }

    @Test
    fun androidClientSourceDoesNotPersistOrUploadAudio() {
        val source = listOf(
            File("src/main/java/com/classmate/app/asr/AndroidSpeechRecognizerClient.kt"),
            File("app/src/main/java/com/classmate/app/asr/AndroidSpeechRecognizerClient.kt"),
        ).first { it.exists() }.readText()
        // No raw-audio capture-to-file or upload in the recognizer client.
        listOf("MediaRecorder", "FileOutputStream", "AudioRecord", ".wav", ".pcm", ".m4a", "HttpURLConnection", "OkHttp").forEach {
            assertFalse("recognizer client must not contain $it", source.contains(it))
        }
    }
}
