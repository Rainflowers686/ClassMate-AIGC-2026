package com.classmate.app.l3

import com.classmate.app.platform.CaptureConfigLoader
import com.classmate.app.platform.OfficialProviderConfigSummary
import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.core.capture.CaptureHttpResponse
import com.classmate.core.capture.CaptureTransport
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.audio.FakeTtsProvider
import com.classmate.core.retrieval.EmbeddingResult
import com.classmate.core.retrieval.EmbeddingVector
import com.classmate.core.retrieval.QueryRewriteResult
import com.classmate.core.retrieval.RetrievalStatus
import com.classmate.core.retrieval.TextSimilarityResult
import com.classmate.core.retrieval.TextSimilarityScore
import com.classmate.core.retrieval.key
import com.classmate.core.tools.FunctionCallingProviderResult
import com.classmate.core.tools.FunctionCallingStatus
import com.classmate.core.tools.OfficialToolCallProposal
import com.classmate.core.translation.TranslationProviderResult
import com.classmate.core.translation.TranslationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class OfficialRuntimeWiringTest {
    private val now = 1_700_000_000_000L
    private val allOfficialConfigured = ProviderConfigSummary.defaults().copy(
        officialProviders = OfficialProviderConfigSummary(
            ocrConfigured = true,
            queryRewriteConfigured = true,
            textSimilarityConfigured = true,
            embeddingConfigured = true,
            translationConfigured = true,
            ttsConfigured = true,
            functionCallingConfigured = true,
            asrLongConfigured = true,
        ),
    )

    @Test
    fun officialRuntimeSuccessEnrichesMainPipelineOutputs() {
        val base = L3LearningPipeline().buildFromText(
            title = L3DemoSeeds.lessonTitle,
            text = L3DemoSeeds.lessonText,
            sourceType = L3SourceType.TEXT,
            providerSummary = allOfficialConfigured,
            now = now,
        )
        val gateway = ProviderBackedOfficialRuntimeGateway(
            queryRewriteProvider = { query ->
                QueryRewriteResult(RetrievalStatus.SUCCESS, "official $query", AiExecutionSource.CLOUD, "ok")
            },
            embeddingProvider = { _ ->
                EmbeddingResult(RetrievalStatus.SUCCESS, EmbeddingVector(listOf(0.2f, 0.8f), AiExecutionSource.CLOUD), "ok")
            },
            textSimilarityProvider = { _, candidates ->
                TextSimilarityResult(
                    RetrievalStatus.SUCCESS,
                    candidates.map { TextSimilarityScore(it.key(), 0.91) },
                    AiExecutionSource.CLOUD,
                    "ok",
                )
            },
            translationProvider = { _, _, _ ->
                TranslationProviderResult(TranslationStatus.TRANSLATED, "translated text", AiExecutionSource.CLOUD, "ok")
            },
            ttsProvider = FakeTtsProvider(),
            functionCallingProvider = { _, _ ->
                FunctionCallingProviderResult(
                    FunctionCallingStatus.TOOL_PROPOSED,
                    OfficialToolCallProposal("searchEvidence", emptyMap()),
                    "ok",
                )
            },
        )

        val enriched = OfficialRuntimeIntegrator.enrich(
            snapshot = base,
            summary = allOfficialConfigured,
            gateway = gateway,
            inputType = ToolInputType.TEXT,
            now = now + 1,
            localTtsAvailable = true,
            edgeModelAvailable = true,
        )

        assertTrue(enriched.stepLogs.any { it.step == "QUERY_REWRITE" && it.status == "QUERY_REWRITE_OFFICIAL_USED" })
        assertTrue(enriched.semanticIndexRecords.isNotEmpty())
        assertTrue(enriched.semanticIndexRecords.all { it.vectorSource == "OFFICIAL" && it.officialVector.isNotEmpty() })
        assertTrue(enriched.semanticSearchResults.single().query.startsWith("official "))
        assertTrue(enriched.similarityMatches.isNotEmpty())
        assertTrue(enriched.similarityMatches.all { it.scoreSource == "OFFICIAL" && it.providerStatus == "OFFICIAL_RUNTIME_USED" })
        assertTrue(enriched.toolStepRecords.any { it.toolName == "QUERY_REWRITE" && it.providerMode == ToolProviderMode.OFFICIAL })
        assertTrue(enriched.diagnostics.any { it.capability == "EMBEDDING" && it.status == "OFFICIAL_RUNTIME_USED" })
        assertTrue(enriched.diagnostics.any { it.capability == "FUNCTION_CALLING" && it.status == "OFFICIAL_RUNTIME_USED" })
        assertTrue(enriched.diagnostics.all { !it.message.contains("Authorization", ignoreCase = true) })
    }

    @Test
    fun missingRuntimeAdapterFallsBackWithoutFakingOfficialVectors() {
        val base = L3LearningPipeline().buildFromText(
            title = L3DemoSeeds.lessonTitle,
            text = L3DemoSeeds.lessonText,
            sourceType = L3SourceType.TEXT,
            providerSummary = allOfficialConfigured,
            now = now,
        )

        val enriched = OfficialRuntimeIntegrator.enrich(
            snapshot = base,
            summary = allOfficialConfigured,
            gateway = ProviderBackedOfficialRuntimeGateway(),
            inputType = ToolInputType.TEXT,
            now = now + 2,
            localTtsAvailable = true,
            edgeModelAvailable = false,
        )

        assertTrue(enriched.stepLogs.any { it.step == "QUERY_REWRITE" && it.status == "QUERY_REWRITE_APP_WIRING_PENDING" })
        assertTrue(enriched.semanticIndexRecords.isNotEmpty())
        assertTrue(enriched.semanticIndexRecords.all { it.vectorSource == "LOCAL_FALLBACK" && it.officialVector.isEmpty() })
        assertTrue(enriched.similarityMatches.all { it.scoreSource == "LOCAL_FALLBACK" })
        assertTrue(enriched.diagnostics.any { it.capability == "ASR_LONG" && it.status == "OFFICIAL_APP_WIRING_PENDING" })
        assertTrue(enriched.diagnostics.any { it.capability == "EDGE_MODEL" && it.status == "LOCAL_FALLBACK_USED" })
        assertFalse(enriched.diagnostics.joinToString("\n") { it.message }.contains("AppKey", ignoreCase = true))
    }

    @Test
    fun ocrEvidenceKeepsOfficialRuntimeProvenanceWhenImagePathUsesOcr() {
        val base = L3LearningPipeline().buildFromText(
            title = "OCR lesson",
            text = L3DemoSeeds.lessonText,
            sourceType = L3SourceType.OCR_IMAGE,
            providerSummary = allOfficialConfigured,
            now = now,
        )

        val enriched = OfficialRuntimeIntegrator.enrich(
            snapshot = base,
            summary = allOfficialConfigured,
            gateway = ProviderBackedOfficialRuntimeGateway(),
            inputType = ToolInputType.IMAGE,
            now = now + 3,
            localTtsAvailable = true,
            edgeModelAvailable = false,
        )

        assertTrue(enriched.evidence.isNotEmpty())
        assertTrue(enriched.evidence.all { it.sourceType == L3SourceType.OCR_IMAGE && it.providerProvenance == "OFFICIAL_RUNTIME_USED" })
        assertTrue(enriched.diagnostics.any { it.capability == "OCR" && it.status == "OFFICIAL_RUNTIME_USED" })
    }

    @Test
    fun productionFactoryInjectsVivoRetrievalAdaptersIntoMainPipeline() {
        val configFile = fakeCaptureConfigFile()
        val transport = RoutingCaptureTransport()
        val gateway = OfficialRuntimeGatewayFactory.production(
            configLoader = CaptureConfigLoader(configFile),
            transport = transport,
        )
        val base = L3LearningPipeline().buildFromText(
            title = L3DemoSeeds.lessonTitle,
            text = L3DemoSeeds.lessonText,
            sourceType = L3SourceType.TEXT,
            providerSummary = allOfficialConfigured,
            now = now,
        )

        val enriched = OfficialRuntimeIntegrator.enrich(
            snapshot = base,
            summary = allOfficialConfigured,
            gateway = gateway,
            inputType = ToolInputType.TEXT,
            now = now + 4,
            localTtsAvailable = true,
            edgeModelAvailable = false,
        )

        assertTrue(enriched.stepLogs.any { it.step == "QUERY_REWRITE" && it.status == "QUERY_REWRITE_OFFICIAL_USED" })
        assertTrue(enriched.semanticIndexRecords.all { it.vectorSource == "OFFICIAL" && it.officialVector.isNotEmpty() })
        assertTrue(enriched.similarityMatches.all { it.scoreSource == "OFFICIAL" })
        assertTrue(enriched.diagnostics.first { it.capability == "QUERY_REWRITE" }.message.contains("official_adapter_injected=true"))
        assertTrue(enriched.diagnostics.first { it.capability == "QUERY_REWRITE" }.message.contains("official_runtime_attempted=true"))
        assertTrue(transport.bodies.any { it.contains("\"prompts\"") && !it.contains("\"query\"") })
        assertTrue(transport.urls.any { it.contains("query-rewrite-api") })
        assertTrue(transport.urls.any { it.contains("embedding-model-api") })
        assertTrue(transport.urls.any { it.contains("similarity-model-api") })
    }

    @Test
    fun productionFactoryMissingConfigFallsBackWithAdapterInjectedProvenance() {
        val gateway = OfficialRuntimeGatewayFactory.production(
            configLoader = CaptureConfigLoader(File("does-not-exist-official-runtime-test.json")),
            transport = RoutingCaptureTransport(),
        )
        val base = L3LearningPipeline().buildFromText(
            title = L3DemoSeeds.lessonTitle,
            text = L3DemoSeeds.lessonText,
            sourceType = L3SourceType.TEXT,
            providerSummary = allOfficialConfigured,
            now = now,
        )

        val enriched = OfficialRuntimeIntegrator.enrich(
            snapshot = base,
            summary = allOfficialConfigured,
            gateway = gateway,
            inputType = ToolInputType.TEXT,
            now = now + 5,
            localTtsAvailable = true,
            edgeModelAvailable = false,
        )

        val rewrite = enriched.diagnostics.first { it.capability == "QUERY_REWRITE" }
        assertEquals("OFFICIAL_RUNTIME_NOT_CONFIGURED", rewrite.status)
        assertTrue(rewrite.message.contains("official_adapter_injected=true"))
        assertTrue(rewrite.message.contains("official_runtime_attempted=true"))
        assertTrue(rewrite.message.contains("OFFICIAL_QUERY_REWRITE_CONFIG_MISSING_AT_RUNTIME"))
        assertTrue(enriched.semanticIndexRecords.all { it.vectorSource == "LOCAL_FALLBACK" })
        assertTrue(enriched.semanticIndexRecords.any { it.embeddingStatus.contains("OFFICIAL_ADAPTER_INJECTED") })
        assertTrue(enriched.similarityMatches.all { it.scoreSource == "LOCAL_FALLBACK" })
    }

    @Test
    fun appViewModelDefaultUsesOfficialRuntimeFactoryNotNoArgGateway() {
        val source = firstExisting(
            "src/main/java/com/classmate/app/state/AppViewModel.kt",
            "app/src/main/java/com/classmate/app/state/AppViewModel.kt",
        ).readText()

        assertTrue(source.contains("OfficialRuntimeGatewayFactory.production()"))
        assertFalse(source.contains("officialRuntimeGateway: OfficialRuntimeGateway = ProviderBackedOfficialRuntimeGateway()"))
    }

    private fun fakeCaptureConfigFile(): File =
        Files.createTempFile("cm-official-runtime", ".json").toFile().apply {
            writeText(
                """
                {
                  "vivoCapture": {
                    "appId": "fake-app-id",
                    "appKey": "fake-app-key",
                    "baseUrl": "https://api-ai.vivo.com.cn"
                  }
                }
                """.trimIndent(),
            )
        }

    private class RoutingCaptureTransport : CaptureTransport {
        val urls = mutableListOf<String>()
        val bodies = mutableListOf<String>()

        override fun postForm(url: String, headers: Map<String, String>, form: Map<String, String>, timeoutMs: Long): CaptureHttpResponse =
            CaptureHttpResponse(200, "{}")

        override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Long): CaptureHttpResponse {
            urls += url
            bodies += body
            return when {
                url.contains("query-rewrite-api") -> CaptureHttpResponse(200, """{"code":0,"data":{"query":"official rewritten query"}}""")
                url.contains("embedding-model-api") -> CaptureHttpResponse(200, """{"code":0,"data":[[0.3,0.7,0.2]]}""")
                url.contains("similarity-model-api") -> CaptureHttpResponse(200, """{"code":0,"data":{"scores":[0.88]}}""")
                else -> CaptureHttpResponse(200, "{}")
            }
        }

        override fun postMultipart(
            url: String,
            headers: Map<String, String>,
            fileField: String,
            fileBytes: ByteArray,
            fileName: String,
            timeoutMs: Long,
        ): CaptureHttpResponse = CaptureHttpResponse(200, "{}")
    }

    private fun firstExisting(vararg paths: String): File =
        paths.map(::File).first { it.exists() }
}
