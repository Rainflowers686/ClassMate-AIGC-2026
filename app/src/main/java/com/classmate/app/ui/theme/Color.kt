package com.classmate.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

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
    val accent: Color,
    val isDark: Boolean,
)

/** Resolved scheme + extended colors for one theme preset and accent combination. */
data class ThemeColors(
    val scheme: ColorScheme,
    val extended: ClassMateExtendedColors,
    val classMate: ClassMateColorScheme,
)

fun themeColors(
    preset: ThemePreset,
    accent: AccentColorPreset = AccentColorPreset.Default,
    dark: Boolean = false,
): ThemeColors {
    val tokens = classMateColorScheme(preset, accent, dark)
    val material = if (tokens.isDark) {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = Color(0xFF0B0B0B),
            primaryContainer = tokens.primaryContainer,
            onPrimaryContainer = tokens.textPrimary,
            secondary = tokens.secondary,
            onSecondary = Color(0xFF0B0B0B),
            secondaryContainer = tokens.secondaryContainer,
            onSecondaryContainer = tokens.textPrimary,
            tertiary = tokens.tertiary,
            onTertiary = Color(0xFF0B0B0B),
            tertiaryContainer = tokens.surfaceContainerHigh,
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
            onPrimary = Color.White,
            primaryContainer = tokens.primaryContainer,
            onPrimaryContainer = tokens.textPrimary,
            secondary = tokens.secondary,
            onSecondary = Color.White,
            secondaryContainer = tokens.secondaryContainer,
            onSecondaryContainer = tokens.textPrimary,
            tertiary = tokens.tertiary,
            onTertiary = Color.White,
            tertiaryContainer = blend(tokens.tertiary, tokens.surface, 0.14f),
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
        accent = resolvedAccent,
        isDark = false,
    )
}

private fun focusImmersionScheme(accent: AccentColorPreset): ClassMateColorScheme {
    val resolvedAccent = accent.resolveFor(ThemePreset.FOCUS_IMMERSION, dark = true)
    return ClassMateColorScheme(
        background = Color(0xFF0B1118),
        surface = Color(0xFF121922),
        surfaceVariant = Color(0xFF2A3945),
        surfaceContainerLow = Color(0xFF18212B),
        surfaceContainerHigh = Color(0xFF22303C),
        primary = resolvedAccent,
        primaryContainer = blend(resolvedAccent, Color(0xFF121922), 0.20f),
        secondary = Color(0xFFC8C6C5),
        secondaryContainer = Color(0xFF2E3C48),
        tertiary = Color(0xFF00DBE9),
        textPrimary = Color(0xFFF0F3F5),
        textSecondary = Color(0xFFB8C4CC),
        border = Color(0xFF667782),
        outline = Color(0xFF334250),
        success = Color(0xFF00DBE9),
        warning = accent.resolveFor(ThemePreset.FOCUS_IMMERSION, dark = true),
        error = Color(0xFFFFB4AB),
        info = Color(0xFF00DBE9),
        focusSurface = Color(0xFF151F29),
        evidenceSurface = Color(0xFF1F2A34),
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
