package com.classmate.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion tokens for ClassMate (UI Design Spec §4 / §6.7).
 *
 *  - All durations are ≤ 250 ms — anything longer must be justified inline.
 *  - Spring is allowed in one place per session (Quiz radio selection).
 *  - When [reduceMotion] is true, animation drivers should snap rather than
 *    interpolate. The host theme inspects [ClassMateColors.enableGlass] or
 *    a system "reduce motion" pref to set this.
 */
@Immutable
data class ClassMateMotion(
    val reduceMotion: Boolean = false
) {
    val short get() = tween<Float>(durationMillis = if (reduceMotion) 0 else 120, easing = FastOutSlowInEasing)
    val medium get() = tween<Float>(durationMillis = if (reduceMotion) 0 else 200, easing = FastOutSlowInEasing)
    val long get() = tween<Float>(durationMillis = if (reduceMotion) 0 else 250, easing = FastOutSlowInEasing)
    val springQuiet
        get() = spring<Float>(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        )

    val shortMs: Int get() = if (reduceMotion) 0 else 120
    val mediumMs: Int get() = if (reduceMotion) 0 else 200
    val longMs: Int get() = if (reduceMotion) 0 else 250
    val statusColorMs: Int get() = if (reduceMotion) 0 else 180
}

val LocalClassMateMotion = staticCompositionLocalOf { ClassMateMotion() }
