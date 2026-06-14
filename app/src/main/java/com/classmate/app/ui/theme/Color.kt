package com.classmate.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Resolved scheme + extended colors for one (theme, dark) combination. */
data class ThemeColors(val scheme: ColorScheme, val extended: ClassMateExtendedColors)

fun themeColors(option: ThemeOption, dark: Boolean): ThemeColors = when (option) {
    ThemeOption.FOCUS ->
        if (dark) ThemeColors(focusDarkScheme, focusExtendedDark) else ThemeColors(focusLightScheme, focusExtendedLight)
    ThemeOption.VITALITY ->
        if (dark) ThemeColors(vitalityDarkScheme, vitalityExtendedDark) else ThemeColors(vitalityLightScheme, vitalityExtendedLight)
    ThemeOption.FLOW ->
        if (dark) ThemeColors(flowDarkScheme, flowExtendedDark) else ThemeColors(flowLightScheme, flowExtendedLight)
}

// ---------------------------------------------------------------------------
// FOCUS — the DEFAULT theme. Warm white-grey paper, restrained blue, hairline
// borders, calm ink. Tokens follow the Stage 9A design handoff (§3):
// bg #F3F4F7 / surface #FFFFFF / primary #3A64D8 / primarySoft #EAEEFB /
// ink #1C1C1E · #5F6168 · #8E9099 / hairline ≈ #3C3C43 @ 10%.
// ---------------------------------------------------------------------------
private val FocusBlue = Color(0xFF3A64D8)
private val FocusBlueSoft = Color(0xFFEAEEFB)
private val FocusInk = Color(0xFF1C1C1E)
private val FocusInk2 = Color(0xFF5F6168)
private val FocusInk3 = Color(0xFF8E9099)
private val FocusHairline = Color(0xFFE4E5EA) // #3C3C43 at ~10% over white, pre-multiplied

val focusLightScheme = lightColorScheme(
    primary = FocusBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = FocusBlueSoft,
    onPrimaryContainer = Color(0xFF1F3A8F),
    secondary = Color(0xFF3F8F86),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2F0EE),
    onSecondaryContainer = Color(0xFF1F4A45),
    tertiary = Color(0xFF7A6BBF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECE9F8),
    onTertiaryContainer = Color(0xFF3E3470),
    background = Color(0xFFF3F4F7),
    onBackground = FocusInk,
    surface = Color(0xFFFFFFFF),
    onSurface = FocusInk,
    surfaceVariant = Color(0xFFF7F8FA),
    onSurfaceVariant = FocusInk2,
    outline = FocusInk3,
    outlineVariant = FocusHairline,
    error = Color(0xFFB0503F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7E6E2),
    onErrorContainer = Color(0xFF6E2A1E),
)

val focusDarkScheme = darkColorScheme(
    primary = Color(0xFF93ACF2),
    onPrimary = Color(0xFF12245E),
    primaryContainer = Color(0xFF243A7E),
    onPrimaryContainer = Color(0xFFDDE5FB),
    secondary = Color(0xFF8CC6BF),
    onSecondary = Color(0xFF0F3833),
    secondaryContainer = Color(0xFF24504A),
    onSecondaryContainer = Color(0xFFD8EEEA),
    tertiary = Color(0xFFBCB1E8),
    onTertiary = Color(0xFF2E2556),
    tertiaryContainer = Color(0xFF453B78),
    onTertiaryContainer = Color(0xFFE8E4F8),
    background = Color(0xFF141417),
    onBackground = Color(0xFFEAEAEC),
    surface = Color(0xFF1C1C20),
    onSurface = Color(0xFFEAEAEC),
    surfaceVariant = Color(0xFF222228),
    onSurfaceVariant = Color(0xFFB9BAC2),
    outline = Color(0xFF8A8B94),
    outlineVariant = Color(0xFF2C2C33),
    error = Color(0xFFEFA294),
    onError = Color(0xFF551F14),
    errorContainer = Color(0xFF6E2A1E),
    onErrorContainer = Color(0xFFF7DDD7),
)

val focusExtendedLight = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFFFBF3DD),
    onEvidenceHighlight = Color(0xFF6B5320),
    evidenceBorder = Color(0xFFD9BC7A),
    success = Color(0xFF4A9B76),
    warning = Color(0xFFB78A3E),
    info = FocusBlue,
    heroGradient = listOf(Color(0xFFF7F8FB), Color(0xFFEEF0F4)),
)

val focusExtendedDark = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFF3C3420),
    onEvidenceHighlight = Color(0xFFF0DFAE),
    evidenceBorder = Color(0xFFB99F5E),
    success = Color(0xFF85C8A8),
    warning = Color(0xFFD9B06A),
    info = Color(0xFF93ACF2),
    heroGradient = listOf(Color(0xFF1C1C22), Color(0xFF141417)),
)

// ---------------------------------------------------------------------------
// VITALITY — opt-in growth/encouragement skin (NOT the default). Light cool
// white #EEF1F6, indigo #515ED0, growth green + warm orange accents (handoff §3).
// ---------------------------------------------------------------------------
private val VitalityIndigo = Color(0xFF515ED0)

val vitalityLightScheme = lightColorScheme(
    primary = VitalityIndigo,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8EAF8),
    onPrimaryContainer = Color(0xFF2D3690),
    secondary = Color(0xFF4D9B80),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F1EA),
    onSecondaryContainer = Color(0xFF1F4A3B),
    tertiary = Color(0xFFE0915C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF9E9DD),
    onTertiaryContainer = Color(0xFF6E3C18),
    background = Color(0xFFEEF1F6),
    onBackground = Color(0xFF1A2030),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2030),
    surfaceVariant = Color(0xFFF4F6FB),
    onSurfaceVariant = Color(0xFF525C70),
    outline = Color(0xFF8B93A6),
    outlineVariant = Color(0xFFE3E7F0),
    error = Color(0xFFB0503F),
    onError = Color(0xFFFFFFFF),
)

val vitalityDarkScheme = darkColorScheme(
    primary = Color(0xFFB9C0F5),
    onPrimary = Color(0xFF1F2766),
    primaryContainer = Color(0xFF3A4498),
    onPrimaryContainer = Color(0xFFE5E8FB),
    secondary = Color(0xFF93CDB6),
    onSecondary = Color(0xFF10382A),
    secondaryContainer = Color(0xFF255944),
    onSecondaryContainer = Color(0xFFDCF2E8),
    tertiary = Color(0xFFF0B98E),
    onTertiary = Color(0xFF4A2B10),
    tertiaryContainer = Color(0xFF6E3C18),
    onTertiaryContainer = Color(0xFFFAE6D6),
    background = Color(0xFF14161E),
    onBackground = Color(0xFFE7E9F0),
    surface = Color(0xFF1B1E28),
    onSurface = Color(0xFFE7E9F0),
    surfaceVariant = Color(0xFF232734),
    onSurfaceVariant = Color(0xFFB7BCCB),
    outline = Color(0xFF878DA0),
    outlineVariant = Color(0xFF2D3140),
    error = Color(0xFFEFA294),
    onError = Color(0xFF551F14),
)

val vitalityExtendedLight = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFFFBF1DC),
    onEvidenceHighlight = Color(0xFF6B5320),
    evidenceBorder = Color(0xFFD9BC7A),
    success = Color(0xFF4D9B80),
    warning = Color(0xFFC79A4B),
    info = VitalityIndigo,
    heroGradient = listOf(Color(0xFFF3F5FB), Color(0xFFE7ECF6)),
)

val vitalityExtendedDark = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFF3C3420),
    onEvidenceHighlight = Color(0xFFF0DFAE),
    evidenceBorder = Color(0xFFB99F5E),
    success = Color(0xFF93CDB6),
    warning = Color(0xFFE0BC7E),
    info = Color(0xFFB9C0F5),
    heroGradient = listOf(Color(0xFF232734), Color(0xFF14161E)),
)

// ---------------------------------------------------------------------------
// FLOW — ambient companion theme for Live Companion / focus sessions ONLY.
// Night-desk palette (handoff §3): bg #111016, surface #16161D, warm amber
// #E0A86A, rain blue #8FB6DD, grass green #8FC6A0. Never the default app skin.
// ---------------------------------------------------------------------------
private val FlowAmber = Color(0xFFE0A86A)

val flowLightScheme = lightColorScheme(
    primary = Color(0xFF8A6A3C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3E7D6),
    onPrimaryContainer = Color(0xFF4A3712),
    secondary = Color(0xFF4F7E9E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCEAF5),
    onSecondaryContainer = Color(0xFF173A52),
    tertiary = Color(0xFF4E8A63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDFF0E4),
    onTertiaryContainer = Color(0xFF1C4A2D),
    background = Color(0xFFF2F1EE),
    onBackground = Color(0xFF222226),
    surface = Color(0xFFFBFAF7),
    onSurface = Color(0xFF222226),
    surfaceVariant = Color(0xFFEDECE7),
    onSurfaceVariant = Color(0xFF5C5C62),
    outline = Color(0xFF8E8E94),
    outlineVariant = Color(0xFFE0DFDA),
    error = Color(0xFFB0503F),
    onError = Color(0xFFFFFFFF),
)

val flowDarkScheme = darkColorScheme(
    primary = FlowAmber,
    onPrimary = Color(0xFF2B1F0E),
    primaryContainer = Color(0xFF3A2E22),
    onPrimaryContainer = Color(0xFFF2DDC0),
    secondary = Color(0xFF8FB6DD),
    onSecondary = Color(0xFF13283C),
    secondaryContainer = Color(0xFF24394E),
    onSecondaryContainer = Color(0xFFD6E6F5),
    tertiary = Color(0xFF8FC6A0),
    onTertiary = Color(0xFF11301C),
    tertiaryContainer = Color(0xFF234534),
    onTertiaryContainer = Color(0xFFD9EFE0),
    background = Color(0xFF111016),
    onBackground = Color(0xFFF2F2F3),
    surface = Color(0xFF16161D),
    onSurface = Color(0xFFF2F2F3),
    surfaceVariant = Color(0xFF1C1C24),
    onSurfaceVariant = Color(0xFFB6B7BC), // ≈ white @ 62% on the night-desk base
    outline = Color(0xFF7E7F86),
    outlineVariant = Color(0xFF2A2A30), // ≈ white @ 10%
    error = Color(0xFFEFA294),
    onError = Color(0xFF551F14),
)

val flowExtendedLight = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFFF3E7D6),
    onEvidenceHighlight = Color(0xFF4A3712),
    evidenceBorder = Color(0xFFC9A86A),
    success = Color(0xFF4E8A63),
    warning = Color(0xFFB78A3E),
    info = Color(0xFF4F7E9E),
    heroGradient = listOf(Color(0xFFF7F4EE), Color(0xFFEFEDE7)),
)

val flowExtendedDark = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFF3A2E22),
    onEvidenceHighlight = Color(0xFFF2DDC0),
    evidenceBorder = Color(0xFFB9905C),
    success = Color(0xFF8FC6A0),
    warning = Color(0xFFD8A86A),
    info = Color(0xFF8FB6DD),
    heroGradient = listOf(Color(0xFF2C2418), Color(0xFF111016)),
)
