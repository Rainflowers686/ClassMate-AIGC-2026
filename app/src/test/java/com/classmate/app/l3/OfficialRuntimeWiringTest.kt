package com.classmate.app.l3

import com.classmate.app.platform.OfficialProviderConfigSummary
import com.classmate.app.platform.ProviderConfigSummary
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
}
