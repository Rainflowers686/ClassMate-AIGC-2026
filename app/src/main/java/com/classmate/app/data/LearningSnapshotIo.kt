package com.classmate.app.data

import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.SnapshotIo
import java.io.File
import kotlinx.serialization.json.Json

/**
 * File-backed [SnapshotIo] for the cross-course learning state
 * (filesDir/classmate_learning_state.json). Persists ONLY business data — the snapshot type has
 * no fields for credentials, prompts, request/response bodies, or reasoning content.
 */
class FileSnapshotIo(private val file: File) : SnapshotIo {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun read(): LearningSnapshot = try {
        if (!file.exists()) LearningSnapshot() else json.decodeFromString(LearningSnapshot.serializer(), file.readText())
    } catch (e: Exception) {
        LearningSnapshot()
    }

    override fun write(snapshot: LearningSnapshot) {
        try {
            file.writeText(json.encodeToString(LearningSnapshot.serializer(), snapshot))
        } catch (e: Exception) {
            // Best-effort; never crash the learning flow on an I/O error.
        }
    }
}
