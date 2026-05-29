package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.SectionHeader
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.app.ui.theme.ThemeId

@Composable
fun SettingsScreen(
    state: ClassMateUiState,
    onThemeChange: (ThemeId) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    AppScaffold(
        title = "设置 · 关于",
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
            GlassCard {
                Column {
                    ThemeId.values().forEach { id ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.themeId == id,
                                onClick = { onThemeChange(id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.brandPrimary,
                                    unselectedColor = colors.fgMuted
                                )
                            )
                            Spacer(Modifier.width(spacing.sm))
                            Text(
                                id.displayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.fgPrimary,
                                fontWeight = if (state.themeId == id) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            SectionHeader(title = "Provider")
            GlassCard {
                Column {
                    Text(
                        "请求 · ${state.requestedProvider}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.fgPrimary
                    )
                    Text(
                        "实际 · ${state.activeProvider}" +
                            if (state.fallbackUsed) "  (已兜底)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.fallbackUsed) colors.statusError else colors.fgMuted
                    )
                    if (state.configHint.isNotBlank()) {
                        Spacer(Modifier.height(spacing.xs))
                        Text(
                            state.configHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.fgMuted
                        )
                    }
                }
            }

            SectionHeader(title = "关于")
            GlassCard {
                Column {
                    Text(
                        "ClassMate v0.4 · foundation rebuild",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.fgPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "证据链式课堂讲解与微测复习助手。本工程不接入小 V / 原子通知 / 负一屏 / 课中录音；BlueLM 接入需要官方契约。",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.fgMuted
                    )
                }
            }
        }
    }
}
