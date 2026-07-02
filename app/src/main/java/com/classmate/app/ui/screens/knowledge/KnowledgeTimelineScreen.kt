package com.classmate.app.ui.screens.knowledge

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.practice.PracticeSearchLauncher
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.components.ChipTone
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.i18n.appStrings
import com.classmate.app.ui.product.QuietCard
import com.classmate.app.ui.components.DifficultyBadge
import com.classmate.app.ui.components.StatusChip
import com.classmate.app.ui.components.EvidenceBlock
import com.classmate.app.ui.components.ExportCenterCard
import com.classmate.app.ui.components.ImportanceBadge
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.ProvenanceChip
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.i18n.Strings
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.practice.KnowledgePointSearch
import com.classmate.core.practice.PracticeMode

@Composable
fun KnowledgeTimelineScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    val result = ui.result
    val session = ui.session

    ClassMateScaffold(
        title = s.knowledgeTitle,
        onBack = { viewModel.goBackOrHome() },
        bottomBar = {
            if (result != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.screen, vertical = Dimens.m)) {
                        PrimaryButton(
                            text = s.knowledgeStartQuiz(result.quizQuestions.size),
                            onClick = { viewModel.startPractice(PracticeMode.QUICK_REVIEW) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = result.quizQuestions.isNotEmpty(),
                        )
                        Spacer(Modifier.height(Dimens.s))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.m)) {
                            SecondaryButton(
                                text = s.knowledgeReviewPlan,
                                onClick = { viewModel.ensureReviewPlan(); viewModel.navigateTo(Screen.REVIEW) },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                text = s.knowledgeFeedback,
                                onClick = { viewModel.navigateTo(Screen.FEEDBACK) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (result == null || session == null) {
            Box(Modifier.padding(padding).fillMaxWidth().padding(Dimens.screen)) {
                Text(s.knowledgeNoResult, style = MaterialTheme.typography.bodyMedium)
            }
            return@ClassMateScaffold
        }

        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            QuietCard {
                Text(session.title.ifBlank { s.untitledCourse }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                ProvenanceChip(result.provenance)
                Spacer(Modifier.height(Dimens.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.xl)) {
                    Stat("${result.knowledgePoints.size}", s.knowledgeStatKp)
                    Stat("${result.quizQuestions.size}", s.knowledgeStatQuiz)
                    Stat("${session.segments.size}", s.knowledgeStatSegments)
                }
            }

            ExportCenterCard(
                viewModel = viewModel,
                buildArtifact = viewModel::buildCurrentReportArtifact,
            )

            // Real-device #10/#17: the ask-this-lesson Q&A box is no longer surfaced; the timeline leads
            // with knowledge points, evidence and 微测.

            val related = ui.l3Pipeline.relatedKnowledgeSummaries.take(4)
            if (related.isNotEmpty()) {
                QuietCard {
                    Text("相关知识点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.s))
                    related.forEach { item ->
                        Text(
                            item.summary + if (item.needsReview) "（待核对）" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (item.evidenceQuotes.isNotEmpty()) {
                            Spacer(Modifier.height(Dimens.xs))
                            EvidenceBlock(quote = item.evidenceQuotes.first(), segmentLabel = s.quizSourceLabel)
                        }
                        Spacer(Modifier.height(Dimens.s))
                    }
                }
            }

            val context = LocalContext.current
            result.knowledgePoints.forEachIndexed { index, kp ->
                val segIndex = session.segments.firstOrNull { it.id == kp.sourceSegmentId }?.index
                KnowledgePointCard(
                    number = index + 1,
                    kp = kp,
                    flagged = kp.id in ui.flaggedKnowledgePointIds,
                    segmentLabel = if (segIndex != null) s.quizSegmentLabel(segIndex) else s.quizSourceLabel,
                    openEvidenceLabel = s.knowledgeOpenEvidence,
                    onOpenEvidence = { viewModel.openEvidence(kp.id) },
                    strings = s,
                    search = viewModel.knowledgePointSearch(kp.id),
                    onOpenSearch = { link ->
                        if (!PracticeSearchLauncher.open(context, link)) viewModel.toast("未找到浏览器，请稍后再试。")
                    },
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KnowledgePointCard(
    number: Int,
    kp: KnowledgePoint,
    flagged: Boolean,
    segmentLabel: String,
    openEvidenceLabel: String,
    onOpenEvidence: () -> Unit,
    strings: Strings,
    search: KnowledgePointSearch.Result,
    onOpenSearch: (com.classmate.core.practice.PracticeSearchLink) -> Unit,
) {
    QuietCard(onClick = onOpenEvidence) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Text("$number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(Dimens.m))
            Text(kp.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            // P0-3: a visible marker after the user flags this point as inaccurate.
            if (flagged) StatusChip("需复核", tone = ChipTone.WARNING)
        }
        Spacer(Modifier.height(Dimens.s))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            ImportanceBadge(kp.importance)
            DifficultyBadge(kp.difficulty)
        }
        Spacer(Modifier.height(Dimens.m))
        Text(kp.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        kp.evidence.firstOrNull()?.let { span ->
            Spacer(Modifier.height(Dimens.m))
            EvidenceBlock(quote = span.quote, segmentLabel = segmentLabel)
        }
        Spacer(Modifier.height(Dimens.s))
        Text(openEvidenceLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

        // P1-3: honest browser-search entry. High-confidence points get real browser links; a 需复核 /
        // low-confidence point shows a "先完善资料" hint instead of a misleading search button.
        Spacer(Modifier.height(Dimens.m))
        when (search) {
            is KnowledgePointSearch.Result.Available -> {
                Text(strings.knowledgeSearchTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    search.links.take(2).forEach { link ->
                        SecondaryButton(
                            text = strings.knowledgeSearchOpen(link.sourceName),
                            onClick = { onOpenSearch(link) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    search.links.drop(2).take(2).forEach { link ->
                        SecondaryButton(
                            text = strings.knowledgeSearchOpen(link.sourceName),
                            onClick = { onOpenSearch(link) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.xs))
                Text(strings.knowledgeSearchHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            KnowledgePointSearch.Result.NeedsReview ->
                Text(strings.knowledgeSearchNeedsReview, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
