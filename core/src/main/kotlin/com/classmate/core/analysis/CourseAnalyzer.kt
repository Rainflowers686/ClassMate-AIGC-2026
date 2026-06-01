package com.classmate.core.analysis

import com.classmate.core.logging.InMemoryRedactedLogger
import com.classmate.core.logging.RedactedLogEntry
import com.classmate.core.logging.RedactedLogger
import com.classmate.core.model.AnalysisProvenance
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.ProviderKind
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
 * The single public entry point for turning a course into an analysis — and the only thing the
 * app talks to. It walks the resolver's ordered providers (BlueLM first); for each success it runs
 * extract -> parse -> validate. On a networked provider whose output parses/validates badly, it
 * performs ONE safe repair retry (a short corrective hint, never the bad response/course text),
 * then falls back. Every step emits a redacted log line with safe diagnostics
 * (validation_error_type / response_content_length / json_extracted) — never the body.
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

            fun process(rawText: String): Processed {
                val contentLength = rawText.length
                val jsonText = JsonExtractor.extract(rawText)
                    ?: return Processed.Fail("JSON_PARSE_FAILED", null, contentLength, jsonExtracted = false, repairHint = "上一次输出不是合法 JSON")
                val provenance = AnalysisProvenance(
                    provider = provider.kind,
                    fallbackUsed = fallbackUsed,
                    modelLabel = provider.kind.displayName,
                    createdAtEpochMs = clock(),
                )
                val parsed = parser.parse(jsonText, request.session, provenance)
                    ?: return Processed.Fail("JSON_PARSE_FAILED", null, contentLength, jsonExtracted = true, repairHint = "上一次输出不是合法 JSON")
                val report = validator.validate(parsed, request.session)
                if (report.ok) return Processed.Ok(parsed, report, contentLength)
                val vet = report.validationErrorType ?: "VALIDATION_ERROR"
                return Processed.Fail(vet, report, contentLength, jsonExtracted = true, repairHint = repairHintFor(vet))
            }

            when (val raw = provider.generate(request)) {
                is ProviderResult.Failure -> {
                    logger.log(transportFail(provider.kind.name, raw.latencyMs, fallbackUsed, raw.error.type.name))
                    lastError = raw.error
                }

                is ProviderResult.Success -> {
                    when (val first = process(raw.rawModelText)) {
                        is Processed.Ok -> {
                            logger.log(okEntry(provider.kind.name, raw.latencyMs, fallbackUsed, first.contentLength))
                            return AnalysisOutcome.Success(first.result, first.report, logger.entries())
                        }

                        is Processed.Fail -> {
                            logger.log(validationFail(provider.kind.name, raw.latencyMs, fallbackUsed, first))
                            lastError = ProviderError(errorTypeFor(first), provider.kind)
                            lastReport = first.report

                            // One-shot repair retry for networked providers only.
                            if (provider.kind != ProviderKind.LOCAL_FALLBACK) {
                                when (val raw2 = provider.generate(request.copy(repairHint = first.repairHint))) {
                                    is ProviderResult.Success -> when (val second = process(raw2.rawModelText)) {
                                        is Processed.Ok -> {
                                            logger.log(okEntry(provider.kind.name, raw2.latencyMs, fallbackUsed, second.contentLength))
                                            return AnalysisOutcome.Success(second.result, second.report, logger.entries())
                                        }
                                        is Processed.Fail -> {
                                            logger.log(validationFail(provider.kind.name, raw2.latencyMs, fallbackUsed, second))
                                            lastError = ProviderError(errorTypeFor(second), provider.kind)
                                            lastReport = second.report
                                        }
                                    }
                                    is ProviderResult.Failure -> {
                                        logger.log(transportFail(provider.kind.name, raw2.latencyMs, fallbackUsed, raw2.error.type.name))
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
        "EVIDENCE_MISMATCH" -> "证据片段无法在原文中逐字定位"
        "REFERENCE_BROKEN" -> "题目引用了不存在的知识点 title"
        "SCHEMA_MISSING_FIELD" -> "缺少必要字段或选项"
        else -> "上一次输出未通过校验"
    }

    private fun okEntry(provider: String, latencyMs: Long, fallbackUsed: Boolean, contentLength: Int) =
        RedactedLogEntry(
            provider = provider,
            status = "OK",
            latencyMs = latencyMs,
            validation = "PASS",
            fallbackUsed = fallbackUsed,
            errorType = null,
            validationErrorType = null,
            responseContentLength = contentLength,
            jsonExtracted = true,
        )

    private fun validationFail(provider: String, latencyMs: Long, fallbackUsed: Boolean, fail: Processed.Fail) =
        RedactedLogEntry(
            provider = provider,
            status = "OK",
            latencyMs = latencyMs,
            validation = "FAIL",
            fallbackUsed = fallbackUsed,
            errorType = errorTypeFor(fail).name,
            validationErrorType = fail.validationErrorType,
            responseContentLength = fail.contentLength,
            jsonExtracted = fail.jsonExtracted,
        )

    private fun transportFail(provider: String, latencyMs: Long, fallbackUsed: Boolean, errorType: String) =
        RedactedLogEntry(
            provider = provider,
            status = "FAIL",
            latencyMs = latencyMs,
            validation = "SKIPPED",
            fallbackUsed = fallbackUsed,
            errorType = errorType,
        )

    private sealed interface Processed {
        data class Ok(val result: CourseAnalysisResult, val report: ValidationReport, val contentLength: Int) : Processed
        data class Fail(
            val validationErrorType: String,
            val report: ValidationReport?,
            val contentLength: Int,
            val jsonExtracted: Boolean,
            val repairHint: String,
        ) : Processed
    }
}
