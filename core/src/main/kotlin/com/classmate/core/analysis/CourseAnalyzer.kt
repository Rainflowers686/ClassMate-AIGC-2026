package com.classmate.core.analysis

import com.classmate.core.logging.InMemoryRedactedLogger
import com.classmate.core.logging.RedactedLogEntry
import com.classmate.core.logging.RedactedLogger
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.ProviderKind
import com.classmate.core.parser.AnalysisJsonParser
import com.classmate.core.parser.JsonExtractor
import com.classmate.core.parser.JsonParseStrategy
import com.classmate.core.parser.LlmDraftAssembler
import com.classmate.core.parser.LlmDraftAssemblyStats
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
 * The single public entry point for turning a course into an analysis. BlueLM is now treated
 * as a compact draft generator: the model emits ClassMateLlmDraftV1, while deterministic local
 * code assembles ids, evidence spans, references, and UI-ready domain objects. The legacy
 * WireAnalysis parser remains as a compatibility fallback for existing tests/providers.
 */
class CourseAnalyzer(
    private val resolver: ProviderResolver,
    private val parser: AnalysisJsonParser = AnalysisJsonParser(),
    private val draftAssembler: LlmDraftAssembler = LlmDraftAssembler(),
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

            fun provenance() = AnalysisProvenance(
                provider = provider.kind,
                fallbackUsed = fallbackUsed,
                modelLabel = provider.kind.displayName,
                createdAtEpochMs = clock(),
            )

            fun process(rawText: String): Processed {
                val contentLength = rawText.length
                val extracted = JsonExtractor.extractWithStrategy(rawText)
                    ?: return Processed.Fail(
                        validationErrorType = "JSON_PARSE_FAILED",
                        report = null,
                        contentLength = contentLength,
                        jsonExtracted = false,
                        parseStrategy = JsonParseStrategy.FAILED.name,
                        draftStats = null,
                        repairHint = "previous output was not a JSON object; return compact ClassMateLlmDraftV1 JSON only",
                    )

                draftAssembler.assemble(extracted.jsonText, request.session, provenance())?.let { assembled ->
                    val report = validator.validate(assembled.result, request.session)
                    if (report.ok) {
                        return Processed.Ok(
                            result = assembled.result,
                            report = report,
                            contentLength = contentLength,
                            parseStrategy = extracted.strategy.name,
                            draftStats = assembled.stats,
                        )
                    }
                    val vet = report.validationErrorType ?: "VALIDATION_ERROR"
                    return Processed.Fail(
                        validationErrorType = vet,
                        report = report,
                        contentLength = contentLength,
                        jsonExtracted = true,
                        parseStrategy = extracted.strategy.name,
                        draftStats = assembled.stats,
                        repairHint = repairHintFor(vet),
                    )
                }

                val parsed = parser.parse(extracted.jsonText, request.session, provenance())
                    ?: return Processed.Fail(
                        validationErrorType = "JSON_PARSE_FAILED",
                        report = null,
                        contentLength = contentLength,
                        jsonExtracted = true,
                        parseStrategy = extracted.strategy.name,
                        draftStats = null,
                        repairHint = "previous output was not compact ClassMateLlmDraftV1 JSON",
                    )
                val report = validator.validate(parsed, request.session)
                if (report.ok) {
                    return Processed.Ok(parsed, report, contentLength, extracted.strategy.name, null)
                }
                val vet = report.validationErrorType ?: "VALIDATION_ERROR"
                return Processed.Fail(
                    validationErrorType = vet,
                    report = report,
                    contentLength = contentLength,
                    jsonExtracted = true,
                    parseStrategy = extracted.strategy.name,
                    draftStats = null,
                    repairHint = repairHintFor(vet),
                )
            }

            when (val raw = provider.generate(request)) {
                is ProviderResult.Failure -> {
                    logger.log(transportFail(provider.kind.name, raw.latencyMs, fallbackUsed, raw.error))
                    lastError = raw.error
                }

                is ProviderResult.Success -> {
                    when (val first = process(raw.rawModelText)) {
                        is Processed.Ok -> {
                            logger.log(okEntry(provider.kind.name, raw.latencyMs, fallbackUsed, first, false, null))
                            return AnalysisOutcome.Success(first.result, first.report, logger.entries())
                        }

                        is Processed.Fail -> {
                            logger.log(validationFail(provider.kind.name, raw.latencyMs, fallbackUsed, first, false, null))
                            lastError = ProviderError(errorTypeFor(first), provider.kind)
                            lastReport = first.report

                            if (provider.kind != ProviderKind.LOCAL_FALLBACK) {
                                val repairRequest = request.copy(repairHint = first.repairHint)
                                when (val raw2 = provider.generate(repairRequest)) {
                                    is ProviderResult.Success -> when (val second = process(raw2.rawModelText)) {
                                        is Processed.Ok -> {
                                            logger.log(okEntry(provider.kind.name, raw2.latencyMs, fallbackUsed, second, true, "SUCCESS"))
                                            return AnalysisOutcome.Success(second.result, second.report, logger.entries())
                                        }
                                        is Processed.Fail -> {
                                            logger.log(validationFail(provider.kind.name, raw2.latencyMs, fallbackUsed, second, true, second.validationErrorType))
                                            lastError = ProviderError(errorTypeFor(second), provider.kind)
                                            lastReport = second.report
                                        }
                                    }
                                    is ProviderResult.Failure -> {
                                        logger.log(transportFail(provider.kind.name, raw2.latencyMs, fallbackUsed, raw2.error, true, raw2.error.type.name))
                                        lastError = raw2.error
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return AnalysisOutcome.Failure(lastError, lastReport, logger.entries())
    }

    private fun errorTypeFor(fail: Processed.Fail): ProviderErrorType =
        if (fail.validationErrorType == "JSON_PARSE_FAILED") ProviderErrorType.PARSE_ERROR else ProviderErrorType.VALIDATION_ERROR

    private fun repairHintFor(validationErrorType: String): String = when (validationErrorType) {
        "EVIDENCE_MISMATCH" -> "some evidenceQuote values were not copied exactly from source; copy shorter verbatim quotes"
        "REFERENCE_BROKEN" -> "quiz knowledgePointTitle must exactly equal one knowledgePoints title"
        "SCHEMA_MISSING_FIELD" -> "required fields were missing; return all ClassMateLlmDraftV1 fields"
        else -> "previous output failed validation; return shorter compact ClassMateLlmDraftV1 JSON only"
    }

    private fun okEntry(
        provider: String,
        latencyMs: Long,
        fallbackUsed: Boolean,
        ok: Processed.Ok,
        repairAttempted: Boolean,
        repairResult: String?,
    ) = RedactedLogEntry(
        provider = provider,
        status = "OK",
        latencyMs = latencyMs,
        validation = "PASS",
        fallbackUsed = fallbackUsed,
        errorType = null,
        validationErrorType = null,
        responseContentLength = ok.contentLength,
        jsonExtracted = true,
        parseStrategy = ok.parseStrategy,
        draftKpCount = ok.draftStats?.draftKpCount,
        draftQuizCount = ok.draftStats?.draftQuizCount,
        keptKpCount = ok.draftStats?.keptKpCount,
        keptQuizCount = ok.draftStats?.keptQuizCount,
        droppedEvidenceCount = ok.draftStats?.droppedEvidenceCount,
        repairAttempted = repairAttempted,
        repairResult = repairResult,
    )

    private fun validationFail(
        provider: String,
        latencyMs: Long,
        fallbackUsed: Boolean,
        fail: Processed.Fail,
        repairAttempted: Boolean,
        repairResult: String?,
    ) = RedactedLogEntry(
        provider = provider,
        status = "OK",
        latencyMs = latencyMs,
        validation = "FAIL",
        fallbackUsed = fallbackUsed,
        errorType = errorTypeFor(fail).name,
        validationErrorType = fail.validationErrorType,
        responseContentLength = fail.contentLength,
        jsonExtracted = fail.jsonExtracted,
        parseStrategy = fail.parseStrategy,
        draftKpCount = fail.draftStats?.draftKpCount,
        draftQuizCount = fail.draftStats?.draftQuizCount,
        keptKpCount = fail.draftStats?.keptKpCount,
        keptQuizCount = fail.draftStats?.keptQuizCount,
        droppedEvidenceCount = fail.draftStats?.droppedEvidenceCount,
        repairAttempted = repairAttempted,
        repairResult = repairResult,
    )

    private fun transportFail(
        provider: String,
        latencyMs: Long,
        fallbackUsed: Boolean,
        error: ProviderError,
        repairAttempted: Boolean = false,
        repairResult: String? = null,
    ) = RedactedLogEntry(
        provider = provider,
        status = "FAIL",
        latencyMs = latencyMs,
        validation = "SKIPPED",
        fallbackUsed = fallbackUsed,
        errorType = error.type.name,
        requestProfile = error.requestProfile,
        timeoutMs = error.timeoutMs,
        networkSubtype = error.networkSubtype,
        model = error.model,
        maxTokens = error.maxTokens,
        repairAttempted = repairAttempted,
        repairResult = repairResult,
    )

    private sealed interface Processed {
        data class Ok(
            val result: CourseAnalysisResult,
            val report: ValidationReport,
            val contentLength: Int,
            val parseStrategy: String,
            val draftStats: LlmDraftAssemblyStats?,
        ) : Processed

        data class Fail(
            val validationErrorType: String,
            val report: ValidationReport?,
            val contentLength: Int,
            val jsonExtracted: Boolean,
            val parseStrategy: String,
            val draftStats: LlmDraftAssemblyStats?,
            val repairHint: String,
        ) : Processed
    }
}
