package com.classmate.app.ui.screens.evidence

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.l3.Evidence
import com.classmate.app.l3.EvidenceAsset
import com.classmate.app.l3.L3SourceType
import com.classmate.core.evidence.EvidenceRelationLevel
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
    val l3Evidence = ui.selectedEvidenceId?.let { id -> ui.l3Pipeline.evidence.firstOrNull { it.id == id } }
    val l3Asset = l3Evidence?.assetId?.let { id -> ui.l3Pipeline.evidenceAssets.firstOrNull { it.id == id } }
    val kp = result?.knowledgePoint(ui.selectedKnowledgePointId ?: "")

    ClassMateScaffold(title = "查看证据", onBack = { viewModel.goBack() }) { padding ->
        if (l3Evidence != null) {
            // The real, user-facing excerpt — never raw ids, MIME types or file paths on a study page.
            val excerpt = l3Evidence.text.ifBlank { l3Asset?.text.orEmpty() }
            val material = listOf(
                l3Evidence.sourceLabel,
                l3Asset?.sourceLabel.orEmpty(),
                l3Evidence.fileName,
                l3Asset?.fileName.orEmpty(),
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            val location = evidenceLocation(l3Evidence, l3Asset)
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screen)
                    .padding(bottom = Dimens.xxl),
                verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
            ) {
                ClassMateCard {
                    Text(l3SourceTitle(l3Evidence.sourceType), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    if (material.isNotBlank()) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text("来源资料：$material", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (location.isNotBlank()) {
                        Text(location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(Dimens.m))
                    if (excerpt.isNotBlank()) {
                        Text("原文片段", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(Dimens.xs))
                        Text("「$excerpt」", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        // Honest weak-binding warning: if the excerpt shares no keywords with the points /
                        // questions it supports, say so instead of presenting it as solid evidence.
                        val boundContext = (
                            ui.l3Pipeline.knowledgePoints.filter { l3Evidence.id in it.sourceEvidenceIds }.joinToString(" ") { it.title } + " " +
                                ui.l3Pipeline.questions.filter { l3Evidence.id in it.evidenceIds }.joinToString(" ") { it.stem }
                            ).trim()
                        if (boundContext.isNotBlank() &&
                            viewModel.evidenceRelationLevel(l3Evidence.id, boundContext) == EvidenceRelationLevel.WEAK
                        ) {
                            Spacer(Modifier.height(Dimens.s))
                            Text(
                                "该证据片段可能与当前知识点关联较弱，请结合原文核对。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        // Honest: don't pretend an excerpt exists when we couldn't locate one.
                        Text("该内容暂无可回溯的原文片段。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(Dimens.xs))
                        Text("它可能来自本地基础整理，或未能定位到原始材料位置，请结合课堂材料人工确认。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // OCR image evidence: show the picture itself, never raw file paths.
                val imageRef = l3Evidence.imageRef.ifBlank { l3Asset?.imageRef.orEmpty() }
                val thumbnailRef = l3Evidence.thumbnailRef.ifBlank { l3Asset?.thumbnailRef.orEmpty() }
                if (l3Evidence.sourceType == L3SourceType.OCR_IMAGE || imageRef.isNotBlank()) {
                    ClassMateCard {
                        Text("图片证据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val previewBitmap = remember(thumbnailRef, imageRef) {
                            runCatching {
                                val ref = thumbnailRef.ifBlank { imageRef }
                                if (ref.isBlank()) null else BitmapFactory.decodeFile(ref)
                            }.getOrNull()
                        }
                        Spacer(Modifier.height(Dimens.s))
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "OCR 图片证据预览",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text("图片预览暂不可用，已保留 OCR 文本作为证据。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Audio / transcript evidence: transcript text + honest confidence note, never raw refs.
                val transcriptSegment = l3Evidence.transcriptSegment.ifBlank { l3Asset?.transcriptSegment.orEmpty() }
                val audioRef = l3Evidence.audioRef.ifBlank { l3Asset?.audioRef.orEmpty() }
                if (l3Evidence.sourceType == L3SourceType.AUDIO_TRANSCRIPT ||
                    l3Evidence.sourceType == L3SourceType.MANUAL_TRANSCRIPT ||
                    l3Evidence.sourceType == L3SourceType.RECORDING_ARTIFACT ||
                    audioRef.isNotBlank()
                ) {
                    ClassMateCard {
                        Text("音频 / 转写证据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (transcriptSegment.isNotBlank()) {
                            Spacer(Modifier.height(Dimens.s))
                            Text("转写片段：「$transcriptSegment」", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        val audioSegments = ui.l3Pipeline.transcriptSegments.filter {
                            it.sourceId == l3Evidence.sourceId || it.sourceId == audioRef || it.text == transcriptSegment
                        }
                        val lowConfidenceCount = audioSegments.count { it.lowConfidence }
                        if (lowConfidenceCount > 0) {
                            Spacer(Modifier.height(Dimens.xs))
                            Text("低置信片段：$lowConfidenceCount 条，请确认后再作为高可信复习材料。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        val relatedKnowledgeCount = ui.l3Pipeline.knowledgePoints.count { l3Evidence.id in it.sourceEvidenceIds }
                        val relatedWrongCount = ui.l3Pipeline.wrongBook.count { l3Evidence.id in it.evidenceIds }
                        val relatedReviewCount = ui.l3Pipeline.reviewQueue.count { item ->
                            item.evidenceId == l3Evidence.id || ui.l3Pipeline.knowledgePoints.firstOrNull { it.id == item.knowledgePointId }?.sourceEvidenceIds?.contains(l3Evidence.id) == true
                        }
                        Spacer(Modifier.height(Dimens.xs))
                        Text("关联知识点 $relatedKnowledgeCount · 关联错题 $relatedWrongCount · 关联复习任务 $relatedReviewCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("当前保留转写证据，播放定位待真机验证。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (l3Asset == null && l3Evidence.assetId != null) {
                    ClassMateCard {
                        Text("证据资产缺失，但保留文本证据。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }

                val linkedKnowledge = ui.l3Pipeline.knowledgePoints.filter { l3Evidence.id in it.sourceEvidenceIds }
                val linkedQuestions = ui.l3Pipeline.questions.filter { l3Evidence.id in it.evidenceIds }
                if (linkedKnowledge.isNotEmpty()) {
                    ClassMateCard {
                        Text("关联知识点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(Dimens.s))
                        linkedKnowledge.forEach { Text(it.title, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                if (linkedQuestions.isNotEmpty()) {
                    ClassMateCard {
                        Text("关联微测题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(Dimens.s))
                        linkedQuestions.forEach { question ->
                            Text(question.stem, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(Dimens.xs))
                        }
                    }
                }
            }
            return@ClassMateScaffold
        }
        if (kp == null || session == null) {
            val message = if (ui.selectedEvidenceId != null) {
                "该证据暂时无法回溯：它可能来自本地整理，或原始材料已变更。"
            } else {
                "未选择知识点。"
            }
            Box(Modifier.padding(padding).fillMaxWidth().padding(Dimens.screen)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
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

private fun l3SourceTitle(sourceType: L3SourceType): String = when (sourceType) {
    L3SourceType.TEXT -> "来自课堂文本"
    L3SourceType.OCR_IMAGE -> "来自 OCR 图片"
    L3SourceType.DOCUMENT -> "来自文档"
    L3SourceType.AUDIO_TRANSCRIPT -> "来自课堂录音转写"
    L3SourceType.MANUAL_TRANSCRIPT -> "来自手动转写"
    L3SourceType.RECORDING_ARTIFACT -> "来自录音"
    L3SourceType.QUESTION_BANK -> "来自题库"
    L3SourceType.WEB -> "来自网络资料"
}

/** A user-readable location line from structured fields only (page / paragraph / time) — no raw refs. */
private fun evidenceLocation(evidence: Evidence, asset: EvidenceAsset?): String {
    val parts = mutableListOf<String>()
    evidence.page?.let { parts += "第 $it 页" }
    evidence.blockIndex?.let { parts += "第 ${it + 1} 段" }
    val start = evidence.segmentStartMs ?: asset?.startMs
    val end = evidence.segmentEndMs ?: asset?.endMs
    if (start != null || end != null) {
        val range = listOfNotNull(start?.let { "${it / 1000}s" }, end?.let { "${it / 1000}s" }).joinToString("–")
        if (range.isNotBlank()) parts += "时间 $range"
    }
    return if (parts.isEmpty()) "" else "位置：" + parts.joinToString(" · ")
}
