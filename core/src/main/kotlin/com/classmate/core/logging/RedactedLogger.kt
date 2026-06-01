package com.classmate.core.logging

/**
 * A log line that is SAFE BY CONSTRUCTION. There is simply nowhere to put an appId, appKey,
 * Authorization header, prompt, course text, or vendor response body — the type only has the
 * whitelisted fields. This is the enforcement mechanism behind the logging rules in
 * SECURITY.md, not just a convention.
 */
data class RedactedLogEntry(
    val provider: String,        // ProviderKind.name
    val status: String,          // "OK" | "FAIL"
    val latencyMs: Long,
    val validation: String,      // "PASS" | "FAIL" | "SKIPPED"
    val fallbackUsed: Boolean,
    val errorType: String? = null, // short enum name only, e.g. "HTTP_NON_2XX"
    // Safe diagnostics for the parse/validation stage. All non-sensitive: a short classification,
    // a length (a number, never the body), and a boolean. No prompt / body / secret can appear.
    val validationErrorType: String? = null, // JSON_PARSE_FAILED | SCHEMA_MISSING_FIELD | EVIDENCE_MISMATCH | REFERENCE_BROKEN
    val responseContentLength: Int? = null,
    val jsonExtracted: Boolean? = null,
    val requestProfile: String? = null, // DIAGNOSTIC | ANALYSIS | DEFAULT
    val timeoutMs: Long? = null,
    val networkSubtype: String? = null, // DNS | CONNECT | TLS | WRITE | READ | SOCKET_TIMEOUT | UNKNOWN
    val model: String? = null,
    val maxTokens: Int? = null,
    val parseStrategy: String? = null,
    val draftKpCount: Int? = null,
    val draftQuizCount: Int? = null,
    val keptKpCount: Int? = null,
    val keptQuizCount: Int? = null,
    val droppedEvidenceCount: Int? = null,
    val repairAttempted: Boolean? = null,
    val repairResult: String? = null,
) {
    /** A single, copy-pasteable line for proof/logs_redacted. */
    fun format(): String = buildString {
        append("provider=").append(provider)
        append(" status=").append(status)
        append(" latency_ms=").append(latencyMs)
        append(" validation=").append(validation)
        append(" fallback_used=").append(fallbackUsed)
        if (errorType != null) append(" error_type=").append(errorType)
        if (validationErrorType != null) append(" validation_error_type=").append(validationErrorType)
        if (responseContentLength != null) append(" response_content_length=").append(responseContentLength)
        if (jsonExtracted != null) append(" json_extracted=").append(jsonExtracted)
        if (requestProfile != null) append(" request_profile=").append(requestProfile)
        if (timeoutMs != null) append(" timeout_ms=").append(timeoutMs)
        if (networkSubtype != null) append(" network_subtype=").append(networkSubtype)
        if (model != null) append(" model=").append(model)
        if (maxTokens != null) append(" max_tokens=").append(maxTokens)
        if (parseStrategy != null) append(" parse_strategy=").append(parseStrategy)
        if (draftKpCount != null) append(" draft_kp_count=").append(draftKpCount)
        if (draftQuizCount != null) append(" draft_quiz_count=").append(draftQuizCount)
        if (keptKpCount != null) append(" kept_kp_count=").append(keptKpCount)
        if (keptQuizCount != null) append(" kept_quiz_count=").append(keptQuizCount)
        if (droppedEvidenceCount != null) append(" dropped_evidence_count=").append(droppedEvidenceCount)
        if (repairAttempted != null) append(" repair_attempted=").append(repairAttempted)
        if (repairResult != null) append(" repair_result=").append(repairResult)
    }
}

interface RedactedLogger {
    fun log(entry: RedactedLogEntry)
    fun entries(): List<RedactedLogEntry>
}

/** Default in-memory sink. The UI can render these; exporting them yields a safe audit log. */
class InMemoryRedactedLogger : RedactedLogger {
    private val buffer = mutableListOf<RedactedLogEntry>()
    override fun log(entry: RedactedLogEntry) { buffer += entry }
    override fun entries(): List<RedactedLogEntry> = buffer.toList()
}

/** Defensive helper for the rare place a string must be shown near sensitive data. */
object Redaction {
    fun mask(secret: String?): String = "***"
}
