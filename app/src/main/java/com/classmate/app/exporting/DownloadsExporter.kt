package com.classmate.app.exporting

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/**
 * Save-failure fallback for the real-device bug where the system document picker reports
 * "无法保存文档". This writes the export bytes straight into the public Downloads collection via
 * scoped [MediaStore] (Android 10+), which needs NO dangerous storage permission — the MediaStore
 * Downloads API grants the app write access to its own inserted item.
 *
 * On API < 29 the scoped collection does not exist and the legacy path would require a dangerous
 * storage permission we deliberately do not request, so we return [Result.Unsupported] with a
 * clear message that points the user at "保存到文件…" or "分享…" instead. Nothing here ever throws
 * out — every failure becomes a typed result with user-facing guidance.
 */
object DownloadsExporter {

    sealed interface Result {
        data class Saved(val displayName: String, val location: String) : Result
        data class Unsupported(val reason: String) : Result
        data class Failed(val reason: String) : Result
    }

    fun saveToDownloads(context: Context, artifact: ExportArtifact): Result =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, artifact)
        } else {
            Result.Unsupported("当前系统版本较低，无法直接写入下载目录。请改用“保存到文件…”或“分享…”。")
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(context: Context, artifact: ExportArtifact): Result =
        try {
            val resolver = context.contentResolver
            val pending = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, artifact.fileName)
                put(MediaStore.Downloads.MIME_TYPE, artifact.mimeType.substringBefore(';').trim())
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, pending)
            if (uri == null) {
                Result.Failed("系统未返回下载目录写入位置，请改用系统分享。")
            } else {
                val wrote = resolver.openOutputStream(uri)?.use { it.write(artifact.bytes); true } ?: false
                if (!wrote) {
                    resolver.delete(uri, null, null)
                    Result.Failed("无法打开下载目录写入流，请改用系统分享。")
                } else {
                    resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                    Result.Saved(artifact.fileName, "Downloads / 下载")
                }
            }
        } catch (e: Exception) {
            Result.Failed("写入下载目录失败：${e.message ?: "未知错误"}。可改用系统分享。")
        }
}
