package com.classmate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState

/**
 * Hotword entry. The spec recommends 5–20 keywords (§2.1 R2); we surface a
 * count + range warning but do not block — the user is allowed to skip
 * hotwords entirely, and DemoProvider doesn't actually consume them.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HotwordScreen(
    state: ClassMateUiState,
    onPendingChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("课程热词", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "推荐 5–20 个关键概念。可跳过 — DemoProvider 不强制要求。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.pendingHotword,
                onValueChange = onPendingChange,
                label = { Text("输入新热词") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
            Button(onClick = onAdd, enabled = state.pendingHotword.isNotBlank()) {
                Text("添加")
            }
        }

        Text(
            "已添加 ${state.hotwords.size} 个热词",
            style = MaterialTheme.typography.bodySmall
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            state.hotwords.forEach { h ->
                AssistChip(
                    onClick = { onRemove(h) },
                    label = { Text(h) }
                )
            }
        }

        Spacer(Modifier.height(0.dp))

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Button(onClick = onNext) { Text("下一步：分析") }
        }
    }
}
