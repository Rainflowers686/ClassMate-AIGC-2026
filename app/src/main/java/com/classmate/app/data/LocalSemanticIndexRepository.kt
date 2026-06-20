package com.classmate.app.data

import com.classmate.app.l3.LocalSemanticIndexRecord
import java.io.File
import java.util.Base64

class LocalSemanticIndexRepository(private val file: File? = null) {
    fun save(records: List<LocalSemanticIndexRecord>) {
        val target = file ?: return
        runCatching {
            target.parentFile?.mkdirs()
            target.writeText(encode(records), Charsets.UTF_8)
        }
    }

    fun load(): List<LocalSemanticIndexRecord> {
        val source = file ?: return emptyList()
        return runCatching {
            if (!source.exists()) emptyList() else decode(source.readText(Charsets.UTF_8))
        }.getOrDefault(emptyList())
    }

    companion object {
        fun disabled(): LocalSemanticIndexRepository = LocalSemanticIndexRepository(null)

        private fun encode(records: List<LocalSemanticIndexRecord>): String =
            buildString {
                appendLine("classmate_semantic_index_v1")
                records.forEach { record ->
                    appendLine(
                        listOf(
                            record.id,
                            record.sourceType,
                            record.sourceId,
                            record.ownerType,
                            record.ownerId,
                            record.embeddingStatus,
                            record.createdAt.toString(),
                            record.vector.joinToString(","),
                            record.tokens.joinToString("|") { token -> token.replace("|", "") },
                            Base64.getEncoder().encodeToString(record.text.toByteArray(Charsets.UTF_8)),
                            record.vectorSource,
                            record.localVector.joinToString(","),
                            record.officialVector.joinToString(","),
                        ).joinToString("\t"),
                    )
                }
            }

        private fun decode(raw: String): List<LocalSemanticIndexRecord> =
            raw.lineSequence()
                .drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("\t")
                    if (parts.size < 10) {
                        null
                    } else {
                        LocalSemanticIndexRecord(
                            id = parts[0],
                            sourceType = parts[1],
                            sourceId = parts[2],
                            ownerType = parts[3],
                            ownerId = parts[4],
                            text = String(Base64.getDecoder().decode(parts[9]), Charsets.UTF_8),
                            embeddingStatus = parts[5],
                            vector = parts[7].split(",").mapNotNull { it.toDoubleOrNull() },
                            tokens = parts[8].split("|").filter { it.isNotBlank() },
                            createdAt = parts[6].toLongOrNull() ?: 0L,
                            vectorSource = parts.getOrNull(10).orEmpty().ifBlank { parts[5] },
                            localVector = parts.getOrNull(11)?.split(",")?.mapNotNull { it.toDoubleOrNull() }
                                ?.takeIf { it.isNotEmpty() }
                                ?: parts[7].split(",").mapNotNull { it.toDoubleOrNull() },
                            officialVector = parts.getOrNull(12)?.split(",")?.mapNotNull { it.toDoubleOrNull() }.orEmpty(),
                        )
                    }
                }
                .toList()
    }
}
