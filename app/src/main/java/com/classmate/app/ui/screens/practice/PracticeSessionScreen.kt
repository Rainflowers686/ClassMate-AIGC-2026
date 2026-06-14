package com.classmate.app.ui.screens.practice

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.practice.PracticeSearchLauncher
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ChipTone
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.product.QuietCard
import com.classmate.app.ui.components.EmptyStateCard
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.StatusChip
import com.classmate.app.ui.design.Dimens
import com.classmate.core.practice.PracticeItemType
import com.classmate.core.practice.PracticeOutcome
import com.classmate.core.practice.PracticeSearchEngine
import com.classmate.core.practice.PracticeSession
import com.classmate.core.practice.displayZh

@Composable
fun PracticeSessionScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val session = ui.practiceSession

    ClassMateScaffold(title = "专项练习", onBack = { viewModel.exitPractice() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            if (session == null) {
                EmptyStateCard(title = "没有进行中的练习", message = "请从复习或课程详情开始一轮专项练习。")
                return@Column
            }
            val result = ui.practiceResult
            if (result != null) {
                PracticeSummary(viewModel, session)
                return@Column
            }
            val item = session.items.getOrNull(ui.practiceIndex) ?: return@Column

            QuietCard {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    StatusChip(session.mode.displayZh(), tone = ChipTone.PRIMARY)
                    StatusChip("第 ${ui.practiceIndex + 1} / ${session.items.size} 题", tone = ChipTone.NEUTRAL)
                    StatusChip(item.type.displayZh(), tone = ChipTone.INFO)
                    if (item.needsRecheck) StatusChip("需复核", tone = ChipTone.WARNING)
                }
                Spacer(Modifier.height(Dimens.s))
                Text(item.knowledgePointTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.xs))
                Text(item.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                if (item.options.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.s))
                    item.options.forEach { opt ->
                        val mark = if (ui.practiceRevealed && opt.correct) "  ✅" else ""
                        Text("${opt.id}. ${opt.text}$mark", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Dimens.xxs))
                    }
                }

                if (ui.practiceRevealed) {
                    Spacer(Modifier.height(Dimens.s))
                    if (item.answer.isNotBlank()) {
                        Text("答案 / 解释：${item.answer}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item.evidenceQuote?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(Dimens.xxs))
                        Text("证据：「$it」", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(Dimens.s))
                    SecondaryButton(
                        text = if (item.type == PracticeItemType.QUIZ_RETRY) "查看答案" else "查看答案 / 证据",
                        onClick = { viewModel.revealPracticeAnswer() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Feedback buttons — horizontal/compact, never a vertical stack.
            Text("你的掌握情况", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                SecondaryButton("我答对了", onClick = { viewModel.answerPractice(PracticeOutcome.CORRECT) })
                SecondaryButton("我答错了", onClick = { viewModel.answerPractice(PracticeOutcome.WRONG) })
                SecondaryButton("已掌握", onClick = { viewModel.answerPractice(PracticeOutcome.MASTERED) })
                SecondaryButton("需要多练", onClick = { viewModel.answerPractice(PracticeOutcome.NEED_MORE_PRACTICE) })
            }
            Text("选择后自动进入下一题。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PracticeSummary(viewModel: AppViewModel, session: PracticeSession) {
    val context = LocalContext.current
    val result = viewModel.ui.practiceResult ?: return
    QuietCard {
        Text("本轮结果", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            StatusChip("正确 ${result.correctCount}", tone = ChipTone.SUCCESS)
            StatusChip("错误 ${result.wrongCount}", tone = ChipTone.WARNING)
            StatusChip("已掌握 ${result.masteredCount}", tone = ChipTone.PRIMARY)
            StatusChip("需要多练 ${result.needMorePracticeCount}", tone = ChipTone.INFO)
        }
        Spacer(Modifier.height(Dimens.s))
        Text("下一步建议：${result.nextSuggestion}", style = MaterialTheme.typography.bodyMedium)
    }

    // Phase D: on-device BlueLM explanation / next-step. Safety placeholder when unavailable — never
    // a fabricated rule explanation.
    QuietCard {
        Text("下一步建议（端侧蓝心）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(
            "由端侧 BlueLM 3B 生成错题解释与练习方向；端侧不可用时仅显示安全占位，不伪造解释。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        viewModel.ui.onDevicePracticeSuggestion?.let { suggestion ->
            Spacer(Modifier.height(Dimens.s))
            Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = if (viewModel.ui.onDevicePracticeSuggestionRunning) "生成中…" else "生成端侧下一步建议",
            onClick = { viewModel.generateOnDevicePracticeSuggestion() },
            enabled = !viewModel.ui.onDevicePracticeSuggestionRunning,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (result.needPracticeItems.isNotEmpty()) {
        QuietCard {
            Text("需要多练 · 推荐搜索", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(Dimens.s))
            result.needPracticeItems.forEach { need ->
                Text(need.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("推荐搜索词：${need.recommendedSearchQuery}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.xxs))
                SecondaryButton(
                    text = "找练习题",
                    onClick = {
                        val link = PracticeSearchEngine.primaryLink(session.courseTitle, need.title)
                        if (!PracticeSearchLauncher.open(context, link)) {
                            viewModel.toast("未找到浏览器，可手动搜索：${need.recommendedSearchQuery}")
                        }
                    },
                )
                Spacer(Modifier.height(Dimens.s))
            }
        }
    }

    PrimaryButton(text = "完成练习", onClick = { viewModel.exitPractice() }, modifier = Modifier.fillMaxWidth())
}
