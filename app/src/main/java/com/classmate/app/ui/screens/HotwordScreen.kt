package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HotwordScreen(
    state: ClassMateUiState,
    onPendingChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    val limitReached = state.hotwords.size >= 20
    AppScaffold(
        title = "热词",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "下一步：分析",
                        onClick = onNext,
                        enabled = state.segments.isNotEmpty() || state.courseText.isNotBlank()
                    )
                },
                secondary = {
                    OutlinedActionButton(text = "返回", onClick = onBack)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                "5–20 个词，模型在分析时会保留它们的原始拼写。",
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.pendingHotword,
                    onValueChange = onPendingChange,
                    label = { Text("添加热词") },
                    singleLine = true,
                    shape = shapes.medium,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.outline,
                        cursorColor = colors.brandPrimary,
                        focusedLabelColor = colors.brandPrimary,
                        unfocusedLabelColor = colors.fgMuted
                    )
                )
                Spacer(Modifier.width(spacing.sm))
                IconButton(
                    onClick = onAdd,
                    enabled = state.pendingHotword.isNotBlank() && !limitReached
                ) {
                    Text("＋", color = colors.brandPrimary, style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                "已添加 (${state.hotwords.size}/20)" + if (limitReached) "  · 已达上限" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (limitReached) colors.statusWarning else colors.fgMuted
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                state.hotwords.forEach { word ->
                    AssistChip(
                        onClick = { onRemove(word) },
                        label = { Text("$word ×") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colors.surface,
                            labelColor = colors.brandPrimary
                        ),
                        shape = shapes.pill
                    )
                }
            }
            Spacer(Modifier.height(spacing.sm))
        }
    }
}
