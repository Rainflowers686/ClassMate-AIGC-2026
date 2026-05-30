package com.classmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.TinyCaption
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing

/**
 * Two flat text inputs (no heavy outlined boxes), gentle helper copy, and a
 * subtle "示例课程" line above the primary action. Optimized for paste.
 */
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
        title = "导入课程内容",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "下一步：补充热词",
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
            TinyCaption("支持讲稿、TTS 字幕、公开课文本段落。仅文本输入，不接入录音。")

            // Course title — single-line, ghost-styled text field (no card chrome).
            FlatField(
                value = state.courseTitle,
                onValueChange = onTitleChange,
                placeholder = "课程标题（可选）",
                singleLine = true
            )

            // Body text — borderless container w/ counter inside.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, shapes.large)
                    .border(1.dp, colors.brandPrimary.copy(alpha = 0.22f), shapes.large)
                    .padding(horizontal = spacing.md, vertical = spacing.sm)
            ) {
                FlatField(
                    value = state.courseText,
                    onValueChange = onTextChange,
                    placeholder = "在此粘贴或输入课程文本…",
                    singleLine = false,
                    minHeight = 200.dp,
                    chromeless = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${state.courseText.length} / 8000 字",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.fgMuted
                    )
                    TextButton(onClick = onLoadDemo) {
                        Text("载入示例课程", color = colors.brandPrimary)
                    }
                }
            }

            Text(
                "示例课程：高等数学《泰勒公式与麦克劳林公式》——一键填好标题、文本与热词。",
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )

            Spacer(Modifier.height(spacing.sm))
        }
    }
}

/**
 * Borderless text field used inside our card-style containers — looks like
 * a normal input line rather than a Material outlined box. When
 * [chromeless] is true the field has no background and no internal padding,
 * suitable for embedding in another container that already paints the
 * surface (see body-text box above).
 */
@Composable
private fun FlatField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    chromeless: Boolean = false
) {
    val colors = LocalClassMateColors.current
    val shapes = LocalClassMateShapes.current
    val spacing = LocalClassMateSpacing.current
    val container: Modifier = if (chromeless) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .background(colors.surface, shapes.medium)
            .border(1.dp, colors.brandPrimary.copy(alpha = 0.22f), shapes.medium)
    }
    val heightMod = if (minHeight.value > 0f) Modifier.height(minHeight) else Modifier
    Box(
        modifier = container.then(heightMod)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.fgMuted
                )
            },
            singleLine = singleLine,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (chromeless) 0.dp else spacing.sm),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = colors.brandPrimary,
                focusedTextColor = colors.fgPrimary,
                unfocusedTextColor = colors.fgPrimary
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = colors.fgPrimary,
                fontWeight = if (singleLine) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}
