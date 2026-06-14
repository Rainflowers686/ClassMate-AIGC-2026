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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.classmate.app.state.AnalysisSourceReport
import com.classmate.app.state.AnalysisStatus
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ClassMatePageScaffold
import com.classmate.app.ui.components.DiagnosticDetailsCard
import com.classmate.app.ui.components.ErrorBreakdownCard
import com.classmate.app.ui.components.PageHero
import com.classmate.app.ui.components.PremiumCard
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.SourceBadge
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

    ClassMatePageScaffold(contextLabel = "分析", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.l),
        ) {
            Spacer(Modifier.height(Dimens.xs))
            PageHero(
                overline = if (failed) "智能分析" else "智能分析 · 进行中",
                title = if (failed) "分析未完成" else "正在理解这节课",
                subtitle = ui.session?.title?.ifBlank { "未命名课程" } ?: "课程",
                trailing = { SourceBadge(ui.analysisSourceReport?.finalSource ?: "云端蓝心") },
            )
            if (!failed) {
                PremiumCard {
                    Text("蓝心大模型正在处理课堂内容", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.s))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                }
            }

            PremiumCard {
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
                // Structured source breakdown — cloud / on-device / final source — never a raw log dump.
                val report = ui.analysisSourceReport
                val rows = if (report != null) {
                    listOf(
                        "云端蓝心" to report.cloudStatus,
                        "端侧蓝心" to if (report.onDeviceAttempted) "已尝试" else "未尝试",
                        "端侧结果" to AnalysisSourceReport.onDeviceReasonZh(report.onDeviceReason),
                        "最终结果" to report.finalSource,
                    )
                } else {
                    listOf("最终结果" to "安全占位")
                }
                ErrorBreakdownCard(
                    rows = rows,
                    advice = "可重试云端蓝心，或返回手动整理资料；端侧蓝心相关问题可在设置 · 能力中心检查模型路径与诊断。",
                )

                // Stage 8D-2 raw safe lines are kept available but COLLAPSED — no log wall by default.
                ui.onDeviceAnalysisDiagnostic?.let { diag -> DiagnosticDetailsCard(lines = diag.safeLines()) }

                PrimaryButton(
                    text = "重试分析",
                    onClick = { viewModel.retryAnalysis() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = "返回手动整理资料",
                    onClick = { viewModel.goBack() },
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
