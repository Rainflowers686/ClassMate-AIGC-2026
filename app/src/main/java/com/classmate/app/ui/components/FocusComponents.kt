package com.classmate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.theme.ClassMateTheme

/**
 * Stage 9A Focus component kit. Everything here consumes theme tokens only (no hardcoded brand
 * colors), stays hairline-and-soft-shadow (no heavy glass/blur), and keeps debug detail collapsed
 * by default.
 */

/** The honest answer-source badge: 云端蓝心 / 端侧蓝心 / 安全占位 with a quiet semantic dot. */
@Composable
fun SourceBadge(label: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    val dot = when {
        label.contains("云端") -> cs.primary
        label.contains("端侧") -> ext.success
        else -> cs.outline
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = cs.surfaceVariant,
        contentColor = cs.onSurfaceVariant,
        border = BorderStroke(0.75.dp, cs.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
        }
    }
}

/** Page-level hero header: large quiet title + supporting line, no poster gradient. */
@Composable
fun HeroHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Square-ish quick entry tile with an icon chip — the Home "what do I do next" grid. */
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainer: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

/** One step of the analysis pipeline: dot/check state + label + caption. */
@Composable
fun ProgressStepRow(
    title: String,
    caption: String,
    state: StepState,
    isLast: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val ext = ClassMateTheme.extended
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val (fill, ring) = when (state) {
                StepState.DONE -> ext.success to ext.success
                StepState.ACTIVE -> cs.primary to cs.primary
                StepState.PENDING -> cs.surface to cs.outlineVariant
            }
            Surface(shape = CircleShape, color = fill, border = BorderStroke(1.5.dp, ring)) {
                Box(Modifier.size(14.dp))
            }
            if (!isLast) {
                Box(
                    Modifier
                        .width(1.5.dp)
                        .height(34.dp)
                        .background(if (state == StepState.DONE) ext.success.copy(alpha = 0.45f) else cs.outlineVariant),
                )
            }
        }
        Spacer(Modifier.width(Dimens.m))
        Column(Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (state == StepState.PENDING) cs.onSurfaceVariant else cs.onSurface,
            )
            Text(caption, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

enum class StepState { DONE, ACTIVE, PENDING }

/** Structured failure breakdown — never a raw log dump. */
@Composable
fun ErrorBreakdownCard(
    rows: List<Pair<String, String>>,
    advice: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = cs.surface,
        border = BorderStroke(0.75.dp, cs.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(Dimens.cardPadding)) {
            Text("本次分析结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.s))
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f).padding(start = Dimens.l),
                        maxLines = 3,
                    )
                }
            }
            Spacer(Modifier.height(Dimens.s))
            Surface(shape = MaterialTheme.shapes.small, color = cs.surfaceVariant) {
                Text(
                    advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Collapsible developer diagnostics. Safe lines / probe output live HERE — collapsed by default so
 * no screen reads like an engineering log. Title is intentionally fixed to "开发诊断详情".
 */
@Composable
fun DiagnosticDetailsCard(
    lines: List<String>,
    modifier: Modifier = Modifier,
    title: String = "开发诊断详情",
) {
    if (lines.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceVariant,
        border = BorderStroke(0.75.dp, cs.outlineVariant),
    ) {
        Column(Modifier.clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                Text(if (expanded) "收起" else "展开", style = MaterialTheme.typography.labelMedium, color = cs.primary)
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                lines.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Settings theme explainer: swatch strip + name + scope. Preview only — no heavy glass. */
@Composable
fun ThemePreviewCard(
    name: String,
    tagline: String,
    description: String,
    backgroundColor: Color,
    surfaceColor: Color,
    accentColor: Color,
    secondaryColor: Color = accentColor,
    tertiaryColor: Color = accentColor,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = ClassMateTheme.colors
    val container by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = if (tokens.isDark) 0.09f else 0.08f) else tokens.surface.copy(alpha = if (tokens.isDark) 0.94f else 0.98f),
        animationSpec = tween(durationMillis = 200),
        label = "theme-preview-container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.32f) else tokens.outline.copy(alpha = 0.18f),
        animationSpec = tween(durationMillis = 200),
        label = "theme-preview-border",
    )
    val titleColor by animateColorAsState(
        targetValue = if (selected) accentColor else tokens.textPrimary,
        animationSpec = tween(durationMillis = 200),
        label = "theme-preview-title",
    )
    val elevation by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "theme-preview-elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.001f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "theme-preview-scale",
    )
    val border = BorderStroke(if (selected) 1.dp else 0.75.dp, borderColor)
    val base = modifier.fillMaxWidth()
    Surface(
        modifier = (if (onClick != null) base.scale(scale).clickable { onClick() } else base).defaultMinSize(minHeight = 100.dp),
        shape = RoundedCornerShape(19.dp),
        color = container,
        border = border,
        shadowElevation = elevation,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 42.dp, height = 32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(width = 16.dp, height = 14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(surfaceColor.copy(alpha = 0.62f)),
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(width = 16.dp, height = 3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(secondaryColor.copy(alpha = 0.34f)),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(width = 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(tertiaryColor.copy(alpha = 0.62f)),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .height(2.dp)
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.28f)),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .height(2.dp)
                        .fillMaxWidth(0.42f)
                        .background(tertiaryColor.copy(alpha = 0.42f)),
                )
                if (selected) {
                    Surface(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp),
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.92f),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (tokens.isDark) Color.Black else Color.White,
                            modifier = Modifier.padding(2.dp).size(10.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(Dimens.s))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tagline, style = MaterialTheme.typography.labelMedium, color = tokens.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = tokens.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
