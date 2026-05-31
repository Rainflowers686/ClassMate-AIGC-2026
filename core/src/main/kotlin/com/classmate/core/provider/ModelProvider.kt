package com.classmate.core.provider

import com.classmate.core.model.CourseSession
import com.classmate.core.model.ProviderKind

/** Everything a provider needs to produce an analysis for one session. */
data class AnalysisRequest(
    val session: CourseSession,
    val maxKnowledgePoints: Int = 12,
    val questionsPerKnowledgePoint: Int = 1,
)

/**
 * The single abstraction over "something that can turn a course into an analysis".
 *
 * Implementations never reach the UI directly — they are hidden behind
 * [ProviderResolver] / CourseAnalyzer so that no raw provider (or its credentials) can leak
 * into the presentation layer.
 */
interface ModelProvider {
    val kind: ProviderKind

    /** True only if this provider can actually run now (configured, has real creds, transport wired). */
    fun isAvailable(): Boolean

    /** Produce a raw result. Must never throw for expected failures — return [ProviderResult.Failure]. */
    fun generate(request: AnalysisRequest): ProviderResult
}
