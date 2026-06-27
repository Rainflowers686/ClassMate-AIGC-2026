package com.classmate.app.exporting

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class ExportIntentSpec(
    val action: String,
    val mimeType: String,
    val fileName: String,
    val grantRead: Boolean = false,
)

object ExportIntentFactory {
    fun saveAsSpec(artifact: ExportArtifact): ExportIntentSpec =
        ExportIntentSpec(Intent.ACTION_CREATE_DOCUMENT, artifact.mimeType, artifact.fileName)

    fun shareSpec(artifact: ExportArtifact): ExportIntentSpec =
        ExportIntentSpec(Intent.ACTION_SEND, artifact.mimeType, artifact.fileName, grantRead = true)

    fun createSaveAsIntent(artifact: ExportArtifact): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = artifact.mimeType
            putExtra(Intent.EXTRA_TITLE, artifact.fileName)
        }

    fun createShareIntent(uri: Uri, artifact: ExportArtifact): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = artifact.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun createShareChooser(context: Context, artifact: ExportArtifact): Intent {
        val uri = writeShareCache(context, artifact)
        return Intent.createChooser(createShareIntent(uri, artifact), "分享 ClassMate 导出文件")
    }

    /**
     * Share an existing on-disk audio recording. The file is copied into the FileProvider-exposed
     * share cache (the app-private recordings dir is not exposed), then offered via a chooser so the
     * user can save/send it instead of hunting through a system file manager.
     */
    fun shareAudioFileChooser(context: Context, file: File): Intent {
        val dir = File(context.cacheDir, "export_share")
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, file.name)
        file.copyTo(target, overwrite = true)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "导出 / 分享课堂录音")
    }

    fun writeToUri(context: Context, uri: Uri, artifact: ExportArtifact) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(artifact.bytes) }
            ?: error("Cannot open selected file")
    }

    fun writeShareCache(context: Context, artifact: ExportArtifact): Uri {
        val dir = File(context.cacheDir, "export_share")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, artifact.fileName)
        file.writeBytes(artifact.bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Legacy/last-resort internal backup into app-private files/exports. This is NEVER the user's
     * primary delivery path — it is only a safety net so an export is not lost when both the
     * document picker and Downloads fallback are unavailable. The directory is app-private and is
     * NOT exposed through the FileProvider, so it never leaks outside the app sandbox.
     */
    fun writeInternalBackup(context: Context, artifact: ExportArtifact): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, artifact.fileName)
        file.writeBytes(artifact.bytes)
        return file
    }
}

