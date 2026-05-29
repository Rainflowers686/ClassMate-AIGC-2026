package com.classmate.app.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateMotion

/**
 * Status semantics for the small color dot used in HeroCard and other
 * status rows. Mapped to theme tokens by [statusDotColor].
 */
enum class StatusTone { Neutral, Success, Warning, Error, Brand }

@Composable
fun StatusDot(
    tone: StatusTone,
    modifier: Modifier = Modifier,
    sizeDp: Int = 10
) {
    val target = statusDotColor(tone)
    val motion = LocalClassMateMotion.current
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = motion.statusColorMs),
        label = "statusDot"
    )
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(animated, CircleShape)
    )
}

@Composable
fun statusDotColor(tone: StatusTone): Color {
    val c = LocalClassMateColors.current
    return when (tone) {
        StatusTone.Neutral -> c.fgMuted
        StatusTone.Success -> c.statusSuccess
        StatusTone.Warning -> c.statusWarning
        StatusTone.Error -> c.statusError
        StatusTone.Brand -> c.brandPrimary
    }
}
