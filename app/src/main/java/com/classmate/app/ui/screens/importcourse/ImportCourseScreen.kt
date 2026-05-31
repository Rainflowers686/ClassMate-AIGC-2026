package com.classmate.app.ui.screens.importcourse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.design.Dimens

@Composable
fun ImportCourseScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    ClassMateScaffold(title = "导入课堂文本", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            ClassMateCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(Dimens.s))
                    Text("仅文本输入", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(Dimens.s))
                Text(
                    "本轮只支持粘贴课堂文本，暂不接入录音 / 语音转写。把课件、笔记或老师讲述的整理文本粘进来即可。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = ui.courseTitle,
                onValueChange = viewModel::updateCourseTitle,
                label = { Text("课程标题（可选）") },
                placeholder = { Text("例如：高等数学 · 无穷级数") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )

            OutlinedTextField(
                value = ui.courseText,
                onValueChange = viewModel::updateCourseText,
                label = { Text("课堂文本") },
                placeholder = { Text("把一节课的内容粘贴到这里……") },
                minLines = 8,
                maxLines = 16,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )

            Text(
                "已输入 ${ui.courseText.count { !it.isWhitespace() }} 字",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Dimens.xxs))

            PrimaryButton(
                text = "开始分析",
                onClick = { viewModel.startAnalysis() },
                enabled = ui.courseText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "加载示例课程（级数）",
                onClick = { viewModel.loadSample() },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "分析将优先调用蓝心大模型；若未配置密钥或调用失败，会自动回退到本地兜底，确保流程不中断。每个结论都会绑定原文证据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
