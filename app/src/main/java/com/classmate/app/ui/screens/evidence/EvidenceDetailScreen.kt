package com.classmate.app.ui.screens.evidence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.DifficultyBadge
import com.classmate.app.ui.components.HighlightedSegmentText
import com.classmate.app.ui.components.ImportanceBadge
import com.classmate.app.ui.components.Pill
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.design.Dimens
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType

@Composable
fun EvidenceDetailScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val result = ui.result
    val session = ui.session
    val kp = result?.knowledgePoint(ui.selectedKnowledgePointId ?: "")

    ClassMateScaffold(title = "证据链", onBack = { viewModel.goBack() }) { padding ->
        if (kp == null || session == null) {
            Box(Modifier.padding(padding).fillMaxWidth().padding(Dimens.screen)) {
                Text("未选择知识点。", style = MaterialTheme.typography.bodyMedium)
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
            ClassMateCard {
                Text(kp.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    ImportanceBadge(kp.importance)
                    DifficultyBadge(kp.difficulty)
                }
                Spacer(Modifier.height(Dimens.m))
                Text(kp.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            ClassMateCard {
                Text("为什么这个知识点成立", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Text(
                    "它不是凭空生成的：下面的原文片段（高亮处）正是它的依据。ClassMate 只保留能在课堂原文中定位证据的结论。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Group evidence by segment and render the source with the cited spans highlighted.
            val bySegment = kp.evidence.groupBy { it.sourceSegmentId }
            bySegment.forEach { (segmentId, spans) ->
                val segment = session.segment(segmentId) ?: return@forEach
                ClassMateCard {
                    Text("第 ${segment.index} 段原文", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(Dimens.s))
                    HighlightedSegmentText(
                        text = segment.text,
                        highlights = spans.map { it.startChar..(it.endChar - 1) },
                    )
                }
            }

            if (kp.relatedPointIds.isNotEmpty()) {
                ClassMateCard {
                    Text("相关知识点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                        kp.relatedPointIds.forEach { relatedId ->
                            val related = result.knowledgePoint(relatedId) ?: return@forEach
                            Pill(
                                text = related.title,
                                container = MaterialTheme.colorScheme.secondaryContainer,
                                content = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier,
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.m)) {
                SecondaryButton(
                    text = "证据不对",
                    onClick = {
                        viewModel.submitFeedback(FeedbackType.EVIDENCE_WRONG, FeedbackTargetKind.KNOWLEDGE_POINT, kp.id)
                    },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "已掌握",
                    onClick = {
                        viewModel.submitFeedback(FeedbackType.ALREADY_MASTERED, FeedbackTargetKind.KNOWLEDGE_POINT, kp.id)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
