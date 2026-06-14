package com.classmate.app.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultimodalImportCatalogTest {

    @Test
    fun multimodalEntriesIncludeRequiredSources() {
        val ids = MultimodalImportCatalog.entries.map { it.id }.toSet()

        assertTrue(MultimodalEntryId.AUDIO in ids)
        assertTrue(MultimodalEntryId.VIDEO in ids)
        assertTrue(MultimodalEntryId.SLIDE_IMAGE in ids)
        assertTrue(MultimodalEntryId.BLACKBOARD_PHOTO in ids)
        assertTrue(MultimodalEntryId.PDF_HANDOUT in ids)
        assertTrue(MultimodalEntryId.NETWORK_VIDEO_LINK in ids)
    }

    @Test
    fun unsupportedEntriesDoNotRequireNetworkOrProviderCalls() {
        MultimodalImportCatalog.entries.filterNot { it.availableNow }.forEach { entry ->
            assertFalse(entry.requiresNetwork)
        }
    }

    @Test
    fun linkEntryStatesNoCrawling() {
        val link = MultimodalImportCatalog.byId(MultimodalEntryId.NETWORK_VIDEO_LINK)

        assertTrue(link.detail.contains("不抓取"))
        assertTrue(link.detail.contains("文本") || link.detail.contains("字幕"))
    }

    @Test
    fun fileMetadataSummaryIsLocalOnly() {
        val metadata = SelectedLocalFileMetadata(
            entryTitle = "本地音频",
            fileName = "lesson-audio.m4a",
            mimeType = "audio/mp4",
            sizeBytes = 1234L,
        )

        assertEquals("本地音频", metadata.entryTitle)
        assertTrue(metadata.summary().contains("只记录元数据"))
        assertTrue(metadata.summary().contains("不解析"))
        assertTrue(metadata.summary().contains("不上传"))
    }
}
