package com.classmate.app.ui.screens.course

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
                        Text(
                            "查看全部 ›",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { viewModel.navigateTo(Screen.KNOWLEDGE) },
                        )
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
        // Each stat is a real shortcut to its surface; only tappable when it actually has content (no
        // fake clickability on empty data).
        fun tapTo(enabled: Boolean, action: () -> Unit): Modifier =
            if (enabled) Modifier.clickable { action() } else Modifier
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            StatusChip("资料 ${l3.evidenceAssets.size}", tone = ChipTone.INFO, modifier = tapTo(l3.knowledgePoints.isNotEmpty()) { viewModel.navigateTo(Screen.KNOWLEDGE) })
            StatusChip("知识点 ${l3.knowledgePoints.size}", tone = ChipTone.INFO, modifier = tapTo(l3.knowledgePoints.isNotEmpty()) { viewModel.navigateTo(Screen.KNOWLEDGE) })
            StatusChip("微测 ${l3.questions.size}", tone = ChipTone.INFO, modifier = tapTo(l3.questions.isNotEmpty()) { viewModel.navigateTo(Screen.QUIZ) })
            StatusChip("错题 ${l3.wrongBook.size}", tone = if (l3.wrongBook.isEmpty()) ChipTone.NEUTRAL else ChipTone.WARNING, modifier = tapTo(l3.wrongBook.isNotEmpty()) { viewModel.ensureReviewPlan(); viewModel.navigateTo(Screen.REVIEW) })
            StatusChip("今日复习 ${l3.reviewQueue.size}", tone = ChipTone.INFO, modifier = tapTo(l3.reviewQueue.isNotEmpty()) { viewModel.ensureReviewPlan(); viewModel.navigateTo(Screen.REVIEW) })
            StatusChip("证据 ${l3.evidence.size}", tone = ChipTone.INFO, modifier = tapTo(l3.evidence.isNotEmpty()) { l3.evidence.firstOrNull()?.let { viewModel.openEvidenceById(it.id) } })
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
        if (l3.knowledgeGraphEdges.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("知识点地图", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${l3.knowledgeGraphEdges.size} 条知识点关联", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.xs))
            SecondaryButton("查看知识结构", onClick = { viewModel.navigateTo(Screen.KNOWLEDGE) }, modifier = Modifier.fillMaxWidth())
        }
        // Secondary / experimental study assets live in a folded "更多操作" drawer so they never
        // compete with the core loop (knowledge / quiz / review / evidence / export).
        Spacer(Modifier.height(Dimens.s))
        ProductCollapse(title = "更多操作 · 听背文稿 / 实验功能") {
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
                    "生成听背文稿",
                    onClick = { viewModel.generateAudioReviewScript() },
                    modifier = Modifier.fillMaxWidth(),
                )
                l3.audioReviewAssets.lastOrNull()?.takeIf { it.script.isNotBlank() }?.let { asset ->
                    val clipboard = LocalClipboardManager.current
                    Spacer(Modifier.height(Dimens.xs))
                    Text(
                        if (asset.audioRef != null) "听背音频已生成" else "听背文稿已生成（当前设备暂未生成音频，可先用文稿复习）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Dimens.xxs))
                    Text(asset.script, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 8)
                    Spacer(Modifier.height(Dimens.xs))
                    SecondaryButton(
                        "复制听背文稿",
                        onClick = { clipboard.setText(AnnotatedString(asset.script)); viewModel.toast("听背文稿已复制，可粘贴分享。") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (l3.qualityWarnings.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("需确认内容", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.qualityWarnings.take(3).forEach { warning ->
                Text(warning.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                weak.evidenceIds.firstOrNull { viewModel.hasRetraceableEvidence(it) }?.let { evidenceId ->
                    Spacer(Modifier.height(Dimens.xs))
                    SecondaryButton("查看诊断证据", onClick = { viewModel.openEvidenceById(evidenceId) }, modifier = Modifier.fillMaxWidth())
                }
            }
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
