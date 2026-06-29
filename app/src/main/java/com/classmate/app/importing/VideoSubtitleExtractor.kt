package com.classmate.app.importing

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteBuffer

/**
 * P0-3: a REAL attempt to read an embedded subtitle / timed-text track out of a video container using the
 * platform [MediaExtractor]. Most user videos have NO text track — in that case this returns null and the
 * UI honestly says auto-extraction is unavailable (导入字幕文件 / 粘贴 / 实时转写 instead). It never fabricates
 * subtitles, and it never claims success when no text track exists.
 */
object VideoSubtitleExtractor {

    // Embedded text-track MIME types Android may expose (SRT / TTML / WebVTT / 3GPP timed text / CEA).
    private val TEXT_MIME_HINTS = listOf(
        "text/", "application/x-subrip", "application/ttml+xml", "application/x-quicktime-tx3g",
        "application/x-media-cea-608", "application/cea-608", "application/cea-708",
    )

    private const val MAX_CHARS = 200_000

    /** Returns the concatenated subtitle text, or null when no readable embedded text track is present. */
    fun extract(context: Context, uri: Uri): String? {
        val extractor = MediaExtractor()
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
                val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                    val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty().lowercase()
                    TEXT_MIME_HINTS.any { mime.startsWith(it) }
                } ?: return null
                extractor.selectTrack(trackIndex)
                val buffer = ByteBuffer.allocate(64 * 1024)
                val out = StringBuilder()
                while (out.length < MAX_CHARS) {
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    val bytes = ByteArray(size)
                    buffer.get(bytes, 0, size)
                    buffer.clear()
                    val piece = String(bytes, Charsets.UTF_8).trim()
                    if (piece.isNotBlank()) out.append(piece).append('\n')
                    if (!extractor.advance()) break
                }
                out.toString().trim().takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            // SECURITY/STABILITY: never surface decoder internals; treat any failure as "no extractable track".
            null
        } finally {
            runCatching { extractor.release() }
        }
    }
}
