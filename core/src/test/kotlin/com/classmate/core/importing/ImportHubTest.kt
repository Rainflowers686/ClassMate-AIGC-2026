package com.classmate.core.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportHubTest {
    @Test
    fun textAndMarkdownDraftsAreAccepted() {
        val txt = ImportHub.validateText("Lesson", "plain transcript", ImportSourceType.TXT_FILE, "lesson.txt")
        val md = ImportHub.validateText("Lesson", "# notes", ImportSourceType.MARKDOWN_FILE, "lesson.md")

        assertTrue(txt.accepted)
        assertTrue(md.accepted)
        assertTrue(txt.draft?.text?.contains("plain transcript") == true)
        assertTrue(md.draft?.text?.contains("# notes") == true)
    }

    @Test
    fun placeholderSourcesDoNotCreateDraftsAndUseHonestCopy() {
        val expected = mapOf(
            ImportSourceType.AUDIO_FILE to ImportCapabilityStatus.NEEDS_CONFIG,
            ImportSourceType.VIDEO_FILE to ImportCapabilityStatus.UNSUPPORTED,
            ImportSourceType.IMAGE_OCR to ImportCapabilityStatus.NEEDS_CONFIG,
            ImportSourceType.NETWORK_VIDEO_LINK to ImportCapabilityStatus.UNSUPPORTED,
        )
        expected.forEach { (source, status) ->
            val result = ImportHub.validateText("Lesson", "content", source)
            assertFalse(result.accepted)
            assertTrue(result.draft == null)
            assertEquals(status, ImportHub.capabilities.first { it.sourceType == source }.status)
            assertTrue(result.message.contains("粘贴") || result.message.contains("不解析") || result.message.contains("不抓取") || result.message.contains("手动"))
        }
    }

    @Test
    fun everyImportCapabilityHasExplicitStatus() {
        assertEquals(ImportCapabilityStatus.AVAILABLE, ImportHub.capabilities.first { it.sourceType == ImportSourceType.PASTE_TEXT }.status)
        assertEquals(ImportCapabilityStatus.AVAILABLE, ImportHub.capabilities.first { it.sourceType == ImportSourceType.TXT_FILE }.status)
        assertEquals(ImportCapabilityStatus.AVAILABLE, ImportHub.capabilities.first { it.sourceType == ImportSourceType.MARKDOWN_FILE }.status)
        assertTrue(ImportHub.capabilities.all { it.label.isNotBlank() && it.message.isNotBlank() })
    }
}
