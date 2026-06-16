package com.classmate.app.ui.flow

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientSoundCatalogTest {

    private fun firstExisting(vararg candidates: String): File =
        candidates.map { File(it) }.firstOrNull { it.exists() } ?: File(candidates.first())

    @Test
    fun catalogHasSixLicensedRawSounds() {
        assertTrue(AmbientSoundCatalog.all.size >= 6)
        val ids = AmbientSoundCatalog.all.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
        AmbientSoundCatalog.all.forEach { sound ->
            assertTrue(sound.rawResId != 0)
            assertTrue(sound.fileName.startsWith("flow_"))
            assertTrue(sound.fileName.endsWith(".mp3"))
            assertTrue(sound.originalUrl.startsWith("https://assets.mixkit.co/"))
            assertTrue(sound.licenseName.contains("Mixkit"))
            assertTrue(sound.licenseUrl == AmbientSoundCatalog.LICENSE_URL)
            assertFalse(sound.attributionRequired)
            assertTrue(sound.commercialUseAllowed)
            val raw = firstExisting(
                "src/main/res/raw/${sound.fileName}",
                "app/src/main/res/raw/${sound.fileName}",
            )
            assertTrue("missing raw file ${sound.fileName}", raw.exists())
            assertTrue("raw file is empty ${sound.fileName}", raw.length() > 1024)
        }
    }

    @Test
    fun licenseDocumentRecordsAllBundledLoops() {
        val doc = firstExisting(
            "../docs/current/ambient_audio_assets.md",
            "docs/current/ambient_audio_assets.md",
        ).readText()
        AmbientSoundCatalog.all.forEach { sound ->
            assertTrue(doc.contains(sound.fileName))
            assertTrue(doc.contains(sound.originalUrl))
            assertTrue(doc.contains(sound.licenseName))
        }
        assertFalse(doc.contains("AI 生成白噪音"))
        assertFalse(doc.contains("实时生成背景音"))
        assertFalse(doc.contains("unclear", ignoreCase = true))
    }

    @Test
    fun playerSupportsLoopAndVolumeWithoutExtraPermissions() {
        val source = firstExisting(
            "src/main/java/com/classmate/app/ui/flow/AmbientSoundPlayer.kt",
            "app/src/main/java/com/classmate/app/ui/flow/AmbientSoundPlayer.kt",
        ).readText()
        assertTrue(source.contains("MediaPlayer"))
        assertTrue(source.contains("isLooping = true"))
        assertTrue(source.contains("setVolume"))
        assertFalse(source.contains("RECORD_AUDIO"))
    }
}
