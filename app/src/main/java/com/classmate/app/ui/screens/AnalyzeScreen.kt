package com.classmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.GlassCard
import com.classmate.app.ui.designsystem.HeroCard
import com.classmate.app.ui.designsystem.HeroMetric
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.SectionHeader
import com.classmate.app.ui.designsystem.StatusTone
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.core.validation.ValidationIssueKind

/**
 * Project's "face page". The HeroCard's four metrics — provider, fallback,
 * structure validity, strict / lenient evidence match — are the single most
 * important visual on the entire app and should always lead.
 */
@Composable
fun AnalyzeScreen(
    state: ClassMateUiState,
    onRunSegment: () -> Unit,
    onRunAnalyze: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current

    val metrics = remember(
        state.fallbackUsed,
        state.structureValid,
        state.strictMatchRate,
        state.lenientMatchRate
    ) {
        buildMetrics(state)
    }

    AppScaffold(
        title = "分段与分析",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "继续到时间轴",
                        onClick = onNext,
                        enabled = state.analysisResult != null
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
            HeroCard(
                eyebrow = "Provider · 校验 · 证据",
                title = state.activeProvider,
                subtitle = "config 请求: ${state.requestedProvider}  ·  分段 ${state.segments.size}  ·  热词 ${state.hotwords.size}",
                metrics = metrics,
                footnote = state.configHint.ifBlank { null },
                cta = {
                    PrimaryButton(
                        text = "调用 ${state.requestedProvider} 分析",
                        onClick = onRunAnalyze,
                        enabled = !state.isLoading && (state.segments.isNotEmpty() || state.courseText.isNotBlank()),
                        loading = state.isLoading
                    )
                }
            )

            state.lastProviderError?.let {
                Text(
                    "Provider note: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.statusError
                )
            }
            state.errorMessage?.let {
                Text(
                    "Error: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.statusError
                )
            }

            if (state.validationIssues.isNotEmpty()) {
                GlassCard {
                    Column {
                        Text(
                            "校验问题 (${state.validationIssues.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.statusError
                        )
                        Spacer(Modifier.height(spacing.xs))
                        state.validationIssues.take(5).forEach { issue ->
                            Text(
                                "• ${humanReadable(issue.kind)}  · ${issue.ownerId}  · ${issue.detail}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.fgSecondary
                            )
                        }
                        if (state.validationIssues.size > 5) {
                            Text(
                                "…还有 ${state.validationIssues.size - 5} 条未显示",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.fgMuted
                            )
                        }
                    }
                }
            }

            SectionHeader(title = "段落预览", trailing = "${state.segments.size} 段")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                items(state.segments, key = { it.segmentId }) { seg ->
                    GlassCard {
                        Column {
                            Text(
                                "${seg.segmentId}  ·  ${seg.timeRange}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.fgSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                seg.text.take(80) + if (seg.text.length > 80) "…" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.fgPrimary
                            )
                        }
                    }
                }
            }

            OutlinedActionButton(text = "重新运行分段 (Segmenter)", onClick = onRunSegment)
        }
    }
}

private fun buildMetrics(state: ClassMateUiState): List<HeroMetric> {
    val fallbackTone = if (state.fallbackUsed) StatusTone.Error else StatusTone.Success
    val structureTone = when (state.structureValid) {
        true -> StatusTone.Success
        false -> StatusTone.Error
        null -> StatusTone.Neutral
    }
    val strictTone = matchRateTone(state.strictMatchRate)
    val lenientTone = matchRateTone(state.lenientMatchRate)
    return listOf(
        HeroMetric(
            label = "兜底",
            value = if (state.fallbackUsed) "是" else "否",
            tone = fallbackTone
        ),
        HeroMetric(
            label = "校验",
            value = when (state.structureValid) {
                true -> "通过"
                false -> "有问题"
                null -> "—"
            },
            tone = structureTone
        ),
        HeroMetric(
            label = "证据 strict",
            value = percent(state.strictMatchRate),
            tone = strictTone
        ),
        HeroMetric(
            label = "证据 lenient",
            value = percent(state.lenientMatchRate),
            tone = lenientTone
        )
    )
}

private fun matchRateTone(rate: Double?): StatusTone = when {
    rate == null -> StatusTone.Neutral
    rate >= 0.999 -> StatusTone.Success
    rate >= 0.7 -> StatusTone.Warning
    else -> StatusTone.Error
}

private fun percent(rate: Double?): String =
    if (rate == null) "—" else "%.0f%%".format(rate * 100)

private fun humanReadable(kind: ValidationIssueKind): String = when (kind) {
    ValidationIssueKind.EMPTY_SEGMENTS -> "结果段落为空"
    ValidationIssueKind.KP_SOURCE_SEGMENT_NOT_IN_RESULT -> "知识点引用了不存在的结果段"
    ValidationIssueKind.KP_SOURCE_SEGMENT_NOT_IN_INPUT -> "知识点引用了不在输入中的段"
    ValidationIssueKind.KP_IMPORTANCE_OUT_OF_RANGE -> "知识点 importance 越界"
    ValidationIssueKind.KP_DIFFICULTY_OUT_OF_RANGE -> "知识点 difficulty 越界"
    ValidationIssueKind.QUIZ_SOURCE_SEGMENT_NOT_IN_RESULT -> "题目引用了不存在的结果段"
    ValidationIssueKind.QUIZ_SOURCE_SEGMENT_NOT_IN_INPUT -> "题目引用了不在输入中的段"
    ValidationIssueKind.QUIZ_RELATED_KP_MISSING -> "题目关联的知识点缺失"
    ValidationIssueKind.QUIZ_ANSWER_INDEX_OUT_OF_RANGE -> "题目正确答案越界"
    ValidationIssueKind.REVIEW_PLAN_KP_MISSING -> "复习计划引用的知识点缺失"
}
