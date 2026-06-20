package com.classmate.app.data

import com.classmate.app.l3.LocalSemanticIndexRecord
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSemanticIndexRepositoryTest {
    @Test
    fun semanticIndexPersistsAndReloadsLocalRecords() {
        val file = Files.createTempDirectory("cm-semantic-index").resolve("index.json").toFile()
        val repo = LocalSemanticIndexRepository(file)
        val records = listOf(
            LocalSemanticIndexRecord(
                id = "r1",
                sourceType = "TEXT",
                sourceId = "lesson_1",
                ownerType = "EVIDENCE",
                ownerId = "ev_1",
                text = "电磁感应和磁通量变化有关",
                embeddingStatus = "LOCAL_LEXICAL_VECTOR",
                vector = listOf(0.5, 0.5),
                tokens = listOf("电磁感应", "磁通量"),
                createdAt = 1_700_000_000_000L,
            ),
        )

        repo.save(records)
        val loaded = repo.load()

        assertEquals(1, loaded.size)
        assertEquals("ev_1", loaded.single().ownerId)
        assertTrue(loaded.single().text.contains("磁通量"))
        assertEquals(listOf(0.5, 0.5), loaded.single().vector)
    }
}
