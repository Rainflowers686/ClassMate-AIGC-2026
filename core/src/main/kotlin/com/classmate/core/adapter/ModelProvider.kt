package com.classmate.core.adapter

import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult

/**
 * Unified entry point for course-analysis model calls.
 *
 * The UI layer MUST depend on this interface, never on a concrete provider —
 * that's the whole reason for the adapter. v0.2.5 ships three placeholders;
 * v0.3 wires BlueLM and the OpenAI-compatible fallback to real HTTP.
 */
interface ModelProvider {
    /** Stable identifier used in logs and config; e.g. "demo", "bluelm", "compatible". */
    val name: String

    /**
     * Run a single course-analysis pass.
     *
     * Implementations MUST return JSON-schema-conforming output OR throw.
     * They MUST NOT silently fake a result — only [LocalRuleProvider] is
     * allowed to generate non-model output, and it does so deterministically
     * from real input and tags itself as `name == "local"`.
     */
    suspend fun analyzeCourse(input: CourseAnalysisInput): CourseAnalysisResult
}
