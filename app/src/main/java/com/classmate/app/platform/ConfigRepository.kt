package com.classmate.app.platform

import com.classmate.core.model.ProviderKind
import com.classmate.core.provider.Credential
import com.classmate.core.provider.ProviderConfig
import com.classmate.core.provider.ProviderConfigBundle
import com.classmate.core.provider.ResolverPolicy
import com.classmate.core.validation.ProviderConfigSafetyCheck
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

data class ProviderConfigSummary(
    val source: String,
    val activeProvider: String,
    val providerOrder: List<String>,
    val blueLmConfigured: Boolean,
    val primaryReady: Boolean,
    val localFallbackEnabled: Boolean,
    val providers: List<ProviderSummary>,
) {
    companion object {
        fun defaults(): ProviderConfigSummary =
            fromBundle("defaults", ProviderConfigBundle.defaults(), primaryReady = false)

        fun fromBundle(
            source: String,
            bundle: ProviderConfigBundle,
            primaryReady: Boolean,
        ): ProviderConfigSummary {
            val providers = bundle.configs.values
                .sortedBy { it.kind.ordinal }
                .map { ProviderSummary.fromConfig(it) }
            return ProviderConfigSummary(
                source = source,
                activeProvider = bundle.primary.name,
                providerOrder = bundle.order.map { it.name },
                blueLmConfigured = bundle.configOf(ProviderKind.BLUELM)?.hasRealCredential() == true,
                primaryReady = primaryReady,
                localFallbackEnabled = bundle.configOf(ProviderKind.LOCAL_FALLBACK)?.enabled == true,
                providers = providers,
            )
        }
    }
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
                maskedAppKey = (credential as? Credential.BlueLm)?.appKey.maskForUi(),
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
        val bluelm = providers?.obj("bluelm")
        val compatible = providers?.obj("compatible")
        val localFallback = providers?.obj("localFallback")

        val bluelmConfig = buildBlueLmConfig(defaults.configOf(ProviderKind.BLUELM), bluelm)
        val compatibleConfig = buildCompatibleConfig(defaults.configOf(ProviderKind.COMPATIBLE), compatible)
        val localConfig = ProviderConfig(
            kind = ProviderKind.LOCAL_FALLBACK,
            enabled = localFallback.bool("enabled", defaults.configOf(ProviderKind.LOCAL_FALLBACK)?.enabled ?: true),
        )

        val order = root.obj("resolver")
            ?.arrayStrings("order")
            ?.mapNotNull { it.toProviderKind() }
            ?.takeIf { it.isNotEmpty() }
            ?: defaults.order

        val primary = root.str("activeProvider")?.toProviderKind() ?: defaults.primary
        val resolver = root.obj("resolver")
        val bundle = ProviderConfigBundle(
            primary = primary,
            order = order,
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
        return ConfigLoadResult(
            bundle = bundle,
            summary = ProviderConfigSummary.fromBundle(source, bundle, primaryReady = false),
        )
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
        )
    }

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

private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject

private fun JsonObject.arrayStrings(key: String): List<String>? =
    (get(key) as? kotlinx.serialization.json.JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.takeIf { primitive -> primitive.isString }?.content }
