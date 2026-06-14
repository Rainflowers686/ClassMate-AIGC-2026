package com.classmate.core.ondevice

import java.io.File

/**
 * A bounded, content-free check of the official on-device model directory (Task 3). It only ever
 * asks `exists()` / `canRead()` on the directory and two KNOWN file names — it never lists the
 * directory, never enumerates files, and never reads file contents. The path is only ever surfaced
 * as a label; it is never written into a StudyReport.
 *
 * The two known files come from the official cloud-real-machine preset under [OnDeviceLlmConfig.DEFAULT_MODEL_DIR]:
 *  - [TOKENIZER_FILE] `bluelm_3b_model_vocab.bin` (its absence/unreadability is the usual cause of INIT_-2105)
 *  - [CONFIG_FILE]    `bluelm_mtk_llm_config.json`
 */
data class OnDeviceModelFileStatus(
    val modelDir: String,
    val modelDirExists: Boolean,
    val modelDirReadable: Boolean,
    val tokenizerExists: Boolean,
    val tokenizerReadable: Boolean,
    val configExists: Boolean,
    val configReadable: Boolean,
) {
    /** Short k=v lines for the redacted diagnostic UI. Only the dir label + booleans — no listing. */
    fun safeLines(): List<String> = listOf(
        "model_dir=$modelDir",
        "model_dir_exists=$modelDirExists",
        "model_dir_readable=$modelDirReadable",
        "tokenizer_exists=$tokenizerExists",
        "tokenizer_readable=$tokenizerReadable",
        "config_exists=$configExists",
        "config_readable=$configReadable",
    )

    companion object {
        const val TOKENIZER_FILE = "bluelm_3b_model_vocab.bin"
        const val CONFIG_FILE = "bluelm_mtk_llm_config.json"
    }
}

object OnDeviceModelFileProbe {
    /**
     * Probe [modelDir] and the two known model files. Pure filesystem metadata only — bounded to
     * `exists()`/`canRead()` on three fixed paths. Never throws; on any error the field is false.
     */
    fun probe(modelDir: String): OnDeviceModelFileStatus {
        val dir = safeFile(modelDir)
        val tokenizer = if (dir != null) safeChild(dir, OnDeviceModelFileStatus.TOKENIZER_FILE) else null
        val config = if (dir != null) safeChild(dir, OnDeviceModelFileStatus.CONFIG_FILE) else null
        return OnDeviceModelFileStatus(
            modelDir = modelDir,
            modelDirExists = dir.existsSafely(),
            modelDirReadable = dir.readableSafely(),
            tokenizerExists = tokenizer.existsSafely(),
            tokenizerReadable = tokenizer.readableSafely(),
            configExists = config.existsSafely(),
            configReadable = config.readableSafely(),
        )
    }

    private fun safeFile(path: String): File? =
        runCatching { File(path) }.getOrNull()

    private fun safeChild(dir: File, name: String): File? =
        runCatching { File(dir, name) }.getOrNull()

    private fun File?.existsSafely(): Boolean = this != null && runCatching { exists() }.getOrDefault(false)

    private fun File?.readableSafely(): Boolean = this != null && runCatching { canRead() }.getOrDefault(false)
}
