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
import com.classmate.app.l3.L3SourceType
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

    ClassMateScaffold(title = "证据链", onBack = { viewModel.goBack() }) { padding ->
        if (l3Evidence != null) {
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
                    Spacer(Modifier.height(Dimens.s))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                        Pill(
                            text = l3Evidence.sourceType.name,
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Spacer(Modifier.height(Dimens.m))
                    Text(
                        l3Evidence.text.ifBlank { "Evidence text is missing; only source metadata is retained." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                ClassMateCard {
                    Text("Source asset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(Dimens.s))
                    Text(l3AssetLine("Label", l3Evidence.sourceLabel.ifBlank { l3Asset?.sourceLabel.orEmpty() }), style = MaterialTheme.typography.bodyMedium)
                    Text(l3AssetLine("File", l3Evidence.fileName.ifBlank { l3Asset?.fileName.orEmpty() }), style = MaterialTheme.typography.bodyMedium)
                    Text(l3AssetLine("MIME", l3Evidence.mimeType.ifBlank { l3Asset?.mimeType.orEmpty() }), style = MaterialTheme.typography.bodyMedium)
                    Text(l3AssetLine("Page", l3Evidence.pageHint.ifBlank { l3Asset?.pageHint.orEmpty() }), style = MaterialTheme.typography.bodyMedium)
                    Text(l3AssetLine("Segment", l3Evidence.segmentHint.ifBlank { l3Asset?.segmentHint.orEmpty() }), style = MaterialTheme.typography.bodyMedium)
                    Text(l3AssetLine("Snippet", l3Evidence.snippet.ifBlank { l3Asset?.snippet.orEmpty() }), style = MaterialTheme.typography.bodyMedium)
                    val start = l3Evidence.segmentStartMs ?: l3Asset?.startMs
                    val end = l3Evidence.segmentEndMs ?: l3Asset?.endMs
                    if (start != null || end != null) {
                        Text(l3AssetLine("Time", listOfNotNull(start?.let { "${it / 1000}s" }, end?.let { "${it / 1000}s" }).joinToString(" - ")), style = MaterialTheme.typography.bodyMedium)
                    }
                    val imageRef = l3Evidence.imageRef.ifBlank { l3Asset?.imageRef.orEmpty() }
                    val thumbnailRef = l3Evidence.thumbnailRef.ifBlank { l3Asset?.thumbnailRef.orEmpty() }
                    if (l3Evidence.sourceType == L3SourceType.OCR_IMAGE || imageRef.isNotBlank()) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text("图片缩略图引用", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        val previewBitmap = remember(thumbnailRef, imageRef) {
                            runCatching {
                                val ref = thumbnailRef.ifBlank { imageRef }
                                if (ref.isBlank()) null else BitmapFactory.decodeFile(ref)
                            }.getOrNull()
                        }
                        if (previewBitmap != null) {
                            Spacer(Modifier.height(Dimens.xs))
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "OCR image evidence preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Spacer(Modifier.height(Dimens.xs))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .padding(Dimens.xs),
                            ) {
                                Text("图片预览暂不可用，已保留 OCR 文本和资产引用。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(l3AssetLine("Thumbnail", thumbnailRef), style = MaterialTheme.typography.bodyMedium)
                        Text(l3AssetLine("Image ref", imageRef), style = MaterialTheme.typography.bodyMedium)
                        Text("如果真机 bitmap 暂不可解析，将使用 OCR 文本和资产引用降级展示。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (l3Evidence.sourceType == L3SourceType.DOCUMENT) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text("文档来源已保留：文件名、页码/段落和片段会随 L3 snapshot 持久化。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val audioRef = l3Evidence.audioRef.ifBlank { l3Asset?.audioRef.orEmpty() }
                    val transcriptSegment = l3Evidence.transcriptSegment.ifBlank { l3Asset?.transcriptSegment.orEmpty() }
                    if (l3Evidence.sourceType == L3SourceType.AUDIO_TRANSCRIPT || l3Evidence.sourceType == L3SourceType.MANUAL_TRANSCRIPT || l3Evidence.sourceType == L3SourceType.RECORDING_ARTIFACT || audioRef.isNotBlank()) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text("音频 / 转写证据", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(l3AssetLine("Audio ref", audioRef), style = MaterialTheme.typography.bodyMedium)
                        Text(l3AssetLine("Transcript", transcriptSegment), style = MaterialTheme.typography.bodyMedium)
                        Text("当前保留转写证据，播放定位待真机验证。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (l3Asset == null && l3Evidence.assetId != null) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text("证据资产缺失，但保留文本证据。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                val linkedKnowledge = ui.l3Pipeline.knowledgePoints.filter { l3Evidence.id in it.sourceEvidenceIds }
                val linkedQuestions = ui.l3Pipeline.questions.filter { l3Evidence.id in it.evidenceIds }
                if (linkedKnowledge.isNotEmpty()) {
                    ClassMateCard {
                        Text("Linked knowledge", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(Dimens.s))
                        linkedKnowledge.forEach { Text(it.title, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                if (linkedQuestions.isNotEmpty()) {
                    ClassMateCard {
                        Text("Linked quiz items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

private fun l3SourceTitle(sourceType: L3SourceType): String = when (sourceType) {
    L3SourceType.TEXT -> "Text evidence"
    L3SourceType.OCR_IMAGE -> "OCR image evidence"
    L3SourceType.DOCUMENT -> "Document evidence"
    L3SourceType.AUDIO_TRANSCRIPT -> "Audio transcript evidence"
    L3SourceType.MANUAL_TRANSCRIPT -> "Manual transcript evidence"
    L3SourceType.RECORDING_ARTIFACT -> "Recording evidence"
    L3SourceType.QUESTION_BANK -> "Question bank evidence"
    L3SourceType.WEB -> "Web evidence"
}

private fun l3AssetLine(label: String, value: String): String =
    "$label: ${value.ifBlank { "not available" }}"
