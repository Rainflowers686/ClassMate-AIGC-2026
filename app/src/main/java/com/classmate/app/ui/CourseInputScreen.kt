package com.classmate.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState

@Composable
fun CourseInputScreen(
    state: ClassMateUiState,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onLoadDemo: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "课程内容导入",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "粘贴课程讲稿、TTS 字幕或公开课文本段落。不支持原始课堂录音 — 仅文本。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = state.courseTitle,
            onValueChange = onTitleChange,
            label = { Text("课程标题") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.courseText,
            onValueChange = onTextChange,
            label = { Text("课程文本") },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        OutlinedButton(onClick = onLoadDemo) { Text("加载 demo_course") }

        Spacer(Modifier.height(0.dp))

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            Button(
                onClick = onNext,
                enabled = state.courseText.isNotBlank()
            ) { Text("下一步：热词") }
        }
    }
}
