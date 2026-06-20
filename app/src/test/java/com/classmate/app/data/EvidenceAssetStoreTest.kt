package com.classmate.app.data

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceAssetStoreTest {
    @Test
    fun imageAssetStorePersistsImageAndThumbnailRefs() {
        val dir = Files.createTempDirectory("cm-evidence-assets").toFile()
        val stored = EvidenceAssetStore(dir).saveImage(
            bytes = byteArrayOf(1, 2, 3, 4, 5),
            sourceLabel = "board photo",
            mimeType = "image/png",
            now = 1234L,
        )

        assertEquals("image/png", stored.mimeType)
        assertEquals("board photo", stored.sourceLabel)
        assertTrue(stored.imageRef.endsWith("image_asset_1234.png"))
        assertTrue(stored.thumbnailRef.endsWith("image_asset_1234_thumb.png"))
        assertTrue(java.io.File(stored.imageRef).exists())
        assertTrue(java.io.File(stored.thumbnailRef).exists())
    }
}
