package com.classmate.app.data

import com.classmate.app.ui.theme.AccentColorPreset
import com.classmate.app.ui.theme.ThemePreset
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

data class ThemePreference(
    val themePreset: ThemePreset = ThemePreset.Default,
    val accentColorPreset: AccentColorPreset = AccentColorPreset.Default,
)

class ThemePreferenceRepository(private val file: File?) {

    val isEnabled: Boolean get() = file != null

    fun load(): ThemePreference {
        val f = file ?: return ThemePreference()
        if (!f.exists()) return ThemePreference()
        val obj = runCatching { json.parseToJsonElement(f.readText()).jsonObject }.getOrNull() ?: return ThemePreference()
        return ThemePreference(
            themePreset = obj.str("themePreset")?.let(::themePresetOrNull) ?: ThemePreset.Default,
            accentColorPreset = obj.str("accentColorPreset")?.let(::accentPresetOrNull) ?: AccentColorPreset.Default,
        )
    }

    fun save(preference: ThemePreference): Boolean {
        val f = file ?: return false
        return try {
            f.parentFile?.mkdirs()
            f.writeText(
                buildJsonObject {
                    put("themePreset", preference.themePreset.name)
                    put("accentColorPreset", preference.accentColorPreset.name)
                }.toString(),
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveThemePreset(preset: ThemePreset): ThemePreference {
        val next = load().copy(themePreset = preset)
        save(next)
        return next
    }

    fun saveAccentColorPreset(accent: AccentColorPreset): ThemePreference {
        val next = load().copy(accentColorPreset = accent)
        save(next)
        return next
    }

    companion object {
        fun disabled(): ThemePreferenceRepository = ThemePreferenceRepository(null)

        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

private fun themePresetOrNull(value: String): ThemePreset? =
    ThemePreset.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }

private fun accentPresetOrNull(value: String): AccentColorPreset? =
    AccentColorPreset.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
