package com.classmate.app.data

import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One persisted analysis. Stores only learning business data needed to re-open the timeline —
 * NEVER credentials, prompts, request/response bodies, or reasoning content. The session/result
 * are the app's own domain objects, not vendor payloads.
 */
@Serializable
data class HistoryRecord(
    val id: String,
    val title: String,
    val createdAtEpochMs: Long,
    val providerName: String,
    val profileLabel: String,
    val model: String,
    val knowledgePointCount: Int,
    val quizCount: Int,
    val fallbackUsed: Boolean,
    val validationStatus: String,
    val session: CourseSession,
    val result: CourseAnalysisResult,
)

/** Persistence seam so the ViewModel stays testable (default in-memory) and Android-free. */
interface HistoryStore {
    fun load(): List<HistoryRecord>
    fun save(records: List<HistoryRecord>)
}

/** Default for tests / no-context: keeps records in memory only. */
class InMemoryHistoryStore(initial: List<HistoryRecord> = emptyList()) : HistoryStore {
    private var records: List<HistoryRecord> = initial
    override fun load(): List<HistoryRecord> = records
    override fun save(records: List<HistoryRecord>) { this.records = records }
}

/** Real device store: a single JSON file under filesDir. No extra dependency. */
class FileHistoryStore(private val file: File) : HistoryStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val serializer = ListSerializer(HistoryRecord.serializer())

    override fun load(): List<HistoryRecord> = try {
        if (!file.exists()) emptyList() else json.decodeFromString(serializer, file.readText())
    } catch (e: Exception) {
        emptyList()
    }

    override fun save(records: List<HistoryRecord>) {
        try {
            file.writeText(json.encodeToString(serializer, records))
        } catch (e: Exception) {
            // Persistence is best-effort; never crash the learning flow on an I/O error.
        }
    }
}
