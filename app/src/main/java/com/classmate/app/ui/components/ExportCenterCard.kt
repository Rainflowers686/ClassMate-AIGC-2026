package com.classmate.app.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import com.classmate.app.ui.i18n.appStrings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.classmate.app.state.PolishedExportStatus
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
    title: String? = null,
    description: String? = null,
    buildArtifact: (ExportFileFormat) -> ExportArtifact?,
) {
    val context = LocalContext.current
    val s = appStrings(viewModel.ui.language)
    var selectedFormat by remember { mutableStateOf(ExportFileFormat.PDF) }
    var pendingSave by remember { mutableStateOf<ExportArtifact?>(null) }
    var draftReady by remember { mutableStateOf(viewModel.ui.exportDraftReady) }
    // P0-1/P0-2: AI 精修导出 — a separate, user-initiated long task. When a polished version becomes ready
    // the format buttons default to it, but the user can switch back to the normal version at any time.
    val polished = viewModel.ui.polishedExport
    var usePolished by remember { mutableStateOf(false) }
    LaunchedEffect(polished.ready) { if (polished.ready) usePolished = true }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val artifact = pendingSave
        pendingSave = null
        if (artifact == null) return@rememberLauncherForActivityResult
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) {
            viewModel.recordExportAction(artifact, ExportActionStatus.CANCELED, s.exportSaveCanceled)
            return@rememberLauncherForActivityResult
        }
        try {
            ExportIntentFactory.writeToUri(context, uri, artifact)
            viewModel.recordExportAction(artifact, ExportActionStatus.SAVED_AS, s.exportSavedToChosenLocation)
        } catch (_: Exception) {
            viewModel.recordExportAction(
                artifact,
                ExportActionStatus.FAILED,
                s.exportSaveFailedFallback,
            )
        }
    }

    fun artifactOrNotify(): ExportArtifact? {
        if (usePolished && polished.ready) {
            return try {
                viewModel.buildPolishedArtifact(selectedFormat)
            } catch (_: Exception) {
                viewModel.toast(s.exportFormatFailed(selectedFormat.displayName)); null
            }
        }
        if (!draftReady) {
            viewModel.toast(s.exportNeedDraftToast)
            return null
        }
        return try {
            buildArtifact(selectedFormat)
        } catch (_: Exception) {
            viewModel.toast(s.exportFormatFailed(selectedFormat.displayName))
            null
        }
    }

    ClassMateCard {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(title ?: s.exportCenterTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            HelpHint(title = s.helpExportTitle, points = s.helpExportPoints, dismiss = s.helpDismiss)
        }
        Spacer(Modifier.height(Dimens.s))
        Text(description ?: s.exportCenterDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.s))
        PrimaryButton(
            text = if (draftReady) s.exportRegenerateDraft else s.exportGenerateDraft,
            onClick = { draftReady = viewModel.prepareRefinedExportDraft() || draftReady },
            modifier = Modifier.fillMaxWidth(),
        )
        viewModel.ui.exportDraftMessage?.let {
            Spacer(Modifier.height(Dimens.xs))
            Text(
                "${viewModel.ui.exportDraftSource ?: s.exportDraftSourceFallback} · $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---- P0-1/P0-2: 导出资料升级 / AI 精修导出 (separate long task; normal export above stays usable) ----
        if (viewModel.hasPolishableMaterial()) {
            Spacer(Modifier.height(Dimens.m))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(s.exportPolishTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                when {
                    polished.running -> StatusChip(s.exportPolishRunningChip, tone = ChipTone.INFO)
                    polished.ready -> StatusChip(s.exportPolishReady(polished.sourceZh), tone = ChipTone.SUCCESS)
                    polished.status == PolishedExportStatus.FAILED -> StatusChip(s.exportPolishFailedChip, tone = ChipTone.WARNING)
                    else -> {}
                }
            }
            Spacer(Modifier.height(Dimens.xs))
            Text(s.exportPolishDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Dimens.s))
            when {
                polished.running -> ProminentLoadingCard(
                    title = s.exportPolishTitle,
                    statusText = "${polished.stageIndex.coerceAtMost(polished.stageCount)}/${polished.stageCount} · ${polished.message}",
                    longWaitNote = if (polished.slowNotice) s.exportPolishSlowNote else null,
                    onCancel = { viewModel.cancelPolishedExport() },
                    cancelText = s.exportPolishCancel,
                    language = viewModel.ui.language,
                )
                polished.status == PolishedExportStatus.FAILED -> ProminentErrorCard(
                    whatHappened = s.exportPolishFailedChip,
                    possibleCause = polished.message,
                    nextStep = s.exportPolishDescription,
                    retryText = s.exportPolishRetry,
                    onRetry = { viewModel.startPolishedExport() },
                    secondaryText = s.exportPolishUseNormal,
                    onSecondary = { usePolished = false },
                    language = viewModel.ui.language,
                )
                polished.ready -> ActionButtonRow(
                    primaryText = s.exportPolishRetry,
                    onPrimary = { viewModel.startPolishedExport() },
                    secondaryText = if (usePolished) s.exportPolishVersionNormal else s.exportPolishVersionPolished,
                    onSecondary = { usePolished = !usePolished },
                )
                else -> PrimaryButton(
                    text = s.exportPolishStart,
                    onClick = { viewModel.startPolishedExport() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            polished.message.takeIf { it.isNotBlank() && !polished.running }?.let {
                Spacer(Modifier.height(Dimens.xs))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(Dimens.s))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(s.exportChooseFormat, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (polished.ready) {
                // Which version the format buttons export — the user can switch back to the normal version.
                StatusChip(
                    if (usePolished) s.exportPolishVersionPolished else s.exportPolishVersionNormal,
                    tone = if (usePolished) ChipTone.SUCCESS else ChipTone.NEUTRAL,
                    modifier = Modifier.clickable { usePolished = !usePolished },
                )
            }
        }
        Spacer(Modifier.height(Dimens.xs))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.s),
        ) {
            ExportFileFormat.entries.forEach { format ->
                FormatChip(
                    text = format.displayName,
                    selected = selectedFormat == format,
                    enabled = draftReady || (usePolished && polished.ready),
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
                text = s.exportSaveFile,
                onClick = {
                    val artifact = artifactOrNotify() ?: return@PrimaryButton
                    pendingSave = artifact
                    saveLauncher.launch(ExportIntentFactory.createSaveAsIntent(artifact))
                },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = s.exportSaveDownloads,
                onClick = {
                    val artifact = artifactOrNotify() ?: return@SecondaryButton
                    when (val result = DownloadsExporter.saveToDownloads(context, artifact)) {
                        is DownloadsExporter.Result.Saved ->
                            viewModel.recordExportAction(artifact, ExportActionStatus.SAVED_DOWNLOADS, s.exportSavedDownloads)
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
                text = s.exportShare,
                onClick = {
                    val artifact = artifactOrNotify() ?: return@SecondaryButton
                    try {
                        context.startActivity(ExportIntentFactory.createShareChooser(context, artifact))
                        viewModel.recordExportAction(artifact, ExportActionStatus.SHARED, s.exportShareOpened)
                    } catch (_: ActivityNotFoundException) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, s.exportNoShareApp)
                    } catch (_: Exception) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, s.exportShareFailed)
                    }
                },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = s.exportInternalBackup,
                onClick = {
                    val artifact = artifactOrNotify() ?: return@SecondaryButton
                    try {
                        ExportIntentFactory.writeInternalBackup(context, artifact)
                        viewModel.recordExportAction(artifact, ExportActionStatus.INTERNAL_ONLY, s.exportInternalBackupSaved)
                    } catch (_: Exception) {
                        viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, s.exportInternalBackupFailed)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        viewModel.ui.lastExportReceipt?.let { receipt ->
            Spacer(Modifier.height(Dimens.s))
            if (receipt.lastAction == ExportActionStatus.FAILED) {
                // P0-6: export failures are prominent, not a quiet line — with a real retry on the current draft.
                ProminentErrorCard(
                    whatHappened = s.exportFailedTitle,
                    possibleCause = receipt.message.ifBlank { s.exportFailedCauseFallback },
                    nextStep = s.exportFailedNextStep,
                    retryText = s.exportRetrySave,
                    onRetry = {
                        val artifact = artifactOrNotify()
                        if (artifact != null) {
                            when (val r = DownloadsExporter.saveToDownloads(context, artifact)) {
                                is DownloadsExporter.Result.Saved ->
                                    viewModel.recordExportAction(artifact, ExportActionStatus.SAVED_DOWNLOADS, s.exportSavedDownloads)
                                is DownloadsExporter.Result.Unsupported ->
                                    viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, r.reason)
                                is DownloadsExporter.Result.Failed ->
                                    viewModel.recordExportAction(artifact, ExportActionStatus.FAILED, r.reason)
                            }
                        }
                    },
                    secondaryText = s.exportSwitchHtml,
                    onSecondary = { selectedFormat = ExportFileFormat.HTML; viewModel.toast(s.exportSwitchHtmlToast) },
                    language = viewModel.ui.language,
                )
            } else {
                Text(
                    s.exportLatestReceipt(receipt.fileName, receipt.format.ifBlank { selectedFormat.displayName }, receipt.message.ifBlank { receipt.pathSummary }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
