package com.classmate.app.l3

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFileManagerTest {

    @Test
    fun cleanupOrphansDeletesZeroByteRecordings() {
        val dir = Files.createTempDirectory("cm-recording-orphans").toFile()
        val zero = java.io.File(dir, "recording_zero.m4a")
        zero.writeBytes(ByteArray(0))

        val result = RecordingFileManager(dir).cleanupOrphans(emptySet())

        assertTrue(result.success)
        assertFalse(zero.exists())
    }

    @Test
    fun deleteForRecordsRemovesOnlyAppPrivateRecordings() {
        val dir = Files.createTempDirectory("cm-recording-delete").toFile()
        val file = java.io.File(dir, "recording_1.m4a")
        file.writeBytes(byteArrayOf(1, 2, 3))
        val record = ClassroomRecordingRecord(
            id = "recording_1",
            title = "Physics",
            createdAt = 1L,
            artifactFileName = file.name,
            artifactPath = file.absolutePath,
            fileSizeBytes = file.length(),
        )

        val result = RecordingFileManager(dir).deleteForRecords(listOf(record))

        assertTrue(result.success)
        assertEquals(1, result.deletedCount)
        assertFalse(file.exists())
    }

    @Test
    fun usableRequiresNonEmptyRecordingFile() {
        val dir = Files.createTempDirectory("cm-recording-usable").toFile()
        val file = java.io.File(dir, "recording_2.m4a")
        file.writeBytes(byteArrayOf(1))
        val record = ClassroomRecordingRecord(
            id = "recording_2",
            title = "Physics",
            createdAt = 1L,
            artifactFileName = file.name,
            artifactPath = file.absolutePath,
            fileSizeBytes = file.length(),
        )

        assertTrue(RecordingFileManager(dir).isUsable(record))
        file.writeBytes(ByteArray(0))
        assertFalse(RecordingFileManager(dir).isUsable(record))
    }
}
