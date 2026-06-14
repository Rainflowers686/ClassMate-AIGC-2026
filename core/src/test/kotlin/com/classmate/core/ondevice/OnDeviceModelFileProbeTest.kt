package com.classmate.core.ondevice

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceModelFileProbeTest {

    @Test
    fun missingDirectoryReportsEverythingAbsent() {
        val dir = Files.createTempDirectory("cm-probe").resolve("does-not-exist").toString()
        val status = OnDeviceModelFileProbe.probe(dir)
        assertFalse(status.modelDirExists)
        assertFalse(status.tokenizerExists)
        assertFalse(status.configExists)
    }

    @Test
    fun presentKnownFilesReportExistsAndReadable() {
        val dir = Files.createTempDirectory("cm-probe2").toFile()
        File(dir, OnDeviceModelFileStatus.TOKENIZER_FILE).writeText("VOCAB")
        File(dir, OnDeviceModelFileStatus.CONFIG_FILE).writeText("{}")

        val status = OnDeviceModelFileProbe.probe(dir.absolutePath)
        assertTrue(status.modelDirExists)
        assertTrue(status.modelDirReadable)
        assertTrue(status.tokenizerExists)
        assertTrue(status.tokenizerReadable)
        assertTrue(status.configExists)
        assertTrue(status.configReadable)
    }

    @Test
    fun probeNeverReadsFileContent() {
        val dir = Files.createTempDirectory("cm-probe3").toFile()
        File(dir, OnDeviceModelFileStatus.TOKENIZER_FILE).writeText("SECRET-VOCAB-CONTENT")
        val blob = OnDeviceModelFileProbe.probe(dir.absolutePath).safeLines().joinToString("\n")

        assertFalse("safeLines must not echo file content", blob.contains("SECRET-VOCAB-CONTENT"))
        assertTrue(blob.contains("tokenizer_exists=true"))
        assertTrue(blob.contains("model_dir=${dir.absolutePath}"))
    }

    @Test
    fun knownFileNamesMatchOfficialPreset() {
        assertEquals("bluelm_3b_model_vocab.bin", OnDeviceModelFileStatus.TOKENIZER_FILE)
        assertEquals("bluelm_mtk_llm_config.json", OnDeviceModelFileStatus.CONFIG_FILE)
    }
}
