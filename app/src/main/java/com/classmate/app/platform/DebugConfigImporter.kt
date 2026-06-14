package com.classmate.app.platform

import com.classmate.app.BuildConfig
import com.classmate.core.model.ProviderKind
import com.classmate.core.provider.ProviderConfigBundle
import com.classmate.core.validation.ProviderConfigSafetyCheck
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A non-sensitive summary of a pasted config, shown in the debug import preview. */
data class ConfigImportPreview(
    val valid: Boolean,
    val providersFound: List<String>,
    val bluelmConfigured: Boolean,
    val containsRealSecret: Boolean,
    val providerSummaries: List<ProviderSummary>,
    val runtimeConfig: ProviderConfigBundle?,
    val message: String,
)

/**
 * The PLANNED debug-only entry point for injecting real BlueLM credentials on a developer's
 * device. In round 1 it only *inspects* a pasted config and reports a safe summary — it does
 * not persist anything, and it never echoes a secret value (only field names / booleans).
 *
 * Wiring this to actually build a [com.classmate.core.provider.ProviderConfigBundle] and hand
 * it to the analyzer is a later step, gated behind BuildConfig.DEBUG. Real values still come
 * from config.local.json or this on-device paste — never from the repository.
 */
object DebugConfigImporter {

    private val repository = ConfigRepository()

    fun inspect(jsonText: String, debugEnabled: Boolean = BuildConfig.DEBUG): ConfigImportPreview {
        if (!debugEnabled) {
            return ConfigImportPreview(
                valid = false,
                providersFound = emptyList(),
                bluelmConfigured = false,
                containsRealSecret = false,
                providerSummaries = emptyList(),
                runtimeConfig = null,
                message = "Release build does not expose debug config import.",
            )
        }
        if (jsonText.isBlank()) {
            return ConfigImportPreview(false, emptyList(), false, false, emptyList(), null, "请输入配置 JSON")
        }
        val root = try {
            Json.parseToJsonElement(jsonText)
        } catch (e: Exception) {
            return ConfigImportPreview(false, emptyList(), false, false, emptyList(), null, "无法解析 JSON")
        }
        val rootObject = root as? JsonObject
        val providers = rootObject?.get("providers") as? JsonObject
        val topLevelProvider = rootObject.str("provider")?.toProviderKind()
        val names = providers?.keys?.toList()
            ?: listOfNotNull(topLevelProvider?.name?.lowercase())
        val bluelm = providers?.get("bluelm") as? JsonObject
            ?: rootObject.takeIf { topLevelProvider == ProviderKind.BLUELM }
        val appKey = (bluelm?.get("appKey") as? JsonPrimitive)?.takeIf { it.isString }?.content ?: ""

        val bluelmConfigured = ProviderConfigSafetyCheck.isRealSecret(appKey)
        val safety = ProviderConfigSafetyCheck.inspectExampleConfig(jsonText)
        val containsReal = !safety.isExampleSafe

        val parsed = repository.parseOrDefault(jsonText, source = "debug import")
        val runtimeConfig = if (parsed.error == null) parsed.bundle else null
        val message = when {
            bluelmConfigured -> "已识别 BlueLM 凭据（仅保存在本地内存，不写入仓库）。"
            containsReal -> "检测到疑似真实密钥字段：${safety.findings.joinToString()}。请确认这是本地配置。"
            else -> "仅检测到占位符，未发现真实密钥。"
        }
        return ConfigImportPreview(
            valid = true,
            providersFound = names,
            bluelmConfigured = bluelmConfigured,
            containsRealSecret = containsReal,
            providerSummaries = parsed.summary.providers,
            runtimeConfig = runtimeConfig,
            message = message,
        )
    }
}

private fun JsonObject?.str(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

private fun String.toProviderKind(): ProviderKind? =
    when (trim().lowercase()) {
        "bluelm", "blue_lm", "blue-lm" -> ProviderKind.BLUELM
        "compatible", "openai", "openai-compatible" -> ProviderKind.COMPATIBLE
        "localfallback", "local_fallback", "local-fallback", "local" -> ProviderKind.LOCAL_FALLBACK
        else -> null
    }
