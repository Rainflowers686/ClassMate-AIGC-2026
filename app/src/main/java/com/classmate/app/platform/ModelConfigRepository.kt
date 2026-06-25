package com.classmate.app.platform

import com.classmate.core.validation.ProviderConfigSafetyCheck
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

enum class AiModelProviderMode {
    OFFICIAL_BLUELM,
    CUSTOM,
}

/**
 * Persisted AI model config. It can hold both the official BlueLM config and a custom compatible
 * model config; [mode] selects the active provider. Real secrets live only in the app-private file
 * passed to [ModelConfigRepository] and are never returned to UI callers.
 */
data class ModelApiProfile(
    val label: String = DEFAULT_LABEL,
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val appId: String = "",
    val appKey: String = "",
    val mode: AiModelProviderMode = AiModelProviderMode.OFFICIAL_BLUELM,
    val customApiKey: String = "",
    val customAdvancedJson: String = "",
) {
    fun officialConfigured(): Boolean =
        ProviderConfigSafetyCheck.isRealSecret(appId) && ProviderConfigSafetyCheck.isRealSecret(appKey)

    fun customConfigured(): Boolean =
        ProviderConfigSafetyCheck.isRealSecret(customApiKey) && customAdvancedJsonValid()

    fun hasRealCredential(): Boolean = when (mode) {
        AiModelProviderMode.OFFICIAL_BLUELM -> officialConfigured()
        AiModelProviderMode.CUSTOM -> customConfigured()
    }

    fun customAdvancedJsonValid(): Boolean =
        customAdvancedJson.isBlank() || runCatching { json.parseToJsonElement(customAdvancedJson).jsonObject }.isSuccess

    fun customBaseUrl(default: String): String =
        customConfigValue("baseUrl")
            ?: customConfigValue("endpoint")
            ?: customConfigValue("url")
            ?: default

    fun customModel(default: String): String =
        customConfigValue("model") ?: default

    fun masked(): MaskedModelProfile = MaskedModelProfile(
        label = label,
        baseUrl = baseUrl,
        model = model,
        mode = mode,
        activeProviderLabel = if (mode == AiModelProviderMode.CUSTOM) "其他模型" else "蓝心大模型",
        credentialPresent = hasRealCredential(),
        officialConfigured = officialConfigured(),
        customConfigured = customConfigured(),
        maskedAppId = appId.maskForUi(),
        maskedAppKey = appKey.maskForUi(),
        maskedCustomApiKey = customApiKey.maskForUi(),
        customAdvancedJsonPresent = customAdvancedJson.isNotBlank(),
    )

    companion object {
        const val DEFAULT_LABEL = "蓝心大模型"
        const val DEFAULT_BASE_URL = "https://api-ai.vivo.com.cn/v1"
        const val DEFAULT_MODEL = "qwen3.5-plus"
        const val DEFAULT_APP_ID = "2026374747"

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }

    private fun customConfigValue(key: String): String? =
        runCatching {
            if (customAdvancedJson.isBlank()) null
            else json.parseToJsonElement(customAdvancedJson).jsonObject.str(key)
        }.getOrNull()
}

/** What the Settings page is allowed to render: presence + masked tails only, never secrets. */
data class MaskedModelProfile(
    val label: String,
    val baseUrl: String,
    val model: String,
    val mode: AiModelProviderMode = AiModelProviderMode.OFFICIAL_BLUELM,
    val activeProviderLabel: String = "蓝心大模型",
    val credentialPresent: Boolean,
    val officialConfigured: Boolean = credentialPresent,
    val customConfigured: Boolean = false,
    val maskedAppId: String,
    val maskedAppKey: String,
    val maskedCustomApiKey: String = "absent",
    val customAdvancedJsonPresent: Boolean = false,
)

/**
 * Persists AI model config across restarts. The production app passes an app-private file under
 * filesDir; tests can pass a temp file. [disabled] is an inert no-op used by default call sites.
 */
class ModelConfigRepository(private val file: File?) {

    val isEnabled: Boolean get() = file != null

    fun load(): ModelApiProfile? {
        val f = file ?: return null
        if (!f.exists()) return null
        val text = try {
            f.readText()
        } catch (e: Exception) {
            return null
        }
        val obj = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (e: Exception) {
            return null
        } ?: return null
        return ModelApiProfile(
            label = obj.str("label") ?: ModelApiProfile.DEFAULT_LABEL,
            baseUrl = obj.str("baseUrl") ?: ModelApiProfile.DEFAULT_BASE_URL,
            model = obj.str("model") ?: ModelApiProfile.DEFAULT_MODEL,
            appId = obj.str("appId").orEmpty(),
            appKey = obj.str("appKey").orEmpty(),
            mode = when (obj.str("mode")?.trim()?.uppercase()) {
                AiModelProviderMode.CUSTOM.name -> AiModelProviderMode.CUSTOM
                else -> AiModelProviderMode.OFFICIAL_BLUELM
            },
            customApiKey = obj.str("customApiKey").orEmpty(),
            customAdvancedJson = obj.str("customAdvancedJson").orEmpty(),
        )
    }

    /** True only when the active provider has real, non-placeholder credentials. */
    fun hasUsableProfile(): Boolean = load()?.hasRealCredential() == true

    fun masked(): MaskedModelProfile? = load()?.masked()

    fun save(profile: ModelApiProfile): Boolean {
        val f = file ?: return false
        if (!profile.customAdvancedJsonValid()) return false
        return try {
            f.parentFile?.mkdirs()
            f.writeText(serialize(profile))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveOfficial(baseUrl: String, model: String, appId: String, appKey: String): Boolean {
        // Never persist a UI-masked value (e.g. "ab***yz") as a real credential — it would overwrite a
        // working AppKey with a mask and turn cloud calls into auth/garbage failures. Reject the save.
        if (ProviderConfigSafetyCheck.isMaskedSecret(appKey) || ProviderConfigSafetyCheck.isMaskedSecret(appId)) {
            return false
        }
        val current = load()
        return save(
            ModelApiProfile(
                label = ModelApiProfile.DEFAULT_LABEL,
                baseUrl = baseUrl.trim().ifBlank { ModelApiProfile.DEFAULT_BASE_URL },
                model = model.trim().ifBlank { ModelApiProfile.DEFAULT_MODEL },
                appId = appId.trim(),
                appKey = appKey.trim(),
                mode = AiModelProviderMode.OFFICIAL_BLUELM,
                customApiKey = current?.customApiKey.orEmpty(),
                customAdvancedJson = current?.customAdvancedJson.orEmpty(),
            ),
        )
    }

    fun saveCustom(apiKey: String, advancedJson: String): Boolean {
        val current = load()
        return save(
            ModelApiProfile(
                label = current?.label ?: ModelApiProfile.DEFAULT_LABEL,
                baseUrl = current?.baseUrl ?: ModelApiProfile.DEFAULT_BASE_URL,
                model = current?.model ?: ModelApiProfile.DEFAULT_MODEL,
                appId = current?.appId.orEmpty(),
                appKey = current?.appKey.orEmpty(),
                mode = AiModelProviderMode.CUSTOM,
                customApiKey = apiKey.trim(),
                customAdvancedJson = advancedJson.trim(),
            ),
        )
    }

    fun setMode(mode: AiModelProviderMode): Boolean {
        val current = load() ?: ModelApiProfile(mode = mode)
        return save(current.copy(mode = mode))
    }

    /** Remove all persisted model config. Kept for older callers/tests. */
    fun delete(): Boolean {
        val f = file ?: return false
        return try {
            if (f.exists()) f.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    fun deleteOfficial(): Boolean {
        val current = load() ?: return delete()
        val nextMode = if (current.customConfigured()) AiModelProviderMode.CUSTOM else AiModelProviderMode.OFFICIAL_BLUELM
        return save(current.copy(appId = "", appKey = "", mode = nextMode))
    }

    fun deleteCustom(): Boolean {
        val current = load() ?: return delete()
        return save(
            current.copy(
                customApiKey = "",
                customAdvancedJson = "",
                mode = AiModelProviderMode.OFFICIAL_BLUELM,
            ),
        )
    }

    private fun serialize(profile: ModelApiProfile): String = buildJsonObject {
        put("label", profile.label)
        put("baseUrl", profile.baseUrl)
        put("model", profile.model)
        put("appId", profile.appId)
        put("appKey", profile.appKey)
        put("mode", profile.mode.name)
        put("customApiKey", profile.customApiKey)
        put("customAdvancedJson", profile.customAdvancedJson)
    }.toString()

    companion object {
        fun disabled(): ModelConfigRepository = ModelConfigRepository(null)

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

private fun String?.maskForUi(): String =
    when {
        this == null || isBlank() -> "absent"
        ProviderConfigSafetyCheck.isPlaceholder(this) -> "placeholder"
        length <= 4 -> "***"
        else -> take(2) + "***" + takeLast(2)
    }
