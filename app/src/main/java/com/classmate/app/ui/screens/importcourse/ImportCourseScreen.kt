package com.classmate.app.ui.screens.importcourse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.classmate.app.l3.L3RecordingStatus
import com.classmate.app.l3.DialectMode
import com.classmate.app.l3.InputFileKind
import com.classmate.app.glossary.CourseGlossary
import com.classmate.app.ondevice.BitmapToRgb
import com.classmate.app.ondevice.OnDevicePermissions
import com.classmate.app.importing.MultimodalEntryId
import com.classmate.app.importing.MultimodalImportCatalog
import com.classmate.app.importing.MultimodalImportEntry
import com.classmate.app.importing.OcrImportAssembler
import com.classmate.app.importing.OcrImportKind
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
import com.classmate.app.ui.components.ClassMateScaffold
import com.classmate.app.ui.components.CompactSectionHeader
import com.classmate.app.ui.components.MaterialTrayItem
import com.classmate.app.ui.components.PageHero
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
import com.classmate.app.platform.CaptureConfigLoader
import com.classmate.app.ui.design.Dimens
import com.classmate.app.ui.i18n.appStrings
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.transcript.TranscriptLabels
import java.io.ByteArrayOutputStream

@Composable
fun ImportCourseScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val ui = viewModel.ui
    val pendingFileSource = remember { mutableStateOf(ImportSourceType.TXT_FILE) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val name = readDisplayName(context, uri)
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes == null) {
                viewModel.toast("无法读取该文件，请重试。")
            } else {
                val accepted = viewModel.importSuperhubFile(bytes, name, mimeType)
                if (accepted) viewModel.navigateTo(Screen.IMPORT_TRAY)
            }
        }
    }

    val perms = remember { OnDevicePermissions(context) }
    // Photo Picker (no permission needed) → decode → on-device multimodal draft.
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) handlePickedImage(context, uri, perms, viewModel)
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
    val recordingPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startClassroomRecording() else viewModel.toast("未授权麦克风，仍可粘贴手动转写文本。")
    }
    fun startRecordingWithPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startClassroomRecording() else recordingPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    AiProcessingDialog(
        state = ui.aiProcessing,
        onCancel = viewModel::hideAiProcessing,
        onRetry = viewModel::retryCurrentCapture,
        onContinueManual = viewModel::hideAiProcessing,
    )

    ProductCanvas {
        ProductScaffold(contextLabel = "资料", onBack = { viewModel.goBack() }) { padding ->
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
                    subtitle = "图片、拍照、文本会先形成可编辑学习草稿，用户确认后进入知识地图。",
                )
                // AI 来源：云端优先 · 端侧兜底 — 未配置官方服务不等于没有 AI；端侧蓝心仍可生成学习草稿。
                val captureStatus = remember { CaptureConfigLoader().status() }
                Text(
                    "AI 来源：云端优先 · 端侧兜底。官方 OCR / ASR ${captureStatus.labelZh()}；未配置时端侧蓝心仍可生成图片学习草稿，或粘贴转写文本，用户确认后生成知识地图。",
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
                ClassroomRecordingCard(viewModel, onStartRecording = { startRecordingWithPermission() })
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
            "TXT/Markdown/CSV 会直接进入学习闭环；DOCX/PPTX/XLSX 为 best-effort 抽取；PDF 可手动添加页文本并保留页码证据。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        documentArtifacts.takeLast(3).forEach { artifact ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "${artifact.fileName} · ${artifact.kind.name} · ${artifact.status.name} · ${artifact.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        latestPage?.let { page ->
            Spacer(Modifier.height(Dimens.s))
            StatusChip("PDF page ${page.pageNumber}: ${page.status.name}", tone = ChipTone.INFO)
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
        if (documentEvidence != null) {
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
private fun ClassroomRecordingCard(viewModel: AppViewModel, onStartRecording: () -> Unit) {
    val ui = viewModel.ui
    val active = ui.currentRecording?.status == L3RecordingStatus.RECORDING
    val latestAsrJob = ui.asrLongJobs.lastOrNull()
    var manualTranscript by remember(latestAsrJob?.id) { mutableStateOf("") }
    val audioEvidence = ui.l3Pipeline.evidence.firstOrNull {
        it.sourceType.name == "AUDIO_TRANSCRIPT" || it.sourceType.name == "MANUAL_TRANSCRIPT" || it.audioRef.isNotBlank()
    }
    QuietCard {
        Text("课堂录音记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "录音保存在 App 私有目录。官方 ASR Long 未配置时，录音后请粘贴手动转写文本进入学习闭环。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        Spacer(Modifier.height(Dimens.xs))
        Text(
            "方言/口音增强会保留 raw transcript，并把低置信片段标记给你确认；不会胡编课堂外内容。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        ui.currentRecording?.let {
            StatusChip("录音中", tone = ChipTone.PRIMARY)
            Spacer(Modifier.height(Dimens.xs))
            Text(it.title, style = MaterialTheme.typography.bodyMedium)
        }
        ui.recordingRecords.takeLast(2).forEach { record ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "${record.title} · ${record.status.name} · ${record.asrStatus.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ui.inputArtifacts.takeLast(3).forEach { artifact ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "${artifact.fileName} · ${artifact.kind.name} · ${artifact.status.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ui.importReports.takeLast(2).forEach { report ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "Import report · ${report.sourceType.name} · success ${report.successCount} · warnings ${report.warningCount} · ${report.nextAction}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ui.pdfDocuments.takeLast(2).forEach { doc ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "PDF document · ${doc.fileName} · pages ${doc.pageCount} · ${doc.parserStatus}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ui.pdfPages.takeLast(2).forEach { page ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "PDF page ${page.pageNumber} · ${page.status.name} · OCR ${page.ocrStatus}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ui.asrLongJobs.takeLast(2).forEach { job ->
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "ASR Long job · ${job.status.name} · upload ${job.uploadStatus.ifBlank { "not started" }} · polling ${job.pollingStatus.ifBlank { "not started" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                text = "确认转写并生成课堂学习闭环",
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
        if (audioEvidence != null) {
            Spacer(Modifier.height(Dimens.s))
            SecondaryButton(
                text = "查看音频 / 转写证据",
                onClick = { viewModel.openEvidenceById(audioEvidence.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Dimens.s))
        SecondaryButton(
            text = "进入转写编辑器",
            onClick = { viewModel.navigateTo(Screen.TRANSCRIPT_IMPORT) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.s))
        if (active) {
            PrimaryButton("停止并保存录音", onClick = { viewModel.stopClassroomRecording() }, modifier = Modifier.fillMaxWidth())
        } else {
            PrimaryButton("开始课堂录音", onClick = onStartRecording, modifier = Modifier.fillMaxWidth())
        }
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

/**
 * Phase C draft, restyled (Stage 9C) as a DRAFT DOCUMENT WORKSPACE: a header strip with the origin
 * + an honest status badge, a paper-like editing surface, and a clear two-level footer. The 8E logic
 * (multimodal → editable → confirm → courseText) is unchanged.
 */
@Composable
private fun ImageDraftCard(viewModel: AppViewModel) {
    val ui = viewModel.ui
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
            StatusChip("图片已保存为 evidence asset", tone = ChipTone.SUCCESS)
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "缩略图引用：${ui.imageDraftThumbnailRef.ifBlank { "待生成" }}",
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
                    Text("端侧多模态理解中…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            if (ui.imageDraftManualMode) {
                Text(
                    ui.imageDraftMessage ?: "端侧图像理解暂不可用，可手动输入图片中的学习内容。",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.s))
            }
            // Paper-like editing surface.
            OutlinedTextField(
                value = ui.imageDraftText,
                onValueChange = viewModel::updateImageDraftText,
                label = { Text("可编辑学习文本（确认后作为课程文本）") },
                minLines = 5,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Dimens.m))
            // Two-level footer: confirm dominant, cancel quiet.
            PrimaryButton(text = "确认并进入学习资料", onClick = { viewModel.confirmImageDraft() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Dimens.xs))
            SecondaryButton(text = "取消", onClick = { viewModel.cancelImageDraft() }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(Dimens.s))
        Text(
            "确认后会生成知识点、微测题和复习计划；题目、错题和复习任务都可以回到这张图片证据。端侧/手动草稿不替代 OCR。",
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
    }
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
        viewModel.toast("无法读取该图片，请改用手动输入。")
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
                MaterialTrayItem(
                    title = draft.fileMeta.safeDisplayLabel(),
                    source = OcrImportAssembler.sourceLabel(draft.kind),
                    meta = "${draft.pastedText.count { !it.isWhitespace() }} 字",
                    onRemove = { viewModel.removeOcrImport(draft.id) },
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
                Text("模型 profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(Dimens.s))
                StatusChip(ui.providerConfigSummary.profileLabel, tone = ChipTone.PRIMARY)
                Spacer(Modifier.height(Dimens.s))
                Text("按当前顺序执行：云端蓝心 → 端侧蓝心 → 安全占位（模型全部不可用时的保护）。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PrimaryButton(
                text = "生成知识时间线",
                onClick = { viewModel.startAnalysis() },
                enabled = ui.courseText.isNotBlank() || ui.ocrImports.any { it.pastedText.isNotBlank() },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "生成 L3 本地学习闭环",
                onClick = { viewModel.generateL3PipelineFromCurrentMaterial() },
                enabled = ui.courseText.isNotBlank() || ui.ocrImports.any { it.pastedText.isNotBlank() } || ui.transcripts.any { it.segments.any { seg -> seg.text.isNotBlank() } },
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            OcrImportKind.entries.forEach { kind ->
                SecondaryButton(
                    text = if (kind == selectedKind) "✓ ${shortKind(kind)}" else shortKind(kind),
                    onClick = { onKindSelected(kind) },
                    modifier = Modifier.weight(1f),
                )
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
