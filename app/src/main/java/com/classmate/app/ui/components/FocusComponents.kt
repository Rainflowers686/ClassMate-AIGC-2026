package com.classmate.app.ui.components

import androidx.compose.animation.animateContentSize
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
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val border = if (selected) BorderStroke(1.25.dp, cs.primary) else BorderStroke(0.75.dp, cs.outlineVariant)
    val base = modifier.fillMaxWidth()
    Surface(
        modifier = (if (onClick != null) base.clickable { onClick() } else base).defaultMinSize(minHeight = 104.dp),
        shape = MaterialTheme.shapes.large,
        color = cs.surface,
        border = border,
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 56.dp, height = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor),
            ) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(width = 36.dp, height = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceColor),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .height(5.dp)
                        .fillMaxWidth()
                        .background(accentColor),
                )
                if (selected) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(7.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(accentColor),
                    )
                }
            }
            Spacer(Modifier.width(Dimens.m))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tagline, style = MaterialTheme.typography.labelMedium, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Text("当前", style = MaterialTheme.typography.labelMedium, color = cs.primary, maxLines = 1)
            }
        }
    }
}
