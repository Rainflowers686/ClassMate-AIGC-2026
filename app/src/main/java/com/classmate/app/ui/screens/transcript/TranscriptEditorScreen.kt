package com.classmate.app.ui.screens.transcript

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.components.ChipTone
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.EmptyStateCard
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.StatusChip
import com.classmate.app.ui.design.Dimens
import com.classmate.core.transcript.TranscriptClock
import com.classmate.core.transcript.TranscriptLabels
import com.classmate.core.transcript.TranscriptSegmentDraft

@Composable
fun TranscriptEditorScreen(viewModel: AppViewModel) {
    val draft = viewModel.ui.transcriptDraft

    ClassMateScaffold(title = "转写编辑器", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            if (draft == null) {
                EmptyStateCard(title = "没有可编辑的转写稿", message = "请先返回上一步解析字幕或转写稿。")
                return@Column
            }

            ClassMateCard {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                ) {
                    StatusChip(TranscriptLabels.of(draft.sourceType), tone = ChipTone.INFO)
                    StatusChip("段落 ${draft.segments.size}", tone = ChipTone.NEUTRAL)
                    StatusChip("有时间戳 ${draft.timestampedCount}", tone = ChipTone.NEUTRAL)
                }
                Spacer(Modifier.height(Dimens.s))
                Text(
                    "可编辑文本、修改说话人、调整或清空时间、删除、合并相邻段落与上下移动。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    SecondaryButton("新增段落", onClick = { viewModel.addTranscriptSegment() }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        text = "保存进资料篮",
                        onClick = {
                            viewModel.saveTranscriptToTray()
                            viewModel.navigateTo(Screen.IMPORT_TRAY)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            draft.segments.forEachIndexed { index, seg ->
                SegmentEditorCard(viewModel, seg, index + 1, draft.segments.size)
            }
        }
    }
}

@Composable
private fun SegmentEditorCard(viewModel: AppViewModel, seg: TranscriptSegmentDraft, ordinal: Int, total: Int) {
    // Local clock text seeded once per segment id, so VM-side parsing never fights the keyboard.
    var startText by remember(seg.id) { mutableStateOf(seg.startMs?.let { TranscriptClock.format(it) } ?: "") }
    var endText by remember(seg.id) { mutableStateOf(seg.endMs?.let { TranscriptClock.format(it) } ?: "") }

    ClassMateCard {
        Text("段落 $ordinal / $total", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Dimens.xs))
        SpeakerSelector(selected = seg.speaker, onSelect = { viewModel.setTranscriptSpeaker(seg.id, it) })
        Spacer(Modifier.height(Dimens.s))
        OutlinedTextField(
            value = seg.text,
            onValueChange = { viewModel.editTranscriptSegmentText(seg.id, it) },
            label = { Text("文本") },
            minLines = 1,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.s))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it; viewModel.setTranscriptSegmentStart(seg.id, it) },
                label = { Text("开始") },
                singleLine = true,
                modifier = Modifier.width(116.dp),
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = endText,
                onValueChange = { endText = it; viewModel.setTranscriptSegmentEnd(seg.id, it) },
                label = { Text("结束") },
                singleLine = true,
                modifier = Modifier.width(116.dp),
                shape = MaterialTheme.shapes.medium,
            )
            SecondaryButton(
                text = "清空时间",
                onClick = { startText = ""; endText = ""; viewModel.clearTranscriptSegmentTime(seg.id) },
            )
        }
        Spacer(Modifier.height(Dimens.s))
        // Horizontal action area (never a vertical button stack); scrolls on narrow screens.
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            SecondaryButton("上移", onClick = { viewModel.moveTranscriptSegment(seg.id, up = true) }, enabled = ordinal > 1)
            SecondaryButton("下移", onClick = { viewModel.moveTranscriptSegment(seg.id, up = false) }, enabled = ordinal < total)
            SecondaryButton("合并下一段", onClick = { viewModel.mergeTranscriptSegmentDown(seg.id) }, enabled = ordinal < total)
            SecondaryButton("删除", onClick = { viewModel.deleteTranscriptSegment(seg.id) })
        }
    }
}
