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
import com.classmate.app.ui.i18n.Strings
import com.classmate.app.ui.i18n.appStrings
import com.classmate.core.evidence.EvidenceRelationLevel
import com.classmate.app.state.AppViewModel
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.DifficultyBadge
import com.classmate.app.ui.components.EnhancementPanel
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
    val clipboard = LocalClipboardManager.current
    val result = ui.result
    val session = ui.session
    val s = appStrings(ui.language)
    val l3Evidence = ui.selectedEvidenceId?.let { id -> ui.l3Pipeline.evidence.firstOrNull { it.id == id } }
    val l3Asset = l3Evidence?.assetId?.let { id -> ui.l3Pipeline.evidenceAssets.firstOrNull { it.id == id } }
    val kp = result?.knowledgePoint(ui.selectedKnowledgePointId ?: "")

    ClassMateScaffold(title = s.evidenceView, onBack = { viewModel.goBackOrHome() }) { padding ->
        if (l3Evidence != null) {
            // The real, user-facing excerpt; technical metadata stays out of the study page.
            val excerpt = l3Evidence.text.ifBlank { l3Asset?.text.orEmpty() }
            val material = listOf(
                l3Evidence.sourceLabel,
                l3Asset?.sourceLabel.orEmpty(),
                l3Evidence.fileName,
                l3Asset?.fileName.orEmpty(),
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            val location = evidenceLocation(l3Evidence, l3Asset, s)
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screen)
                    .padding(bottom = Dimens.xxl),
                verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
            ) {
                ClassMateCard {
                    Text(l3SourceTitle(l3Evidence.sourceType, s), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    if (material.isNotBlank()) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text(s.evidenceSourceMaterial(material), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (location.isNotBlank()) {
                        Text(location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(Dimens.m))
                    if (excerpt.isNotBlank()) {
                        Text(s.evidenceOriginalExcerpt, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                                appStrings(ui.language).evidenceWeakNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        // Honest: don't pretend an excerpt exists when we couldn't locate one.
                        Text(s.evidenceNoExcerpt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(Dimens.xs))
                        Text(s.evidenceNoExcerptHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // P0-1: AI evidence explanation — only offered when there is a real excerpt to explain.
                // Routes cloud BlueLM → on-device → conservative local template, with an honest source label.
                if (excerpt.isNotBlank()) {
                    EnhancementPanel(
                        state = ui.evidenceEnhancement,
                        idleTitle = s.evidenceAiExplanationTitle,
                        idleHint = s.evidenceAiExplanationHint,
                        triggerText = s.evidenceAiExplanationTrigger,
                        runningTitle = s.evidenceAiExplanationRunning,
                        onGenerate = { viewModel.generateEvidenceExplanation() },
                        onCopy = { text -> clipboard.setText(AnnotatedString(text)); viewModel.toast(s.evidenceAiExplanationCopied) },
                        onDismiss = { viewModel.clearEvidenceEnhancement() },
                        language = ui.language,
                    )
                } else {
                    ClassMateCard {
                        Text(s.evidenceAiExplanationTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(Dimens.xxs))
                        Text(s.evidenceAiExplanationEmpty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // OCR image evidence: show the picture itself when the asset preview is available.
                val imageRef = l3Evidence.imageRef.ifBlank { l3Asset?.imageRef.orEmpty() }
                val thumbnailRef = l3Evidence.thumbnailRef.ifBlank { l3Asset?.thumbnailRef.orEmpty() }
                if (l3Evidence.sourceType == L3SourceType.OCR_IMAGE || imageRef.isNotBlank()) {
                    ClassMateCard {
                        Text(s.evidenceImageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                                contentDescription = s.evidenceImagePreviewDescription,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(s.evidenceImagePreviewUnavailable, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(s.evidenceAudioTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (transcriptSegment.isNotBlank()) {
                            Spacer(Modifier.height(Dimens.s))
                            Text(s.evidenceTranscriptSegment(transcriptSegment), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        val audioSegments = ui.l3Pipeline.transcriptSegments.filter {
                            it.sourceId == l3Evidence.sourceId || it.sourceId == audioRef || it.text == transcriptSegment
                        }
                        val lowConfidenceCount = audioSegments.count { it.lowConfidence }
                        if (lowConfidenceCount > 0) {
                            Spacer(Modifier.height(Dimens.xs))
                            Text(s.evidenceLowConfidence(lowConfidenceCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        val relatedKnowledgeCount = ui.l3Pipeline.knowledgePoints.count { l3Evidence.id in it.sourceEvidenceIds }
                        val relatedWrongCount = ui.l3Pipeline.wrongBook.count { l3Evidence.id in it.evidenceIds }
                        val relatedReviewCount = ui.l3Pipeline.reviewQueue.count { item ->
                            item.evidenceId == l3Evidence.id || ui.l3Pipeline.knowledgePoints.firstOrNull { it.id == item.knowledgePointId }?.sourceEvidenceIds?.contains(l3Evidence.id) == true
                        }
                        Spacer(Modifier.height(Dimens.xs))
                        Text(s.evidenceRelatedCounts(relatedKnowledgeCount, relatedWrongCount, relatedReviewCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(s.evidencePlaybackPending, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (l3Asset == null && l3Evidence.assetId != null) {
                    ClassMateCard {
                        Text(s.evidenceAssetMissing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }

                val linkedKnowledge = ui.l3Pipeline.knowledgePoints.filter { l3Evidence.id in it.sourceEvidenceIds }
                val linkedQuestions = ui.l3Pipeline.questions.filter { l3Evidence.id in it.evidenceIds }
                if (linkedKnowledge.isNotEmpty()) {
                    // P0-5: lead with the related knowledge — what this evidence supports — for learning value.
                    ClassMateCard {
                        Text(s.evidenceRelatedKnowledgeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(Dimens.xs))
                        Text(s.evidenceRelatedKnowledgeHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        linkedKnowledge.take(5).forEach { kp ->
                            Spacer(Modifier.height(Dimens.s))
                            Text(kp.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            if (kp.explanation.isNotBlank()) {
                                Text(kp.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (linkedQuestions.isNotEmpty()) {
                    ClassMateCard {
                        Text(s.evidenceRelatedQuizTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                s.evidenceRetraceUnavailable
            } else {
                s.evidenceNoKnowledgeSelected
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
                Text(s.evidenceWhyPointTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Text(
                    s.evidenceWhyPointBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Group evidence by segment and render the source with the cited spans highlighted.
            val bySegment = kp.evidence.groupBy { it.sourceSegmentId }
            bySegment.forEach { (segmentId, spans) ->
                val segment = session.segment(segmentId) ?: return@forEach
                ClassMateCard {
                    Text(s.evidenceSegmentOriginal(segment.index), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(Dimens.s))
                    HighlightedSegmentText(
                        text = segment.text,
                        highlights = spans.map { it.startChar..(it.endChar - 1) },
                    )
                }
            }

            if (kp.relatedPointIds.isNotEmpty()) {
                ClassMateCard {
                    Text(s.evidenceRelatedKnowledgeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    text = s.evidenceWrong,
                    onClick = {
                        viewModel.submitFeedback(FeedbackType.EVIDENCE_WRONG, FeedbackTargetKind.KNOWLEDGE_POINT, kp.id)
                    },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = s.mastered,
                    onClick = {
                        viewModel.submitFeedback(FeedbackType.ALREADY_MASTERED, FeedbackTargetKind.KNOWLEDGE_POINT, kp.id)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun l3SourceTitle(sourceType: L3SourceType, s: Strings): String = when (sourceType) {
    L3SourceType.TEXT -> s.evidenceSourceText
    L3SourceType.OCR_IMAGE -> s.evidenceSourceOcrImage
    L3SourceType.DOCUMENT -> s.evidenceSourceDocument
    L3SourceType.AUDIO_TRANSCRIPT -> s.evidenceSourceAudioTranscript
    L3SourceType.MANUAL_TRANSCRIPT -> s.evidenceSourceManualTranscript
    L3SourceType.RECORDING_ARTIFACT -> s.evidenceSourceRecording
    L3SourceType.QUESTION_BANK -> s.evidenceSourceQuestionBank
    L3SourceType.WEB -> s.evidenceSourceWeb
}

/** A user-readable location line from structured fields only (page / paragraph / time) — no raw refs. */
private fun evidenceLocation(evidence: Evidence, asset: EvidenceAsset?, s: Strings): String {
    val parts = mutableListOf<String>()
    evidence.page?.let { parts += s.evidencePage(it) }
    evidence.blockIndex?.let { parts += s.evidenceBlock(it + 1) }
    val start = evidence.segmentStartMs ?: asset?.startMs
    val end = evidence.segmentEndMs ?: asset?.endMs
    if (start != null || end != null) {
        val range = listOfNotNull(start?.let { "${it / 1000}s" }, end?.let { "${it / 1000}s" }).joinToString("–")
        if (range.isNotBlank()) parts += s.evidenceTime(range)
    }
    return if (parts.isEmpty()) "" else s.evidencePosition(parts.joinToString(" · "))
}
