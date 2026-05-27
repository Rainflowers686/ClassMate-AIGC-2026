package com.classmate.core.adapter

/**
 * Configuration loaded from `config.local.json` (or `config.example.json` as a
 * placeholder-only fallback).
 *
 * Each section is independent so a user can have BlueLM keys filled in
 * without a CompatibleProvider endpoint, or vice versa. Held as plain data so
 * the core module stays free of Android dependencies — the app module is
 * responsible for reading the file and instantiating this.
 *
 * The top-level [provider] is the *intent* (which provider the user wants).
 * Whether the matching section is actually usable is checked at call time —
 * see [CompatibleConfig.isUsable] / [BlueLmConfig.isUsable].
 */
data class ProviderConfig(
    val provider: String,
    val compatible: CompatibleConfig = CompatibleConfig(),
    val bluelm: BlueLmConfig = BlueLmConfig()
)

/**
 * OpenAI-compatible (chat/completions style) endpoint. Works with any vendor
 * whose API mirrors the OpenAI shape — DashScope, DeepSeek, vLLM, Together,
 * SiliconFlow, etc. — provided their API key is sent as `Authorization:
 * Bearer …`.
 */
data class CompatibleConfig(
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    val model: String = ""
) {
    /** True iff every field needed to build a real request is non-blank and not a placeholder. */
    fun isUsable(): Boolean =
        apiBaseUrl.isNotBlank() &&
            apiKey.isNotBlank() &&
            model.isNotBlank() &&
            !isPlaceholder(apiBaseUrl) &&
            !isPlaceholder(apiKey) &&
            !isPlaceholder(model)
}

/**
 * vivo BlueLM (蓝心) endpoint. Field shape is a best guess — the official
 * signing scheme is NOT in this repo. [isUsable] is conservative: it returns
 * false until the official contract is wired in. See BlueLMProvider.
 */
data class BlueLmConfig(
    val apiBaseUrl: String = "",
    val appId: String = "",
    val appKey: String = "",
    val model: String = ""
) {
    fun isUsable(): Boolean =
        apiBaseUrl.isNotBlank() &&
            appId.isNotBlank() &&
            appKey.isNotBlank() &&
            model.isNotBlank() &&
            !isPlaceholder(apiBaseUrl) &&
            !isPlaceholder(appId) &&
            !isPlaceholder(appKey) &&
            !isPlaceholder(model)
}

/**
 * Cheap heuristic: if a config value still reads like a template placeholder,
 * treat it as unconfigured. Catches the common "user forgot to fill in
 * config.local.json" case without lying about being ready.
 */
private fun isPlaceholder(value: String): Boolean {
    val v = value.trim().uppercase()
    return v.startsWith("YOUR_") ||
        v.startsWith("FILL_") ||
        v.contains("YOUR-COMPATIBLE-ENDPOINT") ||
        v.contains("EXAMPLE.API")
}
