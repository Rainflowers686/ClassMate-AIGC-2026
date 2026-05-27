package com.classmate.app.data

import android.content.Context
import android.util.Log
import com.classmate.core.adapter.BlueLmConfig
import com.classmate.core.adapter.CompatibleConfig
import com.classmate.core.adapter.ProviderConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads provider credentials from `config.local.json`.
 *
 * Search order — first hit wins:
 *   1. Internal storage:  filesDir/config.local.json
 *   2. App assets:        config.example.json (placeholder-only, never has real keys)
 *
 * config.local.json is NEVER bundled into the APK. It must be pushed onto the
 * device by the developer (e.g. `adb push config.local.json
 * /data/data/com.classmate.app/files/`) once real credentials are in play.
 *
 * v0.3.5 layout — top-level chooses the intended provider; the nested
 * "compatible" / "bluelm" objects hold per-provider credentials. The legacy
 * flat fields are still tolerated (parser uses default values) but only the
 * nested shape is documented.
 *
 * The repository never logs key material. Only field-present flags are
 * surfaced.
 */
object ApiConfigRepository {

    private const val LOG_TAG = "ApiConfigRepository"
    private const val LOCAL_FILE = "config.local.json"
    private const val EXAMPLE_ASSET = "config.example.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun load(context: Context): ResolvedConfig {
        val localFile = File(context.filesDir, LOCAL_FILE)
        if (localFile.exists()) {
            runCatching {
                val raw = json.decodeFromString(RawConfig.serializer(), localFile.readText())
                return ResolvedConfig(raw.toProviderConfig(), loadedFromLocalFile = true)
            }.onFailure { Log.w(LOG_TAG, "failed to parse $LOCAL_FILE; falling back", it) }
        }

        runCatching {
            val raw = context.assets.open(EXAMPLE_ASSET).bufferedReader().use { it.readText() }
            val parsed = json.decodeFromString(RawConfig.serializer(), raw)
            return ResolvedConfig(parsed.toProviderConfig(), loadedFromLocalFile = false)
        }.onFailure { Log.w(LOG_TAG, "failed to parse $EXAMPLE_ASSET asset; using hardcoded defaults", it) }

        // Hardcoded last resort — provider=demo so no network call ever happens.
        return ResolvedConfig(
            providerConfig = ProviderConfig(provider = "demo"),
            loadedFromLocalFile = false
        )
    }

    @Serializable
    private data class RawConfig(
        val provider: String = "demo",
        val compatible: RawCompatible = RawCompatible(),
        val bluelm: RawBlueLm = RawBlueLm()
    ) {
        fun toProviderConfig() = ProviderConfig(
            provider = provider,
            compatible = compatible.toConfig(),
            bluelm = bluelm.toConfig()
        )
    }

    @Serializable
    private data class RawCompatible(
        val api_base_url: String = "",
        val api_key: String = "",
        val model: String = ""
    ) {
        fun toConfig() = CompatibleConfig(
            apiBaseUrl = api_base_url,
            apiKey = api_key,
            model = model
        )
    }

    @Serializable
    private data class RawBlueLm(
        val api_base_url: String = "",
        val app_id: String = "",
        val app_key: String = "",
        val model: String = ""
    ) {
        fun toConfig() = BlueLmConfig(
            apiBaseUrl = api_base_url,
            appId = app_id,
            appKey = app_key,
            model = model
        )
    }

    data class ResolvedConfig(
        val providerConfig: ProviderConfig,
        val loadedFromLocalFile: Boolean
    ) {
        val provider: String get() = providerConfig.provider
    }
}
