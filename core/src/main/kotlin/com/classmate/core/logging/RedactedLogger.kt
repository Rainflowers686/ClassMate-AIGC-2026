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
) {
    /** A single, copy-pasteable line for proof/logs_redacted. */
    fun format(): String = buildString {
        append("provider=").append(provider)
        append(" status=").append(status)
        append(" latency_ms=").append(latencyMs)
        append(" validation=").append(validation)
        append(" fallback_used=").append(fallbackUsed)
        if (errorType != null) append(" error_type=").append(errorType)
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
