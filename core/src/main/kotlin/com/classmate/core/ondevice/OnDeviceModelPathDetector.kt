package com.classmate.core.ondevice

import java.io.File

/**
 * Stage 8E — bounded detection of the official on-device model directory (2026-06-11 doc update).
 *
 * It NEVER scans the disk: only the user-provided path plus four FIXED official candidates are
 * checked, and per candidate it reads nothing but `exists`/`canRead` and the FIRST-LEVEL file
 * names (`listFiles` names only — file contents are never opened). The result is used to recommend
 * switching to the most complete official directory, e.g. when the user still points at the legacy
 * `/sdcard/1225` while the versioned `1.7.0.4_1225_mtk9500` subfolder is present.
 */
data class ModelPathCandidate(
    val path: String,
    val exists: Boolean,
    val readable: Boolean,
    val hasVocab: Boolean,
    val hasConfig: Boolean,
    val hasBaseWeights: Boolean,
    val hasEmbedding: Boolean,
    val hasVitWeights: Boolean,
    val hasDla: Boolean,
) {
    /** Text-generation readiness (vocab + config + base weights + at least one DLA). */
    val textComplete: Boolean get() = exists && readable && hasVocab && hasConfig && hasBaseWeights && hasDla

    /** Full multimodal readiness: text plus the VIT weight files the 3B multimodal model needs. */
    val multimodalComplete: Boolean get() = textComplete && hasVitWeights

    /** Comparable completeness (higher = better candidate). */
    val score: Int
        get() = listOf(exists, readable, hasVocab, hasConfig, hasBaseWeights, hasEmbedding, hasDla, hasVitWeights)
            .count { it }

    /** Content-free k=v lines (booleans only — never a file listing). */
    fun safeLines(): List<String> = listOf(
        "candidate=$path",
        "exists=$exists",
        "readable=$readable",
        "vocab=$hasVocab config=$hasConfig base_weights=$hasBaseWeights embedding=$hasEmbedding",
        "dla=$hasDla vit_weights=$hasVitWeights",
        "text_complete=$textComplete multimodal_complete=$multimodalComplete",
    )
}

/** Outcome of one detection pass: every inspected candidate + the recommended switch (if any). */
data class ModelPathDetection(
    val current: ModelPathCandidate,
    val candidates: List<ModelPathCandidate>,
    /** Non-null when a DIFFERENT candidate is strictly more complete than the current path. */
    val recommended: ModelPathCandidate?,
) {
    val shouldSwitch: Boolean get() = recommended != null
}

object OnDeviceModelPathDetector {

    const val VOCAB_FILE = "bluelm_3b_model_vocab.bin"
    const val CONFIG_FILE = "bluelm_mtk_llm_config.json"
    const val BASE_WEIGHTS_FILE = "bluelm_3b_shared_weights_0.bin"
    const val EMBEDDING_FILE = "bluelm_3b_embedding_int8.bin"

    /** The FIXED candidate list (plus the user path). Nothing else is ever probed. */
    fun candidatePaths(userProvidedPath: String?): List<String> = buildList {
        userProvidedPath?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        add("/sdcard/1225/${OnDeviceLlmConfig.OFFICIAL_MODEL_SUBDIR}")
        add("/storage/emulated/0/1225/${OnDeviceLlmConfig.OFFICIAL_MODEL_SUBDIR}")
        add("/sdcard/1225")
        add("/storage/emulated/0/1225")
    }.distinct()

    /** Inspect one candidate: exists/canRead + first-level file NAMES only (no content reads). */
    fun inspect(path: String): ModelPathCandidate {
        val dir = runCatching { File(path) }.getOrNull()
        val exists = dir != null && runCatching { dir.exists() && dir.isDirectory }.getOrDefault(false)
        val readable = exists && runCatching { dir!!.canRead() }.getOrDefault(false)
        val names: List<String> =
            if (readable) runCatching { dir!!.listFiles()?.map { it.name } ?: emptyList() }.getOrDefault(emptyList())
            else emptyList()
        val lower = names.map { it.lowercase() }
        return ModelPathCandidate(
            path = path,
            exists = exists,
            readable = readable,
            hasVocab = VOCAB_FILE in names,
            hasConfig = CONFIG_FILE in names,
            hasBaseWeights = BASE_WEIGHTS_FILE in names,
            hasEmbedding = EMBEDDING_FILE in names,
            hasVitWeights = lower.any { it.startsWith("shared_weights_vit_") && it.endsWith(".bin") },
            hasDla = lower.any { it.startsWith("bluelm_3b_") && it.endsWith(".dla") },
        )
    }

    /**
     * Detect the best model directory. [recommended][ModelPathDetection.recommended] is set only when
     * another candidate is STRICTLY more complete than the current path (so a working setup is never
     * nagged to switch sideways). [candidates] is injectable ONLY for tests — production always uses
     * the fixed [candidatePaths] list (never a disk scan).
     */
    fun detect(
        currentPath: String,
        candidates: List<String> = candidatePaths(currentPath),
    ): ModelPathDetection {
        val inspected = candidates.map { inspect(it) }
        val current = inspected.firstOrNull { it.path == currentPath.trim() } ?: inspect(currentPath.trim())
        val best = inspected.maxByOrNull { it.score }
        val recommended = best?.takeIf { it.path != current.path && it.score > current.score && it.textComplete }
        return ModelPathDetection(current = current, candidates = inspected, recommended = recommended)
    }
}
