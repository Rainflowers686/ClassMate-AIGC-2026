package com.classmate.app.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Lightweight one-line status strip: a status dot, a short title, and an
 * optional secondary description. Lives inline (not a card) so it looks
 * like a real-app status bar instead of a debug box.
 */
@Composable
fun StatusBar(
    tone: StatusTone,
    title: String,
    secondary: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    val toneColor = statusDotColor(tone)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(toneColor.copy(alpha = 0.06f), shapes.medium)
            .border(1.dp, toneColor.copy(alpha = 0.18f), shapes.medium)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(tone = tone, sizeDp = 8)
        Spacer(Modifier.width(spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = colors.fgPrimary
            )
            if (!secondary.isNullOrBlank()) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgMuted
                )
            }
        }
    }
}

/**
 * Section caption used above list/grid blocks. Lighter than SectionHeader —
 * one tiny eyebrow line in muted color.
 */
@Composable
fun TinyCaption(text: String, modifier: Modifier = Modifier) {
    val colors = LocalClassMateColors.current
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.fgMuted,
        modifier = modifier
    )
}

/**
 * Subtle full-width inline banner (no card chrome) for one-line tips like
 * "已为你生成 3 个知识点和 3 道微测题". Tone selects the accent.
 */
@Composable
fun InlineCalloutLine(
    tone: StatusTone,
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    val toneColor = statusDotColor(tone)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(toneColor.copy(alpha = 0.08f), shapes.medium)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(tone = tone, sizeDp = 8)
            Spacer(Modifier.width(spacing.sm))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.fgPrimary
            )
        }
    }
    // Suppress unused-import for Arrangement when caller doesn't use it.
    @Suppress("UNUSED_EXPRESSION") Arrangement.Start
}
