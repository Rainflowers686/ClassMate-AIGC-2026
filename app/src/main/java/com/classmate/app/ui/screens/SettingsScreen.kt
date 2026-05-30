package com.classmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.SectionHeader
import com.classmate.app.ui.designsystem.StatusBar
import com.classmate.app.ui.designsystem.StatusTone
import com.classmate.app.ui.theme.ClassMateColors
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.app.ui.theme.ThemeId

/**
 * Settings: pickable theme tiles + concise About. Provider info is shown as a
 * single status line, not an engineering panel.
 */
@Composable
fun SettingsScreen(
    state: ClassMateUiState,
    onThemeChange: (ThemeId) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    AppScaffold(
        title = "设置",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = { PrimaryButton(text = "完成", onClick = onBack) }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            SectionHeader(title = "主题")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                ThemeId.values().forEach { id ->
                    ThemeTile(
                        themeId = id,
                        selected = state.themeId == id,
                        onClick = { onThemeChange(id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionHeader(title = "分析来源")
            StatusBar(
                tone = if (state.fallbackUsed) StatusTone.Warning else StatusTone.Success,
                title = providerDisplayName(state),
                secondary = if (state.fallbackUsed) "云端不可用时自动启用本地证据引擎，确保流程不中断"
                else "正常使用本地证据引擎，所有结果可追溯到原文"
            )

            SectionHeader(title = "关于 ClassMate")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, shapes.large)
                    .border(1.dp, colors.outline, shapes.large)
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    "ClassMate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.fgPrimary
                )
                Text(
                    "把课堂内容变成可追溯的知识点、微测与复习计划。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.fgSecondary
                )
                Text(
                    "仅文本输入，不接入录音；所有结果都能在原文中找到出处。",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgMuted
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    "版本 v0.4",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fgMuted
                )
            }
        }
    }
}

@Composable
private fun ThemeTile(
    themeId: ThemeId,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    val tileColors = ClassMateColors.of(themeId, dark = false)
    val borderColor = if (selected) colors.brandPrimary else colors.outline
    val borderWidth = if (selected) 2.dp else 1.dp
    Column(
        modifier = modifier
            .background(colors.surface, shapes.large)
            .border(borderWidth, borderColor, shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        // Mini palette: canvas swatch + 3 token dots.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(tileColors.canvas, shapes.medium)
                .border(1.dp, tileColors.glassStroke, shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Swatch(tileColors.brandPrimary)
                Swatch(tileColors.statusSuccess)
                Swatch(tileColors.evidenceHighlight)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                themeId.displayLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = colors.fgPrimary,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Spacer(Modifier.width(spacing.xs))
                Text(
                    "✓",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.brandPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(color, CircleShape)
    )
}

private fun providerDisplayName(state: ClassMateUiState): String = when (state.activeProvider) {
    "local" -> "本地证据引擎" + if (state.fallbackUsed) "（已自动降级）" else ""
    "compatible" -> "云端大模型（兼容协议）"
    "bluelm" -> "蓝心大模型"
    else -> state.activeProvider
}
