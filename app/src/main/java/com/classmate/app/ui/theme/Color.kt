package com.classmate.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class ClassMateColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHigh: Color,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val outline: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val focusSurface: Color,
    val evidenceSurface: Color,
    val progressSurface: Color,
    val reviewSurface: Color,
    val accent: Color,
    val isDark: Boolean,
)

/** Resolved scheme + extended colors for one theme preset and accent combination. */
data class ThemeColors(
    val scheme: ColorScheme,
    val extended: ClassMateExtendedColors,
    val classMate: ClassMateColorScheme,
)

data class CustomPalette(
    val enabled: Boolean = false,
    val primaryHex: String = DEFAULT_PRIMARY,
    val secondaryHex: String = DEFAULT_SECONDARY,
    val tertiaryHex: String = DEFAULT_TERTIARY,
    val validationWarnings: List<String> = emptyList(),
) {
    companion object {
        const val DEFAULT_PRIMARY = "#55624D"
        const val DEFAULT_SECONDARY = "#755754"
        const val DEFAULT_TERTIARY = "#605F56"
        val Default = CustomPalette()
    }
}

fun themeColors(
    preset: ThemePreset,
    accent: AccentColorPreset = AccentColorPreset.Default,
    dark: Boolean = false,
    customPalette: CustomPalette = CustomPalette.Default,
): ThemeColors {
    val tokens = classMateColorScheme(preset, accent, dark).withCustomPalette(customPalette)
    val material = if (tokens.isDark) {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = bestOnColorFor(tokens.primary),
            primaryContainer = tokens.primaryContainer,
            onPrimaryContainer = tokens.textPrimary,
            secondary = tokens.secondary,
            onSecondary = bestOnColorFor(tokens.secondary),
            secondaryContainer = tokens.secondaryContainer,
            onSecondaryContainer = tokens.textPrimary,
            tertiary = tokens.tertiary,
            onTertiary = bestOnColorFor(tokens.tertiary),
            tertiaryContainer = tokens.tertiaryContainer,
            onTertiaryContainer = tokens.textPrimary,
            background = tokens.background,
            onBackground = tokens.textPrimary,
            surface = tokens.surface,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surfaceVariant,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.border,
            outlineVariant = tokens.outline,
            error = tokens.error,
            onError = Color(0xFF330B08),
            errorContainer = blend(tokens.error, tokens.surface, 0.22f),
            onErrorContainer = tokens.error,
        )
    } else {
        lightColorScheme(
            primary = tokens.primary,
            onPrimary = bestOnColorFor(tokens.primary),
            primaryContainer = tokens.primaryContainer,
            onPrimaryContainer = tokens.textPrimary,
            secondary = tokens.secondary,
            onSecondary = bestOnColorFor(tokens.secondary),
            secondaryContainer = tokens.secondaryContainer,
            onSecondaryContainer = tokens.textPrimary,
            tertiary = tokens.tertiary,
            onTertiary = bestOnColorFor(tokens.tertiary),
            tertiaryContainer = tokens.tertiaryContainer,
            onTertiaryContainer = tokens.textPrimary,
            background = tokens.background,
            onBackground = tokens.textPrimary,
            surface = tokens.surface,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.surfaceVariant,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.border,
            outlineVariant = tokens.outline,
            error = tokens.error,
            onError = Color.White,
            errorContainer = blend(tokens.error, tokens.surface, 0.12f),
            onErrorContainer = tokens.error,
        )
    }
    return ThemeColors(material, extendedColors(tokens), tokens)
}

fun classMateColorScheme(
    preset: ThemePreset,
    accent: AccentColorPreset = AccentColorPreset.Default,
    dark: Boolean = false,
): ClassMateColorScheme = when (preset) {
    ThemePreset.STANDARD_STUDY -> standardStudyScheme(accent, dark)
    ThemePreset.ACTIVE_STUDY -> activeStudyScheme(accent, dark)
    ThemePreset.FOCUS_IMMERSION -> focusImmersionScheme(accent)
}

private fun standardStudyScheme(accent: AccentColorPreset, dark: Boolean): ClassMateColorScheme {
    if (dark) {
        val resolvedAccent = accent.resolveFor(ThemePreset.STANDARD_STUDY, dark = true)
        return ClassMateColorScheme(
            background = Color(0xFF11130F),
            surface = Color(0xFF171A15),
            surfaceVariant = Color(0xFF2B3028),
            surfaceContainerLow = Color(0xFF1E231B),
            surfaceContainerHigh = Color(0xFF30382D),
            primary = resolvedAccent,
            primaryContainer = blend(resolvedAccent, Color(0xFF171A15), 0.28f),
            secondary = Color(0xFFE2C0BA),
            secondaryContainer = Color(0xFF4A3330),
            tertiary = Color(0xFFC8C5B8),
            tertiaryContainer = Color(0xFF35352D),
            textPrimary = Color(0xFFE9ECE2),
            textSecondary = Color(0xFFC0C6B8),
            border = Color(0xFF8B9285),
            outline = Color(0xFF3A4235),
            success = Color(0xFF9CB68E),
            warning = Color(0xFFE2C0BA),
            error = Color(0xFFFFB4AB),
            info = Color(0xFFC8C5B8),
            focusSurface = Color(0xFF1B2018),
            evidenceSurface = Color(0xFF322B1E),
            progressSurface = Color(0xFF26301F),
            reviewSurface = Color(0xFF302B22),
            accent = resolvedAccent,
            isDark = true,
        )
    }
    val resolvedAccent = accent.resolveFor(ThemePreset.STANDARD_STUDY)
    return ClassMateColorScheme(
        background = Color(0xFFF8FAF3),
        surface = Color(0xFFF8FAF3),
        surfaceVariant = Color(0xFFE1E3DC),
        surfaceContainerLow = Color(0xFFF2F4ED),
        surfaceContainerHigh = Color(0xFFE7E9E2),
        primary = resolvedAccent,
        primaryContainer = blend(resolvedAccent, Color(0xFFF8FAF3), 0.22f),
        secondary = Color(0xFF755754),
        secondaryContainer = Color(0xFFFED7D2),
        tertiary = Color(0xFF605F56),
        tertiaryContainer = Color(0xFFE9E6DA),
        textPrimary = Color(0xFF191C18),
        textSecondary = Color(0xFF444841),
        border = Color(0xFF757870),
        outline = Color(0xFFC5C8BE),
        success = Color(0xFF55624D),
        warning = Color(0xFF755754),
        error = Color(0xFFBA1A1A),
        info = Color(0xFF605F56),
        focusSurface = Color(0xFFFFFFFF),
        evidenceSurface = Color(0xFFFBF3DD),
        progressSurface = Color(0xFFEAF0DF),
        reviewSurface = Color(0xFFF6E8E5),
        accent = resolvedAccent,
        isDark = false,
    )
}

private fun activeStudyScheme(accent: AccentColorPreset, dark: Boolean): ClassMateColorScheme {
    if (dark) {
        val resolvedAccent = accent.resolveFor(ThemePreset.ACTIVE_STUDY, dark = true)
        return ClassMateColorScheme(
            background = Color(0xFF0E1420),
            surface = Color(0xFF141C2A),
            surfaceVariant = Color(0xFF243247),
            surfaceContainerLow = Color(0xFF192334),
            surfaceContainerHigh = Color(0xFF263954),
            primary = resolvedAccent,
            primaryContainer = blend(resolvedAccent, Color(0xFF141C2A), 0.30f),
            secondary = Color(0xFF8FC2FF),
            secondaryContainer = Color(0xFF17385F),
            tertiary = Color(0xFFC4CADB),
            tertiaryContainer = Color(0xFF30374A),
            textPrimary = Color(0xFFE8EDF7),
            textSecondary = Color(0xFFC0CAD8),
            border = Color(0xFF8090A4),
            outline = Color(0xFF2E405A),
            success = Color(0xFF79D894),
            warning = Color(0xFFFFC66D),
            error = Color(0xFFFFB4AB),
            info = Color(0xFF8FC2FF),
            focusSurface = Color(0xFF192334),
            evidenceSurface = Color(0xFF26344B),
            progressSurface = Color(0xFF17385F),
            reviewSurface = Color(0xFF26344B),
            accent = resolvedAccent,
            isDark = true,
        )
    }
    val resolvedAccent = accent.resolveFor(ThemePreset.ACTIVE_STUDY)
    return ClassMateColorScheme(
        background = Color(0xFFF8F9FF),
        surface = Color(0xFFF8F9FF),
        surfaceVariant = Color(0xFFD3E4FE),
        surfaceContainerLow = Color(0xFFEFF4FF),
        surfaceContainerHigh = Color(0xFFDCE9FF),
        primary = resolvedAccent,
        primaryContainer = blend(resolvedAccent, Color(0xFFF8F9FF), 0.20f),
        secondary = Color(0xFF0059BB),
        secondaryContainer = Color(0xFFD3E4FE),
        tertiary = Color(0xFF565E74),
        tertiaryContainer = Color(0xFFE1E6F5),
        textPrimary = Color(0xFF0B1C30),
        textSecondary = Color(0xFF3C4A3D),
        border = Color(0xFF6C7B6C),
        outline = Color(0xFFBBCBB9),
        success = Color(0xFF006D32),
        warning = Color(0xFFB26A00),
        error = Color(0xFFBA1A1A),
        info = Color(0xFF0059BB),
        focusSurface = Color(0xFFFFFFFF),
        evidenceSurface = Color(0xFFEFF4FF),
        progressSurface = Color(0xFFE5F3EA),
        reviewSurface = Color(0xFFE8EEF9),
        accent = resolvedAccent,
        isDark = false,
    )
}

private fun focusImmersionScheme(accent: AccentColorPreset): ClassMateColorScheme {
    val resolvedAccent = accent.resolveFor(ThemePreset.FOCUS_IMMERSION, dark = true)
    return ClassMateColorScheme(
        background = Color(0xFF11161B),
        surface = Color(0xFF1A2026),
        surfaceVariant = Color(0xFF303D47),
        surfaceContainerLow = Color(0xFF20272E),
        surfaceContainerHigh = Color(0xFF29333C),
        primary = resolvedAccent,
        primaryContainer = blend(resolvedAccent, Color(0xFF1A2026), 0.16f),
        secondary = Color(0xFFC6CBCC),
        secondaryContainer = Color(0xFF303B45),
        tertiary = Color(0xFF00DBE9),
        tertiaryContainer = Color(0xFF1D4148),
        textPrimary = Color(0xFFE6EAED),
        textSecondary = Color(0xFFB5C0C7),
        border = Color(0xFF60727D),
        outline = Color(0xFF3B4A55),
        success = Color(0xFF00DBE9),
        warning = accent.resolveFor(ThemePreset.FOCUS_IMMERSION, dark = true),
        error = Color(0xFFFFB4AB),
        info = Color(0xFF00DBE9),
        focusSurface = Color(0xFF1C242C),
        evidenceSurface = Color(0xFF24303A),
        progressSurface = Color(0xFF24313A),
        reviewSurface = Color(0xFF1D4148),
        accent = resolvedAccent,
        isDark = true,
    )
}

private fun extendedColors(tokens: ClassMateColorScheme): ClassMateExtendedColors =
    ClassMateExtendedColors(
        evidenceHighlight = tokens.evidenceSurface,
        onEvidenceHighlight = tokens.textPrimary,
        evidenceBorder = tokens.info,
        success = tokens.success,
        warning = tokens.warning,
        info = tokens.info,
        heroGradient = listOf(tokens.surfaceContainerLow, tokens.surfaceContainerHigh),
    )

fun normalizeHexColorOrNull(value: String): String? {
    val cleaned = value.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    if (cleaned.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null
    return "#${cleaned.uppercase()}"
}

fun parseHexColorOrNull(value: String): Color? {
    val normalized = normalizeHexColorOrNull(value) ?: return null
    val rgb = normalized.removePrefix("#").toIntOrNull(16) ?: return null
    return Color(
        red = ((rgb shr 16) and 0xFF) / 255f,
        green = ((rgb shr 8) and 0xFF) / 255f,
        blue = (rgb and 0xFF) / 255f,
        alpha = 1f,
    )
}

fun relativeLuminance(color: Color): Double {
    fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

fun contrastRatio(a: Color, b: Color): Double {
    val lighter = max(relativeLuminance(a), relativeLuminance(b))
    val darker = min(relativeLuminance(a), relativeLuminance(b))
    return (lighter + 0.05) / (darker + 0.05)
}

fun bestOnColorFor(background: Color): Color {
    val dark = Color(0xFF111111)
    val light = Color(0xFFFFFFFF)
    return if (contrastRatio(background, dark) >= contrastRatio(background, light)) dark else light
}

fun isReadable(background: Color, foreground: Color, minRatio: Double = 4.5): Boolean =
    contrastRatio(background, foreground) >= minRatio

fun validateCustomPalette(palette: CustomPalette, background: Color, text: Color): List<String> {
    if (!palette.enabled) return emptyList()
    val warnings = mutableListOf<String>()
    listOf(
        "Primary" to palette.primaryHex,
        "Secondary" to palette.secondaryHex,
        "Tertiary" to palette.tertiaryHex,
    ).forEach { (name, hex) ->
        val color = parseHexColorOrNull(hex)
        if (color == null) {
            warnings += "$name HEX 格式无效"
        } else {
            if (!isReadable(color, bestOnColorFor(color))) warnings += "$name 与自动文字色对比不足"
            if (contrastRatio(color, background) < 1.35) warnings += "$name 与当前背景过近"
            if (contrastRatio(color, text) < 2.0) warnings += "$name 与正文色过近，仅用于强调态"
        }
    }
    return warnings
}

fun ClassMateColorScheme.withCustomPalette(palette: CustomPalette): ClassMateColorScheme {
    if (!palette.enabled) return this
    val customPrimary = parseHexColorOrNull(palette.primaryHex) ?: primary
    val customSecondary = parseHexColorOrNull(palette.secondaryHex) ?: secondary
    val customTertiary = parseHexColorOrNull(palette.tertiaryHex) ?: tertiary
    return copy(
        primary = customPrimary,
        primaryContainer = blend(customPrimary, surface, if (isDark) 0.24f else 0.16f),
        secondary = customSecondary,
        secondaryContainer = blend(customSecondary, surface, if (isDark) 0.22f else 0.13f),
        tertiary = customTertiary,
        tertiaryContainer = blend(customTertiary, surface, if (isDark) 0.20f else 0.12f),
        success = customSecondary,
        warning = blend(customSecondary, surface, if (isDark) 0.62f else 0.72f),
        accent = customPrimary,
        info = customTertiary,
        focusSurface = blend(customTertiary, surface, if (isDark) 0.16f else 0.07f),
        evidenceSurface = blend(customTertiary, surface, if (isDark) 0.18f else 0.10f),
        progressSurface = blend(customSecondary, surface, if (isDark) 0.20f else 0.11f),
        reviewSurface = blend(customTertiary, surface, if (isDark) 0.16f else 0.09f),
    )
}

fun AccentColorPreset.resolveFor(preset: ThemePreset, dark: Boolean = false): Color = when (this) {
    AccentColorPreset.BLUE -> if (dark) Color(0xFF8FB6FF) else Color(0xFF2563EB)
    AccentColorPreset.CYAN -> if (dark) Color(0xFF46DDE5) else Color(0xFF00A0AA)
    AccentColorPreset.GREEN -> when {
        preset == ThemePreset.STANDARD_STUDY && !dark -> Color(0xFF55624D)
        preset == ThemePreset.STANDARD_STUDY -> Color(0xFFA9B99E)
        dark -> Color(0xFF7BD899)
        else -> Color(0xFF006D32)
    }
    AccentColorPreset.PURPLE -> if (dark) Color(0xFFC4B5FD) else Color(0xFF7C3AED)
    AccentColorPreset.AMBER -> if (dark) Color(0xFFEAC16A) else Color(0xFFB26A00)
    AccentColorPreset.ROSE -> if (dark) Color(0xFFFF8FB1) else Color(0xFFFF4B89)
    AccentColorPreset.GRAPHITE -> if (dark) Color(0xFFE2E2E2) else Color(0xFF353535)
    AccentColorPreset.OCEAN -> if (dark) Color(0xFF8FC2FF) else Color(0xFF0059BB)
}

private fun blend(foreground: Color, background: Color, alpha: Float): Color =
    Color(
        red = foreground.red * alpha + background.red * (1f - alpha),
        green = foreground.green * alpha + background.green * (1f - alpha),
        blue = foreground.blue * alpha + background.blue * (1f - alpha),
        alpha = 1f,
    )
