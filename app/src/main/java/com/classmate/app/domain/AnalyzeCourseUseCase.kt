package com.classmate.app.domain

import com.classmate.core.adapter.ModelCallException
import com.classmate.core.adapter.ModelProvider
import com.classmate.core.evidence.EvidenceValidationResult
import com.classmate.core.evidence.EvidenceValidator
import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.validation.ResultValidation
import com.classmate.core.validation.ResultValidator

/**
 * Orchestrates one course-analysis call:
 *   ProviderResolver.primary → analyzeCourse →
 *     ResultValidator(result, input) → EvidenceValidator → result bundle.
 *
 * On any [ModelCallException] from the primary path (or a primary that
 * passes structural checks but flunks Validator), the use case falls back to
 * the local rule provider, which is guaranteed to pass.
 *
 * The use case is pure orchestration: it does not touch the logger or UI.
 * Both responsibilities belong to the caller (ViewModel).
 */
class AnalyzeCourseUseCase(
    private val resolver: ProviderResolver
) {
    suspend fun run(input: CourseAnalysisInput): Outcome {
        require(input.segments.isNotEmpty()) { "AnalyzeCourseUseCase requires at least one input segment" }

        val primary = resolver.resolvePrimary()
        val primaryName = primary.name

        // 1) Try primary
        val primaryAttempt = runAndValidate(primary, input)
        if (primaryAttempt is Outcome.Success) return primaryAttempt
        // For non-local providers, fall back to local on any failure.
        if (primaryName == "local") {
            // Local should not fail; surface as hard failure.
            return primaryAttempt
        }

        // 2) Fall back to local
        val fallback = resolver.localFallback()
        val reason = when (primaryAttempt) {
            is Outcome.HardFailure -> primaryAttempt.errorType
            else -> "unknown"
        }
        val message = when (primaryAttempt) {
            is Outcome.HardFailure -> primaryAttempt.message
            else -> "primary returned unexpected outcome"
        }
        val fallbackResult = runAndValidate(fallback, input)
        return when (fallbackResult) {
            is Outcome.Success -> fallbackResult.copy(
                requestedProvider = primaryName,
                fallbackUsed = true,
                fallbackReason = "$reason: $message"
            )
            is Outcome.HardFailure -> fallbackResult.copy(
                requestedProvider = primaryName,
                message = "local fallback also failed: ${fallbackResult.message}"
            )
        }
    }

    private suspend fun runAndValidate(
        provider: ModelProvider,
        input: CourseAnalysisInput
    ): Outcome {
        val result: CourseAnalysisResult = try {
            provider.analyzeCourse(input)
        } catch (e: ModelCallException) {
            return Outcome.HardFailure(
                requestedProvider = provider.name,
                providerUsed = provider.name,
                errorType = e.reason.name,
                message = e.message ?: e::class.simpleName ?: "ModelCallException"
            )
        } catch (e: Throwable) {
            return Outcome.HardFailure(
                requestedProvider = provider.name,
                providerUsed = provider.name,
                errorType = e::class.simpleName ?: "Throwable",
                message = e.message ?: "unexpected error"
            )
        }

        val structural: ResultValidation = ResultValidator.validate(result, input)
        val evidence: EvidenceValidationResult = EvidenceValidator.validate(input, result)
        // We don't HARD-fail on structural issues here for non-local providers,
        // because the caller may decide to surface the issues to the UI rather
        // than fall back. But: if structural fails AND we're talking to a
        // non-local provider, that's exactly the fallback trigger upstream.
        val structureOk = structural.passed && evidence.schemaPassed
        return if (structureOk || provider.name == "local") {
            Outcome.Success(
                requestedProvider = provider.name,
                providerUsed = provider.name,
                fallbackUsed = false,
                fallbackReason = null,
                result = result,
                structural = structural,
                evidence = evidence
            )
        } else {
            Outcome.HardFailure(
                requestedProvider = provider.name,
                providerUsed = provider.name,
                errorType = "VALIDATION_FAILED",
                message = structural.issues.firstOrNull()?.let { it.kind.name + " " + it.detail }
                    ?: "structure validation failed"
            )
        }
    }

    sealed interface Outcome {
        val requestedProvider: String
        val providerUsed: String

        data class Success(
            override val requestedProvider: String,
            override val providerUsed: String,
            val fallbackUsed: Boolean,
            val fallbackReason: String?,
            val result: CourseAnalysisResult,
            val structural: ResultValidation,
            val evidence: EvidenceValidationResult
        ) : Outcome

        data class HardFailure(
            override val requestedProvider: String,
            override val providerUsed: String,
            val errorType: String,
            val message: String
        ) : Outcome
    }
}
