package com.classmate.core.importing

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
        listOf(
            ImportSourceType.AUDIO_FILE,
            ImportSourceType.VIDEO_FILE,
            ImportSourceType.IMAGE_OCR,
            ImportSourceType.NETWORK_VIDEO_LINK,
        ).forEach { source ->
            val result = ImportHub.validateText("Lesson", "content", source)
            assertFalse(result.accepted)
            assertTrue(result.draft == null)
            assertTrue(result.message.contains("not connected", ignoreCase = true) || result.message.contains("No platform scraping"))
            assertTrue(result.message.contains("paste", ignoreCase = true))
        }
    }
}
