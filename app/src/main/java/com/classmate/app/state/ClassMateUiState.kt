package com.classmate.app.state

import com.classmate.app.asr.AsrSession
import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.ExportReceipt
import com.classmate.app.glossary.CourseGlossary
import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.SelectedLocalFileMetadata
import com.classmate.app.l3.ClassroomRecordingRecord
import com.classmate.app.l3.DialectMode
import com.classmate.app.l3.ExamSession
import com.classmate.app.l3.AsrLongJob
import com.classmate.app.l3.InputArtifact
import com.classmate.app.l3.ImportReport
import com.classmate.app.l3.PdfDocumentArtifact
import com.classmate.app.l3.PdfPageArtifact
import com.classmate.app.l3.L3AsrStatus
import com.classmate.app.l3.L3PipelineSnapshot
import com.classmate.app.l3.SemanticSearchResult
import com.classmate.app.l3.TranslationResultRecord
import com.classmate.app.l3.TranslationSeamResult
import com.classmate.app.l3.TtsPlaybackState
import com.classmate.app.l3.TtsReviewSeamResult
import com.classmate.app.l3.ToolStepRecord
import com.classmate.app.l3.ToolOrchestrationPlan
import com.classmate.app.l3.EdgeStudySeamResult
import com.classmate.app.l3.PracticeAnswerSubmission
import com.classmate.app.l3.PracticeQuestionMode
import com.classmate.app.l3.QuestionBankParseResult
import com.classmate.app.material.MaterialSourceSummary
import com.classmate.app.platform.MaskedModelProfile
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.provider.AnalysisIntensity
import com.classmate.core.ondevice.OnDeviceLlmDiagnostic
import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.app.ui.i18n.AppLanguage
import com.classmate.app.ui.theme.AccentColorPreset
import com.classmate.app.ui.theme.CustomPalette
import com.classmate.app.ui.theme.ThemePreset
import com.classmate.app.ui.theme.TypographyPreset
import com.classmate.core.ask.LessonAnswer
import com.classmate.core.audio.CourseEssenceAudioResult
import com.classmate.core.audio.CourseEssenceScript
import com.classmate.core.logging.RedactedLogEntry
import com.classmate.core.live.TranscriptSession
import com.classmate.core.material.SpeakerLabel
import com.classmate.core.practice.PracticeAttempt
import com.classmate.core.practice.PracticeResult
import com.classmate.core.practice.PracticeSession
import com.classmate.core.safety.TextSafetyResult
import com.classmate.core.translation.TranslationNote
import com.classmate.core.transcript.TranscriptDraft
import com.classmate.core.transcript.TranscriptSourceType
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.FeedbackEvent
import com.classmate.core.model.LearningState
import com.classmate.core.model.ReviewPlan
import com.classmate.app.ondevice.OnDevicePermissionSnapshot
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.capture.CaptureError
import com.classmate.core.capture.ImageStudyDraft
import com.classmate.core.ondevice.ModelPathDetection
import com.classmate.core.ondevice.OnDeviceAnalysisDiagnostic
import com.classmate.core.ondevice.OnDeviceLlmConfig
import com.classmate.core.ondevice.OnDeviceModelFileStatus
import com.classmate.core.ondevice.OnDeviceMultimodalDiagnostic
import com.classmate.core.provider.BlueLMDiagnosticReport
import com.classmate.core.provider.CompatibleDiagnosticReport

enum class SettingsDeepLink {
    NONE,
    AI_MODEL_CONFIG_BLUELM,
}

data class AiConfigPromptUiState(
    val visible: Boolean = false,
    val feature: String = "",
) {
    companion object {
        fun hidden(): AiConfigPromptUiState = AiConfigPromptUiState()
    }
}

/** All UI state in one immutable snapshot, updated via copy() from [AppViewModel]. */
data class ClassMateUiState(
    // appearance
    val theme: ThemePreset = ThemePreset.Default,
    val accentColor: AccentColorPreset = AccentColorPreset.Default,
    val customPalette: CustomPalette = CustomPalette.Default,
    val typographyPreset: TypographyPreset = TypographyPreset.Default,
    val darkMode: Boolean? = null, // null = follow system
    val enableExperimentalImageGeneration: Boolean = false,
    val enableExperimentalVideoGeneration: Boolean = false,
    val enableExperimentalSimultaneousInterpretation: Boolean = false,
    val audioDialectMode: DialectMode = DialectMode.AUTO,

    // import inputs
    val courseTitle: String = "",
    val courseText: String = "",
    val selectedSubject: String = CourseGlossary.DEFAULT_SUBJECT,
    // Advisory result of on-device CourseDomainDetector (set after analysis). The picker above stays the
    // authoritative choice — this only tells the user what was auto-detected and whether to confirm.
    val detectedDomainLabel: String? = null,
    val detectedDomainConfidence: Double = 0.0,
    val detectedDomainNeedsConfirm: Boolean = false,
    // Thinking strength (快速/标准/深度) and live analysis progress so the analyze page is never a frozen spinner.
    val analysisIntensity: AnalysisIntensity = AnalysisIntensity.Default,
    val analysisElapsedMs: Long = 0L,
    val analysisSlowNotice: Boolean = false,
    val lastAnalysisLatencyMs: Long = 0L,
    val longTextInfo: LongTextAnalysisInfo? = null,
    val selectedImportFileMetadata: SelectedLocalFileMetadata? = null,
    val ocrImports: List<OcrImportDraft> = emptyList(),
    val importSourceType: ImportSourceType = ImportSourceType.PASTE_TEXT,
    val lastMaterialSummary: MaterialSourceSummary? = null,
    val l3Pipeline: L3PipelineSnapshot = L3PipelineSnapshot.Empty,
    val questionBankDraft: String = "",
    val questionBankParseResult: QuestionBankParseResult? = null,
    val inputArtifacts: List<InputArtifact> = emptyList(),
    val importReports: List<ImportReport> = emptyList(),
    val pdfDocuments: List<PdfDocumentArtifact> = emptyList(),
    val pdfPages: List<PdfPageArtifact> = emptyList(),
    val asrLongJobs: List<AsrLongJob> = emptyList(),
    val currentRecording: ClassroomRecordingRecord? = null,
    val recordingRecords: List<ClassroomRecordingRecord> = emptyList(),
    // System-back protection while a recording/transcription is in progress (don't silently exit).
    val showRecordingBackPrompt: Boolean = false,
    val asrLongStatus: L3AsrStatus = L3AsrStatus.PENDING_ASR_CONFIG,

    // transcript / subtitle intake (Stage 5B)
    val transcriptSourceType: TranscriptSourceType = TranscriptSourceType.PASTED_TRANSCRIPT,
    val transcriptPasteDraft: String = "",
    val transcriptFileMetadata: SelectedLocalFileMetadata? = null,
    val transcriptDraft: TranscriptDraft? = null, // the draft currently being parsed/edited
    val transcriptParseWarnings: List<String> = emptyList(),
    val transcripts: List<TranscriptDraft> = emptyList(), // finalized transcripts in the material tray
    val liveSpeakerDraft: SpeakerLabel = SpeakerLabel.UNKNOWN,
    val asrSession: AsrSession = AsrSession(), // experimental live system-ASR state

    // adaptive practice session (Stage 7C)
    val practiceSession: PracticeSession? = null,
    val practiceIndex: Int = 0,
    val practiceAttempts: List<PracticeAttempt> = emptyList(),
    val practiceResult: PracticeResult? = null,
    val practiceStartedAt: Long = 0L,
    val practiceRevealed: Boolean = false,
    val practiceQuestionMode: PracticeQuestionMode = PracticeQuestionMode.REAL_QUIZ,
    val practiceSelectedAnswers: Map<String, Set<String>> = emptyMap(),
    val practiceSubmittedAnswers: Map<String, PracticeAnswerSubmission> = emptyMap(),
    val examSession: ExamSession? = null,
    val courseEssenceScript: CourseEssenceScript? = null,
    val courseEssenceAudioResult: CourseEssenceAudioResult? = null,
    val translationNotes: List<TranslationNote> = emptyList(),
    val l3TranslationSeams: List<TranslationSeamResult> = emptyList(),
    val l3TranslationResults: List<TranslationResultRecord> = emptyList(),
    val l3TtsReviewSeam: TtsReviewSeamResult? = null,
    val l3TtsPlaybackState: TtsPlaybackState? = null,
    val l3ToolOrchestrationPlan: ToolOrchestrationPlan? = null,
    val l3ToolStepRecords: List<ToolStepRecord> = emptyList(),
    val l3SemanticSearchResults: List<SemanticSearchResult> = emptyList(),
    val l3EdgeStudySeam: EdgeStudySeamResult? = null,
    val textSafetyResult: TextSafetyResult? = null,

    // analysis
    val session: CourseSession? = null,
    val analysisStatus: AnalysisStatus = AnalysisStatus.IDLE,
    val analysisStageIndex: Int = 0,
    val providerConfigSummary: ProviderConfigSummary = ProviderConfigSummary.defaults(),
    val blueLmDiagnosticRunning: Boolean = false,
    val blueLmDiagnostic: BlueLMDiagnosticReport? = null,
    val compatibleDiagnosticRunning: Boolean = false,
    val compatibleDiagnostic: CompatibleDiagnosticReport? = null,
    // On-device BlueLM 3B (P6) + persistent official-model config (P5) + honest local path.
    val onDeviceDiagnostic: OnDeviceLlmDiagnostic? = null,
    val onDeviceDiagnosticRunning: Boolean = false,
    // Stage 8A-2: editable model path + real text/multimodal probe state.
    val onDeviceModelPath: String = OnDeviceLlmConfig.DEFAULT_MODEL_DIR,
    val onDeviceMultimodalDiagnostic: OnDeviceMultimodalDiagnostic? = null,
    val onDeviceMultimodalRunning: Boolean = false,
    // Stage 8A-2.2: functional-permission snapshot + bounded model-file diagnostic.
    val onDevicePermissions: OnDevicePermissionSnapshot = OnDevicePermissionSnapshot.unknown(),
    val onDeviceModelFiles: OnDeviceModelFileStatus? = null,
    // Stage 8B: on-device "local intelligence" suggestions (Report / Practice). Either real on-device
    // output or the fixed safety placeholder — never the deterministic rule path dressed as advice.
    val onDeviceReportSuggestion: String? = null,
    val onDeviceReportSuggestionRunning: Boolean = false,
    val onDevicePracticeSuggestion: String? = null,
    val onDevicePracticeSuggestionRunning: Boolean = false,
    val onDeviceReviewSuggestion: String? = null,
    val onDeviceReviewSuggestionRunning: Boolean = false,
    // Stage 8D: honest analysis-source breakdown (云端蓝心 / 端侧蓝心 / 安全占位) for the analyze/course UI.
    val analysisSourceReport: AnalysisSourceReport? = null,
    // Stage 8D Phase 7: last on-device CourseAnalysis result code for the Settings offline-mode check.
    val onDeviceAnalysisReason: String? = null,
    val onDeviceAnalysisCheckRunning: Boolean = false,
    // Stage 8D-2: detailed on-device CourseAnalysis diagnostic (sdk/files/permission/generate state).
    val onDeviceAnalysisDiagnostic: OnDeviceAnalysisDiagnostic? = null,
    // Stage 8C: image/photo → on-device multimodal → editable learning-text DRAFT (user-confirmed).
    val imageDraftActive: Boolean = false,
    val imageDraftRunning: Boolean = false,
    val imageDraftText: String = "",
    val imageDraftManualMode: Boolean = false, // true when multimodal is unavailable → manual input
    val imageDraftMessage: String? = null,
    // Stage 8E: honest input-origin label (图片学习输入 / 拍照学习输入) + bounded size diagnostics.
    val imageDraftOrigin: String? = null,
    val imageDraftMeta: String? = null,
    val imageStudyDraft: ImageStudyDraft? = null,
    val imageDraftSource: AiExecutionSource? = null,
    val imageDraftOcrError: CaptureError? = null,
    val imageDraftImageRef: String = "",
    val imageDraftThumbnailRef: String = "",
    val imageDraftMimeType: String = "",
    val imageDraftBatchId: String = "",
    val imageDraftBatchTotal: Int = 0,
    val imageDraftBatchProcessed: Int = 0,
    val audioCaptureRunning: Boolean = false,
    val audioCaptureProgress: Int = 0,
    val audioCaptureMessage: String? = null,
    val aiProcessing: AiProcessingUiState = AiProcessingUiState.hidden(),
    val aiConfigPrompt: AiConfigPromptUiState = AiConfigPromptUiState.hidden(),
    // Stage 8E Phase 1/6: bounded model-path candidate detection + real-image diagnostic meta.
    val modelPathDetection: ModelPathDetection? = null,
    val onDeviceRealImageMeta: String? = null,
    val modelConfigMasked: MaskedModelProfile? = null,
    val localProviderPath: List<String> = emptyList(),
    val result: CourseAnalysisResult? = null,
    val logs: List<RedactedLogEntry> = emptyList(),
    val analysisError: String? = null,

    // history (persisted business data only)
    val history: List<HistoryRecord> = emptyList(),

    // cross-course review queue (persisted; shared by Home/Review/History)
    val learningSnapshot: LearningSnapshot = LearningSnapshot(),

    // learning loop
    val learningState: LearningState? = null,
    val answers: Map<String, String> = emptyMap(), // questionId -> selected optionId
    val revealedQuestionIds: Set<String> = emptySet(),
    val currentQuestionIndex: Int = 0,
    val feedbackEvents: List<FeedbackEvent> = emptyList(),
    val reviewPlan: ReviewPlan? = null,
    val lastExportReceipt: ExportReceipt? = null,
    val exportDraftReady: Boolean = false,
    val exportDraftMessage: String? = null,
    val exportDraftSource: String? = null,
    val liveTranscript: TranscriptSession? = null,
    val liveTitleDraft: String = "",
    val liveSegmentDraft: String = "",
    val liveAnalyzed: Boolean = false,
    // UI language (default Chinese). SYSTEM resolves against the device locale.
    val language: AppLanguage = AppLanguage.ZH,
    val selectedCourseKey: String? = null,
    val askLessonQuestion: String = "",
    val askLessonAnswers: List<LessonAnswer> = emptyList(),
    val askLessonPending: Boolean = false,

    // navigation context
    val selectedKnowledgePointId: String? = null,
    val selectedEvidenceId: String? = null,
    val settingsDeepLink: SettingsDeepLink = SettingsDeepLink.NONE,

    // transient
    val toast: String? = null,
) {
    val answeredCount: Int get() = revealedQuestionIds.size
}

/**
 * Honest record of how a long input was shaped for the model. The full original text is ALWAYS kept as
 * Evidence; this only documents the prompt-budget view (what was sent), never a silent truncation of
 * the user's source. Shown on the analyze/course UI so long-text handling is transparent.
 */
data class LongTextAnalysisInfo(
    val originalLength: Int,
    val analyzedLength: Int,
    val chunkCount: Int,
    val strategy: String,
) {
    val wasShaped: Boolean get() = analyzedLength < originalLength || chunkCount > 1
}
