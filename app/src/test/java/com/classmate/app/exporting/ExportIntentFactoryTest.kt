package com.classmate.app.exporting

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportIntentFactoryTest {
    private val artifact = ExportArtifact(
        displayName = "PDF",
        fileName = "ClassMate-report.pdf",
        mimeType = "application/pdf",
        format = ExportFileFormat.PDF,
        bytes = "%PDF-1.4".toByteArray(),
        createdAt = 1L,
    )

    @Test
    fun saveAsSpecUsesCreateDocumentWithMimeAndFileName() {
        val spec = ExportIntentFactory.saveAsSpec(artifact)

        assertEquals(Intent.ACTION_CREATE_DOCUMENT, spec.action)
        assertEquals("application/pdf", spec.mimeType)
        assertEquals("ClassMate-report.pdf", spec.fileName)
    }

    @Test
    fun shareSpecUsesSendAndGrantsRead() {
        val spec = ExportIntentFactory.shareSpec(artifact)

        assertEquals(Intent.ACTION_SEND, spec.action)
        assertEquals("application/pdf", spec.mimeType)
        assertTrue(spec.grantRead)
    }
}

