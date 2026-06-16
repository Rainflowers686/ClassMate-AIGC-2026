package com.classmate.app.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.classmate.app.data.ExportActionStatus
import com.classmate.app.exporting.DownloadsExporter
import com.classmate.app.exporting.ExportArtifact
import com.classmate.app.exporting.ExportFileFormat
import com.classmate.app.exporting.ExportIntentFactory
import com.classmate.app.state.AppViewModel
import com.classmate.app.ui.design.Dimens

@Composable
fun ExportCenterCard(
    viewModel: AppViewModel,
    title: String = "导出中心",
    description: String = "先生成学习报告草稿，再选择 PDF、Word、HTML、Markdown、Text 或课程精华音频脚本。",
    buildArtifact: (ExportFileFormat) -> ExportArtifact?,
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFileFormat.PDF) }
    var pendingSave by remember { mutableStateOf<ExportArtifact?>(null) }
    var draftReady by remember { mutableStateOf(viewModel.ui.exportDraftReady) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val artifact = pendingSave
        pendingSave = null
        if (artifact == null) return@rememberLauncherForActivityResult
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) {
            viewModel.recordExportAction(artifact, ExportActionStatus.CANCELED, "已取消保存。")
            return@rememberLauncherForActivityResult
        }
        try {
            ExportIntentFactory.writeToUri(context, uri, artifact)
            viewModel.recordExportAction(artifact, ExportActionStatus.SAVED_AS, "已保存到你选择的位置。")
        } catch (_: Exception) {
            viewModel.recordExportAction(
                artifact,
                ExportActionStatus.FAILED,
                "保存失败。可重试、换成 HTML/Text，或使用系统分享。",
            )
        }
    }

    fun artifactOrNotify(): ExportArtifact? {
        if (!draftReady) {
            viewModel.toast("请先生成学习报告草稿，再选择导出格式。")
            return null
        }
        return try {
            buildArtifact(selectedFormat)
        } catch (_: Exception) {
            viewModel.toast("${selectedFormat.displayName} 生成失败，可换成 HTML 或 Text 兜底。")
            null
        }
    }

    ClassMateCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        Text(
            "DOCX 是真实 Word 文档，适合继续编辑；PDF 适合打印；HTML 适合浏览器学习；课程精华音频脚本可在 TTS 未配置时先导出文本。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = if (draftReady) "重新生成学习报告草稿" else "生成学习报告草稿",
            onClick = { draftReady = viewModel.prepareRefinedExportDraft() || draftReady },
            modifier = Modifier.fillMaxWidth(),
        )
        viewModel.ui.exportDraftMessage?.let {
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "${viewModel.ui.exportDraftSource ?: "本地模板整理"} · $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Dimens.s))
        Text("选择导出格式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.xs))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            ExportFileFormat.entries.forEach { format ->
                FormatChip(
                    text = format.displayName,
                    selected = selectedFormat == format,
                    enabled = draftReady,
                    onClick = { selectedFormat = format },
                )
            }
        }
        Spacer(Modifier.height(Dimens.xs))
        Text(
            selectedFormat.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            PrimaryButton(
                text = "保存到文件…",
                onClick = {
                    val artifact = artifactOrNotify() ?: return@PrimaryButton
                    pendingSave = artifact
                    saveLauncher.launch(ExportIntentFactory.createSaveAsIntent(artifact))
                },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "保存到下载目录",
                onClick = {
                    val artifact = artifactOrNotify() ?: return@SecondaryButton
                    when (val result = DownloadsExporter.saveToDownloads(context, artifact)) {
                        is DownloadsExporter.Result.Saved ->
                            viewModel.recordExportAction(artifact, ExportActionStatus.SAVED_DOWNLOADS, "已保存到下载目录 / Downloads。")
                        is DownloadsExporter.Result.Unsupported ->
                            viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, result.reason)
                        is DownloadsExporter.Result.Failed ->
                            viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, result.reason)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Dimens.s))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            SecondaryButton(
                text = "分享…",
                onClick = {
                    val artifact = artifactOrNotify() ?: return@SecondaryButton
                    try {
                        context.startActivity(ExportIntentFactory.createShareChooser(context, artifact))
                        viewModel.recordExportAction(artifact, ExportActionStatus.SHARED, "已打开系统分享面板。")
                    } catch (_: ActivityNotFoundException) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, "没有可用的分享应用。")
                    } catch (_: Exception) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, "分享失败，请重试或改用保存。")
                    }
                },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "内部备份",
                onClick = {
                    val artifact = artifactOrNotify() ?: return@SecondaryButton
                    try {
                        ExportIntentFactory.writeInternalBackup(context, artifact)
                        viewModel.recordExportAction(artifact, ExportActionStatus.INTERNAL_ONLY, "已写入应用内部备份；建议优先使用保存或分享。")
                    } catch (_: Exception) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, "内部备份失败，可改导出 HTML/Text。")
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        viewModel.ui.lastExportReceipt?.let {
            Spacer(Modifier.height(Dimens.s))
            Text(
                "最近导出：${it.fileName} · ${it.format.ifBlank { selectedFormat.displayName }} · ${it.message.ifBlank { it.pathSummary }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    AiProcessingDialog(
        state = viewModel.ui.aiProcessing,
        onCancel = viewModel::hideAiProcessing,
        onRetry = { draftReady = viewModel.prepareRefinedExportDraft() || draftReady },
        onContinueManual = viewModel::hideAiProcessing,
    )
}

@Composable
private fun FormatChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = when {
            selected -> cs.primaryContainer
            enabled -> cs.surfaceVariant
            else -> cs.surfaceVariant.copy(alpha = 0.45f)
        },
        contentColor = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        modifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = Dimens.m, vertical = Dimens.s),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
