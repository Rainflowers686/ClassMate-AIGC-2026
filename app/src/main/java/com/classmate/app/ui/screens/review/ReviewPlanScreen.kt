package com.classmate.app.ui.screens.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.practice.PracticeSearchLauncher
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ExportCenterCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductHero
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.components.Pill
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.design.Dimens
import com.classmate.app.l3.ReviewPlanEnhancementEngine
import com.classmate.core.learning.ReviewEngine
import com.classmate.core.learning.ReviewEventType
import com.classmate.core.learning.ReviewPriorityLevel
import com.classmate.core.learning.ReviewTask
import com.classmate.core.practice.PracticeKeywordSuggestion
import com.classmate.core.practice.PracticeMode
import com.classmate.core.practice.PracticeSearchEngine
import com.classmate.core.practice.PracticeSearchLink
import com.classmate.core.video.VideoRecommendationEngine

@Composable
fun ReviewPlanScreen(viewModel: AppViewModel) {
    val snapshot = viewModel.ui.learningSnapshot
    val now = System.currentTimeMillis()
    val due = ReviewEngine.listDueTasks(snapshot, now)
    val upcoming = ReviewEngine.listUpcomingTasks(snapshot, now)
    val removed = snapshot.tasks.filter { it.manuallyRemoved }

    ProductCanvas {
      ProductScaffold(contextLabel = "复习") { padding ->
        if (snapshot.tasks.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxWidth().padding(horizontal = ProductSpace.gutter)) {
                Spacer(Modifier.height(ProductSpace.tight))
                ProductHero(overline = "学习", title = "复习计划", subtitle = "还没有复习任务。先分析一节课，ClassMate 会基于证据生成可复习的任务。")
            }
            return@ProductScaffold
        }

        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ProductSpace.gutter)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            Spacer(Modifier.height(ProductSpace.tight))
            ProductHero(overline = "学习", title = "复习计划", subtitle = "${due.size} 个任务今日到期 · 按优先级处理薄弱点")
            OverviewCard(
                dueCount = due.size,
                minutes = ReviewEngine.totalDueMinutes(snapshot, now),
                weak = ReviewEngine.weakCount(snapshot),
                needsReview = ReviewEngine.needsHumanReviewCount(snapshot),
            )
            L3LearningLoopCard(viewModel)
            LearningDiagnosisCard(viewModel)
            WeaknessCard(viewModel)
            PracticeEntryCard(viewModel)
            OnDeviceReviewSuggestionCard(viewModel)
            ExportCard(viewModel)

            if (due.isNotEmpty()) {
                Text("今日待复习", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                due.forEachIndexed { index, task -> TaskCard(task, index, due.size, viewModel) }
            } else {
                ClassMateCard {
                    Text("今天没有待复习任务。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (upcoming.isNotEmpty()) {
                Text("近期复习", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ClassMateCard {
                    upcoming.take(8).forEach { task ->
                        Text("${task.title} / ${task.courseTitle}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(Dimens.xxs))
                    }
                }
            }

            if (removed.isNotEmpty()) {
                Text("已移除", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ClassMateCard {
                    removed.forEach { task ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(task.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            ActionChip("恢复") { viewModel.reviewRestore(task.taskId) }
                        }
                        Spacer(Modifier.height(Dimens.xs))
                    }
                }
            }

            ClassMateCard {
                Text("复习计划如何生成", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Text(
                    "复习任务来自校验通过的知识点。答错、太难、需要多练、证据复核以及手动调整优先级都会更新队列，且不会削弱证据校验。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
      }
    }
}

@Composable
private fun L3LearningLoopCard(viewModel: AppViewModel) {
    val l3 = viewModel.ui.l3Pipeline
    if (l3.lessonSource == null) return
    val weak = l3.masteryStats.count { it.state.name == "WEAK" }
    val reviewing = l3.masteryStats.count { it.state.name == "REVIEWING" }
    val mastered = l3.masteryStats.count { it.state.name == "MASTERED" }
    val daily = l3.reviewDailyStats
    ClassMateCard {
        Text("L3 闭环统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.xl)) {
            Stat("${l3.wrongBook.size}", "错题")
            Stat("${l3.reviewQueue.size}", "复习项")
            Stat("$weak", "薄弱")
            Stat("$reviewing", "复习中")
            Stat("$mastered", "掌握")
        }
        if (daily.totalKnowledgePoints > 0) {
            Spacer(Modifier.height(Dimens.s))
            Text(
                "每日复习卡：今日 ${daily.dueToday} · 逾期 ${daily.overdueCount} · 薄弱 ${daily.weakCount} · 已掌握 ${daily.masteredCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.masteryTrendStats.recentSevenDaySummary.isNotBlank()) {
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "趋势：${l3.masteryTrendStats.recentSevenDaySummary} · streak ${l3.masteryTrendStats.reviewCompletionStreak}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.semanticSearchResults.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "语义检索：${l3.semanticSearchResults.first().status} · ${l3.semanticSearchResults.first().hits.size} 条相关证据/题目",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (l3.reviewQueue.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("20 分钟复习计划", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.reviewQueue.take(5).forEach { item ->
                val kp = l3.knowledgePoints.firstOrNull { it.id == item.knowledgePointId }
                val evidenceId = item.evidenceId ?: viewModel.reviewEvidenceIdForKnowledgePoint(item.knowledgePointId)
                val wrongCount = l3.wrongBook.count { it.knowledgePointId == item.knowledgePointId }
                Spacer(Modifier.height(Dimens.xs))
                Text(kp?.title ?: item.knowledgePointId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "安排原因：${ReviewPlanEnhancementEngine.reasonFor(item, wrongCount)} · 优先级 ${item.priority}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "推荐动作：${ReviewPlanEnhancementEngine.actionsFor(item, wrongCount).joinToString(" / ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (evidenceId != null) {
                    Spacer(Modifier.height(Dimens.xxs))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                        ActionChip("查看证据") { viewModel.openEvidenceById(evidenceId) }
                        ActionChip("再测一次") { viewModel.startPractice(com.classmate.core.practice.PracticeMode.QUICK_REVIEW) }
                        if (wrongCount > 0) ActionChip("重练错题") { viewModel.startPractice(com.classmate.core.practice.PracticeMode.WRONG_ANSWER_RETRY) }
                        ActionChip("复述知识点") { viewModel.startSelfAssessment() }
                    }
                }
            }
        }
        if (l3.wrongBook.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("错题本", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.wrongBook.takeLast(3).reversed().forEach { wrong ->
                val evidence = l3.evidence.firstOrNull { it.id in wrong.evidenceIds }?.text ?: "暂无证据"
                val kp = l3.knowledgePoints.firstOrNull { it.id == wrong.knowledgePointId }
                Spacer(Modifier.height(Dimens.xs))
                Text(l3.questions.firstOrNull { it.id == wrong.questionId }?.stem ?: wrong.questionId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("用户答案：${wrong.userAnswer} · 正确答案：${wrong.correctAnswer}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("关联知识点：${kp?.title ?: wrong.knowledgePointId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(wrong.mistakeReason.ifBlank { "错因分析：请回到证据核对题干和选项。" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(wrong.remediationHint.ifBlank { "补救建议：先看证据，再重练这题。" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("解析：${wrong.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("证据：$evidence", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.xxs))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    wrong.evidenceIds.firstOrNull()?.let { evidenceId ->
                        ActionChip("查看证据") { viewModel.openEvidenceById(evidenceId) }
                    }
                    ActionChip("重练这题") { viewModel.retryWrongQuestion(wrong.id) }
                    ActionChip("复习相关知识点") { viewModel.openEvidenceForKnowledgePoint(wrong.knowledgePointId) }
                }
            }
        }
        Spacer(Modifier.height(Dimens.s))
        Text("Evidence chain：${l3.evidence.size} 条证据已绑定到题目和解析。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LearningDiagnosisCard(viewModel: AppViewModel) {
    val diagnosis = viewModel.ui.l3Pipeline.learningDiagnosis
    if (diagnosis.generatedAt == 0L && diagnosis.weakKnowledgePoints.isEmpty() && diagnosis.nextStudyTasks.isEmpty()) return
    ClassMateCard {
        Text("学习诊断", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        if (diagnosis.recentReviewPressure.isNotBlank()) {
            Text(diagnosis.recentReviewPressure, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.xs))
        }
        if (diagnosis.weakKnowledgePoints.isNotEmpty()) {
            Text("薄弱知识点 Top ${diagnosis.weakKnowledgePoints.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            diagnosis.weakKnowledgePoints.forEach { item ->
                Spacer(Modifier.height(Dimens.xs))
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(item.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.evidenceIds.firstOrNull()?.let { evidenceId ->
                    Spacer(Modifier.height(Dimens.xxs))
                    ActionChip("查看诊断证据") { viewModel.openEvidenceById(evidenceId) }
                }
            }
        }
        if (diagnosis.commonMistakeTypes.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("常错题型：${diagnosis.commonMistakeTypes.joinToString(" / ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (diagnosis.masteredKnowledgePoints.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.xs))
            Text("已掌握：${diagnosis.masteredKnowledgePoints.joinToString(" / ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (diagnosis.nextStudyTasks.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("建议下一步", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            diagnosis.nextStudyTasks.forEach { task ->
                Text("• $task", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Phase 4: on-device review suggestion (端侧蓝心). Safety placeholder when unavailable — never fabricated. */
@Composable
private fun OnDeviceReviewSuggestionCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
    ClassMateCard {
        Text("端侧复习建议（端侧蓝心）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "由端侧 BlueLM 3B 解释为什么这些知识点需要复习并给出下一步；端侧不可用时仅显示安全占位，不伪造建议。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ui.onDeviceReviewSuggestion?.let {
            Spacer(Modifier.height(Dimens.s))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = if (ui.onDeviceReviewSuggestionRunning) "生成中…" else "生成端侧复习建议",
            onClick = { viewModel.generateOnDeviceReviewSuggestion() },
            enabled = !ui.onDeviceReviewSuggestionRunning,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PracticeEntryCard(viewModel: AppViewModel) {
    ClassMateCard {
        Text("专项练习", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xxs))
        Text("在 App 内针对性练习，结果会更新复习队列；不联网生成新题，只复用本课题目与知识点。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ActionChip("开始练习") { viewModel.startPractice(PracticeMode.QUICK_REVIEW) }
            ActionChip("错题重练") { viewModel.startPractice(PracticeMode.WRONG_ANSWER_RETRY) }
            ActionChip("随机 3 题") { viewModel.startRandomQuiz(3) }
            ActionChip("随机 5 题") { viewModel.startRandomQuiz(5) }
            ActionChip("随机 10 题") { viewModel.startRandomQuiz(10) }
            ActionChip("模拟考试") { viewModel.startExam() }
            ActionChip("回忆复盘") { viewModel.startSelfAssessment() }
            ActionChip("需要多练") { viewModel.startPractice(PracticeMode.NEED_MORE_PRACTICE) }
            ActionChip("薄弱点专项") { viewModel.startPractice(PracticeMode.WEAKNESS_DRILL) }
        }
    }
}

@Composable
private fun OverviewCard(dueCount: Int, minutes: Int, weak: Int, needsReview: Int) {
    ClassMateCard {
        Text("今日", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xl)) {
            Stat("$dueCount", "待复习")
            Stat("$minutes", "分钟")
            Stat("$weak", "薄弱")
            Stat("$needsReview", "需复核")
        }
    }
}

@Composable
private fun WeaknessCard(viewModel: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val weaknesses = viewModel.weaknessItems()
    ClassMateCard {
        Text("薄弱点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        if (weaknesses.isEmpty()) {
            Text("暂无薄弱点。标记“已掌握”会降低优先级并从此列表移除。", color = cs.onSurfaceVariant)
        } else {
            weaknesses.take(6).forEach { item ->
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${item.courseTitle} / 优先级 ${item.priority}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Text(
                    "答错 ${item.wrongAnswerCount} · 太难 ${item.tooHardCount} · 需要多练 ${item.needExampleCount} · 证据存疑 ${item.evidenceWrongCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
            }
        }
    }
}

@Composable
private fun ExportCard(viewModel: AppViewModel) {
    ExportCenterCard(
        viewModel = viewModel,
        title = "导出复习报告",
        description = "包含复习任务、薄弱点、课程库、证据链和可用的学习报告内容。",
        buildArtifact = viewModel::buildReviewReportArtifact,
    )
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TaskCard(task: ReviewTask, index: Int, total: Int, viewModel: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val video = VideoRecommendationEngine.recommendationsForTask(task).firstOrNull()
    val showPracticeTools = task.counters.needExample > 0 || task.reason.contains("需要多练")
    val practiceLinks = PracticeSearchEngine.panelLinks(task.courseTitle, task.title)
    val practiceSuggestions = PracticeSearchEngine.recommendedKeywords(task.courseTitle, task.title)
    val primaryPracticeLink = practiceLinks.first()
    var showPracticePanel by remember(task.taskId) { mutableStateOf(false) }

    fun copyPracticeQuery() {
        clipboard.setText(AnnotatedString(PracticeSearchLauncher.clipboardText(primaryPracticeLink)))
        viewModel.toast("已复制搜索词")
    }

    ClassMateCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(task.courseTitle, style = MaterialTheme.typography.labelMedium, color = cs.primary)
                Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Pill("${task.estimatedMinutes} 分钟", cs.tertiaryContainer, cs.onTertiaryContainer)
        }
        Spacer(Modifier.height(Dimens.s))
        Text("原因：${task.reason}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            Pill("优先级 ${task.priority}", cs.secondaryContainer, cs.onSecondaryContainer)
            Pill(difficultyZh(task.difficulty), cs.secondaryContainer, cs.onSecondaryContainer)
            if (task.manuallyPinned) Pill("已置顶", cs.primaryContainer, cs.onPrimaryContainer)
            if (task.needsHumanReview) Pill("需复核", cs.errorContainer, cs.onErrorContainer)
        }
        Spacer(Modifier.height(Dimens.s))
        if (showPracticeTools) {
            Text("需要多练：${primaryPracticeLink.query}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.xxs))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                ActionChip("找练习") { showPracticePanel = true }
                ActionChip("复制搜索词") { copyPracticeQuery() }
                ActionChip("推荐关键词") { showPracticePanel = true }
            }
            Spacer(Modifier.height(Dimens.xxs))
            Text(
                "只打开搜索结果，不抓取平台内容。",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
            if (showPracticePanel) {
                PracticeSearchDialog(
                    links = practiceLinks,
                    suggestions = practiceSuggestions,
                    onDismiss = { showPracticePanel = false },
                    onCopy = { copyPracticeQuery() },
                    onOpen = { link ->
                        if (!PracticeSearchLauncher.open(context, link)) {
                            clipboard.setText(AnnotatedString(PracticeSearchLauncher.clipboardText(link)))
                            viewModel.toast("未找到浏览器，已复制搜索词")
                        }
                    },
                )
            }
        }
        if (video != null) {
            if (!showPracticeTools) {
                Text("找练习：${primaryPracticeLink.query}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.xxs))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    practiceLinks.take(2).forEach { link ->
                        ActionChip("找练习·${link.sourceName}") {
                            if (!PracticeSearchLauncher.open(context, link)) {
                                clipboard.setText(AnnotatedString(PracticeSearchLauncher.clipboardText(link)))
                                viewModel.toast("未找到浏览器，已复制搜索词")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Dimens.s))
            }
            Text(
                "视频推荐为补充资源，来自白名单来源，不替代课堂证据。",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dimens.xxs))
            ActionChip("找视频：${video.source}") { uriHandler.openUri(video.searchUrl) }
        }
        Spacer(Modifier.height(Dimens.s))
        Text("来源：${task.sourceProfile}${if (task.sourceModel.isNotBlank()) " / ${task.sourceModel}" else ""}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.m))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ActionChip("打开") { viewModel.openTaskCourse(task) }
            ActionChip("开始练习") { viewModel.startPracticeForTask(task) }
            if (viewModel.reviewEvidenceIdForKnowledgePoint(task.knowledgePointId) != null) {
                ActionChip("查看来源") { viewModel.openEvidenceForReviewTask(task) }
            }
            ActionChip("完成") { viewModel.reviewMarkDone(task.taskId) }
        }
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ActionChip("已掌握") { viewModel.reviewTaskFeedback(task.taskId, ReviewEventType.MASTERED) }
            ActionChip("太难") { viewModel.reviewTaskFeedback(task.taskId, ReviewEventType.TOO_HARD) }
            ActionChip("需要多练") { viewModel.reviewTaskFeedback(task.taskId, ReviewEventType.NEED_EXAMPLE) }
            ActionChip("证据存疑") { viewModel.reviewTaskFeedback(task.taskId, ReviewEventType.EVIDENCE_WRONG) }
        }
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ActionChip("高") { viewModel.reviewSetPriority(task.taskId, ReviewPriorityLevel.HIGH) }
            ActionChip("中") { viewModel.reviewSetPriority(task.taskId, ReviewPriorityLevel.MEDIUM) }
            ActionChip("低") { viewModel.reviewSetPriority(task.taskId, ReviewPriorityLevel.LOW) }
            if (index > 0) ActionChip("上移") { viewModel.reviewMoveUp(task.taskId) }
            if (index < total - 1) ActionChip("下移") { viewModel.reviewMoveDown(task.taskId) }
        }
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ActionChip(if (task.manuallyPinned) "取消置顶" else "置顶") { viewModel.reviewSetPinned(task.taskId, !task.manuallyPinned) }
            ActionChip("移除") { viewModel.reviewRemove(task.taskId) }
        }
    }
}

@Composable
private fun PracticeSearchDialog(
    links: List<PracticeSearchLink>,
    suggestions: List<PracticeKeywordSuggestion>,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onOpen: (PracticeSearchLink) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("练习搜索面板") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.s)) {
                Text("只打开搜索结果，不抓取平台内容。无浏览器时可复制搜索词。", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Text("推荐关键词", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                suggestions.take(4).forEach { item ->
                    Text("${item.label}：${item.query}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                Text("打开外部搜索", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                links.forEach { link ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(link.sourceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(link.query, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        ActionChip("打开") { onOpen(link) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = onCopy) { Text("复制搜索词") }
        },
    )
}

private fun difficultyZh(name: String): String = when (name.uppercase()) {
    "EASY" -> "入门"
    "MEDIUM" -> "进阶"
    "HARD" -> "挑战"
    else -> name
}

@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = cs.surfaceVariant,
        contentColor = cs.onSurfaceVariant,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
