package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

@Composable
fun CourseInputScreen(
    state: ClassMateUiState,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onLoadDemo: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    AppScaffold(
        title = "课程内容导入",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "下一步：热词",
                        onClick = onNext,
                        enabled = state.courseText.isNotBlank()
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
                "粘贴课程讲稿、TTS 字幕或公开课文本段落。不支持原始课堂录音 — 仅文本。",
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )
            OutlinedTextField(
                value = state.courseTitle,
                onValueChange = onTitleChange,
                label = { Text("课程标题") },
                singleLine = true,
                shape = shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.brandPrimary,
                    unfocusedBorderColor = colors.brandPrimary.copy(alpha = 0.28f),
                    cursorColor = colors.brandPrimary,
                    focusedLabelColor = colors.brandPrimary,
                    unfocusedLabelColor = colors.fgMuted,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                )
            )
            OutlinedTextField(
                value = state.courseText,
                onValueChange = onTextChange,
                label = { Text("课程文本") },
                shape = shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.brandPrimary,
                    unfocusedBorderColor = colors.brandPrimary.copy(alpha = 0.28f),
                    cursorColor = colors.brandPrimary,
                    focusedLabelColor = colors.brandPrimary,
                    unfocusedLabelColor = colors.fgMuted,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                )
            )
            Text(
                "字数 ${state.courseText.length} / 8000  ·  支持 demo 一键填入",
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )
            OutlinedActionButton(text = "加载 demo_course", onClick = onLoadDemo)
            Spacer(Modifier.height(spacing.sm))
        }
    }
}
