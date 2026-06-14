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
    description: String = "保存到你选择的位置，或通过系统分享面板发送到微信、QQ、邮箱、网盘、文件管理器等。",
    buildArtifact: (ExportFileFormat) -> ExportArtifact?,
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFileFormat.MARKDOWN) }
    var pendingSave by remember { mutableStateOf<ExportArtifact?>(null) }
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
                "系统文件管理器未能保存。你可以改用系统分享，或保存到下载目录。",
            )
        }
    }

    ClassMateCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Dimens.s))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        Text(
            "Word 兼容 HTML 不是原生 docx；演示幻灯片 HTML 不是原生 pptx。PDF 为基础版报告。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            ExportFileFormat.entries.forEach { format ->
                FormatChip(
                    text = format.displayName,
                    selected = selectedFormat == format,
                    onClick = { selectedFormat = format },
                )
            }
        }
        Spacer(Modifier.height(Dimens.s))
        Text(
            "若系统文件管理器提示“无法保存文档”，可改用“保存到下载目录”或“分享…”。内部备份仅作兜底。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.s))
        // Two horizontal rows (never a vertical button stack): primary delivery on top, fallbacks below.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.s)) {
            PrimaryButton(
                text = "保存到文件…",
                onClick = {
                    val artifact = buildArtifact(selectedFormat) ?: return@PrimaryButton
                    pendingSave = artifact
                    saveLauncher.launch(ExportIntentFactory.createSaveAsIntent(artifact))
                },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "保存到下载目录",
                onClick = {
                    val artifact = buildArtifact(selectedFormat) ?: return@SecondaryButton
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
                    val artifact = buildArtifact(selectedFormat) ?: return@SecondaryButton
                    try {
                        context.startActivity(ExportIntentFactory.createShareChooser(context, artifact))
                        viewModel.recordExportAction(artifact, ExportActionStatus.SHARED, "已打开系统分享面板。")
                    } catch (_: ActivityNotFoundException) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, "没有可用的分享应用。")
                    } catch (_: Exception) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, "分享失败，请重试。")
                    }
                },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "内部备份",
                onClick = {
                    val artifact = buildArtifact(selectedFormat) ?: return@SecondaryButton
                    try {
                        ExportIntentFactory.writeInternalBackup(context, artifact)
                        viewModel.recordExportAction(artifact, ExportActionStatus.INTERNAL_ONLY, "已写入应用内部备份（legacy 兜底，建议改用保存或分享）。")
                    } catch (_: Exception) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, "内部备份失败。")
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
}

@Composable
private fun FormatChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) cs.primaryContainer else cs.surfaceVariant,
        contentColor = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = Dimens.m, vertical = Dimens.s),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
