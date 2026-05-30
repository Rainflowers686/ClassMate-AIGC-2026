package com.classmate.app.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Shared screen skeleton: optional top bar (title + leading back action +
 * optional trailing action), a body region, and an optional bottom action
 * row.
 *
 * v0.4.1 productization pass: lighter top bar (no full-width separator),
 * fixed 16dp horizontal padding for the body, generous bottom safe-area
 * padding. trailing slot used by Home for the Settings/About entry so it
 * stops looking like a primary action.
 */
@Composable
fun AppScaffold(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    BrandSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            if (title != null || onBack != null || trailing != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text("← 返回", color = colors.brandPrimary)
                        }
                        Spacer(Modifier.width(spacing.xs))
                    } else {
                        Spacer(Modifier.width(spacing.sm))
                    }
                    if (title != null) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.fgPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (trailing != null) {
                        trailing()
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg)
            ) {
                content()
            }
            if (bottomBar != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.md)
                ) {
                    bottomBar()
                }
            }
        }
    }
}

/**
 * Two-button bottom row: secondary on the left (~40 % width) + primary on
 * the right (~60 % width). Single-action mode shows the primary at full
 * width. Sizes feel less laboratory than equal-50/50.
 */
@Composable
fun BottomActionRow(
    primary: @Composable () -> Unit,
    secondary: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalClassMateSpacing.current.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (secondary != null) {
            Box(modifier = Modifier.weight(0.42f)) { secondary() }
            Box(modifier = Modifier.weight(0.58f)) { primary() }
        } else {
            Box(modifier = Modifier.weight(1f)) { primary() }
        }
    }
}
