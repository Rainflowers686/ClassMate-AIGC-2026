package com.classmate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.theme.ClassMateTheme

@Composable
fun ClassMateSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
    )
}

@Composable
fun ClassMateTwoLineDescription(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun ClassMateChipText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    ClassMateSingleLineText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

/**
 * The default Focus container: white surface, hairline border, whisper of elevation. Cards are the
 * quiet backbone of the app — they hold information without shouting.
 */
@Composable
fun ClassMateCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = Modifier.fillMaxWidth().then(modifier)
    val tokens = ClassMateTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val surface by animateColorAsState(
        targetValue = if (pressed) tokens.surfaceContainerHigh else tokens.surfaceContainerLow,
        animationSpec = tween(durationMillis = 180),
        label = "card-surface",
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) 0.dp else if (tokens.isDark) 0.dp else 2.dp,
        animationSpec = tween(durationMillis = 180),
        label = "card-elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "card-press",
    )
    Surface(
        modifier = if (onClick != null) {
            base.scale(scale).clickable(interaction, indication = null) { onClick() }
        } else {
            base
        },
        shape = RoundedCornerShape(ClassMateTheme.shapes.cardRadius),
        color = surface,
        contentColor = tokens.textPrimary,
        border = BorderStroke(0.75.dp, tokens.outline.copy(alpha = if (tokens.isDark) 0.45f else 0.28f)),
        shadowElevation = elevation,
    ) {
        Column(Modifier.padding(Dimens.cardPadding), content = content)
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val tokens = ClassMateTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp).defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(ClassMateTheme.shapes.buttonRadius),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (tokens.isDark) 0.dp else 1.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = tokens.surfaceVariant,
            disabledContentColor = tokens.textSecondary,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        ClassMateSingleLineText(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val tokens = ClassMateTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp).defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(ClassMateTheme.shapes.buttonRadius),
        border = BorderStroke(1.dp, tokens.secondary.copy(alpha = 0.32f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = tokens.secondaryContainer.copy(alpha = if (tokens.isDark) 0.34f else 0.42f),
            contentColor = tokens.secondary,
            disabledContainerColor = tokens.surfaceVariant,
            disabledContentColor = tokens.textSecondary,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        ClassMateSingleLineText(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Tertiary / ghost button: lowest visual weight, for quiet actions like "稍后" / "返回手动整理".
 * Same height + radius + single-line text as the others so the system stays consistent.
 */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val tokens = ClassMateTheme.colors
    TextButton(
        onClick = onClick,
        modifier = modifier.height(52.dp).defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(ClassMateTheme.shapes.buttonRadius),
        colors = ButtonDefaults.textButtonColors(
            contentColor = tokens.textSecondary,
            disabledContentColor = tokens.textSecondary.copy(alpha = 0.5f),
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        ClassMateSingleLineText(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Destructive button: careful, NOT a screaming red — uses the muted errorContainer so it reads as
 * "be careful" rather than "primary action". For "清除配置" / "删除记录".
 */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val tokens = ClassMateTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp).defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(ClassMateTheme.shapes.buttonRadius),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            disabledContainerColor = tokens.surfaceVariant,
            disabledContentColor = tokens.textSecondary,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        ClassMateSingleLineText(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Experimental-feature entry: secondary weight + a small warning-tinted badge so it never looks like a
 * primary action. [badge] text is caller-supplied so it can be localized.
 */
@Composable
fun ExperimentalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: String = "实验",
) {
    val tokens = ClassMateTheme.colors
    val warn = ClassMateTheme.extended.warning
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp).defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(ClassMateTheme.shapes.buttonRadius),
        border = BorderStroke(1.dp, warn.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = warn.copy(alpha = 0.06f),
            contentColor = warn,
            disabledContainerColor = tokens.surfaceVariant,
            disabledContentColor = tokens.textSecondary,
        ),
    ) {
        ClassMateSingleLineText(text, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(50), color = warn.copy(alpha = 0.16f)) {
            Text(
                badge,
                style = MaterialTheme.typography.labelSmall,
                color = warn,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Hero panel with the theme's brand gradient — used on Home. */
@Composable
fun GradientHeroPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(ClassMateTheme.extended.heroGradient)),
    ) {
        Column(Modifier.padding(Dimens.xl), content = content)
    }
}

/** Icon badge + title + description row, for value props and feature lists. */
@Composable
fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    iconContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(Dimens.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Compact key → value row used in detail cards. */
@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.45f))
        Spacer(Modifier.width(Dimens.s))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false, modifier = Modifier.weight(0.55f))
    }
}
