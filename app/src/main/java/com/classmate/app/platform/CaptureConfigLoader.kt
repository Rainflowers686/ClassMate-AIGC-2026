package com.classmate.app.platform

import com.classmate.core.capture.CaptureProviderConfig
import com.classmate.core.validation.ProviderConfigSafetyCheck
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Loads the vivo Capture (OCR / ASR) credentials from the SAME local config file used by the model
 * providers (`config.local.json`). The vivo OCR/ASR gateway shares the BlueLM AppKey, so capture reads an
 * optional `vivoCapture { appId, appKey, baseUrl }` block and otherwise falls back to `providers.bluelm`.
 *
 * Security: this loader reads the file at RUNTIME only; it never logs, prints, serializes, or returns raw
 * credential values. Callers receive a [CaptureProviderConfig] (whose toString is redacted) and a value-free
 * [CaptureConfigStatus]. Placeholder values (e.g. "your_AppKey") count as NOT configured.
 */
class CaptureConfigLoader(
    private val localConfigFile: File = ConfigRepository.defaultLocalConfigFile(),
    /**
     * The app-private model-config file the Settings screen writes (classmate_model_config.json).
     * On a real device config.local.json lives at the process working dir (effectively absent), so the
     * Settings-saved AppID/AppKey is the ONLY credential source — capture (OCR/ASR) must fall back to it,
     * otherwise it stays "未配置" even after the user configures the cloud model in Settings.
     */
    private val modelConfigFile: File? = null,
) {
    /**
     * Build a capture config from config.local.json first; if that is missing/unconfigured fall back
     * to the Settings model-config file. Missing/invalid → ABSENT (manual paste / on-device draft still work).
     */
    fun load(): CaptureProviderConfig {
        val primary = readConfigFile()
        if (primary.isConfigured) return primary
        return readModelConfigFile()
    }

    private fun readConfigFile(): CaptureProviderConfig {
        if (!localConfigFile.exists()) return CaptureProviderConfig.ABSENT
        val text = try {
            localConfigFile.readText()
        } catch (_: Exception) {
            return CaptureProviderConfig.ABSENT
        }
        return parse(text)
    }

    private fun readModelConfigFile(): CaptureProviderConfig {
        val file = modelConfigFile ?: return CaptureProviderConfig.ABSENT
        if (!file.exists()) return CaptureProviderConfig.ABSENT
        val text = try {
            file.readText()
        } catch (_: Exception) {
            return CaptureProviderConfig.ABSENT
        }
        return parseModelConfig(text)
    }

    /** Parse the Settings model-config schema (root-level appId / appKey / baseUrl). */
    fun parseModelConfig(jsonText: String): CaptureProviderConfig {
        val root = try {
            json.parseToJsonElement(jsonText) as? JsonObject
        } catch (_: Exception) {
            return CaptureProviderConfig.ABSENT
        } ?: return CaptureProviderConfig.ABSENT
        val appId = root.real("appId") ?: return CaptureProviderConfig.ABSENT
        val appKey = root.real("appKey") ?: return CaptureProviderConfig.ABSENT
        val domain = root.str("baseUrl")?.let { normalizeDomain(it) } ?: "api-ai.vivo.com.cn"
        return CaptureProviderConfig(appId = appId, appKey = appKey, domain = domain)
    }

    /** Parse capture credentials from raw config JSON text (unit-testable with fake config). */
    fun parse(jsonText: String): CaptureProviderConfig {
        val root = try {
            json.parseToJsonElement(jsonText) as? JsonObject
        } catch (_: Exception) {
            return CaptureProviderConfig.ABSENT
        } ?: return CaptureProviderConfig.ABSENT

        val capture = root.obj("vivoCapture")
        val bluelm = root.obj("providers")?.obj("bluelm")

        val appId = capture.real("appId") ?: bluelm.real("appId")
        val appKey = capture.real("appKey") ?: bluelm.real("appKey")
        val baseUrl = capture.str("baseUrl") ?: bluelm.str("baseUrl")
        val domain = baseUrl?.let { normalizeDomain(it) } ?: "api-ai.vivo.com.cn"

        if (appId == null || appKey == null) return CaptureProviderConfig.ABSENT
        return CaptureProviderConfig(appId = appId, appKey = appKey, domain = domain)
    }

    /** A value-free status for UI/diagnostics: present/absent per field, never the values themselves. */
    fun status(jsonText: String? = null): CaptureConfigStatus {
        val config = if (jsonText != null) parse(jsonText) else load()
        // Re-derive field presence without exposing values; fall back to the model-config file.
        val (hasId, hasKey) = if (jsonText != null) {
            fieldPresence(jsonText)
        } else {
            val primary = if (localConfigFile.exists()) {
                fieldPresence(runCatching { localConfigFile.readText() }.getOrDefault(""))
            } else {
                false to false
            }
            if (primary.first && primary.second) primary else modelConfigFieldPresence()
        }
        return CaptureConfigStatus(configured = config.isConfigured, hasAppId = hasId, hasAppKey = hasKey)
    }

    private fun modelConfigFieldPresence(): Pair<Boolean, Boolean> {
        val file = modelConfigFile?.takeIf { it.exists() } ?: return false to false
        val text = runCatching { file.readText() }.getOrDefault("")
        val root = try { json.parseToJsonElement(text) as? JsonObject } catch (_: Exception) { null } ?: return false to false
        return (root.real("appId") != null) to (root.real("appKey") != null)
    }

    private fun fieldPresence(jsonText: String): Pair<Boolean, Boolean> {
        val root = try { json.parseToJsonElement(jsonText) as? JsonObject } catch (_: Exception) { null } ?: return false to false
        val capture = root.obj("vivoCapture")
        val bluelm = root.obj("providers")?.obj("bluelm")
        val hasId = (capture.real("appId") ?: bluelm.real("appId")) != null
        val hasKey = (capture.real("appKey") ?: bluelm.real("appKey")) != null
        return hasId to hasKey
    }

    private fun normalizeDomain(raw: String): String =
        // Host only: capture (OCR/ASR) endpoints live at the host root, so drop any path such as the
        // chat base's "/v1" — otherwise URLs would become host/v1/ocr/... and 404.
        raw.removePrefix("https://").removePrefix("http://").substringBefore('/').trim().ifBlank { "api-ai.vivo.com.cn" }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

/** Value-free capture config status for UI ("已配置 / 未配置 / 缺少字段"). Never carries a credential value. */
data class CaptureConfigStatus(
    val configured: Boolean,
    val hasAppId: Boolean,
    val hasAppKey: Boolean,
) {
    /** A short, honest, value-free Chinese label. */
    fun labelZh(): String = when {
        configured -> "已配置"
        !hasAppId && !hasAppKey -> "未配置"
        !hasAppId -> "缺少 appId"
        !hasAppKey -> "缺少 appKey"
        else -> "未配置"
    }
}

// ── private helpers (value-aware but never log/return raw values) ───────────────────────────────────
private fun JsonObject?.obj(key: String): JsonObject? = this?.get(key) as? JsonObject

private fun JsonObject?.str(key: String): String? =
    (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

/** A "real" (non-blank, non-placeholder, non-masked) string value, or null. */
private fun JsonObject?.real(key: String): String? =
    str(key)?.takeUnless { ProviderConfigSafetyCheck.isPlaceholder(it) || ProviderConfigSafetyCheck.isMaskedSecret(it) }
