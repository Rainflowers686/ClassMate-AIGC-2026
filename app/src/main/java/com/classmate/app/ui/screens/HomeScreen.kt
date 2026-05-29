package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.StatusDot
import com.classmate.app.ui.designsystem.StatusTone
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Brand wordmark + tagline + provider/config status + 2 CTAs + Settings link.
 */
@Composable
fun HomeScreen(
    state: ClassMateUiState,
    onStart: () -> Unit,
    onLoadDemo: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    AppScaffold {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(spacing.xxl))
            Text(
                "ClassMate",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = colors.fgPrimary
            )
            Spacer(Modifier.height(spacing.sm))
            Text(
                "证据链式课堂讲解与微测复习助手",
                style = MaterialTheme.typography.titleMedium,
                color = colors.fgSecondary
            )
            Spacer(Modifier.height(spacing.lg))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(tone = StatusTone.Brand)
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            "Provider · ${state.requestedProvider}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.fgPrimary
                        )
                    }
                    Spacer(Modifier.height(spacing.xs))
                    Text(
                        state.configHint.ifBlank { "等待配置加载" },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.fgMuted
                    )
                }
            }
            Spacer(Modifier.height(spacing.xl))
            PrimaryButton(text = "开始 / Start", onClick = onStart)
            Spacer(Modifier.height(spacing.md))
            OutlinedActionButton(text = "加载 Demo / Load demo", onClick = onLoadDemo)
            Spacer(Modifier.height(spacing.md))
            TextButton(onClick = onOpenSettings) {
                Text("⚙ 主题 / About", color = colors.brandPrimary)
            }
            Spacer(Modifier.height(spacing.lg))
            Text(
                "v0.4 foundation rebuild · 强制证据闭合 · 三主题 · 兜底可见",
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )
        }
    }
}
