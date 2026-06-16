package com.classmate.core.retrieval

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ask.EvidenceRetriever
import com.classmate.core.sample.SampleCourses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrievalProvidersTest {
    private val now = 1_700_000_000_000L
    private val result = SampleCourses.seriesAnalysis(now)

    @Test
    fun configMissingProvidersKeepLocalRetrievalAvailable() {
        val retrieved = RetrieveCourseEvidenceUseCase().retrieve("级数", result)

        assertTrue(retrieved.candidates.isNotEmpty())
        assertEquals(AiExecutionSource.SAFE_PLACEHOLDER, retrieved.source)
    }

    @Test
    fun queryRewriteCanRecoverEvidenceWhenOriginalQueryMisses() {
        val retrieved = RetrieveCourseEvidenceUseCase(
            queryRewriteProvider = QueryRewriteProvider {
                QueryRewriteResult(
                    status = RetrievalStatus.SUCCESS,
                    rewrittenQuery = "级数",
                    source = AiExecutionSource.CLOUD,
                )
            },
        ).retrieve("please explain the main topic", result)

        assertTrue(retrieved.candidates.isNotEmpty())
        assertEquals(AiExecutionSource.CLOUD, retrieved.source)
        assertEquals("级数", retrieved.effectiveQuery)
    }

    @Test
    fun similarityProviderCanRerankLocalEvidenceCandidates() {
        val local = EvidenceRetriever.retrieve("级数", result, max = 6)
        assertTrue(local.size >= 2)
        val preferred = local.last()
        val preferredKey = preferred.key()

        val retrieved = RetrieveCourseEvidenceUseCase(
            textSimilarityProvider = TextSimilarityProvider { _, candidates ->
                TextSimilarityResult(
                    status = RetrievalStatus.SUCCESS,
                    scores = candidates.map {
                        TextSimilarityScore(
                            candidateKey = it.key(),
                            score = if (it.key() == preferredKey) 99.0 else 0.1,
                        )
                    },
                    source = AiExecutionSource.CLOUD,
                )
            },
        ).retrieve("级数", result, max = 6)

        assertEquals(preferred.quote, retrieved.candidates.first().quote)
        assertEquals(AiExecutionSource.CLOUD, retrieved.source)
    }

    @Test
    fun embeddingProviderInterfaceReturnsVectorsWithoutVectorDatabase() {
        val provider = EmbeddingProvider { text ->
            EmbeddingResult(
                status = RetrievalStatus.SUCCESS,
                vector = EmbeddingVector(
                    values = listOf(text.length.toFloat(), 1f),
                    source = AiExecutionSource.CLOUD,
                ),
            )
        }

        val result = provider.embed("evidence one")

        assertEquals(RetrievalStatus.SUCCESS, result.status)
        val vector = result.vector!!
        assertEquals(2, vector.values.size)
        assertEquals(AiExecutionSource.CLOUD, vector.source)
    }
}
