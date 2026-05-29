package com.classmate.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4-step spacing scale (see UI Design Spec §6.4). Every padding / gap / size
 * in the UI MUST come from here; raw dp literals in screens are a code smell.
 */
@Immutable
data class ClassMateSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp
)

val LocalClassMateSpacing = staticCompositionLocalOf { ClassMateSpacing() }
