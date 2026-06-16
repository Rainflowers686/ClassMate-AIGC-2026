package com.classmate.core.retrieval

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ask.AskCandidate
import com.classmate.core.ask.EvidenceRetriever
import com.classmate.core.model.CourseAnalysisResult

enum class RetrievalStatus { SUCCESS, CONFIG_MISSING, FAILED }

data class QueryRewriteResult(
    val status: RetrievalStatus,
    val rewrittenQuery: String = "",
    val source: AiExecutionSource = AiExecutionSource.MANUAL,
    val message: String = "",
)

fun interface QueryRewriteProvider {
    fun rewrite(query: String): QueryRewriteResult
}

class ConfigMissingQueryRewriteProvider : QueryRewriteProvider {
    override fun rewrite(query: String): QueryRewriteResult =
        QueryRewriteResult(
            status = RetrievalStatus.CONFIG_MISSING,
            rewrittenQuery = query,
            source = AiExecutionSource.MANUAL,
            message = "Query rewrite is not configured; original question is used.",
        )
}

data class TextSimilarityScore(
    val candidateKey: String,
    val score: Double,
)

data class TextSimilarityResult(
    val status: RetrievalStatus,
    val scores: List<TextSimilarityScore> = emptyList(),
    val source: AiExecutionSource = AiExecutionSource.MANUAL,
    val message: String = "",
)

fun interface TextSimilarityProvider {
    fun score(query: String, candidates: List<AskCandidate>): TextSimilarityResult
}

class ConfigMissingTextSimilarityProvider : TextSimilarityProvider {
    override fun score(query: String, candidates: List<AskCandidate>): TextSimilarityResult =
        TextSimilarityResult(
            status = RetrievalStatus.CONFIG_MISSING,
            scores = emptyList(),
            source = AiExecutionSource.MANUAL,
            message = "Text similarity is not configured; local evidence ranking is used.",
        )
}

data class EmbeddingVector(
    val values: List<Float>,
    val source: AiExecutionSource,
)

data class EmbeddingResult(
    val status: RetrievalStatus,
    val vector: EmbeddingVector? = null,
    val message: String = "",
)

fun interface EmbeddingProvider {
    fun embed(text: String): EmbeddingResult
}

class ConfigMissingEmbeddingProvider : EmbeddingProvider {
    override fun embed(text: String): EmbeddingResult =
        EmbeddingResult(RetrievalStatus.CONFIG_MISSING, null, "Embedding provider is not configured.")
}

data class RetrievedEvidence(
    val candidates: List<AskCandidate>,
    val originalQuery: String,
    val effectiveQuery: String,
    val rewriteStatus: RetrievalStatus,
    val similarityStatus: RetrievalStatus,
    val source: AiExecutionSource,
) {
    val usedRewrite: Boolean get() = effectiveQuery != originalQuery
}

class RetrieveCourseEvidenceUseCase(
    private val queryRewriteProvider: QueryRewriteProvider = ConfigMissingQueryRewriteProvider(),
    private val textSimilarityProvider: TextSimilarityProvider = ConfigMissingTextSimilarityProvider(),
) {
    fun retrieve(question: String, result: CourseAnalysisResult, max: Int = 6): RetrievedEvidence {
        val local = EvidenceRetriever.retrieve(question, result, max)
        val rewrite = queryRewriteProvider.rewrite(question)
        val effectiveQuery = rewrite.rewrittenQuery.takeIf {
            rewrite.status == RetrievalStatus.SUCCESS && it.isNotBlank()
        } ?: question
        val rewritten = if (effectiveQuery == question) emptyList() else EvidenceRetriever.retrieve(effectiveQuery, result, max)
        val merged = (local + rewritten).distinctBy { it.key() }
            .sortedByDescending { it.score }
            .take(max)
        val similarity = textSimilarityProvider.score(effectiveQuery, merged)
        val reranked = if (similarity.status == RetrievalStatus.SUCCESS && similarity.scores.isNotEmpty()) {
            val scores = similarity.scores.associate { it.candidateKey to it.score }
            merged.sortedWith(
                compareByDescending<AskCandidate> { scores[it.key()] ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.score },
            )
        } else {
            merged
        }
        val source = when {
            similarity.status == RetrievalStatus.SUCCESS || rewrite.status == RetrievalStatus.SUCCESS -> AiExecutionSource.CLOUD
            else -> AiExecutionSource.SAFE_PLACEHOLDER
        }
        return RetrievedEvidence(
            candidates = reranked.take(max),
            originalQuery = question,
            effectiveQuery = effectiveQuery,
            rewriteStatus = rewrite.status,
            similarityStatus = similarity.status,
            source = source,
        )
    }
}

fun AskCandidate.key(): String = listOf(knowledgePointId, segmentId.orEmpty(), quote).joinToString("|")
