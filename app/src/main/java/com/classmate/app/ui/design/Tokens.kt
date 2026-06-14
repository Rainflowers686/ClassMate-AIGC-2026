package com.classmate.app.ui.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Non-spacing design tokens, complementing [Dimens] (spacing) and the Material color/typography/
 * shape themes. Kept calm and restrained on purpose — Focus is the default look: soft radii, low
 * elevation, gentle motion. Screens should reference these instead of hard-coding values so the
 * product stays visually consistent and easy to retune.
 */
object Radii {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 22.dp
    val pill = 50.dp

    val cardShape = RoundedCornerShape(lg)
    val chipShape = RoundedCornerShape(pill)
    val heroShape = RoundedCornerShape(xl)
}

/** Elevation tokens. Focus prefers borders + faint shadows over heavy Material elevation. */
object Elevation {
    val none = 0.dp
    val hairline = 1.dp
    val card = 2.dp
    val raised = 6.dp
}

/** Motion tokens (durations in ms + shared easing). Subtle by design; safe under reduced motion. */
object Motion {
    const val fast = 160
    const val standard = 260
    const val slow = 420

    /** A calm in/out curve used for fades, scales and the Flow breathing ring. */
    val easing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
}
