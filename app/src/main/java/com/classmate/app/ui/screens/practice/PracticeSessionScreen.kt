package com.classmate.app.ui.screens.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classmate.app.l3.PracticeAnswerState
import com.classmate.app.l3.PracticeQuestionMode
import com.classmate.app.l3.PracticeQuestionType
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
import com.classmate.core.practice.PracticeOption
import com.classmate.core.practice.PracticeSearchEngine
import com.classmate.core.practice.PracticeSession
import com.classmate.core.practice.displayZh

@Composable
fun PracticeSessionScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val session = ui.practiceSession
    val screenTitle = when (ui.practiceQuestionMode) {
        PracticeQuestionMode.REAL_QUIZ -> "专项练习"
        PracticeQuestionMode.SELF_ASSESSMENT -> "回忆复盘"
        PracticeQuestionMode.EXAM -> "模拟考试"
    }

    ClassMateScaffold(title = screenTitle, onBack = { viewModel.exitPractice() }) { padding ->
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
            val selected = ui.practiceSelectedAnswers[item.id].orEmpty()
            val submitted = ui.practiceSubmittedAnswers[item.id]
            val answerState = when {
                submitted?.state != null -> submitted.state
                selected.isNotEmpty() -> PracticeAnswerState.ANSWER_SELECTED
                ui.practiceRevealed -> PracticeAnswerState.REVEALED
                else -> PracticeAnswerState.NOT_ANSWERED
            }

            QuietCard {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    StatusChip(if (ui.practiceQuestionMode == PracticeQuestionMode.SELF_ASSESSMENT) "自评复习" else session.mode.displayZh(), tone = ChipTone.PRIMARY)
                    StatusChip("第 ${ui.practiceIndex + 1} / ${session.items.size} 题", tone = ChipTone.NEUTRAL)
                    StatusChip(questionTypeFor(item.options).displayZh(), tone = ChipTone.INFO)
                    if (ui.practiceQuestionMode == PracticeQuestionMode.EXAM) StatusChip("考试模式", tone = ChipTone.WARNING)
                    if (item.needsRecheck) StatusChip("需复核", tone = ChipTone.WARNING)
                }
                Spacer(Modifier.height(Dimens.s))
                Text(item.knowledgePointTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.xs))
                Text(item.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                if (ui.practiceQuestionMode != PracticeQuestionMode.SELF_ASSESSMENT && item.options.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.s))
                    item.options.forEach { opt ->
                        PracticeOptionRow(
                            option = opt,
                            selected = opt.id in selected,
                            submitted = submitted != null,
                            onClick = { viewModel.selectPracticeAnswer(opt.id) },
                        )
                        Spacer(Modifier.height(Dimens.xxs))
                    }

                    Spacer(Modifier.height(Dimens.s))
                    if (submitted == null) {
                        PrimaryButton(
                            text = "提交答案",
                            onClick = { viewModel.submitPracticeAnswer() },
                            enabled = answerState == PracticeAnswerState.ANSWER_SELECTED,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        PracticeAnswerReview(viewModel = viewModel, item = item, selectedAnswers = submitted.selectedAnswers, correct = submitted.correct)
                        Spacer(Modifier.height(Dimens.xs))
                        SecondaryButton(
                            text = "View evidence",
                            onClick = { viewModel.openEvidenceForQuestion(item.quizId ?: item.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    if (ui.practiceRevealed) {
                        Spacer(Modifier.height(Dimens.s))
                        Text("答案 / 解释：${item.answer.ifBlank { "暂无解析" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        item.evidenceQuote?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(Dimens.xxs))
                            Text("证据：「$it」", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SelfAssessmentCard(viewModel = viewModel, itemRevealed = ui.practiceRevealed, itemType = item.type)
                }
            }

            if (ui.practiceQuestionMode != PracticeQuestionMode.SELF_ASSESSMENT) {
                val label = if (ui.practiceIndex == session.items.lastIndex) {
                    if (ui.practiceQuestionMode == PracticeQuestionMode.EXAM) "提交考试" else "完成练习"
                } else {
                    "下一题"
                }
                PrimaryButton(
                    text = label,
                    onClick = { viewModel.nextPracticeQuestion() },
                    enabled = submitted != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text("选择后自动进入下一题。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PracticeOptionRow(
    option: PracticeOption,
    selected: Boolean,
    submitted: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val border = when {
        submitted && option.correct -> BorderStroke(1.dp, cs.primary)
        selected -> BorderStroke(1.dp, cs.primary)
        else -> BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f))
    }
    val container = when {
        submitted && option.correct -> cs.primaryContainer.copy(alpha = 0.55f)
        selected -> cs.primary.copy(alpha = 0.08f)
        else -> cs.surface
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !submitted) { onClick() },
    ) {
        Row(
            Modifier.padding(horizontal = Dimens.m, vertical = Dimens.s),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            Text(option.id, style = MaterialTheme.typography.labelLarge, color = cs.primary, fontWeight = FontWeight.SemiBold)
            Text(
                option.text,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PracticeAnswerReview(viewModel: AppViewModel, item: com.classmate.core.practice.PracticeItem, selectedAnswers: List<String>, correct: Boolean) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (correct) cs.secondaryContainer.copy(alpha = 0.55f) else cs.errorContainer.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Dimens.m), verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            Text(if (correct) "回答正确" else "回答错误", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("你的答案：${selectedAnswers.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Text("正确答案：${item.correctOptionIds.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Text("解析：${item.answer.ifBlank { "暂无解析" }}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
            Text("来源证据：${item.evidenceQuote?.takeIf { it.isNotBlank() } ?: "暂无证据"}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelfAssessmentCard(viewModel: AppViewModel, itemRevealed: Boolean, itemType: PracticeItemType) {
    if (!itemRevealed) {
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = if (itemType == PracticeItemType.QUIZ_RETRY) "查看答案" else "查看答案 / 证据",
            onClick = { viewModel.revealPracticeAnswer() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(Dimens.s))
    Text("掌握度自评", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
        SecondaryButton("我答对了", onClick = { viewModel.answerPractice(PracticeOutcome.CORRECT) })
        SecondaryButton("我答错了", onClick = { viewModel.answerPractice(PracticeOutcome.WRONG) })
        SecondaryButton("已掌握", onClick = { viewModel.answerPractice(PracticeOutcome.MASTERED) })
        SecondaryButton("需要多练", onClick = { viewModel.answerPractice(PracticeOutcome.NEED_MORE_PRACTICE) })
    }
}

private fun questionTypeFor(options: List<PracticeOption>): PracticeQuestionType = when {
    options.isEmpty() -> PracticeQuestionType.SHORT_ANSWER
    options.count { it.correct } > 1 -> PracticeQuestionType.MULTI_CHOICE
    options.size == 2 && options.map { it.text }.any { it.contains("正确") || it.contains("错误") || it.contains("True", ignoreCase = true) || it.contains("False", ignoreCase = true) } -> PracticeQuestionType.TRUE_FALSE
    else -> PracticeQuestionType.SINGLE_CHOICE
}

private fun PracticeQuestionType.displayZh(): String = when (this) {
    PracticeQuestionType.SINGLE_CHOICE -> "单选题"
    PracticeQuestionType.TRUE_FALSE -> "判断题"
    PracticeQuestionType.MULTI_CHOICE -> "多选题"
    PracticeQuestionType.SHORT_ANSWER -> "简答 / 自评"
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
