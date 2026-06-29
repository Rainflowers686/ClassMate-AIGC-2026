package com.classmate.app.state

import com.classmate.core.ai.AiEnhancementType
import com.classmate.core.exporting.PolishedExportPlan
import com.classmate.core.exporting.PolishedStudyPack

/**
 * State for one AI second-pass enhancement surface (study-pack polish, quiz feedback, evidence
 * explanation). Carries the generated text plus an honest source label ("蓝心整理版" / "端侧模型草稿" /
 * "本地整理版" / failure note) — never a raw provider/debug string.
 */
data class EnhancementUiState(
    val type: AiEnhancementType? = null,
    val running: Boolean = false,
    val text: String = "",
    val sourceZh: String = "",
    val failed: Boolean = false,
) {
    val hasResult: Boolean get() = text.isNotBlank() && !running
    companion object {
        fun idle(): EnhancementUiState = EnhancementUiState()
    }
}

/**
 * State for a real on-device TTS audio file generated from the 听背文稿 (P0-2). Carries the safe file name +
 * size + an honest source label ("系统 TTS 生成" / "仅生成文稿" / "TTS 不可用") — never "蓝心 TTS". The raw
 * filePath is for playback/share/delete only and is never shown verbatim in normal UI.
 */
data class TtsAudioUiState(
    val running: Boolean = false,
    val filePath: String = "",
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val failed: Boolean = false,
    val message: String = "",
    val sourceZh: String = "",
) {
    val hasFile: Boolean get() = filePath.isNotBlank() && !running
}

/**
 * The source evidence behind a quiz question, surfaced INSIDE the practice screen (P0-2) so a "根据图片"
 * question always shows its image / OCR / source — never a bare instruction with no context. Carries only
 * learner-facing fields (a local image file path + quote + label), never a raw assetId or provider trace.
 */
data class PracticeEvidenceContext(
    val evidenceId: String,
    val quote: String,
    val imagePath: String,
    val sourceLabel: String,
    val isImage: Boolean,
) {
    val hasImage: Boolean get() = imagePath.isNotBlank()
}

/**
 * P0-1/P0-2: the user-initiated "AI 精修导出 / 导出资料升级" task. It is SEPARATE from the fast default
 * export (which stays instant and unblocked) and from the 30s secondary enhancements — this is an explicit
 * long task that may use the deep/Max thinking path. [pack] is null until a polished version exists;
 * [sourceZh] is always honest ("蓝心精修版" / "端侧精修草稿" / "本地整理版"), never local-as-蓝心.
 */
enum class PolishedExportStatus { IDLE, RUNNING, READY, FAILED }

data class PolishedExportUiState(
    val status: PolishedExportStatus = PolishedExportStatus.IDLE,
    val stageIndex: Int = 0,
    val stageCount: Int = POLISH_STAGES.size,
    val startedAtMs: Long = 0L,
    val slowNotice: Boolean = false,
    val sourceZh: String = "",
    val message: String = "",
    val pack: PolishedStudyPack? = null,
) {
    val running: Boolean get() = status == PolishedExportStatus.RUNNING
    val ready: Boolean get() = status == PolishedExportStatus.READY && pack != null

    companion object {
        /** The visible progress stages, in order. The model call spans the middle stages. */
        val POLISH_STAGES: List<String> = PolishedExportPlan.STAGES
    }
}

/**
 * P1-2: Flow background-music state. Owned by the ViewModel (not the Flow Composable), so navigating away
 * from the Flow page keeps the music in whatever state the USER left it — only an explicit pause/stop or
 * the ViewModel being cleared changes it.
 */
enum class FlowMusicStatus { STOPPED, PLAYING, PAUSED }

data class FlowMusicUiState(
    val status: FlowMusicStatus = FlowMusicStatus.STOPPED,
    val sceneId: String = "rain",
    val soundName: String = "",
    val volume: Float = 0.45f,
) {
    val playing: Boolean get() = status == FlowMusicStatus.PLAYING
}

data class AiProcessingUiState(
    val visible: Boolean = false,
    val title: String = "",
    val steps: List<String> = emptyList(),
    val activeStep: Int = 0,
    val source: String = "",
    val fallbackMessage: String? = null,
    val canCancel: Boolean = false,
    val canRetry: Boolean = false,
    val canContinueManual: Boolean = false,
) {
    companion object {
        fun hidden(): AiProcessingUiState = AiProcessingUiState()
    }
}

