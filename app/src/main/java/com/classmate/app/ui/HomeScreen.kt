package com.classmate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// (fillMaxSize used only on the outer Column)
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateScreen
import com.classmate.app.state.ClassMateUiState

/**
 * Entry point. Two buttons — "Start" walks the user through the full input
 * pipeline; "Load demo" skips straight into a pre-filled CourseInputScreen.
 *
 * Spec §5.1 Step 1 puts the welcome page here; we keep it intentionally
 * sparse so the user picks up the demo flow in two taps.
 */
@Composable
fun HomeScreen(
    state: ClassMateUiState,
    onStart: () -> Unit,
    onLoadDemo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "ClassMate",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "证据链式课程讲解与微测复习助手",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            state.configHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart) { Text("开始 / Start") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onLoadDemo) {
            Text("加载 Demo / Load demo")
        }
        Spacer(Modifier.height(24.dp))
        Text(
            when (state.screen) {
                ClassMateScreen.Home -> "v0.3 主流程骨架。点击开始 → 课程文本 → 热词 → 分析 → 时间轴 → 微测 → 复习计划。"
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
