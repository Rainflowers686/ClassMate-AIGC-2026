package com.classmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.InlineCalloutLine
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.StatusTone
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
    val limit = 20
    val count = state.hotwords.size
    val limitReached = count >= limit
    val belowMin = count < 5
    AppScaffold(
        title = "补充热词",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "下一步：开始分析",
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
            InlineCalloutLine(
                tone = StatusTone.Brand,
                text = "热词会在分析时被优先保留原始表达，减少专业术语被改写。"
            )

            // Inline add-row: ghost text field + plus button. No card chrome.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.surface, shapes.medium)
                        .border(1.dp, colors.brandPrimary.copy(alpha = 0.22f), shapes.medium)
                ) {
                    TextField(
                        value = state.pendingHotword,
                        onValueChange = onPendingChange,
                        placeholder = {
                            Text(
                                "输入新的热词，例如：泰勒公式",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.fgMuted
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = colors.brandPrimary,
                            focusedTextColor = colors.fgPrimary,
                            unfocusedTextColor = colors.fgPrimary
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.fgPrimary)
                    )
                }
                Spacer(Modifier.width(spacing.sm))
                IconButton(
                    onClick = onAdd,
                    enabled = state.pendingHotword.isNotBlank() && !limitReached
                ) {
                    Text(
                        "添加",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.pendingHotword.isNotBlank() && !limitReached)
                            colors.brandPrimary else colors.fgMuted
                    )
                }
            }

            // Empty state — visually fill the void with a soft hint, not a debug message.
            if (state.hotwords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.brandPrimary.copy(alpha = 0.04f), shapes.large)
                        .padding(horizontal = spacing.lg, vertical = spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "建议添加 5–20 个热词。\n比如课程中的人名、术语、英文缩写。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.fgSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    state.hotwords.forEach { word ->
                        AssistChip(
                            onClick = { onRemove(word) },
                            label = { Text("$word  ×") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = colors.brandPrimary.copy(alpha = 0.10f),
                                labelColor = colors.brandPrimary
                            ),
                            shape = shapes.pill
                        )
                    }
                }
            }

            // Small count line, muted by default; warns if at limit or too few.
            val countLine = when {
                limitReached -> "已添加 $count 个，已达上限"
                belowMin -> "已添加 $count 个 · 建议至少 5 个"
                else -> "已添加 $count 个 · 还可添加 ${limit - count} 个"
            }
            Text(
                countLine,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    limitReached -> colors.statusWarning
                    belowMin -> colors.fgMuted
                    else -> colors.fgMuted
                }
            )

            Spacer(Modifier.height(spacing.sm))
        }
    }
}
