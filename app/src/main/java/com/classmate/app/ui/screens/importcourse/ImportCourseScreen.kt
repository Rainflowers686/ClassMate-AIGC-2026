package com.classmate.app.ui.screens.importcourse

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontStyle
import com.classmate.app.asr.AndroidSpeechRecognizerClient
import com.classmate.app.asr.AsrEventListener
import com.classmate.app.asr.AsrSession
import com.classmate.app.asr.AsrState
import com.classmate.app.asr.SpeechRecognitionSettingsTargets
import com.classmate.app.l3.L3RecordingStatus
import com.classmate.app.l3.ClassroomRecordingRecord
import com.classmate.app.l3.DialectMode
import com.classmate.app.l3.InputFileKind
import com.classmate.app.exporting.ExportIntentFactory
import com.classmate.app.glossary.CourseGlossary
import com.classmate.app.ondevice.BitmapToRgb
import com.classmate.app.ondevice.OnDevicePermissions
import com.classmate.app.importing.MultimodalEntryId
import com.classmate.app.importing.MultimodalImportCatalog
import com.classmate.app.importing.MultimodalImportEntry
import com.classmate.app.importing.OcrImportAssembler
import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.OcrImportFileMeta
import com.classmate.app.importing.OcrImportKind
import com.classmate.app.importing.OcrImportStatus
import com.classmate.app.importing.isLowQuality
import com.classmate.app.importing.SelectedLocalFileMetadata
import com.classmate.app.sample.SampleLessonLibrary
import com.classmate.app.state.AppViewModel
import com.classmate.app.state.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import com.classmate.app.ui.components.ActionTile
import com.classmate.app.ui.components.AiProcessingDialog
import com.classmate.app.ui.components.ClassMateCard
import com.classmate.app.ui.components.HelpHint
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.CompactSectionHeader
import com.classmate.app.ui.components.MaterialTrayItem
import com.classmate.app.ui.components.PageHero
import com.classmate.app.ui.components.ActionButtonRow
import com.classmate.app.ui.components.PrimaryButton
import com.classmate.app.ui.components.SecondaryButton
import com.classmate.app.ui.components.SourceBadge
import com.classmate.app.ui.components.StatusChip
import com.classmate.app.ui.components.ChipTone
import com.classmate.app.ui.product.GroupedList
import com.classmate.app.ui.product.ProductCanvas
import com.classmate.app.ui.product.ProductCollapse
import com.classmate.app.ui.product.ProductHero
import com.classmate.app.ui.product.ProductRow
import com.classmate.app.ui.product.ProductScaffold
import com.classmate.app.ui.product.ProductSectionTitle
import com.classmate.app.ui.product.ProductSpace
import com.classmate.app.ui.product.QuietCard
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.i18n.Strings
import com.classmate.app.ui.i18n.appStrings
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.provider.AnalysisIntensity
import com.classmate.core.transcript.TranscriptLabels
import java.io.ByteArrayOutputStream

@Composable
fun ImportCourseScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val ui = viewModel.ui
    val pendingFileSource = remember { mutableStateOf(ImportSourceType.TXT_FILE) }
    // P0-4: multi-file selection — every chosen file is added to the same "本课资料库" (the superhub),
    // not overwriting earlier materials; a single unreadable file is skipped, not the whole batch.
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var acceptedAny = false
            var skipped = 0
            uris.forEach { uri ->
                val name = readDisplayName(context, uri)
                val mimeType = context.contentResolver.getType(uri).orEmpty()
                val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                if (bytes == null) {
                    skipped++
                } else if (viewModel.importSuperhubFile(bytes, name, mimeType)) {
                    acceptedAny = true
                }
            }
            if (skipped > 0) viewModel.toast("有 $skipped 个文件无法读取，已跳过其余已加入资料库。")
            if (acceptedAny) viewModel.navigateTo(Screen.IMPORT_TRAY)
        }
    }

    val perms = remember { OnDevicePermissions(context) }
    // Photo Picker (no permission needed) → decode → on-device multimodal draft.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val batchId = viewModel.beginImageOcrBatch("图片学习输入", uris.size)
            uris.forEachIndexed { index, uri ->
                handlePickedImageBatch(context, uri, perms, viewModel, batchId, index + 1, uris.size)
            }
        }
    }
    // Camera thumbnail (uses the declared CAMERA permission) → decode → on-device multimodal draft.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.runImageDraft(
                BitmapToRgb.toRgbScaled(bitmap),
                perms.allFilesAccessGranted(),
                originalWidth = bitmap.width,
                originalHeight = bitmap.height,
                encodedImageBytes = encodeJpegBytes(bitmap),
            )
        } else {
            viewModel.toast("未拍摄到图片。")
        }
    }
    // P0-1: live speech-to-text engine for INLINE "record + transcribe". Uses the system recognizer as
    // the non-official fallback (no raw audio kept by the recognizer itself); honest states drive the UI.
    val asrEngine = remember { AndroidSpeechRecognizerClient(context) }
    val asrListener = remember {
        object : AsrEventListener {
            override fun onListening() = viewModel.asrOnListening()
            override fun onPartial(text: String) = viewModel.asrOnPartial(text)
            override fun onFinal(text: String, confidence: Double?) = viewModel.asrOnFinal(text, confidence)
            override fun onEndOfSpeech() = viewModel.asrOnEndOfSpeech()
            override fun onError(message: String) { viewModel.asrOnError(message); asrEngine.stop() }
        }
    }
    DisposableEffect(Unit) { onDispose { asrEngine.destroy() } }
    fun beginRecordAndTranscribe(granted: Boolean) {
        val state = viewModel.startRecordingWithTranscription(asrEngine.isAvailable(), granted)
        if (state == AsrState.LISTENING) asrEngine.start(asrListener)
    }
    val recordingPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        beginRecordAndTranscribe(granted)
    }
    fun startRecordingWithPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) beginRecordAndTranscribe(true) else recordingPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    fun stopRecordAndTranscribe() {
        asrEngine.stop()
        viewModel.stopRecordingWithTranscription()
    }
    fun cancelRecordAndTranscribe() {
        asrEngine.stop()
        viewModel.cancelRecordingWithTranscription()
    }

    AiProcessingDialog(
        state = ui.aiProcessing,
        onCancel = viewModel::hideAiProcessing,
        onRetry = viewModel::retryCurrentCapture,
        onContinueManual = viewModel::hideAiProcessing,
    )

    ProductCanvas {
        ProductScaffold(contextLabel = "资料", onBack = { viewModel.goBackOrHome() }) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ProductSpace.gutter)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(ProductSpace.block),
            ) {
                Spacer(Modifier.height(ProductSpace.tight))
                ProductHero(
                    overline = "输入工作台",
                    title = "把课堂内容放进来",
                    subtitle = "图片、拍照、文本会先形成可编辑学习草稿，用户确认后进入知识结构大纲。",
                )
                // AI 来源：云端优先 · 端侧兜底 — 未配置官方服务不等于没有 AI；端侧蓝心仍可生成学习草稿。
                val captureStatus = viewModel.captureConfigStatus()
                Text(
                    "AI 来源：云端优先 · 端侧兜底。官方 OCR / ASR ${captureStatus.labelZh()}；未配置时端侧蓝心仍可生成图片学习草稿，或粘贴转写文本，用户确认后生成知识结构大纲。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // CORE WORKSPACE — active draft floats to the very top as a document; else the primary inputs.
                if (ui.imageDraftActive) {
                    ProductSectionTitle("当前草稿")
                    ImageDraftCard(viewModel)
                } else {
                    ProductSectionTitle("学习输入")
                    GroupedList(
                        rows = listOf(
                            ProductRow("图片学习输入", "课件截图 / 板书 / 题目 · 官方 OCR 按配置启用，端侧蓝心生成可编辑草稿", Icons.Filled.Add, MaterialTheme.colorScheme.primary, onClick = {
                                viewModel.beginImageDraft("图片学习输入")
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }),
                            ProductRow("拍照学习输入", "现场拍摄板书 / 纸质资料 / 习题 · 用户确认后进入课程分析", Icons.Filled.PlayArrow, MaterialTheme.colorScheme.secondary, onClick = {
                                viewModel.beginImageDraft("拍照学习输入")
                                cameraLauncher.launch(null)
                            }),
                            ProductRow("文本粘贴", "课堂笔记 / 讲义文字，直接进入资料篮", Icons.Filled.Edit, MaterialTheme.colorScheme.tertiary, onClick = {
                                viewModel.navigateTo(Screen.IMPORT_TRAY)
                            }),
                        ),
                    )
                }

                // SECONDARY — folded so the workspace stays calm.
                ProductCollapse(title = "更多导入方式") {
                    GroupedList(
                        rows = listOf(
                            ProductRow("课堂转写 / 音视频字幕", "SRT / VTT 或转写稿；不解析音视频本体、不录音、不爬取平台", Icons.Filled.List, onClick = {
                                viewModel.navigateTo(Screen.TRANSCRIPT_IMPORT)
                            }),
                            ProductRow("音频转写（官方 ASR）", "课堂录音转文字草稿；当前未配置时可手动粘贴转写文本", Icons.Filled.PlayArrow, onClick = {
                                viewModel.navigateTo(Screen.TRANSCRIPT_IMPORT)
                            }),
                            ProductRow(".txt / .md 文件", "从系统文件选择器读取文本", Icons.Filled.Add, onClick = {
                                pendingFileSource.value = ImportSourceType.TXT_FILE
                                fileLauncher.launch(arrayOf("text/plain", "text/markdown", "text/*"))
                            }),
                            ProductRow("PDF / Word / PPT / Excel", "DOCX/XLSX/PPTX 轻量抽取；PDF 记录 artifact 并提示手动文本 fallback", Icons.Filled.List, onClick = {
                                fileLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "text/csv",
                                    ),
                                )
                            }),
                            ProductRow("音频文件（ASR Long seam）", "保存 artifact；未配置官方 ASR Long 时粘贴转写文本继续", Icons.Filled.PlayArrow, onClick = {
                                fileLauncher.launch(arrayOf("audio/*"))
                            }),
                            ProductRow("课件 / 板书 / PDF OCR", "官方 OCR 未配置时可粘贴识别文字；文件不上传、不爬取平台", Icons.Filled.Edit, onClick = {
                                viewModel.navigateTo(Screen.IMPORT_TRAY)
                            }),
                            ProductRow("示例课堂", "加载内置长文本，适合真机完整链路演示", Icons.Filled.Star, onClick = {
                                SampleLessonLibrary.lessons.firstOrNull()?.let { viewModel.loadSampleLesson(it.id) }
                                viewModel.navigateTo(Screen.IMPORT_TRAY)
                            }),
                            ProductRow("L3 演示包", "加载课堂材料和题库模板，用于 2–3 分钟闭环演示", Icons.Filled.CheckCircle, onClick = {
                                viewModel.loadL3DemoSeed()
                                viewModel.navigateTo(Screen.IMPORT_TRAY)
                            }),
                        ),
                    )
                }
                DocumentEvidenceIntakeCard(viewModel)
                ClassroomRecordingCard(
                    viewModel,
                    onStartRecording = { startRecordingWithPermission() },
                    onStopRecording = { stopRecordAndTranscribe() },
                    onCancelRecording = { cancelRecordAndTranscribe() },
                )
            }
        }
    }
}

@Composable
private fun DocumentEvidenceIntakeCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val documentArtifacts = ui.inputArtifacts.filter {
        it.kind in setOf(InputFileKind.PDF, InputFileKind.DOCX, InputFileKind.PPTX, InputFileKind.XLSX, InputFileKind.TXT, InputFileKind.MARKDOWN, InputFileKind.CSV)
    }
    if (documentArtifacts.isEmpty() && ui.pdfPages.isEmpty()) return

    val latestPage = ui.pdfPages.lastOrNull()
    var manualPageText by remember(latestPage?.id) { mutableStateOf("") }
    val documentEvidence = ui.l3Pipeline.evidence.firstOrNull { it.sourceType.name == "DOCUMENT" }

    QuietCard {
        Text("文档证据与 PDF 页文本", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "TXT/Markdown/CSV 会直接进入学习闭环；DOCX/PPTX/XLSX 为尽力抽取；PDF 可手动添加页文本并保留页码证据。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        documentArtifacts.takeLast(3).forEach { artifact ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "已记录文档：${artifact.fileName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        latestPage?.let { page ->
            Spacer(Modifier.height(Dimens.s))
            StatusChip("PDF 第 ${page.pageNumber} 页", tone = ChipTone.INFO)
            Spacer(Modifier.height(Dimens.s))
            OutlinedTextField(
                value = manualPageText,
                onValueChange = { manualPageText = it },
                label = { Text("粘贴 PDF 第 ${page.pageNumber} 页文本") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Dimens.s))
            PrimaryButton(
                text = "添加该页文本并生成学习闭环",
                onClick = {
                    if (viewModel.addManualPdfPageText(page.artifactId, page.pageNumber, manualPageText)) {
                        manualPageText = ""
                    }
                },
                enabled = manualPageText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (documentEvidence != null && viewModel.hasRetraceableEvidence(documentEvidence.id)) {
            Spacer(Modifier.height(Dimens.s))
            SecondaryButton(
                text = "查看文档证据",
                onClick = { viewModel.openEvidenceById(documentEvidence.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ClassroomRecordingCard(
    viewModel: AppViewModel,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
) {
    val ui = viewModel.ui
    val active = ui.currentRecording?.status == L3RecordingStatus.RECORDING
    val latestAsrJob = ui.asrLongJobs.lastOrNull()
    var manualTranscript by remember(latestAsrJob?.id) { mutableStateOf("") }
    val audioEvidence = ui.l3Pipeline.evidence.firstOrNull {
        it.sourceType.name == "AUDIO_TRANSCRIPT" || it.sourceType.name == "MANUAL_TRANSCRIPT" || it.audioRef.isNotBlank()
    }
    QuietCard {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("课堂录音记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            HelpHint(
                title = appStrings(ui.language).helpRecordingTitle,
                points = appStrings(ui.language).helpRecordingPoints,
                dismiss = appStrings(ui.language).helpDismiss,
            )
        }
        Spacer(Modifier.height(Dimens.xs))
        Text("录音后可导出，或粘贴转写继续。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // P0-2: after stopping, the honest capture message ("录音已保存，转写暂不可用 …" when system speech
        // recognition is missing) is shown here too, not just on the transcript screen.
        ui.audioCaptureMessage?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(Dimens.xs))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Dimens.s))
        Text("课堂语音模式", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
        ) {
            DialectModeButton(
                label = "普通课堂",
                selected = ui.audioDialectMode == DialectMode.STANDARD_MANDARIN,
                onClick = { viewModel.setAudioDialectMode(DialectMode.STANDARD_MANDARIN) },
            )
            DialectModeButton(
                label = "口音/方言增强",
                selected = ui.audioDialectMode == DialectMode.DIALECT_OR_ACCENT_ENHANCED,
                onClick = { viewModel.setAudioDialectMode(DialectMode.DIALECT_OR_ACCENT_ENHANCED) },
            )
            DialectModeButton(
                label = "多人课堂/混合口音",
                selected = ui.audioDialectMode == DialectMode.CLASSROOM_MIXED_SPEAKERS,
                onClick = { viewModel.setAudioDialectMode(DialectMode.CLASSROOM_MIXED_SPEAKERS) },
            )
        }
        Spacer(Modifier.height(Dimens.s))
        val recordingContext = LocalContext.current
        ui.currentRecording?.let {
            StatusChip("录音中", tone = ChipTone.PRIMARY)
            Spacer(Modifier.height(Dimens.xs))
            Text(it.title, style = MaterialTheme.typography.bodyMedium)
        }
        ui.recordingRecords.takeLast(3).reversed().forEach { record ->
            Spacer(Modifier.height(Dimens.s))
            RecordingRecordRow(
                record = record,
                onShare = {
                    val file = recordingFile(recordingContext, record)
                    if (file != null && file.exists() && file.length() > 0L) {
                        runCatching { recordingContext.startActivity(ExportIntentFactory.shareAudioFileChooser(recordingContext, file)) }
                            .onFailure { viewModel.toast("无法导出录音文件。") }
                    } else {
                        viewModel.toast("录音文件不存在，无法导出。")
                    }
                },
                onDelete = {
                    recordingFile(recordingContext, record)?.let { f -> runCatching { f.delete() } }
                    viewModel.removeRecordingRecord(record.id)
                },
            )
        }
        latestAsrJob?.let { job ->
            Spacer(Modifier.height(Dimens.s))
            OutlinedTextField(
                value = manualTranscript,
                onValueChange = { manualTranscript = it },
                label = { Text("粘贴 / 编辑这段录音的转写文本") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Dimens.s))
            PrimaryButton(
                text = "确认转写",
                onClick = {
                    if (viewModel.applyAsrLongTranscript(job.id, manualTranscript)) {
                        manualTranscript = ""
                    }
                },
                enabled = manualTranscript.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "官方 ASR 未配置时，这条手动转写会进入同一 L3 pipeline，并保留录音文件、时间段和转写片段证据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (audioEvidence != null && viewModel.hasRetraceableEvidence(audioEvidence.id)) {
            Spacer(Modifier.height(Dimens.s))
            SecondaryButton(
                text = "查看音频 / 转写证据",
                onClick = { viewModel.openEvidenceById(audioEvidence.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = "转写编辑",
            onClick = { viewModel.navigateTo(Screen.TRANSCRIPT_IMPORT) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        if (active) {
            // Inline live speech-to-text while recording (P0-1): partial + confirmed segments + honest state.
            LiveTranscriptPanel(ui.asrSession)
            if (ui.asrSession.state == AsrState.UNSUPPORTED) {
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = "打开语音设置",
                    onClick = {
                        openSpeechRecognitionSettings(recordingContext) {
                            viewModel.toast("无法打开系统语音设置，请手动进入系统设置检查语音服务。")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Dimens.s))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                PrimaryButton("停止并保存", onClick = onStopRecording, modifier = Modifier.weight(1f))
                SecondaryButton("取消", onClick = onCancelRecording, modifier = Modifier.weight(1f))
            }
        } else {
            if (ui.asrSession.state == AsrState.UNSUPPORTED) {
                Text(
                    SpeechRecognitionSettingsTargets.unavailableGuidance(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
                SecondaryButton(
                    text = "打开语音设置",
                    onClick = {
                        openSpeechRecognitionSettings(recordingContext) {
                            viewModel.toast("无法打开系统语音设置，请手动进入系统设置检查语音服务。")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Dimens.s))
            }
            PrimaryButton("开始录音并实时转写", onClick = onStartRecording, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun openSpeechRecognitionSettings(context: Context, onFailed: () -> Unit) {
    for (target in SpeechRecognitionSettingsTargets.ordered()) {
        val launched = runCatching {
            context.startActivity(Intent(target.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
        if (launched) return
    }
    onFailed()
}

/** Inline live transcript view for the recording card: honest state + confirmed segments + interim text. */
@Composable
private fun LiveTranscriptPanel(asr: AsrSession) {
    val cs = MaterialTheme.colorScheme
    val stateLabel = when (asr.state) {
        AsrState.LISTENING -> "正在听写…（系统语音识别）"
        AsrState.PROCESSING -> "识别中…"
        AsrState.ERROR -> "识别失败，可手动补充或导入转写稿"
        AsrState.UNSUPPORTED -> "本机语音识别不可用，录音继续；可手动粘贴或导入转写稿"
        AsrState.PERMISSION_REQUIRED -> "未授权麦克风，录音继续；可手动转写"
        else -> "录音中"
    }
    Spacer(Modifier.height(Dimens.s))
    Text(
        "实时转写 · $stateLabel",
        style = MaterialTheme.typography.labelLarge,
        color = if (asr.state == AsrState.ERROR) cs.error else cs.primary,
    )
    if (asr.segments.isNotEmpty()) {
        Spacer(Modifier.height(Dimens.xs))
        Text("已识别 ${asr.segments.size} 段", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        asr.segments.takeLast(3).forEach { seg ->
            Text("· ${seg.text}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
        }
    }
    if (asr.partialText.isNotBlank()) {
        Spacer(Modifier.height(Dimens.xxs))
        Text(
            "临时识别：${asr.partialText}",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
        )
    }
    if (asr.segments.isEmpty() && asr.partialText.isBlank() && asr.state == AsrState.LISTENING) {
        Spacer(Modifier.height(Dimens.xxs))
        Text("开始说话后，识别文本会实时显示在这里。", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun DialectModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        PrimaryButton(label, onClick = onClick)
    } else {
        SecondaryButton(label, onClick = onClick)
    }
}

/** Honest per-recording row: real file name, duration, size, Chinese status + export/delete entries. */
@Composable
private fun RecordingRecordRow(record: ClassroomRecordingRecord, onShare: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val saved = record.status == L3RecordingStatus.SAVED && record.fileSizeBytes > 0L
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(record.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusChip(recordingStatusZh(record.status), tone = if (saved) ChipTone.SUCCESS else ChipTone.WARNING)
        }
        if (saved) {
            Spacer(Modifier.height(Dimens.xxs))
            Text(
                "文件 ${record.artifactFileName ?: "录音.m4a"} · 时长 ${formatRecordingDuration(record.durationMs)} · 大小 ${formatRecordingSize(record.fileSizeBytes)} · 已保存在应用内（可导出）",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dimens.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                SecondaryButton("导出录音", onClick = onShare, modifier = Modifier.weight(1f))
                SecondaryButton("删除", onClick = onDelete, modifier = Modifier.weight(1f))
            }
        } else {
            Spacer(Modifier.height(Dimens.xxs))
            Text(
                record.message.ifBlank { "录音未成功保存，请重试，或导入字幕/转写稿继续。" },
                style = MaterialTheme.typography.bodySmall,
                color = cs.error,
            )
            Spacer(Modifier.height(Dimens.xs))
            SecondaryButton("删除记录", onClick = onDelete, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun recordingFile(context: android.content.Context, record: ClassroomRecordingRecord): java.io.File? {
    val name = record.artifactFileName ?: return null
    return java.io.File(java.io.File(context.filesDir, "classmate_recordings"), name)
}

private fun recordingStatusZh(status: L3RecordingStatus): String = when (status) {
    L3RecordingStatus.IDLE -> "未开始"
    L3RecordingStatus.RECORDING -> "录音中"
    L3RecordingStatus.SAVED -> "已保存"
    L3RecordingStatus.FAILED -> "保存失败"
}

private fun formatRecordingDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun formatRecordingSize(bytes: Long): String = when {
    bytes <= 0L -> "未知"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

/**
 * Phase C draft, restyled (Stage 9C) as a DRAFT DOCUMENT WORKSPACE: a header strip with the origin
 * + an honest status badge, a paper-like editing surface, and a clear two-level footer. The 8E logic
 * (multimodal → editable → confirm → courseText) is unchanged.
 */
@Composable
private fun ImageDraftCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    val cs = MaterialTheme.colorScheme
    QuietCard {
        // Header: origin + live status badge.
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ui.imageDraftOrigin ?: "图片学习输入", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("端侧多模态理解草稿 · 用户未确认", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            }
            SourceBadge(ui.imageDraftSource?.displayZh ?: if (ui.imageDraftManualMode) "手动输入" else "端侧蓝心")
        }
        ui.imageDraftMessage?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(Dimens.xs))
            Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        ui.imageDraftMeta?.let { meta ->
            Spacer(Modifier.height(Dimens.s))
            Text(meta, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
        if (ui.imageDraftImageRef.isNotBlank() || ui.imageDraftThumbnailRef.isNotBlank()) {
            Spacer(Modifier.height(Dimens.s))
            StatusChip(s.ocrAssetSaved, tone = ChipTone.SUCCESS)
            Spacer(Modifier.height(Dimens.xs))
            Text(
                s.ocrThumbnailRef(ui.imageDraftThumbnailRef.ifBlank { s.ocrThumbnailPending }),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Dimens.m))

        if (ui.imageDraftRunning) {
            // Working state inside the document frame.
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(Dimens.l), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Dimens.m))
                    Text(s.ocrUnderstanding, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            if (ui.imageDraftManualMode) {
                Text(
                    ui.imageDraftMessage ?: s.ocrManualFallback,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
            }
            val batch = ui.ocrImports.sortedBy { it.pageIndex ?: Int.MAX_VALUE }
            val isMultiImage = batch.size > 1 || batch.any { it.batchId.isNotBlank() }
            if (isMultiImage) {
                // P0-4: per-image segmented confirm — each image edited / retried / deleted on its own,
                // a failed image never blocks the others, low quality is called out, nothing enters the
                // learning loop until the user explicitly accepts the batch.
                batch.forEachIndexed { index, draft ->
                    OcrImageSegment(number = index + 1, draft = draft, viewModel = viewModel, s = s)
                    Spacer(Modifier.height(Dimens.s))
                }
                val okCount = batch.count { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() }
                val failedCount = batch.count { it.status == OcrImportStatus.FAILED }
                if (failedCount > 0) {
                    Text(
                        s.ocrFailedSummary(failedCount, okCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.error,
                    )
                    Spacer(Modifier.height(Dimens.s))
                }
                ActionButtonRow(
                    primaryText = s.ocrConfirmAddCourse,
                    onPrimary = { viewModel.confirmImageOcrBatch() },
                    secondaryText = s.ocrReselectImages,
                    onSecondary = { viewModel.cancelImageDraft() },
                    tertiaryText = s.cancel,
                    onTertiary = { viewModel.cancelImageDraft() },
                    primaryEnabled = okCount > 0,
                )
            } else {
                // Single image / on-device draft / manual: one editable surface.
                OutlinedTextField(
                    value = ui.imageDraftText,
                    onValueChange = viewModel::updateImageDraftText,
                    label = { Text(s.ocrEditableTextLabel) },
                    minLines = 5,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                if (ui.imageDraftText.trim().length < 20) {
                    Spacer(Modifier.height(Dimens.xs))
                    Text(
                        s.ocrShortReviewWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.error,
                    )
                }
                Spacer(Modifier.height(Dimens.m))
                ActionButtonRow(
                    primaryText = s.ocrConfirmAddCourse,
                    onPrimary = {
                        if (ui.ocrImports.any { it.batchId.isNotBlank() }) viewModel.confirmImageOcrBatch() else viewModel.confirmImageDraft()
                    },
                    secondaryText = s.cancel,
                    onSecondary = { viewModel.cancelImageDraft() },
                    primaryEnabled = ui.imageDraftText.isNotBlank(),
                )
            }
        }
        Spacer(Modifier.height(Dimens.s))
        Text(
            s.ocrConfirmExplanation,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
    }
}

/** P0-4: one image's OCR result inside the confirm page — its own status, editable text, low-quality note,
 *  and per-image delete. A failed image is contained here and never blocks the rest of the batch. */
@Composable
private fun OcrImageSegment(number: Int, draft: OcrImportDraft, viewModel: AppViewModel, s: Strings) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val (statusLabel, tone) = when {
        draft.status == OcrImportStatus.FAILED -> s.ocrStatusFailed to ChipTone.WARNING
        draft.isLowQuality() -> s.ocrStatusLowQuality to ChipTone.WARNING
        draft.status == OcrImportStatus.PENDING -> s.ocrStatusPending to ChipTone.INFO
        else -> s.ocrStatusOk to ChipTone.SUCCESS
    }
    androidx.compose.material3.Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(Dimens.m)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(s.ocrSegmentTitle(number, draft.fileMeta.safeDisplayLabel()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusChip(statusLabel, tone = tone)
            }
            OcrImagePreview(draft.fileMeta.fileName)
            when (draft.status) {
                OcrImportStatus.FAILED -> {
                    Spacer(Modifier.height(Dimens.xs))
                    Text(s.ocrFailure(draft.errorReason.ifBlank { s.ocrFailureFallback }), style = MaterialTheme.typography.bodySmall, color = cs.error)
                    // P0-1: a failed image is NOT a dead end — the user can type the text manually here
                    // (OCR unavailable / unconfigured on this device still lets the material be created).
                    Spacer(Modifier.height(Dimens.xs))
                    OutlinedTextField(
                        value = draft.pastedText,
                        onValueChange = { viewModel.updateOcrImportText(draft.id, it) },
                        label = { Text(s.ocrManualInputLabel(number)) },
                        minLines = 3,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    Spacer(Modifier.height(Dimens.xs))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                        SecondaryButton("重新 OCR", onClick = { retryOcrFromDraft(context, draft, viewModel) }, modifier = Modifier.weight(1f))
                        SecondaryButton(s.ocrDeleteImage, onClick = { viewModel.removeOcrImport(draft.id) }, modifier = Modifier.weight(1f))
                    }
                }
                OcrImportStatus.PENDING -> {
                    Spacer(Modifier.height(Dimens.xs))
                    Text(s.ocrRecognizing, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                OcrImportStatus.OK -> {
                    if (draft.isLowQuality()) {
                        Spacer(Modifier.height(Dimens.xs))
                        Text(s.ocrLowQualityMessage(draft.errorReason), style = MaterialTheme.typography.bodySmall, color = cs.error)
                    }
                    Spacer(Modifier.height(Dimens.xs))
                    OutlinedTextField(
                        value = draft.pastedText,
                        onValueChange = { viewModel.updateOcrImportText(draft.id, it) },
                        label = { Text(s.ocrSegmentTextLabel(number)) },
                        minLines = 3,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    )
                    Spacer(Modifier.height(Dimens.xs))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                        SecondaryButton("重新 OCR", onClick = { retryOcrFromDraft(context, draft, viewModel) }, modifier = Modifier.weight(1f))
                        SecondaryButton(s.ocrDeleteImage, onClick = { viewModel.removeOcrImport(draft.id) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun OcrImagePreview(imagePath: String) {
    if (imagePath.isBlank()) return
    val cs = MaterialTheme.colorScheme
    val bitmap = remember(imagePath) {
        runCatching { BitmapFactory.decodeFile(imagePath) }.getOrNull()
    }
    Spacer(Modifier.height(Dimens.s))
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "OCR 图片预览",
            modifier = Modifier.fillMaxWidth().height(150.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Text(
            "图片预览暂不可用，仍保留可编辑 OCR 文本。",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}

private fun retryOcrFromDraft(context: Context, draft: OcrImportDraft, viewModel: AppViewModel) {
    val file = java.io.File(draft.fileMeta.fileName)
    if (!file.exists() || file.length() <= 0L) {
        viewModel.toast("原图文件不可用，请重新选择图片或手动输入。")
        return
    }
    val bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    if (bitmap == null) {
        viewModel.toast("图片预览不可用，请重新选择图片或手动输入。")
        return
    }
    val page = draft.pageIndex ?: draft.fileMeta.pageIndex ?: 1
    val total = viewModel.ui.imageDraftBatchTotal.coerceAtLeast(viewModel.ui.ocrImports.size.coerceAtLeast(1))
    viewModel.ingestMultiImageOcr(
        BitmapToRgb.toRgbScaled(bitmap),
        OnDevicePermissions(context).allFilesAccessGranted(),
        originalWidth = bitmap.width,
        originalHeight = bitmap.height,
        encodedImageBytes = file.readBytes(),
        pageIndex = page,
        total = total,
        batchId = draft.batchId.ifBlank { viewModel.ui.imageDraftBatchId },
    )
}

/** Decode a picked image Uri to a bitmap (bounded) and run the on-device draft. Never crashes. */
private fun handlePickedImage(context: Context, uri: Uri, perms: OnDevicePermissions, viewModel: AppViewModel) {
    val imageBytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: ByteArray(0)
    val bitmap = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                // Software allocator so the bitmap supports getPixels (hardware bitmaps do not).
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
    if (bitmap == null) {
        viewModel.toast(appStrings(viewModel.ui.language).ocrDecodeFailedToast)
        viewModel.applyImageDraftResult(com.classmate.core.ondevice.OnDeviceImageDraftResult.Unavailable("DECODE_FAILED"))
        return
    }
    viewModel.runImageDraft(
        BitmapToRgb.toRgbScaled(bitmap),
        perms.allFilesAccessGranted(),
        originalWidth = bitmap.width,
        originalHeight = bitmap.height,
        encodedImageBytes = imageBytes,
    )
}

/** Decode one image inside a multi-image batch. A bad image becomes one FAILED draft, not a batch failure. */
private fun handlePickedImageBatch(
    context: Context,
    uri: Uri,
    perms: OnDevicePermissions,
    viewModel: AppViewModel,
    batchId: String,
    pageIndex: Int,
    total: Int,
) {
    val imageBytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: ByteArray(0)
    val bitmap = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
    if (bitmap == null) {
        viewModel.applyImageOcrBatchItem(
            com.classmate.app.importing.OcrImportDraft(
                id = "${batchId}_$pageIndex",
                kind = OcrImportKind.SLIDE_IMAGE,
                fileMeta = OcrImportFileMeta(
                    fileName = readDisplayName(context, uri).ifBlank { "image_$pageIndex" },
                    mimeType = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/*" },
                    sizeBytes = imageBytes.size.toLong().takeIf { it > 0L },
                    displayLabel = appStrings(viewModel.ui.language).ocrImageLabel(pageIndex),
                    pageIndex = pageIndex,
                ),
                pastedText = "",
                status = OcrImportStatus.FAILED,
                errorReason = appStrings(viewModel.ui.language).ocrDecodeFailedReason,
                batchId = batchId,
                pageIndex = pageIndex,
                blockIndex = pageIndex,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
            total,
        )
        return
    }
    viewModel.ingestMultiImageOcr(
        BitmapToRgb.toRgbScaled(bitmap),
        perms.allFilesAccessGranted(),
        originalWidth = bitmap.width,
        originalHeight = bitmap.height,
        encodedImageBytes = imageBytes,
        pageIndex = pageIndex,
        total = total,
        batchId = batchId,
    )
}

@Composable
fun MaterialTrayScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    var selectedOcrKind by remember { mutableStateOf(OcrImportKind.SLIDE_IMAGE) }
    var ocrDisplayLabel by remember { mutableStateOf("") }
    var ocrText by remember { mutableStateOf("") }
    var ocrFileMetadata by remember { mutableStateOf<SelectedLocalFileMetadata?>(null) }
    val context = LocalContext.current
    val metadataLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val entry = MultimodalImportCatalog.entries.firstOrNull { ocrKindForEntry(it.id) == selectedOcrKind }
            val metadata = readMetadata(context, uri, entry?.title ?: selectedOcrKind.displayName)
            viewModel.recordSelectedLocalFileMetadata(metadata)
            ocrFileMetadata = metadata
            if (ocrDisplayLabel.isBlank()) ocrDisplayLabel = metadata.fileName
        }
    }

    ClassMateScaffold(title = "资料篮", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            CompactSectionHeader("已加入资料", "资料会进入 MaterialBundle，再统一生成知识时间线。")
            if (ui.courseText.isNotBlank()) {
                MaterialTrayItem(
                    title = ui.courseTitle.ifBlank { "课堂文本" },
                    source = sourceLabel(ui.importSourceType),
                    meta = "${ui.courseText.count { !it.isWhitespace() }} 字",
                    onRemove = { viewModel.updateCourseText("") },
                )
            } else {
                Text("还没有文本资料。可以在下方粘贴课堂文本，或加入 OCR 资料。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ui.ocrImports.forEach { draft ->
                val statusText = when (draft.status) {
                    OcrImportStatus.PENDING -> "识别中"
                    OcrImportStatus.OK -> if (draft.errorReason.isBlank()) "已确认" else "建议检查"
                    OcrImportStatus.FAILED -> "需手动补充"
                }
                MaterialTrayItem(
                    title = draft.fileMeta.safeDisplayLabel(),
                    source = OcrImportAssembler.sourceLabel(draft.kind),
                    meta = "$statusText · ${draft.pastedText.count { !it.isWhitespace() }} 字",
                    onRemove = { viewModel.removeOcrImport(draft.id) },
                    imagePath = draft.fileMeta.fileName,
                )
            }
            ui.transcripts.forEach { transcript ->
                MaterialTrayItem(
                    title = transcript.displayLabel(),
                    source = TranscriptLabels.of(transcript.sourceType),
                    meta = "${transcript.segments.size} 段 · 时间戳 ${transcript.timestampedCount}",
                    onRemove = { viewModel.removeTranscript(transcript.id) },
                )
            }
            ActionTile(
                "添加课堂转写 / 字幕",
                "导入 SRT/VTT 或粘贴转写稿，进入转写编辑器后加入资料篮。",
                onClick = { viewModel.navigateTo(Screen.TRANSCRIPT_IMPORT) },
            )

            ClassMateCard {
                Text("粘贴 / 编辑课堂文本", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                OutlinedTextField(
                    value = ui.courseText,
                    onValueChange = viewModel::updateCourseText,
                    label = { Text("课堂文本 / 字幕 / 转写稿") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(Dimens.xs))
                val charCount = ui.courseText.length
                val longText = charCount > 4000
                Text(
                    if (longText) {
                        "$charCount 字 · 内容较长：将按段落分段整理，原文完整保留为证据；「深度思考」更适合长文本。"
                    } else {
                        "$charCount 字 · 原文完整保留为证据，不会被静默截断。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (longText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            QuestionBankImportCard(viewModel)

            OcrTrayEditor(
                selectedKind = selectedOcrKind,
                onKindSelected = { selectedOcrKind = it },
                displayLabel = ocrDisplayLabel,
                onDisplayLabelChange = { ocrDisplayLabel = it },
                ocrText = ocrText,
                onOcrTextChange = { ocrText = it },
                onPickMetadata = {
                    metadataLauncher.launch(arrayOf("image/*", "application/pdf"))
                },
                onAdd = {
                    if (viewModel.addOcrImport(selectedOcrKind, ocrDisplayLabel, ocrText, ocrFileMetadata)) {
                        ocrText = ""
                        ocrDisplayLabel = ""
                        ocrFileMetadata = null
                    }
                },
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                SecondaryButton("上一步", onClick = { viewModel.goBack() }, modifier = Modifier.weight(1f))
                PrimaryButton("下一步：分析设置", onClick = { viewModel.navigateTo(Screen.IMPORT_SETTINGS) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuestionBankImportCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
    ClassMateCard {
        Text("题库导入 v1", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "支持 Markdown / CSV 风格粘贴。Word / Excel 先作为 seam：请按模板复制为文本后导入。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.xs))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            StatusChip("Markdown READY", tone = ChipTone.SUCCESS)
            StatusChip("CSV READY", tone = ChipTone.SUCCESS)
            StatusChip("Word/Excel PARSER_PENDING", tone = ChipTone.WARNING)
        }
        Spacer(Modifier.height(Dimens.s))
        OutlinedTextField(
            value = ui.questionBankDraft,
            onValueChange = viewModel::updateQuestionBankDraft,
            label = { Text("Q: ... / A. ... / Answer: B / Explanation: ...") },
            minLines = 5,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        ui.questionBankParseResult?.let { result ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                result.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (result.accepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            SecondaryButton("加载题库模板", onClick = { viewModel.loadL3DemoSeed() }, modifier = Modifier.weight(1f))
            PrimaryButton("导入题库小测", onClick = { viewModel.importQuestionBankDraft() }, enabled = ui.questionBankDraft.isNotBlank(), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ImportSettingsScreen(viewModel: AppViewModel) {
    val ui = viewModel.ui
    val s = appStrings(ui.language)
    ClassMateScaffold(title = "分析设置", onBack = { viewModel.goBack() }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screen)
                .padding(bottom = Dimens.xxl),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardGap),
        ) {
            ClassMateCard {
                Text("课程信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                OutlinedTextField(
                    value = ui.courseTitle,
                    onValueChange = viewModel::updateCourseTitle,
                    label = { Text(s.importCourseTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            ClassMateCard {
                Text("科目 / 术语表", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "内置学科只是术语起始包；可在下方输入任意课程/学科名（机械、医学、法学、经管…），系统会按内容自动识别并动态生成术语。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
                CourseGlossary.subjects.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                        row.forEach { subject ->
                            SecondaryButton(
                                text = if (ui.selectedSubject == subject) "✓ $subject" else subject,
                                onClick = { viewModel.updateSelectedSubject(subject) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(Dimens.xs))
                }
                OutlinedTextField(
                    value = ui.selectedSubject,
                    onValueChange = viewModel::updateSelectedSubject,
                    label = { Text("自定义课程 / 学科名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(Dimens.xs))
                Text("已加载术语表：${ui.selectedSubject} · ${viewModel.selectedGlossaryCount()} 个内置术语（分析时会再按内容动态补充）", color = MaterialTheme.colorScheme.primary)
                ui.detectedDomainLabel?.let { label ->
                    Spacer(Modifier.height(Dimens.xs))
                    val pct = (ui.detectedDomainConfidence * 100).toInt()
                    val advisory = if (ui.detectedDomainNeedsConfirm) {
                        "上次识别：$label（置信度 $pct%，不确定，请确认或在上方修改课程名）"
                    } else {
                        "上次识别：$label（置信度 $pct%，可在上方修改课程名）"
                    }
                    Text(advisory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ClassMateCard {
                Text("思考强度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "快速更快、深度更完整。深度思考适合长文本 / 复杂资料，可能需要 1～3 分钟；深度分析失败不影响证据保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    AnalysisIntensity.entries.forEach { level ->
                        SecondaryButton(
                            text = if (ui.analysisIntensity == level) "✓ ${level.displayName}" else level.displayName,
                            onClick = { viewModel.setAnalysisIntensity(level) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.xs))
                Text(ui.analysisIntensity.expectedHintZh, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            ClassMateCard {
                Text("模型 profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                StatusChip(ui.providerConfigSummary.profileLabel, tone = ChipTone.PRIMARY)
                Spacer(Modifier.height(Dimens.s))
                Text("按当前顺序执行：云端蓝心 → 端侧蓝心 → 安全占位（模型全部不可用时的保护）。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PrimaryButton(
                text = "生成知识时间线",
                onClick = { viewModel.startAnalysis() },
                enabled = ui.courseText.isNotBlank() || ui.ocrImports.any { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "生成 L3 本地学习闭环",
                onClick = { viewModel.generateL3PipelineFromCurrentMaterial() },
                enabled = ui.courseText.isNotBlank() || ui.ocrImports.any { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() } || ui.transcripts.any { it.segments.any { seg -> seg.text.isNotBlank() } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OcrTrayEditor(
    selectedKind: OcrImportKind,
    onKindSelected: (OcrImportKind) -> Unit,
    displayLabel: String,
    onDisplayLabelChange: (String) -> Unit,
    ocrText: String,
    onOcrTextChange: (String) -> Unit,
    onPickMetadata: () -> Unit,
    onAdd: () -> Unit,
) {
    ClassMateCard {
        Text("加入 OCR 资料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text("官方 OCR 按配置启用；未配置或不可用时，可粘贴识别文字进入资料篮。文件不会上传，不爬取第三方平台内容。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        // P0-3: 4 kind buttons in a single weighted row squeezed every label to "..." on a phone. Lay them
        // out 2-per-row so each button is wide enough to show its full label.
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.s)) {
            OcrImportKind.entries.chunked(2).forEach { rowKinds ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
                    rowKinds.forEach { kind ->
                        SecondaryButton(
                            text = if (kind == selectedKind) "✓ ${shortKind(kind)}" else shortKind(kind),
                            onClick = { onKindSelected(kind) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowKinds.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton("选择文件（只记录元数据）", onClick = onPickMetadata, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Dimens.s))
        OutlinedTextField(
            value = displayLabel,
            onValueChange = onDisplayLabelChange,
            label = { Text("文件名 / 显示名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.s))
        OutlinedTextField(
            value = ocrText,
            onValueChange = onOcrTextChange,
            label = { Text("粘贴 OCR 识别文字") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton("加入本节课资料", onClick = onAdd, enabled = ocrText.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }
}

private fun sourceLabel(source: ImportSourceType): String = when (source) {
    ImportSourceType.PASTE_TEXT -> "粘贴文本"
    ImportSourceType.TXT_FILE -> ".txt"
    ImportSourceType.MARKDOWN_FILE -> ".md"
    ImportSourceType.IMAGE_OCR -> "OCR"
    ImportSourceType.AUDIO_FILE -> "音频/转写"
    ImportSourceType.VIDEO_FILE -> "视频字幕/元数据"
    ImportSourceType.NETWORK_VIDEO_LINK -> "网络链接（不抓取）"
}

private fun shortKind(kind: OcrImportKind): String = when (kind) {
    OcrImportKind.SLIDE_IMAGE -> "课件"
    OcrImportKind.BLACKBOARD_PHOTO -> "板书"
    OcrImportKind.PDF_PAGE -> "PDF"
    OcrImportKind.HANDOUT_IMAGE -> "讲义"
}

private fun ocrKindForEntry(id: MultimodalEntryId): OcrImportKind? = when (id) {
    MultimodalEntryId.SLIDE_IMAGE -> OcrImportKind.SLIDE_IMAGE
    MultimodalEntryId.BLACKBOARD_PHOTO -> OcrImportKind.BLACKBOARD_PHOTO
    MultimodalEntryId.PDF_HANDOUT -> OcrImportKind.PDF_PAGE
    else -> null
}

private fun readDisplayName(context: Context, uri: Uri): String {
    var name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx).orEmpty().ifBlank { name }
            }
        }
    }
    return name
}

private fun encodeJpegBytes(bitmap: Bitmap): ByteArray =
    ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        out.toByteArray()
    }

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
