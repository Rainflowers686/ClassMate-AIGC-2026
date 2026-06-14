package com.classmate.core.ondevice

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 8E Phase 1 — bounded model-directory detection: fixed candidates only (never a disk scan),
 * first-level file NAMES only, official versioned path preferred, legacy kept as fallback.
 */
class OnDeviceModelPathDetectorTest {

    private fun dirWith(vararg names: String): String {
        val dir = Files.createTempDirectory("cm-8e-model").toFile()
        names.forEach { File(dir, it).writeText("x") }
        return dir.absolutePath
    }

    private val textFiles = arrayOf(
        "bluelm_3b_model_vocab.bin",
        "bluelm_mtk_llm_config.json",
        "bluelm_3b_shared_weights_0.bin",
        "bluelm_3b_embedding_int8.bin",
        "bluelm_3b_prompt.dla",
    )

    @Test
    fun candidateListIsFixedOfficialFirstAndNeverScans() {
        val candidates = OnDeviceModelPathDetector.candidatePaths("/custom/path")
        // The user path leads, then EXACTLY the four official candidates — nothing else is probed.
        assertEquals(
            listOf(
                "/custom/path",
                "/sdcard/1225/1.7.0.4_1225_mtk9500",
                "/storage/emulated/0/1225/1.7.0.4_1225_mtk9500",
                "/sdcard/1225",
                "/storage/emulated/0/1225",
            ),
            candidates,
        )
        // A user path equal to a fixed candidate is deduplicated (still bounded).
        assertEquals(4, OnDeviceModelPathDetector.candidatePaths("/sdcard/1225").size)
    }

    @Test
    fun inspectDetectsKeyFilesIncludingVitAndDla() {
        val full = OnDeviceModelPathDetector.inspect(dirWith(*textFiles, "shared_weights_vit_0128.bin"))
        assertTrue(full.exists)
        assertTrue(full.readable)
        assertTrue(full.hasVocab)
        assertTrue(full.hasConfig)
        assertTrue(full.hasBaseWeights)
        assertTrue(full.hasEmbedding)
        assertTrue(full.hasDla)
        assertTrue(full.hasVitWeights)
        assertTrue(full.textComplete)
        assertTrue(full.multimodalComplete)
    }

    @Test
    fun textOnlyDirectoryIsTextCompleteButNotMultimodal() {
        val textOnly = OnDeviceModelPathDetector.inspect(dirWith(*textFiles))
        assertTrue(textOnly.textComplete)
        assertFalse(textOnly.multimodalComplete) // no shared_weights_vit_*.bin
    }

    @Test
    fun missingDirectoryIsHonestlyIncomplete() {
        val missing = OnDeviceModelPathDetector.inspect("/definitely/not/here/cm-8e")
        assertFalse(missing.exists)
        assertFalse(missing.textComplete)
        assertEquals(0, missing.score)
    }

    @Test
    fun recommendsStrictlyMoreCompleteCandidateLikeOfficialSubdirOverLegacy() {
        val legacyLike = dirWith("bluelm_3b_model_vocab.bin") // user still points at an incomplete dir
        val officialLike = dirWith(*textFiles, "shared_weights_vit_0128.bin")

        val detection = OnDeviceModelPathDetector.detect(legacyLike, candidates = listOf(legacyLike, officialLike))

        assertTrue(detection.shouldSwitch)
        assertEquals(officialLike, detection.recommended?.path)
        assertEquals(legacyLike, detection.current.path)
    }

    @Test
    fun completeCurrentPathIsNeverNaggedToSwitch() {
        val current = dirWith(*textFiles, "shared_weights_vit_0128.bin")
        val other = dirWith(*textFiles)

        val detection = OnDeviceModelPathDetector.detect(current, candidates = listOf(current, other))

        assertFalse(detection.shouldSwitch)
        assertNull(detection.recommended)
    }

    @Test
    fun safeLinesCarryBooleansOnlyNeverAFileListing() {
        val candidate = OnDeviceModelPathDetector.inspect(dirWith(*textFiles, "shared_weights_vit_0128.bin"))
        val blob = candidate.safeLines().joinToString("\n")
        assertTrue(blob.contains("multimodal_complete=true"))
        // The actual file names beyond the path itself are never echoed.
        assertFalse(blob.contains("shared_weights_vit_0128.bin"))
        assertFalse(blob.contains("bluelm_3b_model_vocab.bin"))
    }
}
