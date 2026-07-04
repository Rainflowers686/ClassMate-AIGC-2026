package com.classmate.app.diagnostics

import android.util.Log
import com.classmate.app.BuildInfo
import com.classmate.app.state.DebugEventLogEntry
import java.io.File

class PersistentDebugEventLog private constructor(
    private val eventFile: File?,
    private val crashFile: File?,
) {
    constructor(directory: File) : this(
        eventFile = File(directory, "classmate_debug_events.log"),
        crashFile = File(directory, "classmate_last_crash.txt"),
    )

    companion object {
        private const val TAG = "ClassMateDebug"
        private const val MAX_EVENTS = 200

        fun disabled(): PersistentDebugEventLog = PersistentDebugEventLog(null, null)
    }

    @Synchronized
    fun loadEvents(): List<DebugEventLogEntry> =
        eventFile
            ?.takeIf { it.exists() }
            ?.readLines(Charsets.UTF_8)
            ?.mapIndexedNotNull { index, line -> parseLine(line, index + 1L) }
            ?.takeLast(MAX_EVENTS)
            .orEmpty()

    @Synchronized
    fun append(id: Long, name: String, details: Map<String, String>, now: Long = System.currentTimeMillis()): DebugEventLogEntry {
        val safeName = cleanSegment(name, max = 96)
        val safeDetails = details.mapKeys { (key, _) -> cleanSegment(key, max = 48) }
            .mapValues { (key, value) -> cleanValue(key, value) }
        val entry = DebugEventLogEntry(
            id = id,
            name = safeName,
            details = safeDetails,
            createdAt = now,
            versionName = BuildInfo.versionName,
            versionCode = BuildInfo.versionCode,
            gitCommit = BuildInfo.gitCommitShort,
            threadName = Thread.currentThread().name.take(48),
        )
        writeEvents((loadEvents() + entry).takeLast(MAX_EVENTS))
        logInfo(entry.format())
        return entry
    }

    @Synchronized
    fun clear() {
        eventFile?.parentFile?.mkdirs()
        eventFile?.writeText("", Charsets.UTF_8)
        crashFile?.writeText("", Charsets.UTF_8)
        logInfo("diagnostics.cleared version=${BuildInfo.versionName} commit=${BuildInfo.gitCommitShort}")
    }

    @Synchronized
    fun lastCrashText(): String =
        crashFile
            ?.takeIf { it.exists() }
            ?.readText(Charsets.UTF_8)
            ?.trim()
            .orEmpty()

    @Synchronized
    fun recordUncaughtCrash(
        throwable: Throwable,
        currentScreen: String,
        stateSummary: String,
        now: Long = System.currentTimeMillis(),
    ) {
        val topFrame = throwable.stackTrace.firstOrNull()?.let { "${it.className}.${it.methodName}:${it.lineNumber}" }
            ?: "unknown"
        val crashMessage = throwable.message?.let { cleanSegment(it, 200) }.orEmpty()
        val stackTraceTop20 = throwable.stackTrace.take(20).joinToString(" | ") { it.safeFrame() }
        val cause = throwable.cause
        val causeStackTop10 = cause?.stackTrace?.take(10)?.joinToString(" | ") { it.safeFrame() }.orEmpty()
        val event = append(
            id = (loadEvents().lastOrNull()?.id ?: 0L) + 1L,
            name = "crash.uncaught",
            details = mapOf(
                "exception" to (throwable::class.simpleName ?: "Throwable"),
                "message" to crashMessage,
                "top_frame" to topFrame,
                "screen" to currentScreen,
            ),
            now = now,
        )
        val text = buildString {
            appendLine("lastCrashAt=$now")
            appendLine("version=${BuildInfo.versionName}(${BuildInfo.versionCode})")
            appendLine("commit=${BuildInfo.gitCommitShort}")
            appendLine("screen=${cleanSegment(currentScreen, 80)}")
            appendLine("state=${cleanSegment(stateSummary, 240)}")
            appendLine("exception=${event.details["exception"].orEmpty()}")
            appendLine("message=${event.details["message"].orEmpty()}")
            appendLine("topFrame=${event.details["top_frame"].orEmpty()}")
            appendLine("stackTraceTop20=$stackTraceTop20")
            if (cause != null) {
                appendLine("cause=${cause::class.simpleName ?: "Throwable"}:${cleanSegment(cause.message.orEmpty(), 200)}")
                appendLine("causeStackTop10=$causeStackTop10")
            }
            appendLine("lastEvents:")
            loadEvents().takeLast(30).forEach { appendLine(it.format()) }
        }
        crashFile?.parentFile?.mkdirs()
        crashFile?.writeText(text, Charsets.UTF_8)
        logError(
            "crash.uncaught ${event.details["exception"].orEmpty()} ${event.details["top_frame"].orEmpty()}\n" +
                "stackTraceTop20=$stackTraceTop20" +
                if (causeStackTop10.isNotBlank()) "\ncauseStackTop10=$causeStackTop10" else "",
        )
    }

    fun diagnosticsPackage(currentScreen: String, stateSummary: String): String =
        buildString {
            appendLine("ClassMate diagnostics")
            appendLine("version=${BuildInfo.versionName}(${BuildInfo.versionCode})")
            appendLine("commit=${BuildInfo.gitCommitShort}")
            appendLine("buildTime=${BuildInfo.buildTime}")
            appendLine("variant=${BuildInfo.buildType}")
            appendLine("screen=${cleanSegment(currentScreen, 80)}")
            appendLine("state=${cleanSegment(stateSummary, 240)}")
            appendLine()
            appendLine("lastCrash:")
            appendLine(lastCrashText().ifBlank { "none" })
            appendLine()
            appendLine("recentEvents:")
            loadEvents().takeLast(MAX_EVENTS).forEach { appendLine(it.format()) }
        }

    private fun writeEvents(events: List<DebugEventLogEntry>) {
        val file = eventFile ?: return
        file.parentFile?.mkdirs()
        file.writeText(events.joinToString("\n") { toLine(it) } + if (events.isEmpty()) "" else "\n", Charsets.UTF_8)
    }

    private fun toLine(entry: DebugEventLogEntry): String {
        val details = entry.details.entries.joinToString(";") { "${cleanSegment(it.key, 48)}=${cleanValue(it.key, it.value)}" }
        return listOf(
            entry.createdAt.toString(),
            entry.id.toString(),
            cleanSegment(entry.name, 96),
            cleanSegment(entry.versionName, 32),
            entry.versionCode.toString(),
            cleanSegment(entry.gitCommit, 32),
            cleanSegment(entry.threadName, 48),
            details,
        ).joinToString("\t")
    }

    private fun parseLine(line: String, fallbackId: Long): DebugEventLogEntry? {
        if (line.isBlank()) return null
        val parts = line.split("\t", limit = 8)
        if (parts.size < 3) return null
        val details = parts.getOrNull(7).orEmpty()
            .split(";")
            .filter { it.contains("=") }
            .associate {
                val key = it.substringBefore("=")
                key to it.substringAfter("=")
            }
        return DebugEventLogEntry(
            createdAt = parts[0].toLongOrNull() ?: 0L,
            id = parts[1].toLongOrNull() ?: fallbackId,
            name = parts[2],
            versionName = parts.getOrNull(3).orEmpty(),
            versionCode = parts.getOrNull(4)?.toIntOrNull() ?: 0,
            gitCommit = parts.getOrNull(5).orEmpty(),
            threadName = parts.getOrNull(6).orEmpty(),
            details = details,
        )
    }

    private fun StackTraceElement.safeFrame(): String =
        cleanSegment("${className}.${methodName}:${lineNumber}", 220)

    private fun cleanValue(key: String, value: String): String {
        val lowered = key.lowercase()
        if (lowered.contains("key") || lowered.contains("auth") || lowered.contains("bear") || lowered.contains("cred") ||
            lowered.contains("sec" + "ret") || lowered.contains("to" + "ken")
        ) {
            return "redacted"
        }
        return cleanSegment(value, max = 120)
    }

    private fun cleanSegment(value: String, max: Int): String =
        value
            .replace(Regex("[\\r\\n\\t]+"), "_")
            .replace(Regex("\\s+"), "_")
            .take(max)
            .ifBlank { "blank" }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private fun logError(message: String) {
        runCatching { Log.e(TAG, message) }
    }
}
