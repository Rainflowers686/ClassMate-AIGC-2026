package com.classmate.core.adapter

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult

/**
 * Placeholder for an OpenAI-compatible HTTP provider (any vendor whose API
 * mirrors the OpenAI chat-completions shape).
 *
 * Exists so the project doesn't single-source on BlueLM — if BlueLM access is
 * blocked at demo time, swapping providers should be a one-line config flip.
 *
 * v0.2.5 status: NOT WIRED. See BlueLMProvider for v0.3 plan.
 */
class CompatibleProvider(
    private val config: ProviderConfig
) : ModelProvider {

    override val name: String = "compatible"

    override suspend fun analyzeCourse(input: CourseAnalysisInput): CourseAnalysisResult {
        throw NotImplementedError(
            "OpenAI-compatible provider not wired in v0.2.5 probe. " +
                "Configured base_url=${config.apiBaseUrl}. " +
                "Fall back to DemoProvider or wait for v0.3."
        )
    }
}
