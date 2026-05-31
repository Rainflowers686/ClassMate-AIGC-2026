package com.classmate.core.analysis

import com.classmate.core.logging.InMemoryRedactedLogger
import com.classmate.core.logging.RedactedLogEntry
import com.classmate.core.logging.RedactedLogger
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.parser.AnalysisJsonParser
import com.classmate.core.parser.JsonExtractor
import com.classmate.core.provider.AnalysisRequest
import com.classmate.core.provider.ProviderError
import com.classmate.core.provider.ProviderErrorType
import com.classmate.core.provider.ProviderResolver
import com.classmate.core.provider.ProviderResult
import com.classmate.core.validation.ResultValidator
import com.classmate.core.validation.ValidationReport

/** The result of an analysis run, plus the redacted log trail that produced it. */
sealed interface AnalysisOutcome {
    val logs: List<RedactedLogEntry>

    data class Success(
        val result: CourseAnalysisResult,
        val report: ValidationReport,
        override val logs: List<RedactedLogEntry>,
    ) : AnalysisOutcome

    data class Failure(
        val lastError: ProviderError?,
        val report: ValidationReport?,
        override val logs: List<RedactedLogEntry>,
    ) : AnalysisOutcome
}

/**
 * The single public entry point for turning a course into an analysis — and the only thing
 * the app talks to. It walks the resolver's ordered providers (BlueLM first), and for each
 * success runs extract -> parse -> validate, falling back on any failure. Every step emits a
 * redacted log line; nothing sensitive is ever recorded.
 *
 * Providers are never exposed here, so no raw provider or credential can leak to the UI.
 */
class CourseAnalyzer(
    private val resolver: ProviderResolver,
    private val parser: AnalysisJsonParser = AnalysisJsonParser(),
    private val validator: ResultValidator = ResultValidator(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun analyze(
        request: AnalysisRequest,
        logger: RedactedLogger = InMemoryRedactedLogger(),
    ): AnalysisOutcome {
        var lastError: ProviderError? = null
        var lastReport: ValidationReport? = null

        for (provider in resolver.providersInOrder()) {
            val fallbackUsed = provider.kind != resolver.primary
            when (val raw = provider.generate(request)) {
                is ProviderResult.Failure -> {
                    logger.log(entry(provider.kind.name, "FAIL", raw.latencyMs, "SKIPPED", fallbackUsed, raw.error.type.name))
                    lastError = raw.error
                }

                is ProviderResult.Success -> {
                    val jsonText = JsonExtractor.extract(raw.rawModelText)
                    if (jsonText == null) {
                        logger.log(entry(provider.kind.name, "OK", raw.latencyMs, "FAIL", fallbackUsed, ProviderErrorType.PARSE_ERROR.name))
                        lastError = ProviderError(ProviderErrorType.PARSE_ERROR, provider.kind)
                        continue
                    }
                    val provenance = AnalysisProvenance(
                        provider = provider.kind,
                        fallbackUsed = fallbackUsed,
                        modelLabel = provider.kind.displayName,
                        createdAtEpochMs = clock(),
                    )
                    val parsed = parser.parse(jsonText, request.session, provenance)
                    if (parsed == null) {
                        logger.log(entry(provider.kind.name, "OK", raw.latencyMs, "FAIL", fallbackUsed, ProviderErrorType.PARSE_ERROR.name))
                        lastError = ProviderError(ProviderErrorType.PARSE_ERROR, provider.kind)
                        continue
                    }
                    val report = validator.validate(parsed, request.session)
                    if (!report.ok) {
                        logger.log(entry(provider.kind.name, "OK", raw.latencyMs, "FAIL", fallbackUsed, ProviderErrorType.VALIDATION_ERROR.name))
                        lastError = ProviderError(ProviderErrorType.VALIDATION_ERROR, provider.kind)
                        lastReport = report
                        continue
                    }
                    logger.log(entry(provider.kind.name, "OK", raw.latencyMs, "PASS", fallbackUsed, null))
                    return AnalysisOutcome.Success(parsed, report, logger.entries())
                }
            }
        }
        return AnalysisOutcome.Failure(lastError, lastReport, logger.entries())
    }

    private fun entry(
        provider: String,
        status: String,
        latencyMs: Long,
        validation: String,
        fallbackUsed: Boolean,
        errorType: String?,
    ) = RedactedLogEntry(provider, status, latencyMs, validation, fallbackUsed, errorType)
}
