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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.ClassMateUiState
import com.classmate.app.ui.designsystem.AppScaffold
import com.classmate.app.ui.designsystem.BottomActionRow
import com.classmate.app.ui.designsystem.HeroCard
import com.classmate.app.ui.designsystem.HeroMetric
import com.classmate.app.ui.designsystem.InlineCalloutLine
import com.classmate.app.ui.designsystem.OutlinedActionButton
import com.classmate.app.ui.designsystem.PrimaryButton
import com.classmate.app.ui.designsystem.SectionHeader
import com.classmate.app.ui.designsystem.StatusTone
import com.classmate.app.ui.designsystem.TinyCaption
import com.classmate.app.ui.theme.LocalClassMateColors
import com.classmate.app.ui.theme.LocalClassMateShapes
import com.classmate.app.ui.theme.LocalClassMateSpacing
import com.classmate.core.validation.ValidationIssueKind

/**
 * Project's "face page". The HeroCard is the analysis status card; the body
 * also surfaces a success callout (with KP/quiz counts), a light segment
 * preview, and any validation issues.
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
    val shapes = LocalClassMateShapes.current

    val metrics = remember(
        state.fallbackUsed,
        state.structureValid,
        state.strictMatchRate,
        state.lenientMatchRate
    ) { buildMetrics(state) }

    val providerLine = providerDisplayLine(state)
    val analysisDone = state.analysisResult != null
    val kpCount = state.knowledgePoints.size
    val quizCount = state.quizzes.size

    AppScaffold(
        title = "分析与证据",
        onBack = onBack,
        bottomBar = {
            BottomActionRow(
                primary = {
                    PrimaryButton(
                        text = "进入知识时间轴",
                        onClick = onNext,
                        enabled = analysisDone
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
                eyebrow = "本次分析",
                title = providerLine,
                subtitle = "${state.segments.size} 段课程  ·  ${state.hotwords.size} 个热词",
                metrics = metrics,
                footnote = null,
                cta = {
                    PrimaryButton(
                        text = if (analysisDone) "重新分析当前课程" else "开始分析",
                        onClick = onRunAnalyze,
                        enabled = !state.isLoading &&
                            (state.segments.isNotEmpty() || state.courseText.isNotBlank()),
                        loading = state.isLoading
                    )
                }
            )

            if (analysisDone) {
                InlineCalloutLine(
                    tone = if (state.fallbackUsed) StatusTone.Warning else StatusTone.Success,
                    text = if (state.fallbackUsed)
                        "已生成 $kpCount 个知识点与 $quizCount 道微测题（云端不可用，已自动降级到本地证据引擎）"
                    else
                        "分析完成，已生成 $kpCount 个知识点与 $quizCount 道微测题，可进入知识时间轴查看"
                )
            }

            state.errorMessage?.let {
                InlineCalloutLine(tone = StatusTone.Error, text = it)
            }

            if (state.validationIssues.isNotEmpty()) {
                ValidationIssuesPanel(state)
            }

            SectionHeader(title = "课程分段", trailing = "${state.segments.size} 段")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                items(state.segments, key = { it.segmentId }) { seg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface, shapes.medium)
                            .border(1.dp, colors.outline, shapes.medium)
                            .padding(horizontal = spacing.md, vertical = spacing.sm),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            seg.timeRange,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.fgMuted,
                            modifier = Modifier.width(60.dp)
                        )
                        Spacer(Modifier.width(spacing.sm))
                        Text(
                            seg.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.fgPrimary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            TinyCaption("分段由本地规则在客户端完成。如需重新分段，可点击下方按钮。")
            OutlinedActionButton(text = "重新分段", onClick = onRunSegment)
            Spacer(Modifier.height(spacing.xs))
        }
    }
}

@Composable
private fun ValidationIssuesPanel(state: ClassMateUiState) {
    val colors = LocalClassMateColors.current
    val spacing = LocalClassMateSpacing.current
    val shapes = LocalClassMateShapes.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.statusError.copy(alpha = 0.06f), shapes.medium)
            .border(1.dp, colors.statusError.copy(alpha = 0.20f), shapes.medium)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
    ) {
        Text(
            "校验提示 · ${state.validationIssues.size} 项",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.statusError
        )
        Spacer(Modifier.height(spacing.xs))
        state.validationIssues.take(5).forEach { issue ->
            Text(
                "· " + issueHumanLine(issue.kind, issue.ownerId),
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgSecondary
            )
        }
        if (state.validationIssues.size > 5) {
            Text(
                "…另有 ${state.validationIssues.size - 5} 项",
                style = MaterialTheme.typography.labelSmall,
                color = colors.fgMuted
            )
        }
    }
}

private fun buildMetrics(state: ClassMateUiState): List<HeroMetric> {
    val fallbackTone = if (state.fallbackUsed) StatusTone.Warning else StatusTone.Success
    val structureTone = when (state.structureValid) {
        true -> StatusTone.Success
        false -> StatusTone.Error
        null -> StatusTone.Neutral
    }
    val strictTone = matchRateTone(state.strictMatchRate)
    val lenientTone = matchRateTone(state.lenientMatchRate)
    return listOf(
        HeroMetric(
            label = "数据来源",
            value = if (state.fallbackUsed) "本地降级" else "正常",
            tone = fallbackTone
        ),
        HeroMetric(
            label = "结构校验",
            value = when (state.structureValid) {
                true -> "通过"
                false -> "有问题"
                null -> "未运行"
            },
            tone = structureTone
        ),
        HeroMetric(
            label = "原文证据命中",
            value = percent(state.strictMatchRate),
            tone = strictTone
        ),
        HeroMetric(
            label = "兜底证据命中",
            value = percent(state.lenientMatchRate),
            tone = lenientTone
        )
    )
}

private fun providerDisplayLine(state: ClassMateUiState): String = when (state.activeProvider) {
    "local" -> "本地证据引擎"
    "compatible" -> "云端大模型"
    "bluelm" -> "蓝心大模型"
    else -> state.activeProvider
}

private fun matchRateTone(rate: Double?): StatusTone = when {
    rate == null -> StatusTone.Neutral
    rate >= 0.999 -> StatusTone.Success
    rate >= 0.7 -> StatusTone.Warning
    else -> StatusTone.Error
}

private fun percent(rate: Double?): String =
    if (rate == null) "—" else "%.0f%%".format(rate * 100)

private fun issueHumanLine(kind: ValidationIssueKind, owner: String): String {
    val ownerPart = if (owner.isBlank()) "" else "（$owner）"
    return when (kind) {
        ValidationIssueKind.EMPTY_SEGMENTS -> "结果段落为空"
        ValidationIssueKind.KP_SOURCE_SEGMENT_NOT_IN_RESULT -> "知识点找不到对应段落$ownerPart"
        ValidationIssueKind.KP_SOURCE_SEGMENT_NOT_IN_INPUT -> "知识点引用了不在课程中的段落$ownerPart"
        ValidationIssueKind.KP_IMPORTANCE_OUT_OF_RANGE -> "知识点的重要度数值越界$ownerPart"
        ValidationIssueKind.KP_DIFFICULTY_OUT_OF_RANGE -> "知识点的难度数值越界$ownerPart"
        ValidationIssueKind.QUIZ_SOURCE_SEGMENT_NOT_IN_RESULT -> "题目找不到对应段落$ownerPart"
        ValidationIssueKind.QUIZ_SOURCE_SEGMENT_NOT_IN_INPUT -> "题目引用了不在课程中的段落$ownerPart"
        ValidationIssueKind.QUIZ_RELATED_KP_MISSING -> "题目关联的知识点不存在$ownerPart"
        ValidationIssueKind.QUIZ_ANSWER_INDEX_OUT_OF_RANGE -> "题目的正确答案越界$ownerPart"
        ValidationIssueKind.REVIEW_PLAN_KP_MISSING -> "复习计划引用的知识点不存在$ownerPart"
    }
}
