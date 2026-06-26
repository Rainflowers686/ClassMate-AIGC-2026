package com.classmate.app.ui.screens.course

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.data.HistoryRecord
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.state.Tab
import com.classmate.app.ui.components.ChipTone
import com.classmate.app.ui.components.DockAction
import com.classmate.app.ui.components.ExportCenterCard
import com.classmate.app.ui.components.KnowledgePathNode
import com.classmate.app.ui.components.LearningActionDock
import com.classmate.app.ui.components.Pill
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.SourceBadge
import com.classmate.app.ui.components.StatusChip
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductCollapse
import com.classmate.app.ui.product.ProductHero
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSectionTitle
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.product.QuietCard
import com.classmate.app.ui.design.Dimens
import com.classmate.core.library.CourseLibraryBuilder
import com.classmate.core.material.MaterialSourceType
import com.classmate.core.ondevice.ProviderPathNode
import com.classmate.core.practice.PracticeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CourseDetailScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val key = ui.selectedCourseKey ?: ui.session?.title?.let { CourseLibraryBuilder.normalizeCourseName(it).lowercase() }
    val summary = viewModel.courseSummaries().firstOrNull { it.courseKey == key }
    val records = key?.let { viewModel.recordsForCourse(it) }.orEmpty()
    val session = ui.session
    val result = ui.result
    val courseName = summary?.courseName ?: session?.title ?: "未命名课程"

    val sourceLabel = ui.analysisSourceReport?.finalSource
        ?: result?.provenance?.modelLabel?.takeIf { it.isNotBlank() }
        ?: summary?.recentProvider
        ?: "暂无"
    val kpCount = summary?.knowledgePointTotal ?: result?.knowledgePoints?.size ?: 0
    val quizCount = summary?.quizTotal ?: result?.quizQuestions?.size ?: 0
    var showDeleteConfirm by remember(key, session?.id) { mutableStateOf(false) }

    ProductScaffold(
        contextLabel = "课程",
        onBack = { if (!viewModel.goBack()) viewModel.selectTab(Tab.HISTORY) },
        actions = {
            if (key != null || session != null) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除课程")
                }
            }
        },
    ) { padding ->
        ProductCanvas {
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screen)
                    .padding(bottom = Dimens.xxxl),
                verticalArrangement = Arrangement.spacedBy(Dimens.l),
            ) {
                Spacer(Modifier.height(Dimens.s))
                // HEADER — title, source, lightweight stats; the original/diagnostics never lead.
                ProductHero(
                    overline = "学习空间",
                    title = courseName,
                    subtitle = "$kpCount 个知识点 · $quizCount 道微测",
                    trailing = { SourceBadge("来源 · $sourceLabel") },
                )
                if (sourceLabel.contains("端侧")) {
                    Text(
                        "端侧蓝心生成；如来自图片 / 拍照学习输入，为端侧多模态草稿，用户已确认。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                L3PipelineStatusCard(viewModel)

                // FIRST-SCREEN CORE — a visual learning map (connected nodes), not a list.
                if (result != null && result.knowledgePoints.isNotEmpty()) {
                    ProductSectionTitle("知识结构大纲", trailing = {
                        Text("查看全部 ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    })
                    val nodes = result.knowledgePoints.take(5)
                    nodes.forEachIndexed { i, kp ->
                        KnowledgePathNode(
                            index = i + 1,
                            title = kp.title,
                            summary = kp.summary,
                            evidence = kp.evidence.firstOrNull()?.quote,
                            isLast = i == nodes.lastIndex,
                            // Tapping a map node opens that point's evidence (原文), not the ask-this-lesson page.
                            onClick = { viewModel.openEvidence(kp.id) },
                        )
                    }
                } else {
                    QuietCard(onClick = { viewModel.navigateTo(Screen.KNOWLEDGE) }) {
                        Text("知识时间线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("打开知识时间线查看知识点与证据链。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // SECOND LAYER — learning actions as a connected dock, not a button pile.
                ProductSectionTitle("今日建议")
                LearningActionDock(
                    listOf(
                        DockAction("问这节课", Icons.Filled.Edit) { viewModel.navigateTo(Screen.KNOWLEDGE) },
                        DockAction("做微测", Icons.Filled.CheckCircle) { viewModel.navigateTo(Screen.QUIZ) },
                        DockAction("复习计划", Icons.Filled.DateRange) { viewModel.ensureReviewPlan(); viewModel.navigateTo(Screen.REVIEW) },
                        DockAction("导出笔记", Icons.Filled.Share) { viewModel.navigateTo(Screen.KNOWLEDGE) },
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    SecondaryButton("专项练习", onClick = { viewModel.startPractice(PracticeMode.QUICK_REVIEW) }, modifier = Modifier.weight(1f))
                    SecondaryButton("错题重练", onClick = { viewModel.startPractice(PracticeMode.WRONG_ANSWER_RETRY) }, modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    SecondaryButton("随机小测", onClick = { viewModel.startRandomQuiz() }, modifier = Modifier.weight(1f))
                    SecondaryButton("模拟考试", onClick = { viewModel.startExam() }, modifier = Modifier.weight(1f))
                }
                // Restrained entry into the immersive Flow companion (sound scenes + focus timer).
                SecondaryButton("心流复习 · 沉浸专注", onClick = { viewModel.navigateTo(Screen.LIVE) }, modifier = Modifier.fillMaxWidth())

                // On-device study advice — kept, restyled.
                if (result != null) {
                    ProductSectionTitle("端侧学习建议")
                    QuietCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("由端侧蓝心整理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            SourceBadge("端侧蓝心")
                        }
                        ui.onDeviceReportSuggestion?.let { suggestion ->
                            Spacer(Modifier.height(Dimens.s))
                            Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(Dimens.m))
                        SecondaryButton(
                            text = if (ui.onDeviceReportSuggestionRunning) "生成中" else "端侧建议",
                            onClick = { viewModel.generateOnDeviceReportSuggestion() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                ProductSectionTitle("导出")
                ExportCenterCard(
                    viewModel = viewModel,
                    title = "导出中心",
                    description = "导出学习包、知识结构大纲、PDF、Word 兼容 HTML 或演示 HTML；导出不含密钥与内部状态。",
                    buildArtifact = viewModel::buildLearningStudyPackArtifact,
                )

                // THIRD LAYER — original material + records, FOLDED so they never outshine the map.
                val materialSummary = ui.lastMaterialSummary
                if (materialSummary != null) {
                    ProductCollapse(title = "资料来源") {
                        val chips = (materialSummary.sourceTypes.map { sourceTypeZh(it) } + materialSummary.transcriptLabels).distinct()
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                            chips.forEach { com.classmate.app.ui.components.StatusChip(it, tone = ChipTone.INFO) }
                        }
                        Spacer(Modifier.height(Dimens.s))
                        Text(materialSummary.exportLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (records.isNotEmpty()) {
                    ProductCollapse(title = "课堂记录（${records.size}）") {
                        records.forEach { record ->
                            Spacer(Modifier.height(Dimens.s))
                            LessonRecordCard(record, onOpen = { viewModel.openHistoryTimeline(record) })
                        }
                    }
                }
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除课程？") },
            text = {
                Text("将删除「$courseName」相关的知识点、题目、错题、复习任务、导出草稿或本地记录。删除后首页、历史记录和复习列表将不再显示该课程。")
            },
            confirmButton = {
                TextButton(onClick = {
                    val deleted = key?.let { viewModel.deleteCourse(it) } ?: viewModel.deleteCurrentCourse()
                    if (!deleted) viewModel.toast("删除失败，请稍后重试。")
                    showDeleteConfirm = false
                }) { Text("删除课程") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun L3PipelineStatusCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val l3 = ui.l3Pipeline
    if (l3.lessonSource == null) return
    QuietCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("L3 学习闭环", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${l3.evidence.size} 条证据 · ${l3.questions.size} 道微测 · ${l3.reviewQueue.size} 个复习项",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(if (l3.wrongBook.isEmpty()) "错题 0" else "错题 ${l3.wrongBook.size}", tone = if (l3.wrongBook.isEmpty()) ChipTone.NEUTRAL else ChipTone.WARNING)
        }
        if (l3.summary.isNotBlank()) {
            Spacer(Modifier.height(Dimens.s))
            Text(l3.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(Dimens.s))
        Text("学习状态总览", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            StatusChip("资料 ${l3.evidenceAssets.size}", tone = ChipTone.INFO)
            StatusChip("知识点 ${l3.knowledgePoints.size}", tone = ChipTone.INFO)
            StatusChip("微测 ${l3.questions.size}", tone = ChipTone.INFO)
            StatusChip("错题 ${l3.wrongBook.size}", tone = if (l3.wrongBook.isEmpty()) ChipTone.NEUTRAL else ChipTone.WARNING)
            StatusChip("今日复习 ${l3.reviewQueue.size}", tone = ChipTone.INFO)
            StatusChip("证据 ${l3.evidence.size}", tone = ChipTone.INFO)
            if (l3.qualityWarnings.isNotEmpty()) StatusChip("需确认 ${l3.qualityWarnings.size}", tone = ChipTone.WARNING)
        }
        if (l3.wrongBook.isNotEmpty() || l3.reviewQueue.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            SecondaryButton(
                "继续复习",
                onClick = { viewModel.ensureReviewPlan(); viewModel.navigateTo(Screen.REVIEW) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Dimens.xs))
        SecondaryButton(
            "生成学习包",
            onClick = { viewModel.prepareRefinedExportDraft() },
            modifier = Modifier.fillMaxWidth(),
        )
        if (l3.evidenceAssets.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                l3.evidenceAssets.groupBy { it.type }.forEach { (type, assets) ->
                    StatusChip("${type.name} ${assets.size}", tone = ChipTone.INFO)
                }
            }
            l3.evidence.firstOrNull()?.let { evidence ->
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    "查看首条证据",
                    onClick = { viewModel.openEvidenceById(evidence.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        val providerSteps = l3.stepLogs.filter { it.step in listOf("OCR", "QUERY_REWRITE", "EMBEDDING", "TEXT_SIMILARITY") }
        if (providerSteps.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                providerSteps.forEach { step ->
                    StatusChip("${step.step}: ${step.status}", tone = ChipTone.INFO)
                }
            }
        }
        if (l3.knowledgeGraphEdges.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("知识点地图", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.knowledgeGraphEdges.take(3).forEach { edge ->
                val from = l3.knowledgePoints.firstOrNull { it.id == edge.fromKnowledgePointId }?.title.orEmpty()
                val to = l3.knowledgePoints.firstOrNull { it.id == edge.toKnowledgePointId }?.title.orEmpty()
                Text("$from → $to · ${edge.relation.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (l3.transcriptSegments.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("Transcript timeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.transcriptSegments.take(4).forEach { segment ->
                val start = segment.startMs?.let { "${it / 1000}s" } ?: "--"
                val end = segment.endMs?.let { "${it / 1000}s" } ?: "--"
                Text(
                    "$start-$end · ${segment.sourceType.name}${if (segment.fallbackGenerated) " · fallback" else ""} · ${segment.text.take(42)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (l3.diagnostics.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("L3 能力诊断", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                l3.diagnostics.take(12).forEach { status ->
                    StatusChip("${status.capability}: ${status.status}", tone = ChipTone.NEUTRAL)
                }
            }
        }
        if (l3.officialCapabilityContributions.isNotEmpty() || l3.capabilityPlans.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("学习闭环能力贡献", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.capabilityPlans.lastOrNull()?.userVisibleSummary.orEmpty().take(4).forEach { summary ->
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                l3.officialCapabilityContributions.take(8).forEach { contribution ->
                    StatusChip("${contribution.capabilityId.name}: ${contribution.runtimeAvailability.name}", tone = ChipTone.INFO)
                }
            }
        }
        Spacer(Modifier.height(Dimens.s))
        Text("学习增强入口", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
            if (ui.enableExperimentalImageGeneration) {
                SecondaryButton(
                    "生成学习图解提示词",
                    onClick = { viewModel.generateVisualStudyPrompt() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (ui.enableExperimentalVideoGeneration) {
                SecondaryButton(
                    "生成复习短视频分镜",
                    onClick = { viewModel.generateReviewVideoStoryboard() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (ui.enableExperimentalSimultaneousInterpretation) {
                SecondaryButton(
                    "生成双语转写草稿",
                    onClick = { viewModel.generateBilingualTranscriptDraft() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SecondaryButton(
                "生成听背复习脚本",
                onClick = { viewModel.generateAudioReviewScript() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        l3.visualStudyAssets.lastOrNull()?.let { asset ->
            Spacer(Modifier.height(Dimens.xs))
            Text("Study diagram prompt: ${asset.status.name} · ${asset.prompt.take(80)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        l3.reviewVideoPlans.lastOrNull()?.let { plan ->
            Spacer(Modifier.height(Dimens.xs))
            Text("Review video storyboard: ${plan.status.name} · scenes ${plan.scenes.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (l3.bilingualTranscriptSegments.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.xs))
            Text("Bilingual transcript draft: ${l3.bilingualTranscriptSegments.size} segments · interpretation pending/config required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        l3.audioReviewAssets.lastOrNull()?.let { asset ->
            Spacer(Modifier.height(Dimens.xs))
            Text("Audio review script: ${asset.status.name} · ${asset.script.take(80)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (l3.qualityWarnings.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("质量护栏", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.qualityWarnings.take(3).forEach { warning ->
                Text(warning.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (l3.inputArtifacts.isNotEmpty() || l3.asrJobs.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("输入状态：${l3.inputArtifacts.size} 个 artifact · ${l3.asrJobs.size} 个 ASR job", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        l3.asrQualityEvaluations.lastOrNull()?.let { eval ->
            Spacer(Modifier.height(Dimens.s))
            Text(
                "ASR quality: CER ${"%.2f".format(eval.charErrorRate)} 路 glossary ${"%.0f".format(eval.glossaryHitRate * 100)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.inputReports.isNotEmpty() || l3.pdfPages.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text(
                "Import reports: ${l3.inputReports.size} · PDF docs: ${l3.pdfDocuments.size} · PDF pages: ${l3.pdfPages.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.semanticIndexChunks.isNotEmpty() || l3.toolOrchestrationPlan != null || l3.toolStepRecords.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text(
                "Semantic index: ${l3.semanticIndexRecords.size} records · Tool steps: ${l3.toolStepRecords.size} · ${l3.toolOrchestrationPlan?.plannedTools?.joinToString(" -> ").orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.semanticSearchResults.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            val result = l3.semanticSearchResults.first()
            Text(
                "Local semantic search: ${result.status} · top hit ${result.hits.firstOrNull()?.ownerType.orEmpty()} ${"%.2f".format(result.hits.firstOrNull()?.score ?: 0.0)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.similarQuestionRecommendations.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("相似题推荐（实验）：${l3.similarQuestionRecommendations.size} 条 · ${l3.similarQuestionRecommendations.first().status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (l3.examReports.isNotEmpty()) {
            val report = l3.examReports.last()
            Spacer(Modifier.height(Dimens.s))
            Text(
                "Exam report: score ${report.score} · accuracy ${"%.0f".format(report.accuracy * 100)}% · weak ${report.weakKnowledgePointIds.size} · evidence ${report.evidenceIds.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.translationResults.isNotEmpty() || l3.ttsPlaybackStates.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text(
                "Study aids: translation ${l3.translationResults.lastOrNull()?.status?.name ?: "not requested"} · TTS ${l3.ttsPlaybackStates.lastOrNull()?.status?.name ?: "not requested"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.masteryTrendStats.recentSevenDaySummary.isNotBlank()) {
            Spacer(Modifier.height(Dimens.s))
            Text("Mastery trend: ${l3.masteryTrendStats.recentSevenDaySummary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (l3.learningDiagnosis.generatedAt > 0L) {
            Spacer(Modifier.height(Dimens.s))
            Text("学习诊断", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                l3.learningDiagnosis.recentReviewPressure.ifBlank { "暂无复习压力。" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            l3.learningDiagnosis.weakKnowledgePoints.firstOrNull()?.let { weak ->
                Spacer(Modifier.height(Dimens.xs))
                Text("优先处理：${weak.title} · ${weak.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                weak.evidenceIds.firstOrNull()?.let { evidenceId ->
                    Spacer(Modifier.height(Dimens.xs))
                    SecondaryButton("查看诊断证据", onClick = { viewModel.openEvidenceById(evidenceId) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (l3.distractorExplanations.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text(
                "Distractor explanations: ${l3.distractorExplanations.size} · ${l3.distractorExplanations.first().status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LessonRecordCard(record: HistoryRecord, onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    QuietCard(onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(record.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(formatTime(record.createdAtEpochMs), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Pill(ProviderPathNode.sourceLabelZh(record.providerName), cs.surfaceVariant, cs.onSurfaceVariant)
        }
    }
}

private fun sourceTypeZh(type: MaterialSourceType): String = when (type) {
    MaterialSourceType.TRANSCRIPT -> "课堂转写"
    MaterialSourceType.MANUAL_NOTE -> "手动笔记"
    MaterialSourceType.IMPORTED_TEXT -> "粘贴文本"
    MaterialSourceType.TXT_FILE -> "TXT 文件"
    MaterialSourceType.MARKDOWN_FILE -> "Markdown 文件"
    MaterialSourceType.AUDIO_FILE -> "音频文件"
    MaterialSourceType.VIDEO_FILE -> "视频文件"
    MaterialSourceType.SLIDE_OCR -> "课件 OCR"
    MaterialSourceType.BLACKBOARD_OCR -> "板书 OCR"
    MaterialSourceType.PDF_OCR -> "讲义/PDF OCR"
}

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))
