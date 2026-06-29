package com.classmate.app.data

import com.classmate.core.exporting.ExportDocument
import com.classmate.core.exporting.ExportFormat
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportStoreTest {

    @Test
    fun inMemoryStoreDeletesOnlyMatchingCourseDrafts() {
        val store = InMemoryExportStore()
        store.save(document("Physics-study-pack.md"))
        store.save(document("Math-study-pack.md"))

        val result = store.deleteForCourse(courseTitles = setOf("Physics"), sessionIds = emptySet())

        assertTrue(result.success)
        assertEquals(1, result.deletedCount)
        assertEquals(listOf("Math-study-pack.md"), store.saved.map { it.fileName })
    }

    @Test
    fun fileStoreDeletesOnlyAppPrivateDraftsForCourse() {
        val dir = Files.createTempDirectory("cm-export-store").toFile()
        val store = FileExportStore(dir)
        store.save(document("lesson-session_1.md"))
        store.save(document("other.md"))

        val result = store.deleteForCourse(courseTitles = emptySet(), sessionIds = setOf("session_1"))

        assertTrue(result.success)
        assertEquals(1, result.deletedCount)
        assertFalse(java.io.File(dir, "lesson-session_1.md").exists())
        assertTrue(java.io.File(dir, "other.md").exists())
    }

    private fun document(fileName: String) = ExportDocument(
        fileName = fileName,
        format = ExportFormat.MARKDOWN,
        mimeType = ExportFormat.MARKDOWN.mimeType,
        content = "# study pack",
    )
}
