package com.classmate.app.data

import com.classmate.app.ui.i18n.AppLanguage
import com.classmate.app.ui.theme.AccentColorPreset
import com.classmate.app.ui.theme.CustomPalette
import com.classmate.app.ui.theme.ThemePreset
import com.classmate.app.ui.theme.TypographyPreset
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
    val customPalette: CustomPalette = CustomPalette.Default,
    val typographyPreset: TypographyPreset = TypographyPreset.Default,
    val language: AppLanguage = AppLanguage.ZH,
    val enableExperimentalImageGeneration: Boolean = false,
    val enableExperimentalVideoGeneration: Boolean = false,
    val enableExperimentalSimultaneousInterpretation: Boolean = false,
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
            customPalette = CustomPalette(
                enabled = obj.bool("customColorEnabled") ?: false,
                primaryHex = obj.str("customPrimaryHex") ?: CustomPalette.DEFAULT_PRIMARY,
                secondaryHex = obj.str("customSecondaryHex") ?: CustomPalette.DEFAULT_SECONDARY,
                tertiaryHex = obj.str("customTertiaryHex") ?: CustomPalette.DEFAULT_TERTIARY,
            ),
            typographyPreset = obj.str("typographyPreset")?.let(::typographyPresetOrNull) ?: TypographyPreset.Default,
            language = obj.str("language")?.let(::languageOrNull) ?: AppLanguage.ZH,
            enableExperimentalImageGeneration = obj.bool("enableExperimentalImageGeneration") ?: false,
            enableExperimentalVideoGeneration = obj.bool("enableExperimentalVideoGeneration") ?: false,
            enableExperimentalSimultaneousInterpretation = obj.bool("enableExperimentalSimultaneousInterpretation") ?: false,
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
                    put("customColorEnabled", preference.customPalette.enabled)
                    put("customPrimaryHex", preference.customPalette.primaryHex)
                    put("customSecondaryHex", preference.customPalette.secondaryHex)
                    put("customTertiaryHex", preference.customPalette.tertiaryHex)
                    put("typographyPreset", preference.typographyPreset.name)
                    put("language", preference.language.name)
                    put("enableExperimentalImageGeneration", preference.enableExperimentalImageGeneration)
                    put("enableExperimentalVideoGeneration", preference.enableExperimentalVideoGeneration)
                    put("enableExperimentalSimultaneousInterpretation", preference.enableExperimentalSimultaneousInterpretation)
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

    fun saveCustomPalette(customPalette: CustomPalette): ThemePreference {
        val next = load().copy(customPalette = customPalette)
        save(next)
        return next
    }

    fun saveTypographyPreset(preset: TypographyPreset): ThemePreference {
        val next = load().copy(typographyPreset = preset)
        save(next)
        return next
    }

    fun saveLanguage(language: AppLanguage): ThemePreference {
        val next = load().copy(language = language)
        save(next)
        return next
    }

    fun resetAdvancedAppearance(): ThemePreference {
        val next = load().copy(customPalette = CustomPalette.Default, typographyPreset = TypographyPreset.Default)
        save(next)
        return next
    }

    fun saveExperimentalImageGeneration(enabled: Boolean): ThemePreference {
        val next = load().copy(enableExperimentalImageGeneration = enabled)
        save(next)
        return next
    }

    fun saveExperimentalVideoGeneration(enabled: Boolean): ThemePreference {
        val next = load().copy(enableExperimentalVideoGeneration = enabled)
        save(next)
        return next
    }

    fun saveExperimentalSimultaneousInterpretation(enabled: Boolean): ThemePreference {
        val next = load().copy(enableExperimentalSimultaneousInterpretation = enabled)
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

private fun typographyPresetOrNull(value: String): TypographyPreset? =
    TypographyPreset.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }

private fun languageOrNull(value: String): AppLanguage? =
    AppLanguage.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

private fun JsonObject.bool(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.content?.let { value ->
        when {
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            else -> null
        }
    }
