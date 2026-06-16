package com.classmate.core.provider

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ask.AskCandidate
import com.classmate.core.audio.CourseEssenceAudioStatus
import com.classmate.core.audio.CourseEssenceScript
import com.classmate.core.audio.VivoTtsProvider
import com.classmate.core.capture.CaptureHttpResponse
import com.classmate.core.capture.CaptureProviderConfig
import com.classmate.core.capture.CaptureTransport
import com.classmate.core.capture.VivoEmbeddingProvider
import com.classmate.core.capture.VivoQueryRewriteProvider
import com.classmate.core.capture.VivoTextSimilarityProvider
import com.classmate.core.retrieval.RetrievalStatus
import com.classmate.core.retrieval.VivoEmbeddingLearningProvider
import com.classmate.core.retrieval.VivoQueryRewriteLearningProvider
import com.classmate.core.retrieval.VivoTextSimilarityLearningProvider
import com.classmate.core.retrieval.key
import com.classmate.core.safety.OnDeviceTextSafetyAvailability
import com.classmate.core.safety.OnDeviceTextSafetyBridgeResult
import com.classmate.core.safety.OnDeviceTextSafetyProvider
import com.classmate.core.safety.TextSafetyStatus
import com.classmate.core.translation.TranslationStatus
import com.classmate.core.translation.VivoTranslationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialProviderSeamsTest {
    private val configured = CaptureProviderConfig("test-id", "test-key", "example.invalid")

    @Test
    fun vivoTtsProviderIsConfigMissingSafeAndParsesFakeAudio() {
        val script = CourseEssenceScript("open", listOf("point"), emptyList(), emptyList(), "close")

        val missing = VivoTtsProvider().synthesize(script)
        assertEquals(CourseEssenceAudioStatus.SCRIPT_ONLY_CONFIG_MISSING, missing.status)
        assertFalse(missing.hasAudio)

        val ready = VivoTtsProvider(configured, JsonTransport("""{"code":0,"data":{"audio":"FAKE_AUDIO_BYTES"}}"""))
            .synthesize(script)
        assertEquals(CourseEssenceAudioStatus.AUDIO_READY, ready.status)
        assertTrue(ready.hasAudio)
        assertEquals(AiExecutionSource.CLOUD, ready.source)
    }

    @Test
    fun vivoTranslationProviderIsConfigMissingSafeAndParsesDerivedNote() {
        val missing = VivoTranslationProvider().translate("A pointer stores an address.", "en", "zh-CN")
        assertEquals(TranslationStatus.CONFIG_MISSING, missing.status)

        val translated = VivoTranslationProvider(configured, JsonTransport("""{"code":0,"data":{"translation":"指针保存地址。"}}"""))
            .translate("A pointer stores an address.", "en", "zh-CN")
        assertEquals(TranslationStatus.TRANSLATED, translated.status)
        assertEquals("指针保存地址。", translated.translatedText)
        assertEquals(AiExecutionSource.CLOUD, translated.source)
    }

    @Test
    fun onDeviceTextSafetyProviderFallsBackAndWarnsOnRisk() {
        val unavailable = OnDeviceTextSafetyProvider().check("normal study note")
        assertEquals(TextSafetyStatus.UNAVAILABLE, unavailable.status)
        assertTrue(unavailable.canShareOrExport)

        val risky = OnDeviceTextSafetyProvider {
            OnDeviceTextSafetyBridgeResult(OnDeviceTextSafetyAvailability.AVAILABLE, risky = true)
        }.check("review before sharing")
        assertEquals(TextSafetyStatus.UNSAFE, risky.status)
        assertFalse(risky.canShareOrExport)
    }

    @Test
    fun retrievalAdaptersMapOfficialCaptureProvidersToLearningRetrievalContracts() {
        val rewrite = VivoQueryRewriteLearningProvider(
            VivoQueryRewriteProvider(configured, JsonTransport("""{"code":0,"data":{"query":"级数 收敛 条件"}}""")),
        ).rewrite("series?")
        assertEquals(RetrievalStatus.SUCCESS, rewrite.status)
        assertEquals("级数 收敛 条件", rewrite.rewrittenQuery)

        val candidates = listOf(
            AskCandidate("kp1", "A", "s1", "first evidence", 1),
            AskCandidate("kp2", "B", "s2", "second evidence", 1),
        )
        val similarity = VivoTextSimilarityLearningProvider(
            VivoTextSimilarityProvider(configured, JsonTransport("""{"code":0,"data":{"scores":[0.1,0.9]}}""")),
        ).score("question", candidates)
        assertEquals(RetrievalStatus.SUCCESS, similarity.status)
        assertEquals(candidates[1].key(), similarity.scores[1].candidateKey)
        assertEquals(0.9, similarity.scores[1].score, 0.001)

        val embedding = VivoEmbeddingLearningProvider(
            VivoEmbeddingProvider(configured, JsonTransport("""{"code":0,"data":[[0.1,0.2,0.3]]}""")),
        ).embed("evidence")
        assertEquals(RetrievalStatus.SUCCESS, embedding.status)
        assertEquals(3, embedding.vector!!.values.size)
    }

    private class JsonTransport(private val body: String) : CaptureTransport {
        override fun postForm(url: String, headers: Map<String, String>, form: Map<String, String>, timeoutMs: Long): CaptureHttpResponse =
            CaptureHttpResponse(200, body)

        override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): CaptureHttpResponse =
            CaptureHttpResponse(200, this.body)

        override fun postMultipart(
            url: String,
            headers: Map<String, String>,
            fileField: String,
            fileBytes: ByteArray,
            fileName: String,
            timeoutMs: Long,
        ): CaptureHttpResponse = CaptureHttpResponse(200, body)
    }
}
