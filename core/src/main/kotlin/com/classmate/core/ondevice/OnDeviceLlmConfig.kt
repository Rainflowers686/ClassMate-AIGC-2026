package com.classmate.core.ondevice

/**
 * Runtime configuration for the on-device BlueLM 3B SDK. Maps 1:1 to the official `LlmConfig` public
 * fields: modelPath / nPredict / nCtx / nThreads / topK / topP / temperature / npuPower / multimodal.
 *
 * The model directory default is the official cloud-real-machine preset ([DEFAULT_MODEL_DIR]); the
 * model file names inside it are vendor-fixed and must NOT be renamed. We never scan or enumerate
 * user files under this path — it is only carried as a label and handed to the SDK when present.
 */
data class OnDeviceLlmConfig(
    val modelPath: String = DEFAULT_MODEL_DIR,
    val nPredict: Int = DEFAULT_N_PREDICT,
    val nCtx: Int = DEFAULT_N_CTX,
    val nThreads: Int = DEFAULT_N_THREADS,
    val npuPower: Int = DEFAULT_NPU_POWER,
    val temperature: Double = 0.6,
    val topP: Double = 0.9,
    val topK: Int = 40,
    /** Set true to init the multimodal (VIT) pipeline; pure-text generation keeps this false. */
    val multimodal: Boolean = false,
) {
    /** Apply a [OnDeviceLlmTaskProfile]'s sampling preset on top of the device base config. */
    fun withProfile(profile: OnDeviceLlmTaskProfile): OnDeviceLlmConfig = copy(
        temperature = profile.temperature,
        topP = profile.topP,
        topK = profile.topK,
        nPredict = profile.maxOutputTokens,
    )

    companion object {
        /** The versioned official model folder name from the latest (2026-06-11) SDK doc/Java sample. */
        const val OFFICIAL_MODEL_SUBDIR = "1.7.0.4_1225_mtk9500"

        /** Pre-2026-06-11 docs/demos used the bare directory; kept as a legacy candidate. */
        const val LEGACY_MODEL_DIR = "/sdcard/1225"

        /**
         * Official preset model directory on the X300 Pro cloud real machine per the 2026-06-11 doc
         * update: the versioned subdirectory is the real model root. Do not rename files inside it.
         */
        const val DEFAULT_MODEL_DIR = "$LEGACY_MODEL_DIR/$OFFICIAL_MODEL_SUBDIR"

        // Device-permitting defaults: keep context and NPU power high where the hardware allows,
        // without assuming a specific device here (the real bridge clamps to capabilities).
        const val DEFAULT_N_PREDICT = 512
        const val DEFAULT_N_CTX = 4096
        const val DEFAULT_N_THREADS = 4
        const val DEFAULT_NPU_POWER = 3
    }
}
