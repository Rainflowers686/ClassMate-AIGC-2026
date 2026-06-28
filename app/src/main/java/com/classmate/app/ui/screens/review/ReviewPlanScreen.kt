package com.classmate.app.ui.screens.review

import androidx.compose.foundation.BorderStroke
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
import com.classmate.app.video.BilibiliSearch
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ExportCenterCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.HelpHint
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductHero
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.components.Pill
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.design.Dimens
import com.classmate.app.l3.ReviewPlanEnhancementEngine
import com.classmate.app.ui.i18n.appStrings
import com.classmate.core.evidence.EvidenceRelationLevel
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
                val s = appStrings(viewModel.ui.language)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(s.helpReviewTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    HelpHint(title = s.helpReviewTitle, points = s.helpReviewPoints, dismiss = s.helpDismiss)
                }
                Spacer(Modifier.height(Dimens.xs))
                Text(
                    "复习任务来自校验通过的知识点；详情见右上角「?」。",
                    style = MaterialTheme.typography.bodySmall,
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
        if (l3.reviewQueue.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("20 分钟复习计划", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.reviewQueue.take(5).forEach { item ->
                val kp = l3.knowledgePoints.firstOrNull { it.id == item.knowledgePointId }
                val evidenceId = item.evidenceId ?: viewModel.reviewEvidenceIdForKnowledgePoint(item.knowledgePointId)
                val wrongCount = l3.wrongBook.count { it.knowledgePointId == item.knowledgePointId }
                Spacer(Modifier.height(Dimens.xs))
                Text(kp?.title ?: "复习知识点", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                Spacer(Modifier.height(Dimens.xxs))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    EvidenceActionChip(viewModel, evidenceId, kp?.title.orEmpty())
                    ActionChip("再测一次") { viewModel.startPractice(com.classmate.core.practice.PracticeMode.QUICK_REVIEW) }
                    if (wrongCount > 0) ActionChip("重练错题") { viewModel.startPractice(com.classmate.core.practice.PracticeMode.WRONG_ANSWER_RETRY) }
                    ActionChip("复述知识点") { viewModel.startSelfAssessment() }
                }
            }
        }
        if (l3.wrongBook.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.s))
            Text("错题本", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            l3.wrongBook.takeLast(3).reversed().forEach { wrong ->
                val evidence = l3.evidence.firstOrNull { it.id in wrong.evidenceIds }?.text?.ifBlank { null } ?: "暂无可回溯原文片段"
                val kp = l3.knowledgePoints.firstOrNull { it.id == wrong.knowledgePointId }
                Spacer(Modifier.height(Dimens.xs))
                Text(l3.questions.firstOrNull { it.id == wrong.questionId }?.stem ?: "错题", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("用户答案：${wrong.userAnswer} · 正确答案：${wrong.correctAnswer}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("关联知识点：${kp?.title ?: wrong.knowledgePointId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(wrong.mistakeReason.ifBlank { "错因分析：请回到证据核对题干和选项。" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(wrong.remediationHint.ifBlank { "补救建议：先看证据，再重练这题。" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("解析：${wrong.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("证据：$evidence", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Dimens.xxs))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    val wrongContext = (l3.questions.firstOrNull { it.id == wrong.questionId }?.stem.orEmpty() + " " + (kp?.title ?: ""))
                    EvidenceActionChip(viewModel, wrong.evidenceIds.firstOrNull(), wrongContext)
                    ActionChip("重练这题") { viewModel.retryWrongQuestion(wrong.id) }
                    ActionChip("复习相关知识点") { viewModel.openEvidenceForKnowledgePoint(wrong.knowledgePointId) }
                }
            }
        }
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
                Spacer(Modifier.height(Dimens.xxs))
                EvidenceActionChip(viewModel, item.evidenceIds.firstOrNull(), item.title + " " + item.reason, "查看诊断证据")
            }
            // P0-2: AI remediation plan + the gated 薄弱点专项 practice (admits only answerable questions).
            val we = viewModel.ui.weaknessEnhancement
            val clipboard = LocalClipboardManager.current
            Spacer(Modifier.height(Dimens.s))
            when {
                we.running -> Text("正在生成 AI 加练方案…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                we.hasResult -> {
                    Text("AI 加练方案 · ${we.sourceZh}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.xxs))
                    Text(we.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                we.failed -> Text("AI 加练方案暂时无法生成，可重试或直接开始薄弱点专项。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(Dimens.xs))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                ActionChip(if (we.running) "生成中…" else "AI 加练方案") { viewModel.generateWeaknessRemediation() }
                ActionChip("开始薄弱点专项") { viewModel.startPractice(PracticeMode.WEAKNESS_DRILL) }
                if (we.hasResult) ActionChip("复制") {
                    clipboard.setText(AnnotatedString(we.text)); viewModel.toast("已复制加练方案。")
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
            ActionChip("随机 5 题") { viewModel.startRandomQuiz(5) }
            ActionChip("模拟考试") { viewModel.startExam() }
            ActionChip("回忆复盘") { viewModel.startSelfAssessment() }
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
        Spacer(Modifier.height(Dimens.m))
        // Core learning actions only — open / practice / evidence / done. Priority/move/pin admin
        // controls are intentionally not shown to keep this a study page, not a backstage console.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ActionChip("B站搜讲解") {
                val link = BilibiliSearch.linkFor(task.title, task.courseTitle)
                if (!PracticeSearchLauncher.open(context, link)) {
                    clipboard.setText(AnnotatedString(link.query))
                    viewModel.toast("未找到浏览器，已复制 B站搜索词。")
                }
            }
        }
        Spacer(Modifier.height(Dimens.xs))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ActionChip("打开") { viewModel.openTaskCourse(task) }
            ActionChip("开始练习") { viewModel.startPracticeForTask(task) }
            EvidenceActionChip(viewModel, viewModel.reviewEvidenceIdForKnowledgePoint(task.knowledgePointId), "")
            ActionChip("完成") { viewModel.reviewMarkDone(task.taskId) }
        }
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ActionChip("已掌握") { viewModel.reviewTaskFeedback(task.taskId, ReviewEventType.MASTERED) }
            ActionChip("太难") { viewModel.reviewTaskFeedback(task.taskId, ReviewEventType.TOO_HARD) }
            ActionChip("需要多练") { viewModel.reviewTaskFeedback(task.taskId, ReviewEventType.NEED_EXAMPLE) }
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

/**
 * Evidence entry that tells the truth about the binding: a real "查看证据" only when the excerpt actually
 * relates to [context]; "证据待核对" when it resolves but shares no keywords (likely mis-bound); and
 * "暂无可回溯证据" when there is no retraceable excerpt at all.
 */
@Composable
private fun EvidenceActionChip(viewModel: AppViewModel, evidenceId: String?, context: String, label: String? = null) {
    val s = appStrings(viewModel.ui.language)
    when (viewModel.evidenceRelationLevel(evidenceId, context)) {
        EvidenceRelationLevel.STRONG -> ActionChip(label ?: s.evidenceView) { viewModel.openEvidenceById(evidenceId.orEmpty()) }
        EvidenceRelationLevel.WEAK -> EvidenceHintChip(s.evidenceCheck)
        EvidenceRelationLevel.MISSING -> EvidenceHintChip(s.evidenceNone)
    }
}

/** Non-actionable, honest chip shown in place of "查看证据" when no retraceable evidence exists. */
@Composable
private fun EvidenceHintChip(text: String) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = cs.surface,
        contentColor = cs.onSurfaceVariant,
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
