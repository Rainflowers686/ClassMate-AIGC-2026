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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
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
import com.classmate.core.provider.AnalysisIntensity

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

    // Keep the screen awake while analysis runs (深度 mode can take 1–3 min) so the phone does not
    // sleep mid-analysis; restore on completion / leaving the screen. No extra permission needed.
    val view = LocalView.current
    DisposableEffect(ui.analysisStatus) {
        view.keepScreenOn = ui.analysisStatus == AnalysisStatus.RUNNING
        onDispose { view.keepScreenOn = false }
    }

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
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${ui.analysisIntensity.displayName} · ${ui.analysisEstimateText.ifBlank { ui.analysisIntensity.expectedHintZh }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Dimens.s))
                    // Honest "working" heartbeat: we cannot know the model's true progress, so we never
                    // fake a determinate percent (no frozen-then-jump bar). The stage list below is the
                    // real progress signal.
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp))
                    Spacer(Modifier.height(Dimens.s))
                    Text(
                        "已用时 ${ui.analysisElapsedMs / 1000} 秒",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (ui.analysisSlowNotice) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text(
                            "正在进行云端深度分析，复杂资料可能需要 1～3 分钟；可继续等待，或返回改用「快速」或本地基础整理。证据已保存，深度分析失败不影响。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
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

            // While still waiting AND past the slow threshold: let the user stop waiting on the cloud.
            if (!failed && ui.analysisSlowNotice) {
                SecondaryButton(
                    text = "立即改用本地基础整理（不调用 AI）",
                    onClick = { viewModel.switchToLocalRuleNow() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = "改用快速强度重试",
                    onClick = { viewModel.retryFast() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = "返回手动整理资料",
                    onClick = { viewModel.goBack() },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                val cloudCode = report?.cloudStatus.orEmpty()
                val readAdvice = if (cloudCode.contains("NETWORK:READ") || cloudCode.contains("SOCKET_TIMEOUT")) {
                    "云端读取响应失败，可能是网络波动或响应过慢（已自动重试仍未成功）。可重试、改用「快速」强度，或先用本地基础整理；证据已保存。"
                } else {
                    "可重试云端蓝心，或返回手动整理资料；端侧蓝心相关问题可在设置 · 能力中心检查模型路径与诊断。"
                }
                ErrorBreakdownCard(
                    rows = rows,
                    advice = readAdvice,
                )

                // Stage 8D-2 raw safe lines are kept available but COLLAPSED — no log wall by default.
                ui.onDeviceAnalysisDiagnostic?.let { diag -> DiagnosticDetailsCard(lines = diag.safeLines()) }

                val permissionBlocked = ui.analysisSourceReport?.onDeviceReason == "PERMISSION_MISSING"
                PremiumCard {
                    Text("下一步", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (permissionBlocked) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "端侧模型缺少目录访问权限（PERMISSION_MISSING）。可在设置 · 能力中心授权并重新检测，或直接用云端 / 本地基础整理继续。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(Dimens.s))
                    Text("分析强度", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(Dimens.xs))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                        AnalysisIntensity.entries.forEach { level ->
                            SecondaryButton(
                                text = if (ui.analysisIntensity == level) "✓ ${level.displayName}" else level.displayName,
                                onClick = { viewModel.setAnalysisIntensity(level) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                PrimaryButton(
                    text = "重试云端蓝心",
                    onClick = { viewModel.retryAnalysis() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = "使用本地基础整理（不调用 AI）",
                    onClick = { viewModel.generateWithLocalRuleAnalysis() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = if (permissionBlocked) "去授权端侧模型目录" else "前往设置 · 能力中心（重新检测端侧）",
                    onClick = { viewModel.goToOnDeviceSettings() },
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
