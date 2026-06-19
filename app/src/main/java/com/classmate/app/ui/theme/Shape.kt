package com.classmate.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ClassMateShapeScheme(
    val cardRadius: Dp,
    val buttonRadius: Dp,
    val pillRadius: Dp,
    val modalRadius: Dp,
    val focusPanelRadius: Dp,
)

@Immutable
data class ClassMateSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 20.dp,
    val xl: Dp = 24.dp,
)

fun classMateShapeScheme(preset: ThemePreset): ClassMateShapeScheme = when (preset) {
    ThemePreset.STANDARD_STUDY -> ClassMateShapeScheme(
        cardRadius = 16.dp,
        buttonRadius = 999.dp,
        pillRadius = 999.dp,
        modalRadius = 24.dp,
        focusPanelRadius = 24.dp,
    )
    ThemePreset.ACTIVE_STUDY -> ClassMateShapeScheme(
        cardRadius = 12.dp,
        buttonRadius = 8.dp,
        pillRadius = 999.dp,
        modalRadius = 22.dp,
        focusPanelRadius = 16.dp,
    )
    ThemePreset.FOCUS_IMMERSION -> ClassMateShapeScheme(
        cardRadius = 16.dp,
        buttonRadius = 999.dp,
        pillRadius = 999.dp,
        modalRadius = 28.dp,
        focusPanelRadius = 32.dp,
    )
}

fun materialShapesFor(preset: ThemePreset): Shapes {
    val s = classMateShapeScheme(preset)
    return Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(if (preset == ThemePreset.ACTIVE_STUDY) 8.dp else 10.dp),
        medium = RoundedCornerShape(s.cardRadius),
        large = RoundedCornerShape(if (preset == ThemePreset.ACTIVE_STUDY) 16.dp else s.cardRadius),
        extraLarge = RoundedCornerShape(s.modalRadius),
    )
}

val ClassMateShapes = materialShapesFor(ThemePreset.Default)
