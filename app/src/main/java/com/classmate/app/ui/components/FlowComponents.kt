package com.classmate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.design.Motion
import com.classmate.app.ui.design.Radii
import com.classmate.app.ui.flow.FlowScene
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * Fixed Flow accent (cool blue → teal). Used to give the Live Companion a calm "Flow" mood without
 * switching the global theme (which would also retint the system status bar). Mirrors the Flow
 * palette in Color.kt.
 */
object FlowAccent {
    val primary = Color(0xFF4E63A6)
    val secondary = Color(0xFF3E8E9E)
    val gradient = listOf(primary, secondary)
}

/**
 * A calm circular timer. When [running] it breathes (gentle scale + glow); otherwise it is static.
 * The breathing is intentionally subtle and never essential — the elapsed label is always legible,
 * so it degrades gracefully under reduced-motion preferences.
 */
@Composable
fun BreathingTimerRing(
    elapsedLabel: String,
    statusLabel: String,
    running: Boolean,
    accent: Color = FlowAccent.primary,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "breath")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = Motion.slow * 6, easing = Motion.easing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val scale = if (running) pulse else 1f

    Box(modifier = modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                    radius = r,
                ),
                radius = r * scale,
            )
            drawCircle(
                color = accent.copy(alpha = 0.55f),
                radius = r * 0.72f,
                style = Stroke(width = 3f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(elapsedLabel, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A selectable Flow ambience scene tile backed by local licensed loops; see [com.classmate.app.ui.flow.FlowScenes]. */
@Composable
fun FlowSceneCard(scene: FlowScene, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val tokens = ClassMateTheme.colors
    val container by animateColorAsState(
        targetValue = if (selected) tokens.primaryContainer else tokens.focusSurface,
        animationSpec = tween(durationMillis = 220),
        label = "flow-scene-container",
    )
    val border by animateColorAsState(
        targetValue = if (selected) tokens.primary else tokens.outline.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 220),
        label = "flow-scene-border",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected && !tokens.isDark) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "flow-scene-elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.015f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "flow-scene-scale",
    )
    Surface(
        modifier = modifier.width(150.dp).scale(scale).clickable { onClick() },
        shape = Radii.cardShape,
        color = container,
        contentColor = cs.onSurface,
        border = BorderStroke(1.dp, border),
        shadowElevation = elevation,
    ) {
        Column(Modifier.padding(Dimens.m)) {
            Text(scene.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.xxs))
            Text(scene.description, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}
