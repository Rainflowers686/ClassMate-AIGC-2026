package com.classmate.core.provider

import com.classmate.core.model.ProviderKind
import com.classmate.core.parser.WireAnalysis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The safety net — never the headline. Always available, no network, deterministic. It runs
 * the bounded [LocalHeuristicExtractor] and emits the SAME wire JSON a model would, so the
 * fallback output is parsed + validated through the identical pipeline.
 *
 * Per the product rules, this exists only to keep ClassMate usable (and the demo alive) when
 * BlueLM cannot be reached. It is explicitly not a substitute for real model understanding.
 */
class LocalFallbackProvider(
    private val extractor: LocalHeuristicExtractor = LocalHeuristicExtractor(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ModelProvider {

    private val json = Json { encodeDefaults = true }

    override val kind: ProviderKind = ProviderKind.LOCAL_FALLBACK

    override fun isAvailable(): Boolean = true

    override fun generate(request: AnalysisRequest): ProviderResult {
        val start = clock()
        return try {
            val wire = extractor.extract(
                session = request.session,
                maxKnowledgePoints = request.maxKnowledgePoints,
                questionsPerKnowledgePoint = request.questionsPerKnowledgePoint,
            )
            ProviderResult.Success(kind, clock() - start, json.encodeToString<WireAnalysis>(wire))
        } catch (e: Exception) {
            ProviderResult.Failure(kind, clock() - start, ProviderError(ProviderErrorType.UNKNOWN, kind))
        }
    }
}
