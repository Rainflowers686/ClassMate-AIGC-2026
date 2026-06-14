package com.classmate.app.platform

import com.classmate.core.validation.ProviderConfigSafetyCheck
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * One persisted official-model API profile. Only the official cloud BlueLM path is modelled here —
 * the competition main config page manages exactly this (蓝心大模型). [appId]/[appKey] are secrets:
 * they live ONLY in the app's private files dir on the device and are NEVER written to the repo,
 * logs, exports, screenshots, or docs. The UI only ever sees [masked].
 */
data class ModelApiProfile(
    val label: String = DEFAULT_LABEL,
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val appId: String = "",
    val appKey: String = "",
) {
    fun hasRealCredential(): Boolean =
        ProviderConfigSafetyCheck.isRealSecret(appId) && ProviderConfigSafetyCheck.isRealSecret(appKey)

    fun masked(): MaskedModelProfile = MaskedModelProfile(
        label = label,
        baseUrl = baseUrl,
        model = model,
        credentialPresent = hasRealCredential(),
        maskedAppId = appId.maskForUi(),
        maskedAppKey = appKey.maskForUi(),
    )

    companion object {
        const val DEFAULT_LABEL = "蓝心大模型"
        const val DEFAULT_BASE_URL = "https://api-ai.vivo.com.cn/v1"
        const val DEFAULT_MODEL = "qwen3.5-plus"
    }
}

/** What the Settings page is allowed to render: presence + masked tails only, never the secret. */
data class MaskedModelProfile(
    val label: String,
    val baseUrl: String,
    val model: String,
    val credentialPresent: Boolean,
    val maskedAppId: String,
    val maskedAppKey: String,
)

/**
 * Persists the official model API profile across app restarts. Backed by a single JSON file in the
 * app's private files dir (passed in from the composition root). When constructed with a null file
 * (the [disabled] default used by most unit tests) it is an inert no-op: load() returns null and
 * save()/delete() do nothing, so existing call sites that don't opt in see zero behaviour change.
 *
 * Security: the stored file holds real credentials but lives only in app-private storage. It is
 * additionally listed in .gitignore as defence-in-depth so it can never be committed.
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
        )
    }

    /** True only if a profile with real credentials is persisted. */
    fun hasUsableProfile(): Boolean = load()?.hasRealCredential() == true

    fun masked(): MaskedModelProfile? = load()?.masked()

    /** Persist [profile]. Returns false in disabled mode or on I/O failure. */
    fun save(profile: ModelApiProfile): Boolean {
        val f = file ?: return false
        return try {
            f.parentFile?.mkdirs()
            f.writeText(serialize(profile))
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Remove the persisted profile so the official path is no longer configured. */
    fun delete(): Boolean {
        val f = file ?: return false
        return try {
            if (f.exists()) f.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    private fun serialize(profile: ModelApiProfile): String = buildJsonObject {
        put("label", profile.label)
        put("baseUrl", profile.baseUrl)
        put("model", profile.model)
        put("appId", profile.appId)
        put("appKey", profile.appKey)
    }.toString()

    companion object {
        /** An inert repository (no persistence). Used as the default so opt-in is explicit. */
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
