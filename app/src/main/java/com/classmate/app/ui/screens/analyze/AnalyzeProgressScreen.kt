package com.classmate.app.ui.screens.analyze

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AnalysisStatus
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.design.Dimens

private val STAGES = listOf(
    "读取课堂内容" to "切分段落，为每段建立可引用的位置",
    "蓝心大模型理解" to "理解课程并提炼真正的概念（BlueLM 优先）",
    "知识点抽取" to "合并重复表述，保留有学习价值的知识点",
    "证据校验" to "确认每个结论都能在原文中找到证据",
    "微测生成" to "生成考查理解 / 应用 / 错因的微测题",
    "复习计划生成" to "为后续复习准备依据，答题后据此优化",
)

@Composable
fun AnalyzeProgressScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val failed = ui.analysisStatus == AnalysisStatus.FAILED
    val progress = (ui.analysisStageIndex.coerceIn(0, STAGES.size)).toFloat() / STAGES.size

    ClassMateScaffold(title = "理解这节课", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            ClassMateCard {
                Text(
                    if (failed) "分析未完成" else "正在理解这节课……",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Dimens.xs))
                Text(
                    if (failed) {
                        "本次没有得到可用结果。通常本地兜底会接管，确保流程不中断——你可以重试。"
                    } else {
                        ui.session?.title?.ifBlank { "未命名课程" } ?: "课程"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!failed) {
                    Spacer(Modifier.height(Dimens.l))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                }
            }

            ClassMateCard {
                STAGES.forEachIndexed { index, (title, desc) ->
                    val stageNumber = index + 1
                    val state = when {
                        failed -> StageState.PENDING
                        stageNumber < ui.analysisStageIndex -> StageState.DONE
                        stageNumber == ui.analysisStageIndex -> StageState.ACTIVE
                        else -> StageState.PENDING
                    }
                    StageRow(title, desc, state)
                    if (index != STAGES.lastIndex) Spacer(Modifier.height(Dimens.m))
                }
            }

            if (failed) {
                PrimaryButton(
                    text = "重试分析",
                    onClick = { viewModel.retryAnalysis() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private enum class StageState { DONE, ACTIVE, PENDING }

@Composable
private fun StageRow(title: String, desc: String, state: StageState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when (state) {
                StageState.DONE -> Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                StageState.ACTIVE -> CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                )
                StageState.PENDING -> Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) { Box(Modifier.size(28.dp)) {} }
            }
        }
        Spacer(Modifier.width(Dimens.m))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (state == StageState.PENDING) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
