package com.classmate.app.ui.screens.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.design.Dimens
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType

@Composable
fun FeedbackScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    var note by remember { mutableStateOf("") }

    ClassMateScaffold(title = "反馈", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            ClassMateCard {
                Text("你的反馈会改变下一轮", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Text(
                    "告诉 ClassMate 哪里不对、哪里太难、哪里漏了。这些信号会更新你的掌握度，并重排复习计划——这就是“反馈优化下一轮”。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("补充说明（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.medium,
            )

            FeedbackType.entries.forEach { type ->
                FeedbackOptionCard(
                    title = type.displayZh,
                    hint = hintFor(type),
                    onClick = {
                        viewModel.submitFeedback(type, FeedbackTargetKind.ANALYSIS, null, note)
                        note = ""
                    },
                )
            }

            if (ui.feedbackEvents.isNotEmpty()) {
                ClassMateCard {
                    Text("已记录的反馈（${ui.feedbackEvents.size}）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.s))
                    ui.feedbackEvents.asReversed().forEach { event ->
                        Text(
                            "• ${event.type.displayZh}" + if (event.note.isNotBlank()) " — ${event.note}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Dimens.xxs))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackOptionCard(title: String, hint: String, onClick: () -> Unit) {
    ClassMateCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("记录 ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun hintFor(type: FeedbackType): String = when (type) {
    FeedbackType.TOO_EASY -> "整体偏简单，希望更有挑战"
    FeedbackType.TOO_HARD -> "整体偏难，需要放慢节奏"
    FeedbackType.NOT_ACCURATE -> "某些理解不准确"
    FeedbackType.EVIDENCE_WRONG -> "证据与结论对不上"
    FeedbackType.MISSING_KEY_POINT -> "漏掉了重要知识点"
    FeedbackType.ALREADY_MASTERED -> "这些我已经掌握了"
    FeedbackType.NEED_MORE_EXAMPLES -> "希望多练几道"
}
