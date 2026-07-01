package com.classmate.app.platform

import com.classmate.core.model.ProviderKind
import com.classmate.core.provider.Credential
import com.classmate.core.provider.LearnerProfile
import com.classmate.core.provider.ProviderConfig
import com.classmate.core.provider.ProviderConfigBundle
import com.classmate.core.provider.ResolverPolicy
import com.classmate.core.validation.ProviderConfigSafetyCheck
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

data class ProviderConfigSummary(
    val source: String,
    val profile: String,
    val profileLabel: String,
    val activeProvider: String,
    val providerOrder: List<String>,
    val blueLmConfigured: Boolean,
    val compatibleConfigured: Boolean,
    val primaryReady: Boolean,
    val localFallbackEnabled: Boolean,
    val providers: List<ProviderSummary>,
    val officialProviders: OfficialProviderConfigSummary = OfficialProviderConfigSummary(),
) {
    /**
     * Short, honest mode label for the Home chip: the first provider in the resolver order that
     * is actually ready to serve. Mirrors what the analyzer would pick, so the chip never lies.
     */
    val modeLabel: String
        get() {
            for (name in providerOrder) {
                when (name) {
                    ProviderKind.BLUELM.name -> if (blueLmConfigured) return "官方 BlueLM"
                    ProviderKind.COMPATIBLE.name -> if (compatibleConfigured) return "自有模型"
                    ProviderKind.LOCAL_FALLBACK.name -> if (localFallbackEnabled) return "安全占位"
                }
            }
            return "安全占位"
        }

    companion object {
        fun defaults(): ProviderConfigSummary =
            fromBundle("defaults", ProviderConfigBundle.defaults(), primaryReady = false)

        fun fromBundle(
            source: String,
            bundle: ProviderConfigBundle,
            primaryReady: Boolean,
            officialProviders: OfficialProviderConfigSummary = OfficialProviderConfigSummary(),
        ): ProviderConfigSummary {
            val providers = bundle.configs.values
                .sortedBy { it.kind.ordinal }
                .map { ProviderSummary.fromConfig(it) }
            return ProviderConfigSummary(
                source = source,
                profile = bundle.profile.name,
                profileLabel = bundle.profile.displayName,
                activeProvider = bundle.primary.name,
                providerOrder = bundle.order.map { it.name },
                blueLmConfigured = bundle.configOf(ProviderKind.BLUELM)?.hasRealCredential() == true,
                compatibleConfigured = bundle.configOf(ProviderKind.COMPATIBLE)?.hasRealCredential() == true,
                primaryReady = primaryReady,
                localFallbackEnabled = bundle.configOf(ProviderKind.LOCAL_FALLBACK)?.enabled == true,
                providers = providers,
                officialProviders = officialProviders,
            )
        }
    }
}

data class OfficialProviderConfigSummary(
    val ocrConfigured: Boolean = false,
    val queryRewriteConfigured: Boolean = false,
    val textSimilarityConfigured: Boolean = false,
    val embeddingConfigured: Boolean = false,
    val translationConfigured: Boolean = false,
    val ttsConfigured: Boolean = false,
    val functionCallingConfigured: Boolean = false,
    val realtimeAsrConfigured: Boolean = false,
    val asrLongConfigured: Boolean = false,
) {
    val anyConfigured: Boolean
        get() = listOf(
            ocrConfigured,
            queryRewriteConfigured,
            textSimilarityConfigured,
            embeddingConfigured,
            translationConfigured,
            ttsConfigured,
            functionCallingConfigured,
            realtimeAsrConfigured,
            asrLongConfigured,
        ).any { it }
}

data class ProviderSummary(
    val provider: String,
    val enabled: Boolean,
    val baseUrl: String,
    val model: String,
    val credentialPresent: Boolean,
    val maskedAppId: String,
    val maskedAppKey: String,
) {
    companion object {
        fun fromConfig(config: ProviderConfig): ProviderSummary {
            val credential = config.credential
            return ProviderSummary(
                provider = config.kind.name,
                enabled = config.enabled,
                baseUrl = config.baseUrl,
                model = config.model,
                credentialPresent = config.hasRealCredential(),
                maskedAppId = (credential as? Credential.BlueLm)?.appId.maskForUi(),
                maskedAppKey = when (credential) {
                    is Credential.BlueLm -> credential.appKey.maskForUi()
                    is Credential.ApiKey -> credential.apiKey.maskForUi()
                    else -> null.maskForUi()
                },
            )
        }
    }
}

data class SafeConfigError(
    val code: String,
    val message: String,
)

data class ConfigLoadResult(
    val bundle: ProviderConfigBundle,
    val summary: ProviderConfigSummary,
    val error: SafeConfigError? = null,
)

class ConfigRepository(
    private val localConfigFile: File = defaultLocalConfigFile(),
) {
    val localConfigPath: String = localConfigFile.absoluteFile.toPath().normalize().toString()

    fun loadLocalOrDefault(): ConfigLoadResult {
        if (!localConfigFile.exists()) {
            return fallback("CONFIG_NOT_FOUND", "$LOCAL_CONFIG_FILE was not found; using safe fallback providers.")
        }
        val text = try {
            localConfigFile.readText()
        } catch (e: Exception) {
            return fallback("CONFIG_READ_FAILED", "Could not read $LOCAL_CONFIG_FILE; using safe fallback providers.")
        }
        return parseOrDefault(text, source = "config.local.json")
    }

    fun parseOrDefault(jsonText: String, source: String): ConfigLoadResult {
        val root = try {
            json.parseToJsonElement(jsonText) as? JsonObject
        } catch (e: Exception) {
            return fallback("CONFIG_INVALID_JSON", "Config JSON is invalid; using safe fallback providers.")
        } ?: return fallback("CONFIG_INVALID_ROOT", "Config root must be a JSON object; using safe fallback providers.")

        val defaults = ProviderConfigBundle.defaults()
        val providers = root.obj("providers")
        val topLevelProvider = root.str("provider")?.toProviderKind()
        // The profile (official_bluelm / demo_compatible / local_only) is the source of truth for
        // the resolver order; if absent we infer it from the top-level provider.
        val profile = LearnerProfile.fromWire(root.str("profile")) ?: inferProfile(topLevelProvider)

        val bluelm = providers?.obj("bluelm") ?: root.takeIf { topLevelProvider == ProviderKind.BLUELM }
        val compatible = providers?.obj("compatible") ?: root.takeIf { topLevelProvider == ProviderKind.COMPATIBLE }

        // BlueLM stays usable as primary (official) or secondary (demo); disabled only in local-only.
        val bluelmConfig = buildBlueLmConfig(defaults.configOf(ProviderKind.BLUELM), bluelm)
            .copy(enabled = profile != LearnerProfile.LOCAL_ONLY)
        // Compatible is only enabled in the explicitly-labelled demo profile.
        val compatibleConfig = buildCompatibleConfig(defaults.configOf(ProviderKind.COMPATIBLE), compatible)
            .copy(enabled = profile == LearnerProfile.DEMO_COMPATIBLE)
        val localConfig = ProviderConfig(kind = ProviderKind.LOCAL_FALLBACK, enabled = true)

        val resolver = root.obj("resolver")
        val bundle = ProviderConfigBundle.forProfile(
            profile = profile,
            configs = mapOf(
                ProviderKind.BLUELM to bluelmConfig,
                ProviderKind.COMPATIBLE to compatibleConfig,
                ProviderKind.LOCAL_FALLBACK to localConfig,
            ),
            policy = ResolverPolicy(
                fallbackOnNon2xx = resolver.bool("fallbackOnNon2xx", defaults.policy.fallbackOnNon2xx),
                fallbackOnParseError = resolver.bool("fallbackOnParseError", defaults.policy.fallbackOnParseError),
                fallbackOnValidationError = resolver.bool("fallbackOnValidationError", defaults.policy.fallbackOnValidationError),
            ),
        )
        val officialProvidersSummary = parseOfficialProviders(root.obj("officialProviders"))
        return ConfigLoadResult(
            bundle = bundle,
            summary = ProviderConfigSummary.fromBundle(
                source = source,
                bundle = bundle,
                primaryReady = false,
                officialProviders = officialProvidersSummary,
            ),
        )
    }

    private fun inferProfile(topLevelProvider: ProviderKind?): LearnerProfile = when (topLevelProvider) {
        ProviderKind.COMPATIBLE -> LearnerProfile.DEMO_COMPATIBLE
        ProviderKind.LOCAL_FALLBACK -> LearnerProfile.LOCAL_ONLY
        else -> LearnerProfile.OFFICIAL_BLUELM
    }

    private fun buildBlueLmConfig(default: ProviderConfig?, json: JsonObject?): ProviderConfig {
        val appId = json.str("appId").orEmpty()
        val appKey = json.str("appKey").orEmpty()
        val credential = if (appId.isNotBlank() || appKey.isNotBlank()) {
            Credential.BlueLm(appId, appKey)
        } else {
            Credential.None
        }
        return ProviderConfig(
            kind = ProviderKind.BLUELM,
            enabled = json.bool("enabled", default?.enabled ?: true),
            baseUrl = json.str("baseUrl") ?: default?.baseUrl.orEmpty(),
            model = json.str("model") ?: default?.model.orEmpty(),
            timeoutMs = json.long("timeoutMs", default?.timeoutMs ?: 30_000),
            credential = credential,
            temperature = json.double("temperature", default?.temperature ?: 0.3),
            maxTokens = json.int("maxTokens", default?.maxTokens ?: 4096),
            stream = json.bool("stream", default?.stream ?: false),
            requestIdQueryName = json.str("requestIdQueryName") ?: default?.requestIdQueryName ?: "request_id",
        )
    }

    private fun buildCompatibleConfig(default: ProviderConfig?, json: JsonObject?): ProviderConfig {
        val apiKey = json.str("apiKey").orEmpty()
        return ProviderConfig(
            kind = ProviderKind.COMPATIBLE,
            enabled = json.bool("enabled", default?.enabled ?: false),
            baseUrl = json.str("baseUrl") ?: default?.baseUrl.orEmpty(),
            model = json.str("model") ?: default?.model.orEmpty(),
            timeoutMs = json.long("timeoutMs", default?.timeoutMs ?: 30_000),
            credential = if (apiKey.isBlank()) Credential.None else Credential.ApiKey(apiKey),
            temperature = json.double("temperature", default?.temperature ?: 0.3),
            maxTokens = json.int("maxTokens", default?.maxTokens ?: 4096),
            stream = json.bool("stream", default?.stream ?: false),
        )
    }

    private fun parseOfficialProviders(root: JsonObject?): OfficialProviderConfigSummary =
        OfficialProviderConfigSummary(
            ocrConfigured = root.providerReady("ocr"),
            queryRewriteConfigured = root.providerReady("queryRewrite", "query_rewrite"),
            textSimilarityConfigured = root.providerReady("textSimilarity", "text_similarity", "similarity"),
            embeddingConfigured = root.providerReady("embedding", "textVector", "text_vector"),
            translationConfigured = root.providerReady("translation"),
            ttsConfigured = root.providerReady("tts", "audioGeneration", "audio_generation"),
            functionCallingConfigured = root.providerReady("functionCalling", "function_calling"),
            realtimeAsrConfigured = root.providerReady("realtimeAsr", "asrRealtime", "asr_ws", "asrWebSocket"),
            asrLongConfigured = root.providerReady("asrLong", "asr_long", "longAsr"),
        )

    private fun fallback(code: String, message: String): ConfigLoadResult {
        val bundle = ProviderConfigBundle.defaults()
        return ConfigLoadResult(
            bundle = bundle,
            summary = ProviderConfigSummary.fromBundle("defaults", bundle, primaryReady = false),
            error = SafeConfigError(code, message),
        )
    }

    companion object {
        const val LOCAL_CONFIG_FILE = "config.local.json"

        fun defaultLocalConfigFile(): File = File(LOCAL_CONFIG_FILE).absoluteFile.toPath().normalize().toFile()

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

private fun String?.maskForUi(): String =
    when {
        this == null || isBlank() -> "absent"
        ProviderConfigSafetyCheck.isPlaceholder(this) -> "placeholder"
        length <= 4 -> "***"
        else -> take(2) + "***" + takeLast(2)
    }

private fun String.toProviderKind(): ProviderKind? =
    when (trim().lowercase()) {
        "bluelm", "blue_lm", "blue-lm" -> ProviderKind.BLUELM
        "compatible", "openai", "openai-compatible" -> ProviderKind.COMPATIBLE
        "localfallback", "local_fallback", "local-fallback", "local" -> ProviderKind.LOCAL_FALLBACK
        else -> null
    }

private fun JsonObject?.str(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

private fun JsonObject?.bool(key: String, default: Boolean): Boolean =
    (this?.get(key) as? JsonPrimitive)?.booleanOrNull ?: default

private fun JsonObject?.long(key: String, default: Long): Long =
    (this?.get(key) as? JsonPrimitive)?.longOrNull ?: default

private fun JsonObject?.int(key: String, default: Int): Int =
    (this?.get(key) as? JsonPrimitive)?.longOrNull?.toInt() ?: default

private fun JsonObject?.double(key: String, default: Double): Double =
    (this?.get(key) as? JsonPrimitive)?.doubleOrNull ?: default

private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject

private fun JsonObject.arrayStrings(key: String): List<String>? =
    (get(key) as? kotlinx.serialization.json.JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }

private fun JsonObject?.providerReady(vararg keys: String): Boolean {
    val group = keys.firstNotNullOfOrNull { this?.obj(it) } ?: return false
    val enabled = group.bool("enabled", true)
    val hasEndpoint = group.hasAny("baseUrl", "baseURL", "base_url", "url", "endpoint", "domain", "host")
    val hasAuth = group.hasAny(
        "authValue",
        "authorizationValue",
        "appKey",
        "appKEY",
        "app_key",
        "apiKey",
        "api_key",
        "token",
    )
    return enabled && hasEndpoint && hasAuth
}

private fun JsonObject.hasAny(vararg keys: String): Boolean =
    keys.any { key -> str(key)?.isNotBlank() == true }
