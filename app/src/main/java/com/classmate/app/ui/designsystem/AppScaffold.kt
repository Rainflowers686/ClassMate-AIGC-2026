package com.classmate.app.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
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
 * Shared screen skeleton: optional top bar (title + leading back action) and
 * an optional bottom action row. The body fills the middle and applies
 * horizontal padding from spacing.lg.
 *
 * The top bar is INSIDE [BrandSurface] so the canvas gradient still shows
 * through, but we honor system bars via systemBarsPadding on the column.
 */
@Composable
fun AppScaffold(
    title: String? = null,
    onBack: (() -> Unit)? = null,
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
            if (title != null || onBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text("← 返回", color = colors.brandPrimary)
                        }
                    }
                    if (title != null) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.fgPrimary,
                            modifier = Modifier.padding(start = spacing.sm)
                        )
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
                        .padding(spacing.lg)
                ) {
                    bottomBar()
                }
            }
        }
    }
    // Suppress unused-warning for unused vals in some paths
    @Suppress("UNUSED_EXPRESSION") PaddingValues(0.dp)
}

/**
 * Two-button bottom row used by many screens: a tertiary "back" action on
 * the left and a primary continue CTA on the right.
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
            Box(modifier = Modifier.weight(1f)) { secondary() }
            Box(modifier = Modifier.weight(1f)) { primary() }
        } else {
            Box(modifier = Modifier.weight(1f)) { primary() }
        }
    }
}
