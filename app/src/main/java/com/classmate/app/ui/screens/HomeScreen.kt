package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.StatusBar
import com.classmate.app.ui.designsystem.StatusTone
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Brand wordmark + tagline + inline provider status strip + primary/secondary
 * CTA. The Settings entry is moved to the trailing slot of the top bar so it
 * stops looking like a third primary action.
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
    val providerLabel = providerDisplayName(state)
    val providerTone = if (state.fallbackUsed) StatusTone.Warning else StatusTone.Success
    AppScaffold(
        trailing = {
            TextButton(onClick = onOpenSettings) {
                Text("设置", color = colors.brandPrimary)
            }
        }
    ) {
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
                "把课堂内容变成可追溯的知识点、微测与复习计划。",
                style = MaterialTheme.typography.titleMedium,
                color = colors.fgSecondary
            )
            Spacer(Modifier.height(spacing.lg))
            StatusBar(
                tone = providerTone,
                title = "当前分析方式：$providerLabel",
                secondary = state.configHint.ifBlank { null }
            )
            Spacer(Modifier.height(spacing.xl))
            PrimaryButton(text = "开始导入课程", onClick = onStart)
            Spacer(Modifier.height(spacing.md))
            OutlinedActionButton(text = "体验示例课程", onClick = onLoadDemo)
            Spacer(Modifier.weight(1f))
            Text(
                "ClassMate v0.4 · 仅文本输入 · 不接入录音、不收集隐私",
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )
            Spacer(Modifier.height(spacing.sm))
        }
    }
}

private fun providerDisplayName(state: ClassMateUiState): String {
    // Active provider is internally "local" / "compatible" / "bluelm". Map to
    // user-facing Chinese names; if a fallback fired, surface that fact.
    val baseName = when (state.activeProvider) {
        "local" -> "本地证据引擎"
        "demo" -> "本地证据引擎"
        "compatible" -> "云端大模型（兼容协议）"
        "bluelm" -> "蓝心大模型"
        else -> "本地证据引擎"
    }
    return if (state.fallbackUsed && (state.activeProvider == "local" || state.activeProvider == "demo")) {
        "$baseName（已自动降级）"
    } else {
        baseName
    }
}
