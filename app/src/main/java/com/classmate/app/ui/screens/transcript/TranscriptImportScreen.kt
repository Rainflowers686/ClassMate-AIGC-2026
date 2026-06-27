package com.classmate.app.ui.screens.transcript

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.glossary.CourseGlossary
import com.classmate.app.importing.SelectedLocalFileMetadata
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import com.classmate.app.ui.components.AiProcessingDialog
import com.classmate.app.ui.components.ChipTone
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.StatusChip
import com.classmate.app.ui.design.Dimens
import com.classmate.core.transcript.TranscriptClock
import com.classmate.core.transcript.TranscriptLabels
import com.classmate.core.transcript.TranscriptSourceType
import com.classmate.core.transcript.zhLabelOrNull

@Composable
fun TranscriptImportScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val ui = viewModel.ui
    val draft = ui.transcriptDraft

    val textLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            viewModel.recordTranscriptFileMetadata(readMetadata(context, uri, "字幕/转写文本"))
            viewModel.updateTranscriptPaste(content)
        }
    }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val metadata = readMetadata(context, uri, "本地媒体文件")
            viewModel.recordTranscriptFileMetadata(metadata)
            if (metadata.mimeType.startsWith("audio/")) {
                val bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                if (bytes == null) {
                    viewModel.toast("无法读取音频文件，可粘贴转写文本继续。")
                } else {
                    viewModel.transcribeAudioFile(bytes, metadata.fileName, metadata.mimeType)
                }
            } else {
                viewModel.toast("视频文件当前只记录元数据，请导入字幕或粘贴转写文本。")
            }
            return@rememberLauncherForActivityResult
        }
        if (uri != null) viewModel.recordTranscriptFileMetadata(readMetadata(context, uri, "本地媒体文件"))
    }

    ClassMateScaffold(title = "课堂转写 / 音视频字幕", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            AiProcessingDialog(
                state = ui.aiProcessing,
                onCancel = viewModel::hideAiProcessing,
                onRetry = viewModel::retryCurrentCapture,
                onContinueManual = viewModel::hideAiProcessing,
            )
            ClassMateCard {
                Text("导入字幕 / 转写稿", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xs))
                listOf(
                    "官方 ASR 按配置启用；未配置或不可用时，可以粘贴转写文本继续学习。",
                    "暂不支持自动读取视频内嵌字幕，请导入字幕文件（SRT/VTT/TXT）或粘贴字幕内容；视频文件仅记录文件名/类型/大小。",
                    "不会爬取第三方平台内容，请粘贴你有权使用的字幕或转写稿。",
                    "确认后的转写草稿会进入课程分析；手动粘贴不会被标记为 ASR 结果。",
                ).forEach {
                    Text("· $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Dimens.xxs))
                }
            }

            // --- source type selection (horizontal, never per-character wrapping chips) ---
            ClassMateCard {
                Text("资料类型", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.s),
                ) {
                    importableTypes.forEach { type ->
                        SourceChip(
                            text = TranscriptLabels.of(type),
                            selected = ui.transcriptSourceType == type,
                            onClick = { viewModel.selectTranscriptSourceType(type) },
                        )
                    }
                }
                ui.transcriptFileMetadata?.let {
                    Spacer(Modifier.height(Dimens.s))
                    Text("已记录文件：${it.fileName} · ${it.mimeType.ifBlank { "类型未知" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(Dimens.s))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    SecondaryButton(
                        text = "选择音视频（只记录元数据）",
                        onClick = { mediaLauncher.launch(arrayOf("audio/*", "video/*")) },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "导入字幕/文本文件",
                        onClick = { textLauncher.launch(arrayOf("text/plain", "application/x-subrip", "text/vtt", "text/*", "*/*")) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OutlinedTextField(
                value = ui.transcriptPasteDraft,
                onValueChange = viewModel::updateTranscriptPaste,
                label = { Text("粘贴 SRT / VTT / TXT 字幕或转写稿") },
                minLines = 6,
                maxLines = 14,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )

            // --- glossary nudge (display-only, never evidence, bounded) ---
            // Honestly separate REAL terms (matched in the current transcript) from the built-in DEMO pack.
            val hints = viewModel.transcriptGlossaryHints()
            val hasRealInput = ui.transcriptPasteDraft.isNotBlank() || ui.transcriptDraft != null
            ClassMateCard {
                Text("术语表", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.xxs))
                when {
                    hints.isNotEmpty() -> Text(
                        "从当前转写稿提取（仅提示，不改写原文）：${hints.joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    hasRealInput -> Text(
                        "已粘贴转写稿，解析后会从中提取术语。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Text(
                        "暂无真实转写内容，导入字幕或粘贴转写稿后将从中提取术语。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Dimens.xxs))
                Text(
                    "内置术语参考（演示数据）：${ui.selectedSubject} · ${CourseGlossary.countFor(ui.selectedSubject)} 个",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrimaryButton(
                text = "解析转写稿",
                onClick = { viewModel.parseTranscript() },
                enabled = ui.transcriptPasteDraft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            SecondaryButton(
                text = "生成手动转写草稿",
                onClick = { viewModel.createManualTranscriptDraftFromPaste() },
                enabled = ui.transcriptPasteDraft.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            ui.audioCaptureMessage?.takeIf { it.isNotBlank() }?.let {
                ClassMateCard {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (ui.transcriptParseWarnings.isNotEmpty()) {
                ClassMateCard {
                    Text("解析提示", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(Dimens.xxs))
                    ui.transcriptParseWarnings.forEach {
                        Text("· $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (draft != null && draft.segments.isNotEmpty()) {
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
                    draft.segments.take(4).forEach { seg ->
                        val time = seg.startMs?.let { TranscriptClock.formatRange(it, seg.endMs) + " · " } ?: ""
                        val sp = seg.speaker.zhLabelOrNull()?.let { "[$it] " } ?: ""
                        Text("$time$sp${seg.text}", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(Dimens.xxs))
                    }
                    if (draft.segments.size > 4) {
                        Text("… 共 ${draft.segments.size} 段，可进入编辑器调整。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(Dimens.s))
                    PrimaryButton(
                        text = "转写编辑",
                        onClick = { viewModel.navigateTo(Screen.TRANSCRIPT_EDITOR) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private val importableTypes = listOf(
    TranscriptSourceType.AUDIO_TRANSCRIPT,
    TranscriptSourceType.VIDEO_SUBTITLE,
    TranscriptSourceType.SRT_FILE,
    TranscriptSourceType.VTT_FILE,
    TranscriptSourceType.PASTED_TRANSCRIPT,
)

private fun readMetadata(context: Context, uri: Uri, entryTitle: String): SelectedLocalFileMetadata {
    var name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    var size: Long? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) name = cursor.getString(nameIndex).orEmpty().ifBlank { name }
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex).takeIf { it >= 0L }
        }
    }
    return SelectedLocalFileMetadata(entryTitle, name.ifBlank { "未命名文件" }, context.contentResolver.getType(uri).orEmpty(), size)
}
