package com.classmate.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.classmate.app.state.AiProcessingUiState
import com.classmate.app.ui.design.Dimens

@Composable
fun AiProcessingDialog(
    state: AiProcessingUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onContinueManual: () -> Unit,
) {
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = { if (state.canCancel) onCancel() },
        title = { Text(state.title.ifBlank { "AI 处理中" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                if (state.source.isNotBlank()) {
                    Text(
                        "当前来源：${state.source}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                state.steps.forEachIndexed { index, step ->
                    val marker = if (index == state.activeStep) "●" else "○"
                    Text("$marker $step", style = MaterialTheme.typography.bodySmall)
                }
                state.fallbackMessage?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(Dimens.xs))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (state.canContinueManual) {
                    TextButton(onClick = onContinueManual) { Text("继续手动编辑") }
                }
                if (state.canRetry) {
                    TextButton(onClick = onRetry) { Text("重试") }
                }
            }
        },
        dismissButton = {
            if (state.canCancel) TextButton(onClick = onCancel) { Text("取消") }
        },
    )
}
