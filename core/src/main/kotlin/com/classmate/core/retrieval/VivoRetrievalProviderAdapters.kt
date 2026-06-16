package com.classmate.core.retrieval

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ask.AskCandidate
import com.classmate.core.capture.CaptureError
import com.classmate.core.capture.CaptureResult
import com.classmate.core.capture.EmbeddingProvider as CaptureEmbeddingProvider
import com.classmate.core.capture.QueryRewriteProvider as CaptureQueryRewriteProvider
import com.classmate.core.capture.TextSimilarityProvider as CaptureTextSimilarityProvider

class VivoQueryRewriteLearningProvider(
    private val delegate: CaptureQueryRewriteProvider,
    private val history: () -> List<Pair<String, String>> = { emptyList() },
) : QueryRewriteProvider {
    override fun rewrite(query: String): QueryRewriteResult =
        when (val result = delegate.rewrite(query, history())) {
            is CaptureResult.Success -> QueryRewriteResult(
                status = RetrievalStatus.SUCCESS,
                rewrittenQuery = result.value.ifBlank { query },
                source = AiExecutionSource.CLOUD,
                message = "Official query rewrite served the retrieval query.",
            )
            is CaptureResult.Failure -> QueryRewriteResult(
                status = if (result.failure.error == CaptureError.ConfigMissing) RetrievalStatus.CONFIG_MISSING else RetrievalStatus.FAILED,
                rewrittenQuery = query,
                source = AiExecutionSource.MANUAL,
                message = "Official query rewrite unavailable; original question is used.",
            )
        }
}

class VivoTextSimilarityLearningProvider(
    private val delegate: CaptureTextSimilarityProvider,
) : TextSimilarityProvider {
    override fun score(query: String, candidates: List<AskCandidate>): TextSimilarityResult {
        if (candidates.isEmpty()) return TextSimilarityResult(RetrievalStatus.SUCCESS, emptyList(), AiExecutionSource.CLOUD)
        return when (val result = delegate.similarity(query, candidates.map { it.quote })) {
            is CaptureResult.Success -> {
                val scores = result.value.mapIndexedNotNull { index, score ->
                    candidates.getOrNull(index)?.let { TextSimilarityScore(it.key(), score) }
                }
                TextSimilarityResult(
                    status = RetrievalStatus.SUCCESS,
                    scores = scores,
                    source = AiExecutionSource.CLOUD,
                    message = "Official text similarity reranked evidence candidates.",
                )
            }
            is CaptureResult.Failure -> TextSimilarityResult(
                status = if (result.failure.error == CaptureError.ConfigMissing) RetrievalStatus.CONFIG_MISSING else RetrievalStatus.FAILED,
                scores = emptyList(),
                source = AiExecutionSource.MANUAL,
                message = "Official text similarity unavailable; local evidence order remains.",
            )
        }
    }
}

class VivoEmbeddingLearningProvider(
    private val delegate: CaptureEmbeddingProvider,
) : EmbeddingProvider {
    override fun embed(text: String): EmbeddingResult =
        when (val result = delegate.embed(listOf(text))) {
            is CaptureResult.Success -> {
                val first = result.value.firstOrNull().orEmpty()
                if (first.isEmpty()) {
                    EmbeddingResult(RetrievalStatus.FAILED, null, "Official embedding response was empty.")
                } else {
                    EmbeddingResult(
                        status = RetrievalStatus.SUCCESS,
                        vector = EmbeddingVector(first.map { it.toFloat() }, AiExecutionSource.CLOUD),
                        message = "Official embedding parsed successfully.",
                    )
                }
            }
            is CaptureResult.Failure -> EmbeddingResult(
                status = if (result.failure.error == CaptureError.ConfigMissing) RetrievalStatus.CONFIG_MISSING else RetrievalStatus.FAILED,
                vector = null,
                message = "Official embedding unavailable; vector search remains disabled.",
            )
        }
}
