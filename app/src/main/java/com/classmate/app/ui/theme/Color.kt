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
// FOCUS — academic ink + paper, knowledge indigo, amber evidence (DEFAULT)
// ---------------------------------------------------------------------------
private val FocusIndigo = Color(0xFF3B4CC0)
private val FocusIndigoLight = Color(0xFF5A6FE0)

val focusLightScheme = lightColorScheme(
    primary = FocusIndigo,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE1FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5A6478),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE2F2),
    onSecondaryContainer = Color(0xFF171C28),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08C),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFF6F5F1),
    onBackground = Color(0xFF1B1C22),
    surface = Color(0xFFFCFBF8),
    onSurface = Color(0xFF1B1C22),
    surfaceVariant = Color(0xFFE5E3DB),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFFB7B6C0),
    outlineVariant = Color(0xFFD6D4CC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

val focusDarkScheme = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF06138C),
    primaryContainer = Color(0xFF2233A6),
    onPrimaryContainer = Color(0xFFDEE1FF),
    secondary = Color(0xFFC2C7DD),
    onSecondary = Color(0xFF2B3142),
    secondaryContainer = Color(0xFF414759),
    onSecondaryContainer = Color(0xFFDEE2F9),
    tertiary = Color(0xFFEEC048),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFE08C),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF1B1B21),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val focusExtendedLight = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFFFFE9A8),
    onEvidenceHighlight = Color(0xFF4A3A00),
    evidenceBorder = Color(0xFFE0A100),
    success = Color(0xFF2E7D5B),
    warning = Color(0xFFB5651D),
    info = FocusIndigo,
    heroGradient = listOf(FocusIndigo, FocusIndigoLight),
)

val focusExtendedDark = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFF4A3D17),
    onEvidenceHighlight = Color(0xFFFFE6A0),
    evidenceBorder = Color(0xFFCDA434),
    success = Color(0xFF7FD6A8),
    warning = Color(0xFFE0A45E),
    info = Color(0xFFB9C3FF),
    heroGradient = listOf(Color(0xFF2233A6), FocusIndigo),
)

// ---------------------------------------------------------------------------
// VITALITY — youthful violet + coral, encouraging, growth
// ---------------------------------------------------------------------------
private val VitalityViolet = Color(0xFF6D49E0)
private val VitalityCoral = Color(0xFFFF6B6B)

val vitalityLightScheme = lightColorScheme(
    primary = VitalityViolet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFFE5484D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD7),
    onSecondaryContainer = Color(0xFF410006),
    tertiary = Color(0xFFB36B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF291800),
    background = Color(0xFFFBFAFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEAE2F2),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFE3DCEC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

val vitalityDarkScheme = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378A),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFFFB3AD),
    onSecondary = Color(0xFF680010),
    secondaryContainer = Color(0xFF8C1D24),
    onSecondaryContainer = Color(0xFFFFDAD7),
    tertiary = Color(0xFFFFB95C),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF624000),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF15131C),
    onBackground = Color(0xFFE7E0EC),
    surface = Color(0xFF1E1B26),
    onSurface = Color(0xFFE7E0EC),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCBC4CF),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val vitalityExtendedLight = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFFFFE3A3),
    onEvidenceHighlight = Color(0xFF4A3500),
    evidenceBorder = Color(0xFFFFA000),
    success = Color(0xFF16A34A),
    warning = Color(0xFFEA580C),
    info = VitalityViolet,
    heroGradient = listOf(VitalityViolet, VitalityCoral),
)

val vitalityExtendedDark = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFF514321),
    onEvidenceHighlight = Color(0xFFFFE3A3),
    evidenceBorder = Color(0xFFE0A33A),
    success = Color(0xFF6FE0A1),
    warning = Color(0xFFFFB37A),
    info = Color(0xFFCFBCFF),
    heroGradient = listOf(Color(0xFF4F378A), Color(0xFFB3506B)),
)

// ---------------------------------------------------------------------------
// FLOW — calm cool mist by day, immersive night blue for deep-focus sessions
// ---------------------------------------------------------------------------
private val FlowBlue = Color(0xFF4E63A6)
private val FlowTeal = Color(0xFF3E8E9E)

val flowLightScheme = lightColorScheme(
    primary = FlowBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE2FA),
    onPrimaryContainer = Color(0xFF0A1B49),
    secondary = Color(0xFF4F7E84),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE7EC),
    onSecondaryContainer = Color(0xFF0B3B3C),
    tertiary = Color(0xFF566076),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDAE2F9),
    onTertiaryContainer = Color(0xFF131C2F),
    background = Color(0xFFEEF1F6),
    onBackground = Color(0xFF222730),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF222730),
    surfaceVariant = Color(0xFFDCE0EA),
    onSurfaceVariant = Color(0xFF454B57),
    outline = Color(0xFFAFB6C4),
    outlineVariant = Color(0xFFCFD5E0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

val flowDarkScheme = darkColorScheme(
    primary = Color(0xFF9FB4F0),
    onPrimary = Color(0xFF142454),
    primaryContainer = Color(0xFF2A3C70),
    onPrimaryContainer = Color(0xFFDCE2FA),
    secondary = Color(0xFF87C7C7),
    onSecondary = Color(0xFF103132),
    secondaryContainer = Color(0xFF2A494A),
    onSecondaryContainer = Color(0xFFBFE6E6),
    tertiary = Color(0xFFBCC6E0),
    onTertiary = Color(0xFF273044),
    tertiaryContainer = Color(0xFF3D465B),
    onTertiaryContainer = Color(0xFFDAE2F9),
    background = Color(0xFF0E1726),
    onBackground = Color(0xFFD7E0F0),
    surface = Color(0xFF14203A),
    onSurface = Color(0xFFD7E0F0),
    surfaceVariant = Color(0xFF2A3A52),
    onSurfaceVariant = Color(0xFFB6C2D6),
    outline = Color(0xFF53627C),
    outlineVariant = Color(0xFF2A3A52),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val flowExtendedLight = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFFCDE7EC),
    onEvidenceHighlight = Color(0xFF0B3B3C),
    evidenceBorder = Color(0xFF2C7A7B),
    success = Color(0xFF2E8B7A),
    warning = Color(0xFFB5762B),
    info = FlowBlue,
    heroGradient = listOf(FlowBlue, FlowTeal),
)

val flowExtendedDark = ClassMateExtendedColors(
    evidenceHighlight = Color(0xFF1B3A4A),
    onEvidenceHighlight = Color(0xFFBFE6F0),
    evidenceBorder = Color(0xFF5FB0C0),
    success = Color(0xFF74D6C0),
    warning = Color(0xFFD9B06A),
    info = Color(0xFF9FB4F0),
    heroGradient = listOf(Color(0xFF1B2A4A), Color(0xFF26506A)),
)
