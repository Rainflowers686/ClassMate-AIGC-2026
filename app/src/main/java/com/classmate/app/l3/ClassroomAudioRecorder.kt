package com.classmate.app.l3

import android.media.MediaRecorder
import java.io.File

data class RecordingArtifactResult(
    val success: Boolean,
    val fileName: String? = null,
    val safeMessage: String,
    val fileSizeBytes: Long = 0L,
)

interface ClassroomAudioRecorder {
    fun start(sessionId: String): RecordingArtifactResult
    fun stop(): RecordingArtifactResult
}

object NoOpClassroomAudioRecorder : ClassroomAudioRecorder {
    override fun start(sessionId: String): RecordingArtifactResult =
        RecordingArtifactResult(false, safeMessage = "当前环境未提供录音器，可继续手动转写。")

    override fun stop(): RecordingArtifactResult =
        RecordingArtifactResult(false, safeMessage = "当前环境未提供录音器，可继续手动转写。")
}

class AndroidClassroomAudioRecorder(private val directory: File) : ClassroomAudioRecorder {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    @Suppress("DEPRECATION")
    override fun start(sessionId: String): RecordingArtifactResult {
        if (recorder != null) return RecordingArtifactResult(false, currentFile?.name, "录音已在进行中。")
        return try {
            directory.mkdirs()
            val file = File(directory, "$sessionId.m4a")
            val next = MediaRecorder()
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setOutputFile(file.absolutePath)
            next.prepare()
            next.start()
            recorder = next
            currentFile = file
            RecordingArtifactResult(true, file.name, "录音已开始，音频保存在 App 私有目录。")
        } catch (_: SecurityException) {
            cleanup()
            RecordingArtifactResult(false, safeMessage = "未授权麦克风，仍可手动转写。")
        } catch (_: Exception) {
            cleanup()
            RecordingArtifactResult(false, safeMessage = "录音启动失败，可继续手动转写。")
        }
    }

    override fun stop(): RecordingArtifactResult {
        val file = currentFile
        val active = recorder ?: return RecordingArtifactResult(false, safeMessage = "当前没有正在进行的录音。")
        return try {
            active.stop()
            active.release()
            recorder = null
            currentFile = null
            // Only claim success when a real, non-empty file actually landed on disk — a recording that
            // is too short, denied, or failed to flush leaves no/zero-length file and must NOT say "已保存".
            if (file != null && file.exists() && file.length() > 0L) {
                RecordingArtifactResult(true, file.name, "录音已保存，可粘贴转写文本进入学习闭环。", file.length())
            } else {
                runCatching { file?.delete() }
                RecordingArtifactResult(false, file?.name, "录音文件为空或保存失败，请重试或改用手动转写。")
            }
        } catch (_: Exception) {
            cleanup()
            runCatching { file?.delete() }
            RecordingArtifactResult(false, file?.name, "录音保存失败，可继续手动转写。")
        }
    }

    private fun cleanup() {
        runCatching { recorder?.release() }
        recorder = null
        currentFile = null
    }
}
