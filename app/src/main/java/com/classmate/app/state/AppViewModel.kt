package com.classmate.app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classmate.app.asr.AsrSession
import com.classmate.app.asr.AsrSessionController
import com.classmate.app.asr.AsrState
import com.classmate.app.asr.SpeechRecognitionReadiness
import com.classmate.app.ui.screens.settings.SettingsPage
import com.classmate.app.asr.AsrTranscriptMapper
import com.classmate.app.capture.CaptureGateway
import com.classmate.app.capture.CaptureGatewayPort
import com.classmate.app.data.BlueLMHttpTransport
import com.classmate.app.data.ExportActionStatus
import com.classmate.app.data.ExportReceipt
import com.classmate.app.data.ExportStore
import com.classmate.app.data.EvidenceAssetStore
import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.HistoryStore
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.data.L3PersistenceRepository
import com.classmate.app.data.LocalSemanticIndexRepository
import com.classmate.app.data.ThemePreferenceRepository
import com.classmate.app.exporting.ExportArtifact
import com.classmate.app.audio.FlowAudioController
import com.classmate.app.audio.NoOpFlowAudioController
import com.classmate.app.ui.flow.AmbientSound
import com.classmate.app.exporting.ExportCenter
import com.classmate.app.exporting.ExportFileFormat
import com.classmate.core.ai.PolishedStudyMaterial
import com.classmate.core.ai.PolishedStudyPackInput
import com.classmate.core.ai.PolishedStudyPackPromptBuilder
import com.classmate.core.exporting.PolishedExportPlan
import com.classmate.core.exporting.PolishedStudyPack
import com.classmate.core.exporting.SafeExportText
import com.classmate.core.audio.ConfigMissingTtsProvider
import com.classmate.core.audio.CourseEssenceAudioExporter
import com.classmate.core.evidence.EvidenceRelation
import com.classmate.core.evidence.EvidenceRelationLevel
import com.classmate.core.ocr.OcrTextPostProcessor
import com.classmate.core.evidence.EvidenceOwnership
import com.classmate.core.exporting.StudyReport
import com.classmate.core.exporting.StudyReportBuilder
import com.classmate.core.exporting.StudyReportRenderer
import com.classmate.app.glossary.CourseGlossary
import com.classmate.app.importing.OcrImportAssembler
import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.OcrImportFileMeta
import com.classmate.app.importing.OcrImportKind
import com.classmate.app.importing.OcrImportStatus
import com.classmate.app.importing.SelectedLocalFileMetadata
import com.classmate.app.l3.ClassroomAudioRecorder
import com.classmate.app.l3.ClassroomRecordingRecord
import com.classmate.app.l3.DialectMode
import com.classmate.app.l3.EvidenceAsset
import com.classmate.app.l3.EvidenceAssetType
import com.classmate.app.l3.QuizOptionIds
import com.classmate.app.l3.ExamSession
import com.classmate.app.l3.ExamStatus
import com.classmate.app.l3.AsrLongJob
import com.classmate.app.l3.AsrLongProductizationEngine
import com.classmate.app.l3.AudioSessionEngine
import com.classmate.app.l3.ExamReportEngine
import com.classmate.app.l3.FeedbackLearningOptimizer
import com.classmate.app.l3.FeedbackOptimizationOutcome
import com.classmate.app.l3.InputArtifactStatus
import com.classmate.app.l3.InputFileKind
import com.classmate.app.l3.InputReportEngine
import com.classmate.app.l3.InputSuperhub
import com.classmate.app.l3.L3OfficialToolSeams
import com.classmate.app.l3.L3AsrStatus
import com.classmate.app.l3.L3DemoSeeds
import com.classmate.app.l3.L3LearningPipeline
import com.classmate.app.l3.L3PipelineSnapshot
import com.classmate.app.l3.L3RecordingStatus
import com.classmate.app.l3.L3SourceType
import com.classmate.app.l3.LearningLoopInput
import com.classmate.app.l3.LearningLoopInputKind
import com.classmate.app.l3.LearningDiagnosisEngine
import com.classmate.app.l3.LearningExportEngine
import com.classmate.app.l3.LearningLoopCapabilityOrchestrator
import com.classmate.app.l3.LocalTtsPlayer
import com.classmate.app.l3.LocalSemanticIndexEngine
import com.classmate.app.l3.DeviceReadinessEngine
import com.classmate.app.l3.ExperimentalStudyAssetEngine
import com.classmate.app.l3.NoOpClassroomAudioRecorder
import com.classmate.app.l3.NoOpLocalTtsPlayer
import com.classmate.app.l3.OfficialRuntimeGateway
import com.classmate.app.l3.OfficialRuntimeGatewayFactory
import com.classmate.app.l3.OfficialRuntimeIntegrator
import com.classmate.app.l3.OfficialRuntimeStatus
import com.classmate.app.l3.OfficialCapabilityRegistry
import com.classmate.app.l3.PdfProcessingEngine
import com.classmate.app.l3.PracticeGradingEngine
import com.classmate.app.l3.PracticeAnswerState
import com.classmate.app.l3.PracticeAnswerSubmission
import com.classmate.app.l3.PracticeQuestionMode
import com.classmate.app.l3.QuestionBankParser
import com.classmate.app.l3.RecordingFileManager
import com.classmate.app.l3.ReviewStatsEngine
import com.classmate.app.l3.SafetyGuardEngine
import com.classmate.app.l3.ToolInputType
import com.classmate.app.l3.ToolOrchestratorProductizationEngine
import com.classmate.app.l3.TranslationProductEngine
import com.classmate.app.l3.TranslationProductStatus
import com.classmate.app.l3.TranslationTargetLanguage
import com.classmate.app.l3.TtsPlaybackEngine
import com.classmate.app.l3.TtsPlaybackProvider
import com.classmate.app.l3.TtsPlaybackStatus
import com.classmate.app.l3.TtsPlaybackSourceType
import com.classmate.app.l3.TranscriptSegment
import com.classmate.app.l3.TranscriptGlossaryExtractor
import com.classmate.app.material.LessonMaterialAssembler
import com.classmate.core.material.LessonContextHints
import com.classmate.app.sample.SampleLessonLibrary
import com.classmate.core.exporting.ContentExporter
import com.classmate.core.exporting.ExportDocument
import com.classmate.core.exporting.ExportFormat
import com.classmate.core.learning.InMemoryLearningStore
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.LearningStore
import com.classmate.core.learning.PracticeHistoryRecord
import com.classmate.core.practice.KnowledgePointSearch
import com.classmate.core.practice.PracticeAttempt
import com.classmate.core.practice.PracticeFeedbackEngine
import com.classmate.core.practice.PracticeGenerationRequest
import com.classmate.core.practice.PracticeItem
import com.classmate.core.practice.PracticeItemType
import com.classmate.core.practice.isAnswerableQuiz
import com.classmate.core.practice.PracticeMode
import com.classmate.core.practice.PracticeOption
import com.classmate.core.practice.PracticeOutcome
import com.classmate.core.practice.PracticeResult
import com.classmate.core.practice.PracticeSession
import com.classmate.core.practice.PracticeSessionEngine
import com.classmate.core.practice.RoutedPracticeGenerationUseCase
import com.classmate.core.safety.TextSafetyGate
import com.classmate.core.safety.BasicTextSafetyProvider
import com.classmate.core.tools.InternalFunctionRouter
import com.classmate.core.tools.InternalToolCall
import com.classmate.core.tools.InternalToolName
import com.classmate.core.learning.ReviewEventType
import com.classmate.core.learning.ReviewPriorityLevel
import com.classmate.core.learning.ReviewTask
import com.classmate.app.ondevice.OnDeviceLlmController
import com.classmate.app.ondevice.OnDevicePermissionSnapshot
import com.classmate.core.ondevice.OnDeviceModelFileProbe
import com.classmate.app.platform.ConfigImportPreview
import com.classmate.app.platform.ConfigRepository
import com.classmate.app.platform.DebugConfigImporter
import com.classmate.app.platform.AiModelProviderMode
import com.classmate.app.platform.MaskedModelProfile
import com.classmate.app.platform.ModelApiProfile
import com.classmate.app.platform.ModelConfigRepository
import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.app.ui.i18n.AppLanguage
import com.classmate.app.ui.theme.AccentColorPreset
import com.classmate.app.ui.theme.CustomPalette
import com.classmate.app.ui.theme.ThemePreset
import com.classmate.app.ui.theme.TypographyPreset
import com.classmate.core.ask.GroundedAskLessonEngine
import com.classmate.core.ask.LocalAskLessonEngine
import com.classmate.core.ai.AiCapability
import com.classmate.core.ai.AiCapabilityResult
import com.classmate.core.ai.AiCapabilityRouter
import com.classmate.core.ai.AiEnhancement
import com.classmate.core.ai.AiEnhancementType
import com.classmate.core.ai.EnhancementPoint
import com.classmate.core.ai.EnhancementPromptBuilder
import com.classmate.core.ai.LocalEnhancementTemplates
import com.classmate.core.ai.RoutedEnhancementUseCase
import com.classmate.core.ai.VariantQuizParser
import com.classmate.core.analysis.CourseDomainDetector
import com.classmate.core.glossary.DynamicGlossaryExtractor
import com.classmate.core.glossary.GlossarySource
import com.classmate.core.provider.AnalysisEstimateInput
import com.classmate.core.provider.AnalysisIntensity
import com.classmate.core.provider.AnalysisTimeEstimator
import com.classmate.core.provider.BlueLmConfigDoctor
import com.classmate.core.provider.BlueLmConfigState
import com.classmate.core.ai.AiExecutionStatus
import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.ai.AiRouteDecision
import com.classmate.core.ai.AiStage
import com.classmate.core.ai.StageOutcome
import com.classmate.core.learning.OnDeviceLocalSuggestion
import com.classmate.core.capture.CaptureError
import com.classmate.core.capture.CaptureResult
import com.classmate.core.capture.ImageStudyDraft
import com.classmate.core.ondevice.CompositeAskChatSeam
import com.classmate.core.ondevice.OnDeviceAnalysisDiagnostic
import com.classmate.core.ondevice.OnDeviceCourseAnalysis
import com.classmate.core.ondevice.OnDeviceImageDraftResult
import com.classmate.core.ondevice.RgbImage
import com.classmate.core.ondevice.OnDeviceGenerationResult
import com.classmate.core.ondevice.OnDeviceLlmDiagnostic
import com.classmate.core.ondevice.OnDeviceLlmTaskProfile
import com.classmate.core.ondevice.OnDevicePromptTemplate
import com.classmate.core.provider.Credential
import com.classmate.core.provider.HttpTimeouts
import com.classmate.core.provider.LearnerProfile
import com.classmate.core.provider.ProviderAskChatClient
import com.classmate.core.analysis.AnalysisOutcome
import com.classmate.core.ai.CourseAnalysisRouting
import com.classmate.core.analysis.CourseAnalyzer
import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.feedback.LearningStateUpdater
import com.classmate.core.importing.ImportHub
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.library.CourseLibraryBuilder
import com.classmate.core.library.CourseRecordSnapshot
import com.classmate.core.logging.RedactedLogEntry
import com.classmate.core.live.TranscriptController
import com.classmate.core.live.TranscriptSession
import com.classmate.core.live.TranscriptStatus
import com.classmate.core.material.SpeakerLabel
import com.classmate.core.transcript.TranscriptDraft
import com.classmate.core.transcript.TranscriptParser
import com.classmate.core.transcript.TranscriptSegmentDraft
import com.classmate.core.transcript.TranscriptSourceType
import com.classmate.core.mindmap.MindMapBuilder
import com.classmate.core.model.FeedbackEvent
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import com.classmate.core.model.Ids
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.LearningState
import com.classmate.core.model.QuizAttempt
import com.classmate.core.model.QuizOption
import com.classmate.core.model.QuizQuestion
import com.classmate.core.model.QuestionType
import com.classmate.core.model.SourceKind
import com.classmate.core.prompt.Prompt
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.provider.AnalysisRequest
import com.classmate.core.provider.BlueLMDiagnosticReport
import com.classmate.core.provider.BlueLMDiagnosticRunner
import com.classmate.core.provider.BlueLmSigner
import com.classmate.core.provider.CompatibleDiagnosticReport
import com.classmate.core.provider.CompatibleDiagnosticRunner
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.ProviderConfig
import com.classmate.core.provider.ProviderConfigBundle
import com.classmate.core.provider.ProviderResolver
import com.classmate.core.provider.UnconfiguredBlueLmSigner
import com.classmate.core.model.ProviderKind
import com.classmate.core.review.ReviewPlanner
import com.classmate.core.sample.SampleCourses
import com.classmate.core.video.VideoRecommendationEngine
import com.classmate.core.weakness.WeaknessHub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

/**
 * The single source of truth for the UI. Holds the in-memory back stack and the learning-loop
 * state, and is the ONLY thing that touches the core pipeline — screens never see a provider.
 *
 * L3 study state is persisted through app-private repositories. Real BlueLM credentials are
 * injected through the config pipeline and never stored in the L3 snapshot.
 */
class AppViewModel(
    private val configRepository: ConfigRepository = ConfigRepository(),
    private val transport: HttpTransport = BlueLMHttpTransport(),
    private val blueLmSigner: BlueLmSigner = UnconfiguredBlueLmSigner,
    private val historyStore: HistoryStore = InMemoryHistoryStore(),
    private val learningStore: LearningStore = InMemoryLearningStore(),
    private val exportStore: ExportStore = InMemoryExportStore(),
    // Persistent official-model config (survives restart). Disabled no-op by default so existing
    // call sites/tests see no change; the composition root opts in with a filesDir-backed file.
    private val modelConfigRepository: ModelConfigRepository = ModelConfigRepository.disabled(),
    // Persistent appearance config. Separate from AI config and contains no secrets.
    private val themePreferenceRepository: ThemePreferenceRepository = ThemePreferenceRepository.disabled(),
    // App-private semantic index. Stores local lexical vectors only; no provider secrets or endpoint data.
    private val semanticIndexRepository: LocalSemanticIndexRepository = LocalSemanticIndexRepository.disabled(),
    // App-private L3 state. Stores study artifacts only: no credentials, keys, endpoints, or local config.
    private val l3PersistenceRepository: L3PersistenceRepository = L3PersistenceRepository.disabled(),
    private val evidenceAssetStore: EvidenceAssetStore = EvidenceAssetStore.disabled(),
    private val localTtsPlayer: LocalTtsPlayer = NoOpLocalTtsPlayer(),
    // Official TTS WebSocket provider (云端). Null until the composition root injects the OkHttp-backed one,
    // so unit tests run with the system-TTS path only. Used config-gated, with a system-TTS fallback.
    private val officialTtsProvider: com.classmate.app.asr.OfficialTtsProvider? = null,
    private val officialRuntimeGateway: OfficialRuntimeGateway = OfficialRuntimeGatewayFactory.production(),
    // On-device BlueLM 3B owner. Defaults to the honest missing-SDK bridge until the AAR is bundled.
    private val onDeviceController: OnDeviceLlmController = OnDeviceLlmController(),
    // Lazy so constructing the VM never reads capture credentials; OCR/ASR config is loaded only when
    // the user actually invokes those capture paths. Tests inject a fake gateway.
    private val captureGatewayProvider: () -> CaptureGatewayPort = { CaptureGateway() },
    private val classroomAudioRecorder: ClassroomAudioRecorder = NoOpClassroomAudioRecorder,
    private val recordingFileManager: RecordingFileManager = RecordingFileManager.disabled(),
    // P1-2: Flow background music. No-op by default (tests / no audio); the composition root injects the
    // real AmbientSoundPlayer-backed controller so playback survives leaving the Flow page.
    private val flowAudioController: FlowAudioController = NoOpFlowAudioController,
    // Stage 8D-2: optional override of the all-files-access signal (tests inject false/true). Used
    // ONLY to CLASSIFY an on-device availability failure (PERMISSION_MISSING vs files/init) — it
    // never blocks the crash-safe generate attempt. Production (null) combines the live permission
    // snapshot with the text-init positive signal, so a model that already generated text can never
    // be re-classified as permission-missing.
    private val allFilesAccessProvider: (() -> Boolean)? = null,
) : ViewModel() {

    private val promptBuilder = PromptBuilder()
    private val contentExporter = ContentExporter()
    private val captureGateway: CaptureGatewayPort by lazy(captureGatewayProvider)
    private val practiceGeneration = RoutedPracticeGenerationUseCase()
    private val internalTools = InternalFunctionRouter(practiceGeneration)
    private val aiConfigPromptedFeatures = mutableSetOf<String>()
    private val l3Pipeline = L3LearningPipeline()

    // THE single active provider config — one source of truth. The Settings model-config
    // summary, CourseAnalyzer (analyzer()) and the BlueLM diagnostic ALL read this exact field,
    // so they can never diverge. It changes ONLY through applyActiveConfig().
    // Default remains inert/fallback-safe; config.local.json may replace credentials locally.
    private val initialConfig = configRepository.loadLocalOrDefault()
    // The active bundle merges any persisted official-model profile (credentials from app-private
    // storage) onto the loaded bundle. The merge ONLY swaps the BlueLM credential/baseUrl/model —
    // it never changes the resolver order, profile, or request building (qwen guard intact).
    private var configBundle: ProviderConfigBundle = applyPersistedModelProfile(initialConfig.bundle)
    private val initialConfigSource: String =
        if (modelConfigRepository.hasUsableProfile()) "saved model config" else initialConfig.summary.source
    private val initialThemePreference = themePreferenceRepository.load()

    /** The active bundle that both the analyzer and the diagnostic run against (also for tests). */
    internal fun activeConfigBundle(): ProviderConfigBundle = configBundle

    /**
     * Stage 8C Phase A: the course-analysis main chain is CLOUD-ONLY. LOCAL_FALLBACK is filtered out
     * so the deterministic rule path can never produce a persisted "knowledge point analysis"; when the
     * cloud fails the app tries the on-device BlueLM 3B seam, then a safety placeholder. The class stays
     * for core-level tests, but the product never routes analysis through it.
     */
    private fun cloudOnlyBundle(): ProviderConfigBundle {
        val order = configBundle.order.filterNot { it == ProviderKind.LOCAL_FALLBACK }
        if (order == configBundle.order) return configBundle
        return configBundle.copy(primary = order.firstOrNull() ?: configBundle.primary, order = order)
    }

    private fun analyzer(): CourseAnalyzer =
        CourseAnalyzer(ProviderResolver(cloudOnlyBundle(), promptBuilder, transport, blueLmSigner))

    /** Test hook: the course-analysis chain order (must never include LOCAL_FALLBACK — Phase A). */
    internal fun analysisProviderOrderForTest(): List<ProviderKind> = cloudOnlyBundle().order

    var ui by mutableStateOf(
        ClassMateUiState(
            theme = initialThemePreference.themePreset,
            accentColor = initialThemePreference.accentColorPreset,
            customPalette = initialThemePreference.customPalette,
            typographyPreset = initialThemePreference.typographyPreset,
            language = initialThemePreference.language,
            enableExperimentalImageGeneration = initialThemePreference.enableExperimentalImageGeneration,
            enableExperimentalVideoGeneration = initialThemePreference.enableExperimentalVideoGeneration,
            enableExperimentalSimultaneousInterpretation = initialThemePreference.enableExperimentalSimultaneousInterpretation,
            providerConfigSummary = providerSummary(initialConfigSource),
            onDeviceDiagnostic = onDeviceController.diagnostic(),
            onDeviceModelPath = onDeviceController.diagnostic().modelDir,
            modelConfigMasked = modelConfigRepository.masked(),
            localProviderPath = onDeviceController.localPathZh(),
        ),
    )
        private set

    // --- bottom-navigation tabs ---
    var currentTab by mutableStateOf(Tab.HOME)
        private set

    /** Switch tab and reset to that tab's root. Learning data lives in [ui], so it is preserved. */
    fun selectTab(tab: Tab) {
        currentTab = tab
        resetTo(tab.root)
        settingsPage = SettingsPage.SETTINGS_HOME // entering a tab root always starts the settings tree at home
    }

    init {
        // Small one-time read of local history + cross-course learning state. Synchronous on
        // purpose so construction needs no Main dispatcher (keeps the ViewModel unit-testable);
        // both files are tiny. Home/Review/History then share this one snapshot.
        val persistedL3 = l3PersistenceRepository.loadSnapshot()
        ui = ui.copy(
            history = historyStore.load(),
            learningSnapshot = learningStore.snapshot(),
            l3Pipeline = persistedL3,
        )
        recordingFileManager.cleanupOrphans(emptySet(), deleteUnknown = false)
    }

    private fun syncLearning(toast: String? = null) {
        ui = ui.copy(learningSnapshot = learningStore.snapshot(), toast = toast ?: ui.toast)
    }

    // --- navigation (compose-observable back stack) ---
    private val backStack = mutableStateListOf(Screen.HOME)
    val currentScreen: Screen get() = backStack.last()
    val canGoBack: Boolean get() = backStack.size > 1

    fun navigateTo(screen: Screen) {
        if (backStack.last() != screen) backStack.add(screen)
    }

    fun goBack(): Boolean =
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            true
        } else {
            false
        }

    /**
     * TopBar back guard (P1-1): pop if there is a previous page, otherwise fall back to Home. A back button
     * must NEVER strand the user on a rootless page or silently do nothing — when the stack is empty it
     * returns to Home instead. (System back at the Home root still exits the app, which is expected.)
     */
    fun goBackOrHome() {
        if (!goBack()) selectTab(Tab.HOME)
    }

    fun resetTo(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    private fun navigateReplacing(screen: Screen) {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        navigateTo(screen)
    }

    // --- unified system-back handling (settings sub-pages live in-page, not on the back stack) ---
    var settingsPage: SettingsPage by mutableStateOf(SettingsPage.SETTINGS_HOME)
        private set

    fun openSettingsPage(page: SettingsPage) { settingsPage = page }
    fun resetSettingsPage() { settingsPage = SettingsPage.SETTINGS_HOME }

    private val onSettingsSubPage: Boolean
        get() = currentScreen == Screen.SETTINGS && settingsPage != SettingsPage.SETTINGS_HOME

    private val recordingActive: Boolean
        get() = ui.currentRecording?.status == L3RecordingStatus.RECORDING

    /** True when a system-back press has somewhere to go inside the app (so we must NOT exit the app). */
    val canHandleSystemBack: Boolean
        get() = recordingActive || onSettingsSubPage || canGoBack

    /**
     * Single entry point for BOTH the system back key/gesture AND the top-left back button, so the two
     * never diverge. Order: protect an in-progress recording -> walk up the settings sub-page tree ->
     * pop the app back stack. Returns true when handled in-app.
     */
    fun handleSystemBack(): Boolean {
        if (recordingActive) { // never silently drop an in-progress recording/transcription
            ui = ui.copy(showRecordingBackPrompt = true)
            return true
        }
        if (onSettingsSubPage) {
            settingsPage = settingsPage.parent ?: SettingsPage.SETTINGS_HOME
            return true
        }
        return goBack()
    }

    fun dismissRecordingBackPrompt() { ui = ui.copy(showRecordingBackPrompt = false) }

    fun stopRecordingFromBackPrompt() {
        ui = ui.copy(showRecordingBackPrompt = false)
        stopRecordingWithTranscription()
    }

    fun cancelRecordingFromBackPrompt() {
        ui = ui.copy(showRecordingBackPrompt = false)
        cancelRecordingWithTranscription()
    }

    // --- appearance ---
    fun setTheme(option: ThemePreset) {
        val next = themePreferenceRepository.saveThemePreset(option)
        ui = ui.copy(theme = next.themePreset, accentColor = next.accentColorPreset, customPalette = next.customPalette, typographyPreset = next.typographyPreset)
    }

    fun setAccentColor(accent: AccentColorPreset) {
        val next = themePreferenceRepository.saveAccentColorPreset(accent)
        ui = ui.copy(theme = next.themePreset, accentColor = next.accentColorPreset, customPalette = next.customPalette, typographyPreset = next.typographyPreset)
    }
    fun setCustomPalette(customPalette: CustomPalette) {
        val next = themePreferenceRepository.saveCustomPalette(customPalette)
        ui = ui.copy(
            theme = next.themePreset,
            accentColor = next.accentColorPreset,
            customPalette = next.customPalette,
            typographyPreset = next.typographyPreset,
            toast = if (next.customPalette.enabled) "自定义色彩已应用。" else "自定义色彩已关闭。",
        )
    }
    fun setTypographyPreset(preset: TypographyPreset) {
        val next = themePreferenceRepository.saveTypographyPreset(preset)
        ui = ui.copy(
            theme = next.themePreset,
            accentColor = next.accentColorPreset,
            customPalette = next.customPalette,
            typographyPreset = next.typographyPreset,
            toast = "字体风格已应用。",
        )
    }
    fun resetCustomPalette() {
        val next = themePreferenceRepository.saveCustomPalette(CustomPalette.Default)
        ui = ui.copy(
            theme = next.themePreset,
            accentColor = next.accentColorPreset,
            customPalette = next.customPalette,
            typographyPreset = next.typographyPreset,
            toast = "高级颜色已恢复默认。",
        )
    }
    fun resetAdvancedAppearance() {
        val next = themePreferenceRepository.resetAdvancedAppearance()
        ui = ui.copy(
            theme = next.themePreset,
            accentColor = next.accentColorPreset,
            customPalette = next.customPalette,
            typographyPreset = next.typographyPreset,
            toast = "高级外观已恢复默认。",
        )
    }
    fun setDarkMode(dark: Boolean?) { ui = ui.copy(darkMode = dark) }
    fun setLanguage(language: AppLanguage) {
        // Persist so the choice survives app restarts (P0-1: language was reverting to Chinese on relaunch).
        themePreferenceRepository.saveLanguage(language)
        ui = ui.copy(language = language)
    }
    fun setExperimentalImageGeneration(enabled: Boolean) {
        val next = themePreferenceRepository.saveExperimentalImageGeneration(enabled)
        ui = ui.copy(
            enableExperimentalImageGeneration = next.enableExperimentalImageGeneration,
            toast = if (enabled) "实验性学习图解入口已开启。" else "实验性学习图解入口已关闭。",
        )
    }
    fun setExperimentalVideoGeneration(enabled: Boolean) {
        val next = themePreferenceRepository.saveExperimentalVideoGeneration(enabled)
        ui = ui.copy(
            enableExperimentalVideoGeneration = next.enableExperimentalVideoGeneration,
            toast = if (enabled) "实验性复习短视频入口已开启。" else "实验性复习短视频入口已关闭。",
        )
    }
    fun setExperimentalSimultaneousInterpretation(enabled: Boolean) {
        val next = themePreferenceRepository.saveExperimentalSimultaneousInterpretation(enabled)
        ui = ui.copy(
            enableExperimentalSimultaneousInterpretation = next.enableExperimentalSimultaneousInterpretation,
            toast = if (enabled) "实验性双语课堂同声传译入口已开启。" else "实验性双语课堂同声传译入口已关闭。",
        )
    }
    fun setAudioDialectMode(mode: DialectMode) {
        ui = ui.copy(
            audioDialectMode = mode,
            toast = when (mode) {
                DialectMode.AUTO -> "课堂转写模式：自动。"
                DialectMode.STANDARD_MANDARIN -> "课堂转写模式：普通课堂。"
                DialectMode.DIALECT_OR_ACCENT_ENHANCED -> "课堂转写模式：口音/方言增强。"
                DialectMode.CLASSROOM_MIXED_SPEAKERS -> "课堂转写模式：多人课堂/混合口音。"
            },
        )
    }

    fun generateVisualStudyPrompt(now: Long = System.currentTimeMillis()): Boolean {
        if (!ui.enableExperimentalImageGeneration) {
            ui = ui.copy(toast = "请先在实验性功能中开启学习图解生成。")
            return false
        }
        val asset = ExperimentalStudyAssetEngine.visualPrompt(ui.l3Pipeline, now)
        if (asset == null) {
            ui = ui.copy(toast = "当前缺少知识点或证据，无法生成学习图解提示词。")
            return false
        }
        val next = refreshCapabilityMatrix(
            ui.l3Pipeline.copy(visualStudyAssets = (ui.l3Pipeline.visualStudyAssets + asset).takeLast(20)),
        )
        ui = ui.copy(l3Pipeline = next, toast = "已生成学习图解提示词；图片生成待配置。")
        persistL3(next)
        return true
    }

    fun generateReviewVideoStoryboard(now: Long = System.currentTimeMillis()): Boolean {
        if (!ui.enableExperimentalVideoGeneration) {
            ui = ui.copy(toast = "请先在实验性功能中开启复习短视频生成。")
            return false
        }
        val plan = ExperimentalStudyAssetEngine.reviewVideoPlan(ui.l3Pipeline, now)
        if (plan == null) {
            ui = ui.copy(toast = "当前缺少知识点或复习任务，无法生成短视频分镜。")
            return false
        }
        val next = refreshCapabilityMatrix(
            ui.l3Pipeline.copy(reviewVideoPlans = (ui.l3Pipeline.reviewVideoPlans + plan).takeLast(20)),
        )
        ui = ui.copy(l3Pipeline = next, toast = "已生成复习短视频分镜；视频生成待配置。")
        persistL3(next)
        return true
    }

    fun generateBilingualTranscriptDraft(now: Long = System.currentTimeMillis()): Boolean {
        if (!ui.enableExperimentalSimultaneousInterpretation) {
            ui = ui.copy(toast = "请先在实验性功能中开启双语课堂同声传译。")
            return false
        }
        val segments = ExperimentalStudyAssetEngine.bilingualTranscript(ui.l3Pipeline, now)
        if (segments.isEmpty()) {
            ui = ui.copy(toast = "当前没有可用于双语转写的音频或转写证据。")
            return false
        }
        val next = refreshCapabilityMatrix(
            ui.l3Pipeline.copy(bilingualTranscriptSegments = (ui.l3Pipeline.bilingualTranscriptSegments + segments).takeLast(40)),
        )
        ui = ui.copy(l3Pipeline = next, toast = "已生成双语转写草稿；同声传译服务待配置。")
        persistL3(next)
        return true
    }

    fun generateAudioReviewScript(now: Long = System.currentTimeMillis()): Boolean {
        val asset = ExperimentalStudyAssetEngine.audioReviewScript(ui.l3Pipeline, now)
        if (asset == null) {
            ui = ui.copy(toast = "当前缺少复习任务或知识点，无法生成听背脚本。")
            return false
        }
        val next = refreshCapabilityMatrix(
            ui.l3Pipeline.copy(audioReviewAssets = (ui.l3Pipeline.audioReviewAssets + asset).takeLast(20)),
        )
        ui = ui.copy(l3Pipeline = next, toast = "已生成听背文稿，可复制或分享复习；如需音频可点「生成听背音频」。")
        persistL3(next)
        return true
    }

    /**
     * P0-2: synthesize the 听背文稿 into a REAL on-device audio file via the system TextToSpeech engine.
     * Honest source label "系统 TTS 生成" (never 蓝心 TTS); 0-byte / failed output is deleted and the text
     * script stays available. Re-generating replaces the previous file; course deletion cleans it up.
     */
    fun generateAudioReviewFile() {
        val script = ui.l3Pipeline.audioReviewAssets.lastOrNull()?.script?.takeIf { it.isNotBlank() }
            ?: run {
                if (!generateAudioReviewScript()) return
                ui.l3Pipeline.audioReviewAssets.lastOrNull()?.script.orEmpty()
            }
        if (script.isBlank()) {
            ui = ui.copy(toast = "暂无可朗读的听背文稿。")
            return
        }
        deleteTtsAudioFileOnly()
        val now = System.currentTimeMillis()
        val courseId = ui.session?.id ?: ui.result?.sessionId ?: "course"
        val fileName = "tts_${courseId}_$now.wav"
        ui = ui.copy(ttsAudio = TtsAudioUiState(running = true))
        // Route: 官方 TTS WebSocket → 系统 Android TTS → 仅保留文稿. Each step is honestly labelled.
        val officialConfig = officialTtsConfig()
        val provider = officialTtsProvider
        viewModelScope.launch {
            if (provider != null && officialConfig != null) {
                val official = withContext(Dispatchers.IO) {
                    runCatching { provider.synthesizeToFile(officialConfig, script, fileName) }.getOrNull()
                }
                if (official != null && official.success && official.filePath.isNotBlank()) {
                    ui = ui.copy(
                        ttsAudio = TtsAudioUiState(
                            filePath = official.filePath,
                            fileName = fileName,
                            sizeBytes = official.sizeBytes,
                            sourceZh = "官方 TTS 生成",
                        ),
                    )
                    return@launch
                }
            }
            if (localTtsPlayer.canAttemptLocalPlayback()) {
                localTtsPlayer.synthesizeToFile("tts_$now", script, fileName) { result ->
                    viewModelScope.launch {
                        ui = if (result.success && result.filePath.isNotBlank()) {
                            ui.copy(
                                ttsAudio = TtsAudioUiState(
                                    filePath = result.filePath,
                                    fileName = fileName,
                                    sizeBytes = result.sizeBytes,
                                    sourceZh = "系统 TTS 生成",
                                ),
                            )
                        } else {
                            ui.copy(ttsAudio = TtsAudioUiState(failed = true, message = "音频生成失败，已保留听背文稿。", sourceZh = "仅生成文稿"))
                        }
                    }
                }
            } else {
                ui = ui.copy(ttsAudio = TtsAudioUiState(failed = true, message = "系统 TTS 不可用，已保留听背文稿。", sourceZh = "TTS 不可用"))
            }
        }
    }

    /** Build the official TTS WS config from the in-memory cloud credential. Null when no AppKey is configured. */
    private fun officialTtsConfig(): com.classmate.core.official.ws.OfficialWsConfig? {
        val appKey = officialCloudAppKeyOrNull() ?: return null
        return com.classmate.core.official.ws.OfficialWsConfig(
            baseUrl = com.classmate.core.official.ws.OfficialTtsWsProtocol.DEFAULT_URL,
            appKey = appKey,
            userId = stableOfficialUserId(),
        )
    }

    /** Reads the BlueLM AppKey from the in-memory bundle. NEVER logged/exported (Credential.toString masks it). */
    private fun officialCloudAppKeyOrNull(): String? {
        val cred = configBundle.configOf(ProviderKind.BLUELM)?.credential as? Credential.BlueLm ?: return null
        return cred.appKey.takeIf { it.isNotBlank() }
    }

    /** A stable, non-privacy 32-char id for the official `user_id` param, generated once and persisted. */
    private fun stableOfficialUserId(): String {
        themePreferenceRepository.load().officialUserId.takeIf { it.isNotBlank() }?.let { return it }
        val generated = (java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID().toString())
            .filter { it.isLetterOrDigit() }.lowercase().take(32)
        themePreferenceRepository.saveOfficialUserId(generated)
        return generated
    }

    fun deleteTtsAudio() {
        deleteTtsAudioFileOnly()
        ui = ui.copy(ttsAudio = TtsAudioUiState(), toast = "已删除听背音频。")
    }

    private fun deleteTtsAudioFileOnly() {
        ui.ttsAudio.filePath.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { java.io.File(path).takeIf { it.exists() }?.delete() }
        }
    }

    private fun refreshCapabilityMatrix(snapshot: L3PipelineSnapshot): L3PipelineSnapshot =
        snapshot.copy(
            officialCapabilityContributions = OfficialCapabilityRegistry.officialMatrix(snapshot, ui.providerConfigSummary),
            qualityWarnings = LearningLoopCapabilityOrchestrator.qualityWarnings(snapshot),
        )

    // --- provider config (single source of truth: configBundle) ---

    /**
     * The ONLY place [configBundle] changes, so the displayed summary always matches the bundle
     * the analyzer and diagnostic will use. The credential lives in memory only — it is never
     * written to config.local.json or the repository.
     */
    private fun applyActiveConfig(bundle: ProviderConfigBundle, source: String, toast: String?) {
        configBundle = bundle
        ui = ui.copy(providerConfigSummary = providerSummary(source), toast = toast)
    }

    fun importDebugProviderConfig(jsonText: String, debugEnabled: Boolean = com.classmate.app.BuildConfig.DEBUG): ConfigImportPreview {
        val preview = DebugConfigImporter.inspect(jsonText, debugEnabled)
        val imported = preview.runtimeConfig
        if (preview.valid && imported != null) {
            val blueLmReady = imported.configOf(ProviderKind.BLUELM)?.hasRealCredential() == true
            applyActiveConfig(
                bundle = imported,
                source = "debug import",
                toast = if (blueLmReady) {
                    "已导入 BlueLM 配置到本次会话（仅内存，不写入仓库）"
                } else {
                    "已导入配置；BlueLM 仍未配置真实凭据"
                },
            )
        } else {
            ui = ui.copy(toast = preview.message)
        }
        return preview
    }

    fun reloadLocalProviderConfig() {
        val result = configRepository.loadLocalOrDefault()
        applyActiveConfig(
            bundle = result.bundle,
            source = result.summary.source,
            toast = result.error?.message ?: "已读取本地 provider 配置",
        )
    }

    fun testBlueLmConnection(debugEnabled: Boolean = com.classmate.app.BuildConfig.DEBUG) {
        if (!debugEnabled) return
        ui = ui.copy(blueLmDiagnosticRunning = true, blueLmDiagnostic = null)
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { runBlueLmConnectionDiagnostic() }
            ui = ui.copy(
                blueLmDiagnosticRunning = false,
                blueLmDiagnostic = report,
            )
        }
    }

    internal fun runBlueLmConnectionDiagnostic(): BlueLMDiagnosticReport =
        BlueLMDiagnosticRunner(transport).run(configBundle.configOf(ProviderKind.BLUELM))

    /**
     * Official-path connection test for the competition main config page. Unlike
     * [testBlueLmConnection] this is NOT debug-gated — testing the official BlueLM cloud model is a
     * first-class action. It sends only the safe "请只回复 OK" diagnostic probe and surfaces a
     * redacted report (never the prompt/body/credentials).
     */
    fun testOfficialModelConnection() {
        ui = ui.copy(blueLmDiagnosticRunning = true, blueLmDiagnostic = null)
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { runBlueLmConnectionDiagnostic() }
            ui = ui.copy(blueLmDiagnosticRunning = false, blueLmDiagnostic = report)
        }
    }

    /** Debug-only custom-model connectivity probe; never presented as the official cloud path. */
    fun testCompatibleConnection(debugEnabled: Boolean = com.classmate.app.BuildConfig.DEBUG) {
        if (!debugEnabled) return
        ui = ui.copy(compatibleDiagnosticRunning = true, compatibleDiagnostic = null)
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { runCompatibleConnectionDiagnostic() }
            ui = ui.copy(compatibleDiagnosticRunning = false, compatibleDiagnostic = report)
        }
    }

    internal fun runCompatibleConnectionDiagnostic(): CompatibleDiagnosticReport =
        CompatibleDiagnosticRunner(transport).run(configBundle.configOf(ProviderKind.COMPATIBLE))

    private fun providerSummary(source: String): ProviderConfigSummary =
        ProviderConfigSummary.fromBundle(
            source = source,
            bundle = configBundle,
            primaryReady = ProviderResolver(configBundle, promptBuilder, transport, blueLmSigner).isPrimaryReady(),
        )

    // --- persistent official-model config (P5): 蓝心大模型 saved across restarts ---

    /**
     * Overlay persisted model credentials from app-private storage onto [base]. Official BlueLM stays
     * on [ProviderKind.BLUELM]; custom models reuse the existing compatible provider instead of
     * introducing another network path. No value is logged or written back to config.local.json.
     */
    private fun applyPersistedModelProfile(base: ProviderConfigBundle): ProviderConfigBundle {
        val profile = modelConfigRepository.load() ?: return base
        if (!profile.hasRealCredential()) return base
        return when (profile.mode) {
            AiModelProviderMode.OFFICIAL_BLUELM -> {
                val blueLm = base.configOf(ProviderKind.BLUELM) ?: return base
                val updated = blueLm.copy(
                    enabled = true,
                    baseUrl = profile.baseUrl.ifBlank { blueLm.baseUrl },
                    model = profile.model.ifBlank { blueLm.model },
                    credential = Credential.BlueLm(profile.appId, profile.appKey),
                )
                ProviderConfigBundle.forProfile(
                    profile = LearnerProfile.OFFICIAL_BLUELM,
                    configs = base.configs + (ProviderKind.BLUELM to updated),
                    policy = base.policy,
                )
            }
            AiModelProviderMode.CUSTOM -> {
                val compatible = base.configOf(ProviderKind.COMPATIBLE)
                    ?: ProviderConfig(kind = ProviderKind.COMPATIBLE, enabled = true)
                val updated = compatible.copy(
                    enabled = true,
                    baseUrl = profile.customBaseUrl(compatible.baseUrl.ifBlank { ModelApiProfile.DEFAULT_BASE_URL }),
                    model = profile.customModel(compatible.model.ifBlank { ModelApiProfile.DEFAULT_MODEL }),
                    credential = Credential.ApiKey(profile.customApiKey),
                )
                ProviderConfigBundle.forProfile(
                    profile = LearnerProfile.DEMO_COMPATIBLE,
                    configs = base.configs + (ProviderKind.COMPATIBLE to updated),
                    policy = base.policy,
                )
            }
        }
    }

    /** Masked view of the saved official-model config for Settings (never the raw secret). */
    fun savedModelConfig(): MaskedModelProfile? = ui.modelConfigMasked

    /**
     * Persist an official BlueLM config and apply it to the active bundle immediately. Credentials
     * are written ONLY to app-private storage; never logged, exported, or echoed back.
     */
    fun saveOfficialModelConfig(baseUrl: String, model: String, appId: String, appKey: String) {
        if (!modelConfigRepository.isEnabled) {
            ui = ui.copy(toast = "当前环境不支持持久化模型配置。")
            return
        }
        val saved = modelConfigRepository.saveOfficial(
            baseUrl = baseUrl,
            model = model,
            appId = appId,
            appKey = appKey,
        )
        if (saved) {
            configBundle = applyPersistedModelProfile(configRepository.loadLocalOrDefault().bundle)
        }
        val profile = modelConfigRepository.load()
        ui = ui.copy(
            providerConfigSummary = providerSummary("saved model config"),
            modelConfigMasked = modelConfigRepository.masked(),
            toast = when {
                BlueLmConfigDoctor.classify(appId, appKey) == BlueLmConfigState.MASKED_KEY_INVALID ->
                    "AppKey 看起来是掩码（***），请输入完整 AppKey 后再保存。"
                !saved -> "保存失败，请重试。"
                profile?.officialConfigured() == true -> "已保存蓝心大模型配置（仅本机，不写入仓库）。"
                else -> "已保存配置；凭据仍为占位符。"
            },
        )
    }

    fun saveCustomModelConfig(apiKey: String, advancedJson: String) {
        if (!modelConfigRepository.isEnabled) {
            ui = ui.copy(toast = "当前环境不支持持久化模型配置。")
            return
        }
        val saved = modelConfigRepository.saveCustom(apiKey = apiKey, advancedJson = advancedJson)
        if (saved) {
            configBundle = applyPersistedModelProfile(configRepository.loadLocalOrDefault().bundle)
        }
        val profile = modelConfigRepository.load()
        ui = ui.copy(
            providerConfigSummary = providerSummary("saved custom model config"),
            modelConfigMasked = modelConfigRepository.masked(),
            toast = when {
                !saved -> "高级 JSON 配置格式不正确，请检查后再保存。"
                profile?.customConfigured() == true -> "已保存自有模型配置（仅本机，不写入仓库）。"
                else -> "已保存自有模型设置；API Key 仍未配置。"
            },
        )
    }

    fun selectAiModelProviderMode(mode: AiModelProviderMode) {
        if (!modelConfigRepository.isEnabled) return
        if (modelConfigRepository.setMode(mode)) {
            configBundle = applyPersistedModelProfile(configRepository.loadLocalOrDefault().bundle)
            ui = ui.copy(
                providerConfigSummary = providerSummary("saved model mode"),
                modelConfigMasked = modelConfigRepository.masked(),
                toast = if (mode == AiModelProviderMode.CUSTOM) "已切换为自有模型。未配置时仍会端侧兜底。" else "已切换为蓝心大模型。",
            )
        }
    }

    /** Delete the saved official-model config; the official path becomes unconfigured again. */
    fun deleteOfficialModelConfig() {
        if (!modelConfigRepository.isEnabled) {
            ui = ui.copy(toast = "当前环境没有可删除的本机模型配置。")
            return
        }
        modelConfigRepository.deleteOfficial()
        configBundle = applyPersistedModelProfile(configRepository.loadLocalOrDefault().bundle)
        ui = ui.copy(
            providerConfigSummary = providerSummary("model config deleted"),
            modelConfigMasked = modelConfigRepository.masked(),
            toast = "已删除本机蓝心大模型配置。",
        )
    }

    fun deleteCustomModelConfig() {
        if (!modelConfigRepository.isEnabled) {
            ui = ui.copy(toast = "当前环境没有可删除的本机模型配置。")
            return
        }
        modelConfigRepository.deleteCustom()
        configBundle = applyPersistedModelProfile(configRepository.loadLocalOrDefault().bundle)
        ui = ui.copy(
            providerConfigSummary = providerSummary("custom model config deleted"),
            modelConfigMasked = modelConfigRepository.masked(),
            toast = "已删除本机自有模型配置。",
        )
    }

    fun testAiConfigReadiness() {
        val masked = modelConfigRepository.masked()
        // Config-STATE doctor: tell MISSING / INCOMPLETE / MASKED_KEY_INVALID / READY apart BEFORE any
        // request, so the user sees CONFIG_REQUIRED rather than a misleading NETWORK error. This is a
        // readiness check only — no network call here (the live probe is the debug BlueLM diagnostic).
        val profile = modelConfigRepository.load()
        val state = BlueLmConfigDoctor.classify(profile?.appId, profile?.appKey)
        ui = ui.copy(
            modelConfigMasked = masked,
            toast = when (state) {
                BlueLmConfigState.READY -> "配置就绪（READY）：当前仅做 readiness 检查，未发送网络请求。"
                BlueLmConfigState.MASKED_KEY_INVALID -> state.labelZh
                else -> "${state.labelZh}。未配置时仍可继续端侧处理或手动编辑。"
            },
        )
    }

    fun promptCloudConfigIfMissing(feature: String) {
        maybePromptMissingCloudConfig(feature)
    }

    fun dismissAiConfigPrompt() {
        ui = ui.copy(aiConfigPrompt = AiConfigPromptUiState.hidden())
    }

    fun goToAiConfigFromPrompt() {
        ui = ui.copy(
            aiConfigPrompt = AiConfigPromptUiState.hidden(),
            settingsDeepLink = SettingsDeepLink.AI_MODEL_CONFIG_BLUELM,
        )
        selectTab(Tab.SETTINGS)
    }

    fun consumeSettingsDeepLink() {
        if (ui.settingsDeepLink != SettingsDeepLink.NONE) {
            ui = ui.copy(settingsDeepLink = SettingsDeepLink.NONE)
        }
    }

    private fun maybePromptMissingCloudConfig(feature: String) {
        if (hasConfiguredCloudModel() || !aiConfigPromptedFeatures.add(feature)) return
        ui = ui.copy(aiConfigPrompt = AiConfigPromptUiState(visible = true, feature = feature))
    }

    private fun hasConfiguredCloudModel(): Boolean =
        configBundle.configOf(ProviderKind.BLUELM)?.hasRealCredential() == true ||
            configBundle.configOf(ProviderKind.COMPATIBLE)?.hasRealCredential() == true

    // --- on-device BlueLM 3B diagnostic (P6/P3) ---

    /** Refresh the functional-permission snapshot + the bounded model-file diagnostic (no content read). */
    fun refreshOnDevicePermissions(snapshot: OnDevicePermissionSnapshot) {
        ui = ui.copy(onDevicePermissions = snapshot)
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { OnDeviceModelFileProbe.probe(ui.onDeviceModelPath) }
            ui = ui.copy(onDeviceModelFiles = files)
        }
    }

    /**
     * Real on-device text self-test (P3/Task 5): refresh perms + file diagnostic, then — only if
     * all-files access is granted — init + ONE fixed-question generation off the UI thread. When not
     * granted, the controller returns a permission-blocked diagnostic WITHOUT calling native init.
     */
    fun testOnDeviceModel(snapshot: OnDevicePermissionSnapshot) {
        ui = ui.copy(onDevicePermissions = snapshot, onDeviceDiagnosticRunning = true)
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { OnDeviceModelFileProbe.probe(ui.onDeviceModelPath) }
            val report = onDeviceController.runTextDiagnostic(snapshot.allFilesAccess)
            ui = ui.copy(
                onDeviceDiagnosticRunning = false,
                onDeviceDiagnostic = report,
                onDeviceModelFiles = files,
                localProviderPath = onDeviceController.localPathZh(),
            )
        }
    }

    /**
     * Experimental multimodal VIT diagnostic (P4/Task 6). The controller refuses to touch native
     * init/callVit unless all-files access is granted AND a text init has already succeeded — this is
     * the multimodal crash guard. Does NOT feed the course material pipeline.
     */
    fun testOnDeviceMultimodal(snapshot: OnDevicePermissionSnapshot) {
        ui = ui.copy(onDevicePermissions = snapshot, onDeviceMultimodalRunning = true, onDeviceRealImageMeta = null)
        viewModelScope.launch {
            val report = onDeviceController.runMultimodalDiagnostic(snapshot.allFilesAccess)
            ui = ui.copy(
                onDeviceMultimodalRunning = false,
                onDeviceMultimodalDiagnostic = report,
            )
        }
    }

    /**
     * Stage 8E P0-4: REAL-image multimodal diagnostic — a user-picked photo runs callVit + generate
     * (same crash gate as the built-in 2x2 probe). Diagnostic only: nothing is persisted, nothing
     * enters CourseAnalysis; the card shows sizes / rgb length / callVit_ret / generate state / a
     * bounded preview.
     */
    fun testOnDeviceMultimodalWithImage(
        image: RgbImage,
        originalWidth: Int,
        originalHeight: Int,
        snapshot: OnDevicePermissionSnapshot,
    ) {
        if (ui.onDeviceMultimodalRunning) return // one multimodal task at a time
        val meta = "原图 ${originalWidth}x$originalHeight → 处理 ${image.width}x${image.height} · RGB ${image.bytes.size} 字节"
        ui = ui.copy(onDevicePermissions = snapshot, onDeviceMultimodalRunning = true, onDeviceRealImageMeta = meta)
        viewModelScope.launch {
            val report = onDeviceController.runRealImageDiagnostic(image, snapshot.allFilesAccess)
            ui = ui.copy(onDeviceMultimodalRunning = false, onDeviceMultimodalDiagnostic = report)
        }
    }

    /**
     * Stage 8E Phase 1: bounded model-directory candidate detection (user path + four fixed official
     * candidates, first-level file names only — never a disk scan). Feeds the Settings recommendation
     * banner and the 一键切换 button.
     */
    fun detectOnDeviceModelPath() {
        viewModelScope.launch {
            ui = ui.copy(modelPathDetection = onDeviceController.detectModelPath())
        }
    }

    /** Update the on-device model directory (P5) and refresh the snapshot. We never scan the path. */
    fun setOnDeviceModelPath(path: String) {
        val trimmed = path.trim()
        ui = ui.copy(onDeviceModelPath = trimmed, onDeviceDiagnosticRunning = true)
        viewModelScope.launch {
            val report = onDeviceController.updateModelPath(trimmed)
            val files = withContext(Dispatchers.IO) { OnDeviceModelFileProbe.probe(report.modelDir) }
            ui = ui.copy(
                onDeviceDiagnosticRunning = false,
                onDeviceDiagnostic = report,
                onDeviceModelPath = report.modelDir,
                onDeviceModelFiles = files,
                localProviderPath = onDeviceController.localPathZh(),
                // Stage 8E: re-detect candidates so the recommendation banner reflects the new path.
                modelPathDetection = onDeviceController.detectModelPath(),
            )
        }
    }

    /** The active on-device readiness snapshot (also for tests). */
    internal fun onDeviceDiagnosticReport(): OnDeviceLlmDiagnostic = onDeviceController.diagnostic()

    /**
     * Phase 7: "端侧独立模式检查" — run a structured CourseAnalysis on a small built-in text using ONLY
     * the on-device model (no cloud config required), and record the result code so Settings can show
     * whether the device can analyse a course offline. Validators still gate; nothing is persisted.
     */
    fun runOfflineOnDeviceAnalysisCheck() {
        ui = ui.copy(onDeviceAnalysisCheckRunning = true)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = CourseSegmenter.buildSession(
                id = "ondevice_check_$now",
                title = "端侧独立模式检查",
                rawText = OFFLINE_CHECK_TEXT,
                nowMs = now,
                sourceKind = SourceKind.PASTED_TEXT,
            )
            val run = onDeviceController.analyzeCourse(session, currentAllFilesAccess())
            val reason = when (val od = run.outcome) {
                is OnDeviceCourseAnalysis.Outcome.Accepted -> "ACCEPTED"
                is OnDeviceCourseAnalysis.Outcome.Rejected -> od.reason
            }
            ui = ui.copy(
                onDeviceAnalysisCheckRunning = false,
                onDeviceAnalysisReason = reason,
                onDeviceAnalysisDiagnostic = run.diagnostic,
            )
        }
    }

    // --- Stage 8B: on-device local-intelligence suggestions (Report C / Practice D) ---

    /**
     * Phase C: generate a local study suggestion for the current course using the on-device BlueLM.
     * Available → "由端侧 BlueLM 生成：…"; unavailable → fixed safety placeholder (never the rule path).
     * The result is also embedded into the exported StudyReport via [buildStudyReport].
     */
    fun generateOnDeviceReportSuggestion() {
        val session = ui.session ?: run { ui = ui.copy(toast = "请先打开已分析的课程。"); return }
        val result = ui.result ?: run { ui = ui.copy(toast = "请先完成课程分析。"); return }
        ui = ui.copy(onDeviceReportSuggestionRunning = true)
        viewModelScope.launch {
            val reviewTopics = result.knowledgePoints.take(6).map { it.title }
            val dueCount = ui.learningSnapshot.tasks.count { !it.manuallyRemoved }
            val prompt = OnDevicePromptTemplate.format(
                OnDeviceLocalSuggestion.buildReportPrompt(session.title, reviewTopics, dueCount),
            )
            val text = onDeviceText(OnDeviceLlmTaskProfile.REPORT, prompt)
            ui = ui.copy(
                onDeviceReportSuggestionRunning = false,
                onDeviceReportSuggestion = OnDeviceLocalSuggestion.label(text),
            )
        }
    }

    /**
     * Phase D: generate an on-device explanation / next-step for the latest practice result.
     * Available → on-device text; unavailable → safety placeholder (never a fabricated rule explanation).
     */
    fun generateOnDevicePracticeSuggestion() {
        val session = ui.session
        val practice = ui.practiceResult ?: run { ui = ui.copy(toast = "请先完成一轮练习。"); return }
        ui = ui.copy(onDevicePracticeSuggestionRunning = true)
        viewModelScope.launch {
            val weakTopics = practice.needPracticeItems.map { it.title }
                .ifEmpty { practice.relatedKnowledgePointTitles }
            val prompt = OnDevicePromptTemplate.format(
                OnDeviceLocalSuggestion.buildPracticePrompt(session?.title.orEmpty(), weakTopics, practice.wrongCount),
            )
            val text = onDeviceText(OnDeviceLlmTaskProfile.PRACTICE, prompt)
            ui = ui.copy(
                onDevicePracticeSuggestionRunning = false,
                onDevicePracticeSuggestion = OnDeviceLocalSuggestion.label(text),
            )
        }
    }

    /**
     * Phase 4: on-device review suggestion — explains why the due knowledge points need review and the
     * next step. Available → on-device text; unavailable → safety placeholder (never fabricated).
     */
    fun generateOnDeviceReviewSuggestion() {
        val dueTitles = ui.learningSnapshot.tasks
            .filterNot { it.manuallyRemoved }
            .sortedByDescending { it.priority }
            .take(6)
            .map { it.title }
        ui = ui.copy(onDeviceReviewSuggestionRunning = true)
        viewModelScope.launch {
            val prompt = OnDevicePromptTemplate.format(
                OnDeviceLocalSuggestion.buildReportPrompt("复习队列", dueTitles, dueTitles.size),
            )
            val text = onDeviceText(OnDeviceLlmTaskProfile.REPORT, prompt)
            ui = ui.copy(
                onDeviceReviewSuggestionRunning = false,
                onDeviceReviewSuggestion = OnDeviceLocalSuggestion.label(text),
            )
        }
    }

    /** Runs the on-device model and returns its text, or null when unavailable/failed (→ placeholder). */
    private suspend fun onDeviceText(profile: OnDeviceLlmTaskProfile, prompt: String): String? =
        when (val r = onDeviceController.generate(profile, prompt)) {
            is OnDeviceGenerationResult.Success -> r.text
            else -> null
        }

    // --- Stage 8C Phase C / 8E: image / photo → editable learning draft ---

    /** Open the image-draft editor. [origin] is the honest input label (图片学习输入 / 拍照学习输入). */
    fun beginImageDraft(origin: String? = null) {
        ui = ui.copy(
            ocrImports = ui.ocrImports.filter { it.batchId.isBlank() },
            imageDraftActive = true, imageDraftRunning = false, imageDraftText = "",
            imageDraftManualMode = false, imageDraftMessage = null,
            imageDraftOrigin = origin, imageDraftMeta = null,
            imageDraftImageRef = "", imageDraftThumbnailRef = "", imageDraftMimeType = "",
            imageDraftBatchId = "", imageDraftBatchTotal = 0, imageDraftBatchProcessed = 0,
        )
    }

    fun beginImageOcrBatch(origin: String? = null, total: Int, now: Long = System.currentTimeMillis()): String {
        val batchId = "ocr_batch_$now"
        ui = ui.copy(
            ocrImports = emptyList(),
            imageDraftActive = true,
            imageDraftRunning = total > 0,
            imageDraftText = "",
            imageDraftManualMode = false,
            imageDraftMessage = if (total > 0) "正在识别 $total 张图片，请稍后检查草稿。" else "未选择图片。",
            imageDraftOrigin = origin ?: "图片学习输入",
            imageDraftMeta = "0/$total",
            imageStudyDraft = null,
            imageDraftSource = null,
            imageDraftOcrError = null,
            imageDraftImageRef = "",
            imageDraftThumbnailRef = "",
            imageDraftMimeType = "",
            imageDraftBatchId = batchId,
            imageDraftBatchTotal = total.coerceAtLeast(0),
            imageDraftBatchProcessed = 0,
        )
        return batchId
    }

    /**
     * Run on-device multimodal understanding over a decoded RGB image (off the UI thread) to produce an
     * editable draft. On unavailable/failure the editor switches to manual input — never crashes, never
     * auto-writes to the knowledge base. Stage 8E: duplicate submissions while running are ignored, and
     * a new image overwrites the previous VIT encoding (the SDK caches exactly one image).
     */
    fun runImageDraft(
        image: RgbImage,
        allFilesGranted: Boolean,
        originalWidth: Int = 0,
        originalHeight: Int = 0,
        encodedImageBytes: ByteArray = image.bytes,
    ) {
        if (ui.imageDraftRunning) return // single in-flight multimodal task; repeat taps are no-ops
        maybePromptMissingCloudConfig("官方 OCR")
        val storedImage = evidenceAssetStore.saveImage(
            bytes = encodedImageBytes,
            sourceLabel = ui.imageDraftOrigin ?: "图片学习输入",
            mimeType = "image/jpeg",
        )
        val meta = buildString {
            if (originalWidth > 0 && originalHeight > 0) append("原图 ${originalWidth}x$originalHeight → ")
            append("处理 ${image.width}x${image.height} · RGB ${image.bytes.size} 字节")
        }
        ui = ui.copy(
            imageDraftActive = true, imageDraftRunning = true, imageDraftManualMode = false,
            imageDraftMessage = null, imageDraftMeta = meta, imageStudyDraft = null,
            imageDraftSource = null, imageDraftOcrError = null,
            imageDraftImageRef = storedImage.imageRef,
            imageDraftThumbnailRef = storedImage.thumbnailRef,
            imageDraftMimeType = storedImage.mimeType,
            aiProcessing = AiProcessingUiState(
                visible = true,
                title = "正在识别图片文字",
                steps = listOf("准备资料", "云端处理中", "端侧兜底", "等待确认"),
                activeStep = 1,
                source = "云端",
                canCancel = true,
                canRetry = true,
                canContinueManual = true,
            ),
        )
        viewModelScope.launch {
            val onDevice = onDeviceController.describeImageToDraft(image, allFilesGranted)
            val onDeviceText = (onDevice as? OnDeviceImageDraftResult.Draft)?.text.orEmpty()
            val routed = withContext(Dispatchers.IO) {
                runCatching {
                    captureGateway.createImageStudyDraftRouted(
                        imageBytes = encodedImageBytes,
                        origin = ui.imageDraftOrigin ?: "图片学习输入",
                        onDeviceDraftText = onDeviceText,
                    )
                }.getOrElse {
                    imageFallbackResult(
                        origin = ui.imageDraftOrigin ?: "图片学习输入",
                        onDeviceText = onDeviceText,
                        status = AiExecutionStatus.FAILED,
                    )
                }
            }
            applyImageStudyDraftResult(routed, onDevice)
        }
    }

    fun ingestMultiImageOcr(
        image: RgbImage,
        allFilesGranted: Boolean,
        originalWidth: Int = 0,
        originalHeight: Int = 0,
        encodedImageBytes: ByteArray = image.bytes,
        pageIndex: Int,
        total: Int,
        batchId: String = ui.imageDraftBatchId,
    ) {
        val activeBatch = batchId.ifBlank { beginImageOcrBatch(ui.imageDraftOrigin, total) }
        val now = System.currentTimeMillis()
        val origin = ui.imageDraftOrigin ?: "图片学习输入"
        val dimensionLabel = if (originalWidth > 0 && originalHeight > 0) " · ${originalWidth}x$originalHeight" else ""
        val storedImage = evidenceAssetStore.saveImage(
            bytes = encodedImageBytes,
            sourceLabel = "$origin 第${pageIndex}张$dimensionLabel",
            mimeType = "image/jpeg",
            now = now + pageIndex,
        )
        val draftId = "${activeBatch}_$pageIndex"
        val pending = OcrImportDraft(
            id = draftId,
            kind = OcrImportKind.SLIDE_IMAGE,
            fileMeta = OcrImportFileMeta(
                fileName = storedImage.imageRef,
                mimeType = storedImage.mimeType,
                sizeBytes = encodedImageBytes.size.toLong(),
                displayLabel = "图片$pageIndex$dimensionLabel",
                pageIndex = pageIndex,
            ),
            pastedText = "",
            status = OcrImportStatus.PENDING,
            batchId = activeBatch,
            pageIndex = pageIndex,
            blockIndex = pageIndex,
            createdAt = now,
            updatedAt = now,
        )
        ui = ui.copy(
            ocrImports = (ui.ocrImports.filterNot { it.id == draftId } + pending).sortedBy { it.pageIndex ?: Int.MAX_VALUE },
            imageDraftActive = true,
            imageDraftRunning = true,
            imageDraftMessage = "正在识别第 $pageIndex / $total 张图片。",
            imageDraftMeta = "${ui.imageDraftBatchProcessed}/$total",
            imageDraftBatchId = activeBatch,
            imageDraftBatchTotal = total,
        )
        viewModelScope.launch {
            val onDevice = onDeviceController.describeImageToDraft(image, allFilesGranted)
            val onDeviceText = (onDevice as? OnDeviceImageDraftResult.Draft)?.text.orEmpty()
            val routed = withContext(Dispatchers.IO) {
                runCatching {
                    captureGateway.createImageStudyDraftRouted(
                        imageBytes = encodedImageBytes,
                        origin = "$origin 第${pageIndex}张",
                        onDeviceDraftText = onDeviceText,
                    )
                }.getOrElse {
                    imageFallbackResult(
                        origin = "$origin 第${pageIndex}张",
                        onDeviceText = onDeviceText,
                        status = AiExecutionStatus.FAILED,
                    )
                }
            }
            val draft = routed.value
            val rawText = draft?.initialEditableText().orEmpty().ifBlank { onDeviceText }
            val clean = OcrTextPostProcessor.clean(rawText)
            // P0-1: distinguish "OCR 未配置" from "识别为空" so the failure reason is honest, and ALWAYS
            // leave the segment editable (the FAILED card now offers a manual-input field) so OCR being
            // unavailable on this device never makes the image a dead end.
            val unconfigured = draft?.ocrError == CaptureError.ConfigMissing ||
                routed.status == AiExecutionStatus.CONFIG_MISSING
            val completed = if (clean.text.isBlank()) {
                pending.copy(
                    status = OcrImportStatus.FAILED,
                    errorReason = if (unconfigured) {
                        "OCR 未配置，请在下方手动输入图片中的文字，或重拍更清晰的图片。"
                    } else {
                        "未识别到可用文字，请在下方手动输入，或重拍更清晰的图片。"
                    },
                    updatedAt = System.currentTimeMillis(),
                )
            } else {
                pending.copy(
                    pastedText = clean.text,
                    status = OcrImportStatus.OK,
                    errorReason = if (clean.needsReview) clean.reviewHint else "",
                    updatedAt = System.currentTimeMillis(),
                )
            }
            applyImageOcrBatchItem(completed, total)
        }
    }

    internal fun applyImageOcrBatchItem(draft: OcrImportDraft, total: Int = ui.imageDraftBatchTotal) {
        val imports = (ui.ocrImports.filterNot { it.id == draft.id } + draft)
            .sortedBy { it.pageIndex ?: Int.MAX_VALUE }
        val merged = OcrImportAssembler.mergeByOrder(imports)
        val processed = imports.count { it.status != OcrImportStatus.PENDING }
        val failedCount = merged.failedDrafts.size
        val reviewCount = merged.okDrafts.count { it.errorReason.isNotBlank() }
        val message = buildString {
            append("已处理 $processed / ${total.coerceAtLeast(imports.size)} 张图片。")
            if (failedCount > 0) append(" $failedCount 张需要手动补充。")
            if (reviewCount > 0) append(" $reviewCount 张建议人工检查识别结果。")
        }
        ui = ui.copy(
            ocrImports = imports,
            imageDraftText = merged.text,
            imageDraftRunning = processed < total.coerceAtLeast(imports.size),
            imageDraftManualMode = merged.okDrafts.isEmpty(),
            imageDraftMessage = message,
            imageDraftMeta = "$processed/${total.coerceAtLeast(imports.size)}",
            imageDraftBatchProcessed = processed,
            imageDraftSource = if (merged.okDrafts.isNotEmpty()) AiExecutionSource.ON_DEVICE else AiExecutionSource.MANUAL,
            aiProcessing = AiProcessingUiState.hidden(),
        )
    }

    /** Apply a draft result to UI state (separated for unit testing without a Main dispatcher). */
    internal fun applyImageDraftResult(result: OnDeviceImageDraftResult) {
        ui = when (result) {
            is OnDeviceImageDraftResult.Draft -> {
                val cleaned = OcrTextPostProcessor.clean(result.text)
                ui.copy(
                    imageDraftActive = true,
                    imageDraftRunning = false,
                    imageDraftText = cleaned.text,
                    imageDraftManualMode = false,
                    imageDraftSource = AiExecutionSource.ON_DEVICE,
                    aiProcessing = AiProcessingUiState.hidden(),
                    imageDraftMessage = "已由端侧蓝心生成图片学习文本草稿，请检查并编辑后确认。" +
                        if (cleaned.needsReview) " " + cleaned.reviewHint else "",
                )
            }
            is OnDeviceImageDraftResult.Unavailable -> ui.copy(
                imageDraftActive = true,
                imageDraftRunning = false,
                imageDraftManualMode = true,
                imageDraftSource = AiExecutionSource.MANUAL,
                aiProcessing = AiProcessingUiState.hidden(),
                imageDraftMessage = "端侧图像理解暂不可用，可手动输入图片内容。",
            )
        }
    }

    /** Apply a routed OCR/on-device image study draft. The draft still needs user confirmation. */
    internal fun applyImageStudyDraftResult(
        result: AiCapabilityResult<ImageStudyDraft>,
        onDeviceResult: OnDeviceImageDraftResult? = null,
    ) {
        val draft = result.value
        if (draft == null) {
            applyImageDraftResult(onDeviceResult ?: OnDeviceImageDraftResult.Unavailable("ROUTED_IMAGE_DRAFT_EMPTY"))
            return
        }
        val cleaned = OcrTextPostProcessor.clean(draft.initialEditableText().trim())
        val text = cleaned.text
        val needsManualText = text.isBlank()
        ui = ui.copy(
            imageDraftActive = true,
            imageDraftRunning = false,
            imageDraftText = text,
            imageDraftManualMode = needsManualText,
            imageDraftMessage = imageDraftStatusMessage(result.source, draft.ocrError) +
                if (cleaned.needsReview) " " + OcrTextPostProcessor.REVIEW_HINT else "",
            imageStudyDraft = draft,
            imageDraftSource = result.source,
            imageDraftOcrError = draft.ocrError,
            aiProcessing = if (needsManualText) {
                AiProcessingUiState(
                    visible = true,
                    title = "等待手动编辑",
                    steps = listOf("准备资料", "云端处理中", "端侧兜底", "等待确认"),
                    activeStep = 3,
                    source = result.source.displayZh,
                    fallbackMessage = "官方 OCR 未配置时，可继续使用端侧蓝心草稿或手动补充。",
                    canCancel = true,
                    canRetry = true,
                    canContinueManual = true,
                )
            } else {
                AiProcessingUiState.hidden()
            },
        )
    }

    private fun imageFallbackResult(origin: String, onDeviceText: String, status: AiExecutionStatus): AiCapabilityResult<ImageStudyDraft> {
        val source = if (onDeviceText.isNotBlank()) AiExecutionSource.ON_DEVICE else AiExecutionSource.MANUAL
        val draft = ImageStudyDraft(
            id = "image_${System.currentTimeMillis()}",
            origin = origin,
            onDeviceDraftText = onDeviceText,
            ocrError = CaptureError.ConfigMissing,
            createdAt = System.currentTimeMillis(),
        )
        return AiCapabilityResult(
            value = draft,
            source = source,
            status = if (onDeviceText.isNotBlank()) AiExecutionStatus.SUCCESS else status,
            decision = AiRouteDecision(
                capability = AiCapability.OCR_TEXT_EXTRACTION,
                preferred = AiExecutionSource.CLOUD,
                attempted = listOf(AiExecutionSource.CLOUD, source).distinct(),
                selected = source,
                reason = "capture fallback",
                userConfirmationRequired = true,
            ),
        )
    }

    private fun imageDraftStatusMessage(source: AiExecutionSource, ocrError: CaptureError?): String = when {
        source == AiExecutionSource.CLOUD -> "官方 OCR 已生成可编辑文字；请检查识别结果，确认后再生成知识结构大纲。"
        source == AiExecutionSource.ON_DEVICE && ocrError == CaptureError.ConfigMissing ->
            "官方 OCR 未配置时，可继续使用端侧蓝心草稿；请检查识别结果，确认后再生成知识结构大纲。"
        source == AiExecutionSource.ON_DEVICE -> "官方 OCR 未成功，已保留端侧蓝心草稿；请检查识别结果，确认后再生成知识结构大纲。"
        else -> "请手动补充图片中的学习内容；确认后再生成知识结构大纲。"
    }

    fun updateImageDraftText(text: String) { ui = ui.copy(imageDraftText = text) }

    /**
     * Confirm the draft → it becomes the course text and enters the EXISTING CourseAnalysis flow.
     * Nothing is written to the knowledge base until the user runs analysis on this text.
     */
    fun confirmImageDraft() {
        val text = ui.imageDraftText.trim()
        if (text.isBlank()) {
            ui = ui.copy(toast = "草稿为空，请先输入或编辑图片内容。")
            return
        }
        val origin = ui.imageDraftOrigin ?: "图片学习输入"
        val draft = ui.imageStudyDraft
        val confirmed = draft?.let { captureGateway.confirmImageDraft(it, text, ui.courseTitle.ifBlank { origin }) }
        val now = System.currentTimeMillis()
        val courseTitle = confirmed?.courseTitle?.takeIf { it.isNotBlank() } ?: ui.courseTitle.ifBlank { origin }
        val courseText = confirmed?.courseText ?: text
        val imageRef = ui.imageDraftImageRef.ifBlank { draft?.id?.let { "image_asset_$it" } ?: "image_asset_$now" }
        val thumbnailRef = ui.imageDraftThumbnailRef.ifBlank { "thumbnail_$imageRef" }
        val mimeType = ui.imageDraftMimeType.ifBlank { "image/jpeg" }
        ui = ui.copy(
            courseTitle = courseTitle,
            courseText = courseText,
            importSourceType = ImportSourceType.IMAGE_OCR,
            imageDraftActive = false,
            imageDraftRunning = false,
            imageDraftText = "",
            imageDraftManualMode = false,
            imageDraftMessage = null,
            imageDraftOrigin = null,
            imageDraftMeta = null,
            imageStudyDraft = null,
            imageDraftSource = null,
            imageDraftOcrError = null,
            imageDraftImageRef = "",
            imageDraftThumbnailRef = "",
            imageDraftMimeType = "",
            imageDraftBatchId = "",
            imageDraftBatchTotal = 0,
            imageDraftBatchProcessed = 0,
            aiProcessing = AiProcessingUiState.hidden(),
        )
        val input = currentLearningLoopInput(
            now = now + 1,
            text = courseText,
            sourceType = L3SourceType.OCR_IMAGE,
            title = courseTitle,
            assets = listOf(
                EvidenceAsset(
                    id = "asset_image_$now",
                    type = EvidenceAssetType.OCR_IMAGE,
                    sourceType = L3SourceType.OCR_IMAGE,
                    text = courseText,
                    sourceLabel = origin,
                    fileName = draft?.origin?.takeIf { it.isNotBlank() } ?: origin,
                    fileExt = mimeType.substringAfterLast('/', "image"),
                    mimeType = mimeType,
                    localUri = imageRef,
                    thumbnailRef = thumbnailRef,
                    imageRef = imageRef,
                    snippet = courseText.take(180),
                    createdAt = now,
                    status = "OCR_TEXT_CONFIRMED",
                ),
            ),
            sourceLabel = origin,
            providerProvenance = providerProvenanceFor(L3SourceType.OCR_IMAGE),
        )
        publishL3Snapshot(
            l3Pipeline.buildFromLearningLoopInput(input, ui.providerConfigSummary, now + 1),
            now + 1,
            "已确认（$origin · 端侧多模态理解草稿），用户确认后进入学习资料，并已生成 L3 学习闭环。",
        )
    }

    fun confirmImageOcrBatch(now: Long = System.currentTimeMillis()): Boolean {
        val batchDrafts = ui.ocrImports.filter {
            ui.imageDraftBatchId.isBlank() || it.batchId == ui.imageDraftBatchId
        }
        val merged = OcrImportAssembler.mergeByOrder(batchDrafts)
        // For a real image batch, the per-image edited text (re-merged) is authoritative; the single merged
        // field is only the fallback. For a single/non-batch draft, the editable field stays authoritative.
        val text = if (batchDrafts.size > 1) {
            merged.text.trim().ifBlank { ui.imageDraftText.trim() }
        } else {
            ui.imageDraftText.trim().ifBlank { merged.text.trim() }
        }
        if (merged.okDrafts.isEmpty() || text.isBlank()) {
            ui = ui.copy(toast = "未识别到可用图片文字，请先手动补充或重新选择图片。")
            return false
        }
        val origin = ui.imageDraftOrigin ?: "图片学习输入"
        val title = ui.courseTitle.ifBlank { origin }
        val assets = merged.okDrafts.map { draft ->
            EvidenceAsset(
                id = "asset_${draft.id}",
                type = EvidenceAssetType.OCR_IMAGE,
                sourceType = L3SourceType.OCR_IMAGE,
                text = draft.pastedText,
                sourceLabel = draft.fileMeta.safeDisplayLabel(),
                fileName = draft.fileMeta.fileName,
                fileExt = draft.fileMeta.fileName.substringAfterLast('.', "image"),
                mimeType = draft.fileMeta.mimeType.orEmpty().ifBlank { "image/jpeg" },
                localUri = draft.fileMeta.fileName,
                thumbnailRef = draft.fileMeta.fileName,
                imageRef = draft.fileMeta.fileName,
                pageHint = draft.pageIndex?.let { "image $it" }.orEmpty(),
                segmentHint = "image ${draft.pageIndex ?: 1}",
                snippet = draft.pastedText.take(180),
                createdAt = draft.createdAt,
                status = "OCR_TEXT_CONFIRMED",
            )
        }
        ui = ui.copy(
            courseTitle = title,
            courseText = text,
            importSourceType = ImportSourceType.IMAGE_OCR,
            ocrImports = emptyList(),
            imageDraftActive = false,
            imageDraftRunning = false,
            imageDraftText = "",
            imageDraftManualMode = false,
            imageDraftMessage = null,
            imageDraftOrigin = null,
            imageDraftMeta = null,
            imageStudyDraft = null,
            imageDraftSource = null,
            imageDraftOcrError = null,
            imageDraftImageRef = "",
            imageDraftThumbnailRef = "",
            imageDraftMimeType = "",
            imageDraftBatchId = "",
            imageDraftBatchTotal = 0,
            imageDraftBatchProcessed = 0,
            aiProcessing = AiProcessingUiState.hidden(),
        )
        val input = currentLearningLoopInput(
            now = now,
            text = text,
            sourceType = L3SourceType.OCR_IMAGE,
            title = title,
            assets = assets,
            sourceLabel = origin,
            providerProvenance = providerProvenanceFor(L3SourceType.OCR_IMAGE),
        )
        publishL3Snapshot(
            l3Pipeline.buildFromLearningLoopInput(input, ui.providerConfigSummary, now),
            now,
            "已确认图片 OCR 批次，成功图片 ${merged.okDrafts.size} 张，失败 ${merged.failedDrafts.size} 张；已生成 L3 学习闭环。",
        )
        return true
    }

    /** Cancel the draft — nothing enters the course text or the knowledge base. */
    fun cancelImageDraft() {
        val activeBatchId = ui.imageDraftBatchId
        ui = ui.copy(
            ocrImports = if (activeBatchId.isBlank()) ui.ocrImports else ui.ocrImports.filterNot { it.batchId == activeBatchId },
            imageDraftActive = false, imageDraftRunning = false, imageDraftText = "",
            imageDraftManualMode = false, imageDraftMessage = null,
            imageDraftOrigin = null, imageDraftMeta = null,
            imageStudyDraft = null, imageDraftSource = null, imageDraftOcrError = null,
            imageDraftImageRef = "", imageDraftThumbnailRef = "", imageDraftMimeType = "",
            imageDraftBatchId = "", imageDraftBatchTotal = 0, imageDraftBatchProcessed = 0,
            aiProcessing = AiProcessingUiState.hidden(),
        )
    }

    // --- import ---
    fun updateCourseTitle(value: String) { ui = ui.copy(courseTitle = value) }
    fun updateCourseText(value: String) { ui = ui.copy(courseText = value, importSourceType = ImportSourceType.PASTE_TEXT) }
    fun updateSelectedSubject(value: String) { ui = ui.copy(selectedSubject = value) }

    fun loadSample() {
        loadSampleLesson(SampleLessonLibrary.default.id)
    }

    fun loadSampleLesson(id: String): Boolean {
        val lesson = SampleLessonLibrary.byId(id) ?: return false
        ui = ui.copy(
            courseTitle = lesson.title,
            courseText = lesson.body,
            selectedSubject = lesson.subject,
            ocrImports = emptyList(),
            transcripts = emptyList(),
            transcriptDraft = null,
            importSourceType = ImportSourceType.PASTE_TEXT,
            toast = "已加载示例课堂：${lesson.title}",
        )
        return true
    }

    fun selectedGlossaryCount(): Int = CourseGlossary.countFor(ui.selectedSubject)

    fun recordSelectedLocalFileMetadata(metadata: SelectedLocalFileMetadata) {
        ui = ui.copy(
            selectedImportFileMetadata = metadata,
            toast = metadata.summary(),
        )
    }

    fun addOcrImport(
        kind: OcrImportKind,
        displayLabel: String,
        pastedText: String,
        fileMetadata: SelectedLocalFileMetadata? = null,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val cleanResult = OcrTextPostProcessor.clean(pastedText)
        val clean = cleanResult.text.trim()
        if (clean.isBlank()) {
            ui = ui.copy(toast = "请先粘贴 OCR 识别文字。")
            return false
        }
        val ordinal = ui.ocrImports.count { it.kind == kind } + 1
        val label = displayLabel.trim().ifBlank { OcrImportAssembler.defaultDisplayLabel(kind, ordinal) }
        val draft = OcrImportDraft(
            id = "ocr_${now}_${ui.ocrImports.size + 1}",
            kind = kind,
            fileMeta = OcrImportFileMeta(
                fileName = fileMetadata?.fileName?.takeIf { it.isNotBlank() } ?: label,
                mimeType = fileMetadata?.mimeType?.takeIf { it.isNotBlank() },
                sizeBytes = fileMetadata?.sizeBytes,
                displayLabel = label,
                pageIndex = ordinal,
            ),
            pastedText = clean,
            status = OcrImportStatus.OK,
            errorReason = if (cleanResult.needsReview) cleanResult.reviewHint else "",
            pageIndex = ordinal,
            blockIndex = ordinal,
            createdAt = now,
            updatedAt = now,
        )
        ui = ui.copy(
            ocrImports = ui.ocrImports + draft,
            toast = if (cleanResult.needsReview) {
                "已加入本节课资料，建议人工检查 OCR 识别结果。"
            } else {
                "已加入本节课资料：${OcrImportAssembler.sourceLabel(kind)}"
            },
        )
        return true
    }

    fun updateOcrImportText(id: String, text: String, now: Long = System.currentTimeMillis()) {
        val cleanResult = OcrTextPostProcessor.clean(text)
        ui = ui.copy(
            ocrImports = ui.ocrImports.map { draft ->
                if (draft.id == id) {
                    draft.copy(
                        pastedText = cleanResult.text,
                        status = if (cleanResult.text.isBlank()) OcrImportStatus.FAILED else OcrImportStatus.OK,
                        errorReason = if (cleanResult.text.isBlank()) {
                            "OCR 文本为空，请重新输入。"
                        } else if (cleanResult.needsReview) {
                            cleanResult.reviewHint
                        } else {
                            ""
                        },
                        updatedAt = now,
                    )
                } else {
                    draft
                }
            },
        )
    }

    fun removeOcrImport(id: String) {
        ui = ui.copy(
            ocrImports = ui.ocrImports.filterNot { it.id == id },
            toast = "已移除 OCR 资料。",
        )
    }

    fun importTextDraft(title: String, text: String, sourceType: ImportSourceType, fileName: String? = null): Boolean {
        val result = ImportHub.validateText(title, text, sourceType, fileName)
        val draft = result.draft
        return if (result.accepted && draft != null) {
            ui = ui.copy(
                courseTitle = draft.title.ifBlank { fileName?.substringBeforeLast('.') ?: ui.courseTitle },
                courseText = draft.text,
                ocrImports = emptyList(),
                importSourceType = sourceType,
                toast = result.message,
            )
            true
        } else {
            ui = ui.copy(toast = result.message)
            false
        }
    }

    fun importSuperhubFile(
        bytes: ByteArray,
        fileName: String,
        mimeType: String = "",
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val artifact = InputSuperhub.parseFile(bytes, fileName, mimeType, now)
        val report = InputReportEngine.reportFor(artifact)
        val pdfPages = InputReportEngine.pdfPagesFor(artifact)
        val pdfDocument = PdfProcessingEngine.documentFor(artifact)
        ui = ui.copy(
            inputArtifacts = (ui.inputArtifacts + artifact).takeLast(20),
            importReports = (ui.importReports + report).takeLast(20),
            pdfDocuments = (ui.pdfDocuments + listOfNotNull(pdfDocument)).takeLast(20),
            pdfPages = (ui.pdfPages + pdfPages).takeLast(50),
        )
        val text = artifact.extractedText
        return when {
            artifact.status in setOf(InputArtifactStatus.EMPTY_FILE, InputArtifactStatus.READ_FAILED, InputArtifactStatus.UNSUPPORTED_FORMAT, InputArtifactStatus.FORMAT_ERROR) -> {
                ui = ui.copy(toast = artifact.message)
                false
            }
            artifact.kind == InputFileKind.AUDIO -> {
                createAsrLongJobForArtifact(artifact.id, now)
                ui = ui.copy(toast = artifact.message)
                false
            }
            artifact.kind == InputFileKind.IMAGE -> {
                ui = ui.copy(toast = artifact.message)
                false
            }
            artifact.kind == InputFileKind.PDF && text.isBlank() -> {
                ui = ui.copy(toast = artifact.message)
                false
            }
            text.isNotBlank() -> {
                val parsedBank = if (artifact.kind in setOf(InputFileKind.CSV, InputFileKind.XLSX, InputFileKind.DOCX)) {
                    QuestionBankParser.parse(text, ui.courseTitle.ifBlank { artifact.fileName.substringBeforeLast('.') }, now)
                } else {
                    null
                }
                if (parsedBank?.accepted == true && parsedBank.bank != null) {
                    ui = ui.copy(questionBankDraft = text, questionBankParseResult = parsedBank)
                    importQuestionBankDraft(now)
                } else {
                    ui = ui.copy(
                        courseTitle = ui.courseTitle.ifBlank { artifact.fileName.substringBeforeLast('.') },
                        courseText = text,
                        importSourceType = if (artifact.kind == InputFileKind.MARKDOWN) ImportSourceType.MARKDOWN_FILE else ImportSourceType.TXT_FILE,
                        toast = artifact.message,
                    )
                    generateL3PipelineFromCurrentMaterial(now + 1)
                }
            }
            else -> {
                ui = ui.copy(toast = artifact.message)
                false
            }
        }
    }

    fun addManualPdfPageText(
        artifactId: String,
        pageNumber: Int,
        text: String,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (text.isBlank()) {
            ui = ui.copy(toast = "PDF page text is empty; paste page text before continuing.")
            return false
        }
        val page = ui.pdfPages.firstOrNull { it.artifactId == artifactId && it.pageNumber == pageNumber }
            ?: com.classmate.app.l3.PdfPageArtifact(
                id = "pdf_page_${artifactId}_$pageNumber",
                artifactId = artifactId,
                pageNumber = pageNumber,
                status = com.classmate.app.l3.PdfPageStatus.PDF_TEXT_PARSER_PENDING,
            )
        val updated = InputReportEngine.withManualPageText(page, text, now)
        val pages = (ui.pdfPages.filterNot { it.id == updated.id || (it.artifactId == artifactId && it.pageNumber == pageNumber) } + updated)
            .sortedWith(compareBy({ it.artifactId }, { it.pageNumber }))
        ui = ui.copy(
            pdfPages = pages,
            courseTitle = ui.courseTitle.ifBlank { "PDF page $pageNumber" },
            courseText = buildString {
                if (ui.courseText.isNotBlank()) appendLine(ui.courseText.trim())
                appendLine(text.trim())
            }.trim(),
            toast = "PDF page text added; it can now enter the L3 pipeline.",
        )
        val input = currentLearningLoopInput(
            now = now + 1,
            text = text,
            sourceType = L3SourceType.DOCUMENT,
            title = ui.courseTitle.ifBlank { "PDF page $pageNumber" },
            assets = listOf(
                EvidenceAsset(
                    id = "asset_${updated.id}",
                    type = EvidenceAssetType.DOCUMENT,
                    sourceType = L3SourceType.DOCUMENT,
                    text = text,
                    sourceLabel = "PDF page $pageNumber",
                    fileName = ui.inputArtifacts.firstOrNull { it.id == artifactId }?.fileName.orEmpty(),
                    fileExt = "pdf",
                    pageHint = "page $pageNumber",
                    segmentHint = "manual page text",
                    snippet = text.take(180),
                    createdAt = now,
                    status = updated.status.name,
                ),
            ),
            sourceLabel = "PDF page $pageNumber",
        )
        return publishL3Snapshot(
            l3Pipeline.buildFromLearningLoopInput(input, ui.providerConfigSummary, now + 1),
            now + 1,
            "PDF page text entered the learning loop.",
        )
    }

    private fun createAsrLongJobForArtifact(artifactId: String, now: Long) {
        val job = AsrLongProductizationEngine.createJob(artifactId, ui.providerConfigSummary, now)
        ui = ui.copy(
            asrLongJobs = (ui.asrLongJobs + job).takeLast(20),
            asrLongStatus = job.status,
            audioCaptureMessage = job.errorMessage ?: "ASR Long job 已进入等待配置/输入状态。",
        )
    }

    fun applyAsrLongTranscript(
        jobId: String,
        transcriptText: String,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val job = ui.asrLongJobs.firstOrNull { it.id == jobId } ?: return false
        if (transcriptText.isBlank()) {
            ui = ui.copy(toast = "转写文本为空，无法回填课堂学习包。")
            return false
        }
        val audioArtifact = ui.inputArtifacts.firstOrNull { it.id == job.audioArtifactId }
        val glossary = TranscriptGlossaryExtractor.extract(
            courseName = ui.courseTitle,
            knowledgePoints = ui.l3Pipeline.knowledgePoints,
            fileName = audioArtifact?.fileName ?: job.audioArtifactId,
        )
        val filled = AsrLongProductizationEngine.applyTranscript(
            job = job,
            transcript = transcriptText,
            sourceId = "asr_source_$now",
            now = now,
            glossary = glossary,
            dialectMode = ui.audioDialectMode,
        )
        val transcriptForLearning = filled.transcriptText.ifBlank { transcriptText.trim() }
        ui = ui.copy(
            asrLongJobs = ui.asrLongJobs.map { if (it.id == jobId) filled else it },
            asrLongStatus = L3AsrStatus.TRANSCRIPT_READY,
            courseTitle = ui.courseTitle.ifBlank { "课堂转写" },
            courseText = buildString {
                if (ui.courseText.isNotBlank()) appendLine(ui.courseText.trim())
                appendLine(transcriptForLearning)
            }.trim(),
            toast = "ASR transcript filled; it can now enter the L3 learning package.",
        )
        val sourceTitle = ui.courseTitle.ifBlank { "课堂转写" }
        val segments = filled.transcriptSegments.ifEmpty {
            listOf(
                TranscriptSegment(
                    segmentId = "seg_${job.id}_1",
                    sourceId = "asr_source_$now",
                    startMs = 0L,
                    endMs = null,
                    text = transcriptForLearning,
                    sourceType = L3SourceType.AUDIO_TRANSCRIPT,
                    fallbackGenerated = true,
                    rawText = transcriptText.trim(),
                    correctedText = transcriptForLearning,
                    dialectMode = ui.audioDialectMode,
                ),
            )
        }
        val assets = segments.mapIndexed { index, segment ->
            val segmentText = segment.correctedText.ifBlank { segment.text }
            EvidenceAsset(
                id = "asset_${job.id}_${segment.segmentId}",
                type = EvidenceAssetType.AUDIO,
                sourceType = L3SourceType.AUDIO_TRANSCRIPT,
                text = segmentText,
                sourceLabel = audioArtifact?.fileName ?: "ASR transcript",
                fileName = audioArtifact?.fileName ?: job.audioArtifactId,
                fileExt = (audioArtifact?.fileName ?: job.audioArtifactId).substringAfterLast('.', ""),
                mimeType = ui.selectedImportFileMetadata?.mimeType.orEmpty(),
                audioRef = audioArtifact?.id ?: job.audioArtifactId,
                segmentHint = "segment ${index + 1}",
                startMs = segment.startMs,
                endMs = segment.endMs,
                transcriptSegment = segmentText,
                snippet = segmentText.take(180),
                createdAt = now,
                status = if (segment.lowConfidence) "TRANSCRIPT_READY_NEEDS_CONFIRMATION" else "TRANSCRIPT_READY",
            )
        }
        val audioSessionArtifact = AudioSessionEngine.artifactFor(
            id = job.audioArtifactId,
            fileName = audioArtifact?.fileName ?: job.audioArtifactId,
            audioRef = audioArtifact?.id ?: job.audioArtifactId,
            durationMs = segments.lastOrNull()?.endMs ?: 1L,
            createdAt = audioArtifact?.createdAt ?: now,
            fileSizeBytes = audioArtifact?.sizeBytes ?: 0L,
            mimeType = ui.selectedImportFileMetadata?.mimeType ?: "audio/mp4",
            sourceLabel = audioArtifact?.fileName ?: "ASR transcript",
        ).copy(
            segmentCount = segments.size,
            processingStatus = AudioSessionEngine.overallStatus(filled.chunks),
        )
        val input = currentLearningLoopInput(
            now = now + 1,
            text = transcriptForLearning,
            sourceType = L3SourceType.AUDIO_TRANSCRIPT,
            title = sourceTitle,
            assets = assets,
            sourceLabel = audioArtifact?.fileName ?: "ASR transcript",
            providerProvenance = providerProvenanceFor(L3SourceType.AUDIO_TRANSCRIPT),
        )
        val snapshot = l3Pipeline.buildFromLearningLoopInput(input, ui.providerConfigSummary, now + 1).copy(
            asrJobs = listOf(filled),
            audioArtifacts = listOf(audioSessionArtifact),
            audioChunks = filled.chunks,
            asrQualityEvaluations = listOfNotNull(filled.qualityEvaluation),
        )
        return publishL3Snapshot(snapshot, now + 1, "ASR transcript has entered the L3 learning package.")
    }

    fun loadL3DemoSeed() {
        ui = ui.copy(
            courseTitle = L3DemoSeeds.lessonTitle,
            courseText = L3DemoSeeds.lessonText,
            questionBankDraft = L3DemoSeeds.questionBankMarkdown,
            importSourceType = ImportSourceType.PASTE_TEXT,
            toast = "已加载 L3 演示课堂与题库模板。",
        )
    }

    fun updateQuestionBankDraft(value: String) {
        ui = ui.copy(questionBankDraft = value, questionBankParseResult = null)
    }

    fun importQuestionBankDraft(now: Long = System.currentTimeMillis()): Boolean {
        val parsed = QuestionBankParser.parse(
            raw = ui.questionBankDraft,
            title = ui.courseTitle.ifBlank { "导入题库" },
            now = now,
        )
        if (!parsed.accepted || parsed.bank == null) {
            ui = ui.copy(questionBankParseResult = parsed, toast = parsed.message)
            return false
        }
        val snapshot = l3Pipeline.buildFromQuestionBank(
            title = ui.courseTitle.ifBlank { parsed.bank.title },
            bank = parsed.bank,
            providerSummary = ui.providerConfigSummary,
            now = now,
        )
        val published = publishL3Snapshot(snapshot, now, parsed.message)
        ui = ui.copy(questionBankParseResult = parsed)
        return published
    }

    fun generateL3PipelineFromCurrentMaterial(now: Long = System.currentTimeMillis()): Boolean {
        val text = l3MaterialText()
        if (text.isBlank()) {
            ui = ui.copy(toast = "请先导入课堂材料、OCR 文本或转写稿。")
            return false
        }
        val snapshot = l3Pipeline.buildFromText(
            title = ui.courseTitle.ifBlank { "L3 学习资料" },
            text = text,
            sourceType = currentL3SourceType(),
            providerSummary = ui.providerConfigSummary,
            now = now,
        )
        return publishL3Snapshot(snapshot, now, "已生成 L3 学习闭环：摘要、知识点、微测、错题和复习队列。")
    }

    fun startClassroomRecording(now: Long = System.currentTimeMillis()) {
        if (ui.currentRecording?.status == L3RecordingStatus.RECORDING) {
            ui = ui.copy(toast = "课堂录音已在进行中。")
            return
        }
        val id = "recording_$now"
        val artifact = classroomAudioRecorder.start(id)
        val record = ClassroomRecordingRecord(
            id = id,
            title = ui.courseTitle.ifBlank { "课堂录音" },
            createdAt = now,
            status = if (artifact.success) L3RecordingStatus.RECORDING else L3RecordingStatus.FAILED,
            artifactFileName = artifact.fileName,
            asrStatus = if (ui.providerConfigSummary.officialProviders.asrLongConfigured) L3AsrStatus.PENDING_ASR_CONFIG else L3AsrStatus.ASR_NOT_CONFIGURED,
            message = artifact.safeMessage,
        )
        ui = ui.copy(
            currentRecording = if (artifact.success) record else null,
            recordingRecords = if (artifact.success) ui.recordingRecords else ui.recordingRecords + record,
            asrLongStatus = record.asrStatus,
            toast = artifact.safeMessage,
        )
    }

    fun stopClassroomRecording(now: Long = System.currentTimeMillis()) {
        val current = ui.currentRecording
        if (current == null || current.status != L3RecordingStatus.RECORDING) {
            ui = ui.copy(toast = "当前没有正在进行的课堂录音。")
            return
        }
        val artifact = classroomAudioRecorder.stop()
        val saved = current.copy(
            endedAt = now,
            durationMs = (now - current.createdAt).coerceAtLeast(0L),
            status = if (artifact.success) L3RecordingStatus.SAVED else L3RecordingStatus.FAILED,
            artifactFileName = artifact.fileName ?: current.artifactFileName,
            fileSizeBytes = artifact.fileSizeBytes,
            asrStatus = if (ui.providerConfigSummary.officialProviders.asrLongConfigured) L3AsrStatus.PENDING_ASR_CONFIG else L3AsrStatus.ASR_NOT_CONFIGURED,
            message = artifact.safeMessage,
        )
        val usableAudioArtifact = artifact.success && recordingFileManager.isUsable(saved)
        if (!usableAudioArtifact) {
            if (artifact.success) recordingFileManager.deleteForRecords(listOf(saved))
            val failedRecord = saved.copy(
                status = L3RecordingStatus.FAILED,
                fileSizeBytes = 0L,
                message = if (artifact.success) "录音文件缺失或为空，已降级为手动转写。" else artifact.safeMessage,
            )
            // No real audio file landed on disk -> never fabricate an AUDIO artifact/evidence or ASR job.
            // Keep the honest FAILED record and point the user to manual transcription.
            ui = ui.copy(
                currentRecording = null,
                recordingRecords = ui.recordingRecords + failedRecord,
                audioCaptureMessage = "录音未成功保存，请重试，或导入字幕/转写稿、粘贴课堂转写继续。",
                toast = failedRecord.message,
            )
            return
        }
        val audioArtifact = com.classmate.app.l3.InputArtifact(
            id = "audio_artifact_$now",
            fileName = saved.artifactFileName ?: "${saved.id}.m4a",
            kind = InputFileKind.AUDIO,
            status = if (saved.asrStatus == L3AsrStatus.ASR_NOT_CONFIGURED) InputArtifactStatus.ASR_NOT_CONFIGURED else InputArtifactStatus.PENDING_ASR_CONFIG,
            message = "课堂录音 artifact 已保存；ASR Long 按配置启用，未配置时走手动转写。",
            createdAt = now,
        )
        ui = ui.copy(
            currentRecording = null,
            recordingRecords = ui.recordingRecords + saved,
            inputArtifacts = (ui.inputArtifacts + audioArtifact).takeLast(20),
            importReports = (ui.importReports + InputReportEngine.reportFor(audioArtifact)).takeLast(20),
            asrLongStatus = saved.asrStatus,
            audioCaptureMessage = if (saved.asrStatus == L3AsrStatus.ASR_NOT_CONFIGURED) "官方 ASR Long 未配置，可粘贴转写文本继续。" else "录音已保存，等待 ASR Long 配置或手动转写。",
            toast = artifact.safeMessage,
        )
        createAsrLongJobForArtifact(audioArtifact.id, now)
    }

    fun cancelClassroomRecording(message: String = "录音已取消，未生成音频证据。") {
        val current = ui.currentRecording
        if (current == null || current.status != L3RecordingStatus.RECORDING) {
            ui = ui.copy(toast = "当前没有正在进行的课堂录音。")
            return
        }
        val artifact = classroomAudioRecorder.cancel()
        val canceled = current.copy(
            status = L3RecordingStatus.FAILED,
            artifactFileName = artifact.fileName ?: current.artifactFileName,
            fileSizeBytes = 0L,
            message = message,
        )
        recordingFileManager.deleteForRecords(listOf(canceled))
        ui = ui.copy(
            currentRecording = null,
            audioCaptureMessage = message,
            toast = message,
        )
    }

    /** Drop a recording record from state and remove the app-private audio file when present. */
    fun removeRecordingRecord(recordId: String, message: String = "录音已删除。") {
        ui.recordingRecords.firstOrNull { it.id == recordId }?.let { recordingFileManager.deleteForRecords(listOf(it)) }
        ui = ui.copy(
            recordingRecords = ui.recordingRecords.filterNot { it.id == recordId },
            currentRecording = ui.currentRecording?.takeUnless { it.id == recordId },
            toast = message,
        )
    }

    // --- P0-1: inline "record + live transcribe" — recording file AND live speech-to-text together ---

    /**
     * Start classroom recording AND live speech-to-text in one action. The recording produces the AUDIO
     * file; the live transcript (official ASR when configured, else the system SpeechRecognizer) shows
     * partial/final text inline. Returns the [AsrState] so the screen knows whether the recognizer is
     * listening or fell back (UNSUPPORTED / PERMISSION_REQUIRED) — the recording still proceeds either way.
     */
    fun startRecordingWithTranscription(
        asrAvailable: Boolean,
        permissionGranted: Boolean,
        now: Long = System.currentTimeMillis(),
    ): AsrState {
        startClassroomRecording(now)
        return asrBegin(asrAvailable, permissionGranted, now)
    }

    /**
     * Stop both. The AUDIO evidence is kept ONLY when a real non-empty file landed on disk; the live
     * transcript is committed as a SEPARATE text evidence (system speech recognition) only when something
     * was actually recognized — a recording with no transcript never fabricates one.
     */
    fun stopRecordingWithTranscription(now: Long = System.currentTimeMillis()) {
        val hadTranscript = ui.asrSession.segments.any { it.text.isNotBlank() }
        // P0-2: when system speech recognition was unavailable, the recording still saved — say so honestly
        // ("转写暂不可用"), never fabricate a transcript and never imply transcription succeeded.
        val asrUnavailable = ui.asrSession.state in setOf(AsrState.UNSUPPORTED, AsrState.PERMISSION_REQUIRED, AsrState.ERROR)
        stopAsr() // folds confirmed ASR segments into ui.transcripts (LIVE_ASR transcript evidence)
        stopClassroomRecording(now) // AUDIO evidence only when a real file is present
        ui = ui.copy(
            audioCaptureMessage = when {
                hadTranscript -> "录音已保存，实时转写已生成文本，可继续整理或生成学习闭环。"
                asrUnavailable -> "录音已保存，转写暂不可用（当前设备未提供系统语音识别）。可稍后使用官方长语音转写，或手动粘贴转写文本。"
                else -> ui.audioCaptureMessage
            },
        )
    }

    /** Cancel both: no AUDIO evidence, no transcript evidence, live text dropped. */
    fun cancelRecordingWithTranscription() {
        ui = ui.copy(asrSession = AsrSession())
        cancelClassroomRecording("录音已取消，未生成音频或转写证据。")
    }

    fun showImportPlaceholder(sourceType: ImportSourceType) {
        ui = ui.copy(toast = ImportHub.placeholderMessage(sourceType))
    }

    private fun l3MaterialText(): String =
        buildString {
            if (ui.courseText.isNotBlank()) appendLine(ui.courseText.trim())
            ui.ocrImports.filter { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() }.forEach { appendLine(it.pastedText.trim()) }
            ui.transcripts.forEach { transcript ->
                transcript.segments.filter { it.text.isNotBlank() }.forEach { appendLine(it.text.trim()) }
            }
        }.trim()

    private fun currentL3SourceType(): L3SourceType = when {
        ui.transcripts.any { it.segments.any { seg -> seg.text.isNotBlank() } } -> {
            if (ui.transcripts.any { it.sourceType == TranscriptSourceType.AUDIO_TRANSCRIPT || it.sourceType == TranscriptSourceType.LIVE_ASR }) {
                L3SourceType.AUDIO_TRANSCRIPT
            } else {
                L3SourceType.MANUAL_TRANSCRIPT
            }
        }
        ui.ocrImports.any { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() } -> L3SourceType.OCR_IMAGE
        ui.importSourceType == ImportSourceType.IMAGE_OCR -> L3SourceType.OCR_IMAGE
        ui.inputArtifacts.lastOrNull { it.extractedText.isNotBlank() }?.kind in setOf(
            InputFileKind.TXT,
            InputFileKind.MARKDOWN,
            InputFileKind.DOCX,
            InputFileKind.PPTX,
            InputFileKind.PDF,
        ) -> L3SourceType.DOCUMENT
        else -> L3SourceType.TEXT
    }

    private fun currentLearningLoopInput(
        now: Long,
        text: String = l3MaterialText(),
        sourceType: L3SourceType = currentL3SourceType(),
        title: String = ui.courseTitle.ifBlank { "L3 learning material" },
        assets: List<EvidenceAsset> = currentEvidenceAssets(now, text, sourceType),
        sourceLabel: String = sourceLabelFor(sourceType),
        providerProvenance: String = providerProvenanceFor(sourceType),
    ): LearningLoopInput =
        LearningLoopInput(
            id = "loop_input_$now",
            title = title,
            kind = learningInputKind(sourceType),
            sourceType = sourceType,
            text = text,
            evidenceAssets = assets,
            sourceLabel = sourceLabel,
            providerProvenance = providerProvenance,
        )

    private fun currentEvidenceAssets(now: Long, text: String, sourceType: L3SourceType): List<EvidenceAsset> {
        val transcriptAssets = ui.transcripts.flatMap { transcript ->
            transcript.segments.filter { it.text.isNotBlank() }.mapIndexed { index, segment ->
                EvidenceAsset(
                    id = "asset_${transcript.id}_${segment.id}",
                    type = EvidenceAssetType.AUDIO,
                    sourceType = if (transcript.sourceType == TranscriptSourceType.AUDIO_TRANSCRIPT || transcript.sourceType == TranscriptSourceType.LIVE_ASR) {
                        L3SourceType.AUDIO_TRANSCRIPT
                    } else {
                        L3SourceType.MANUAL_TRANSCRIPT
                    },
                    text = segment.text,
                    sourceLabel = transcript.displayLabel(),
                    fileName = transcript.fileName.orEmpty(),
                    fileExt = transcript.fileName.orEmpty().substringAfterLast('.', ""),
                    mimeType = transcript.mimeType.orEmpty(),
                    audioRef = transcript.fileName.orEmpty(),
                    segmentHint = "segment ${index + 1}",
                    startMs = segment.startMs,
                    endMs = segment.endMs,
                    transcriptSegment = segment.text,
                    snippet = segment.text.take(180),
                    createdAt = transcript.createdAt.takeIf { it > 0L } ?: now,
                    status = if (transcript.sourceType == TranscriptSourceType.AUDIO_TRANSCRIPT || transcript.sourceType == TranscriptSourceType.LIVE_ASR) {
                        "TRANSCRIPT_READY"
                    } else {
                        "MANUAL_TRANSCRIPT_FALLBACK"
                    },
                )
            }
        }
        if (transcriptAssets.isNotEmpty()) return transcriptAssets

        val ocrAssets = ui.ocrImports.filter { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() }.map { draft ->
            EvidenceAsset(
                id = "asset_${draft.id}",
                type = EvidenceAssetType.OCR_IMAGE,
                sourceType = L3SourceType.OCR_IMAGE,
                text = draft.pastedText,
                sourceLabel = draft.fileMeta.safeDisplayLabel(),
                fileName = draft.fileMeta.fileName,
                fileExt = draft.fileMeta.fileName.substringAfterLast('.', ""),
                mimeType = draft.fileMeta.mimeType.orEmpty(),
                imageRef = draft.fileMeta.fileName,
                thumbnailRef = draft.fileMeta.safeSummary(),
                pageHint = draft.pageIndex?.let { "page $it" }.orEmpty(),
                snippet = draft.pastedText.take(180),
                createdAt = draft.createdAt,
                status = "OCR_TEXT_CONFIRMED",
            )
        }
        if (ocrAssets.isNotEmpty()) return ocrAssets

        val artifact = ui.inputArtifacts.lastOrNull {
            it.extractedText.isNotBlank() || it.kind == InputFileKind.PDF || it.kind == InputFileKind.AUDIO
        }
        if (artifact != null) {
            return listOf(
                EvidenceAsset(
                    id = "asset_${artifact.id}",
                    type = when (artifact.kind) {
                        InputFileKind.IMAGE -> EvidenceAssetType.OCR_IMAGE
                        InputFileKind.AUDIO, InputFileKind.VIDEO -> EvidenceAssetType.AUDIO
                        else -> EvidenceAssetType.DOCUMENT
                    },
                    sourceType = sourceType,
                    text = artifact.extractedText.ifBlank { text.take(240) },
                    sourceLabel = artifact.fileName,
                    fileName = artifact.fileName,
                    fileExt = artifact.fileName.substringAfterLast('.', ""),
                    mimeType = ui.selectedImportFileMetadata?.mimeType.orEmpty(),
                    localUri = artifact.id,
                    imageRef = if (artifact.kind == InputFileKind.IMAGE) artifact.fileName else "",
                    audioRef = if (artifact.kind == InputFileKind.AUDIO) artifact.fileName else "",
                    pageHint = ui.pdfPages.lastOrNull { it.artifactId == artifact.id }?.pageNumber?.let { "page $it" }.orEmpty(),
                    segmentHint = if (artifact.kind == InputFileKind.PPTX) "slide text" else artifact.status.name,
                    transcriptSegment = if (artifact.kind == InputFileKind.AUDIO) artifact.extractedText.ifBlank { text.take(180) } else "",
                    snippet = artifact.extractedText.ifBlank { text }.take(180),
                    createdAt = artifact.createdAt,
                    status = artifact.status.name,
                ),
            )
        }

        return listOf(
            EvidenceAsset(
                id = "asset_text_$now",
                type = when (sourceType) {
                    L3SourceType.OCR_IMAGE -> EvidenceAssetType.OCR_IMAGE
                    L3SourceType.DOCUMENT -> EvidenceAssetType.DOCUMENT
                    L3SourceType.AUDIO_TRANSCRIPT,
                    L3SourceType.MANUAL_TRANSCRIPT,
                    L3SourceType.RECORDING_ARTIFACT -> EvidenceAssetType.AUDIO
                    L3SourceType.WEB -> EvidenceAssetType.WEB
                    else -> EvidenceAssetType.TEXT
                },
                sourceType = sourceType,
                text = text.take(240),
                sourceLabel = sourceLabelFor(sourceType),
                transcriptSegment = if (sourceType == L3SourceType.AUDIO_TRANSCRIPT || sourceType == L3SourceType.MANUAL_TRANSCRIPT) text.take(240) else "",
                snippet = text.take(180),
                createdAt = now,
                status = "TEXT_READY",
            ),
        )
    }

    private fun learningInputKind(sourceType: L3SourceType): LearningLoopInputKind = when (sourceType) {
        L3SourceType.OCR_IMAGE -> LearningLoopInputKind.OCR_IMAGE
        L3SourceType.DOCUMENT -> LearningLoopInputKind.DOCUMENT
        L3SourceType.AUDIO_TRANSCRIPT -> LearningLoopInputKind.AUDIO_TRANSCRIPT
        L3SourceType.MANUAL_TRANSCRIPT -> LearningLoopInputKind.MANUAL_TRANSCRIPT
        L3SourceType.QUESTION_BANK -> LearningLoopInputKind.QUESTION_BANK
        L3SourceType.WEB -> LearningLoopInputKind.WEB
        L3SourceType.RECORDING_ARTIFACT -> LearningLoopInputKind.AUDIO_TRANSCRIPT
        L3SourceType.TEXT -> if (ui.importSourceType == ImportSourceType.MARKDOWN_FILE) LearningLoopInputKind.MARKDOWN else LearningLoopInputKind.TEXT
    }

    private fun sourceLabelFor(sourceType: L3SourceType): String = when (sourceType) {
        L3SourceType.TEXT -> "pasted text"
        L3SourceType.OCR_IMAGE -> "OCR image"
        L3SourceType.DOCUMENT -> "document"
        L3SourceType.AUDIO_TRANSCRIPT -> "audio transcript"
        L3SourceType.MANUAL_TRANSCRIPT -> "manual transcript"
        L3SourceType.RECORDING_ARTIFACT -> "recording artifact"
        L3SourceType.QUESTION_BANK -> "question bank"
        L3SourceType.WEB -> "web source"
    }

    private fun providerProvenanceFor(sourceType: L3SourceType): String = when (sourceType) {
        L3SourceType.OCR_IMAGE -> "OCR:${ui.providerConfigSummary.officialProviders.ocrConfigured}"
        L3SourceType.AUDIO_TRANSCRIPT, L3SourceType.MANUAL_TRANSCRIPT, L3SourceType.RECORDING_ARTIFACT -> "ASR:${ui.asrLongStatus.name}"
        L3SourceType.DOCUMENT -> ui.inputArtifacts.lastOrNull()?.status?.name.orEmpty()
        else -> ""
    }

    private fun publishL3Snapshot(snapshot: L3PipelineSnapshot, now: Long, toast: String): Boolean {
        val publishSnapshot = snapshot.lessonSource?.let { source ->
            if (snapshot.evidenceAssets.isNotEmpty()) {
                snapshot
            } else {
                l3Pipeline.attachEvidenceAssets(
                    snapshot = snapshot,
                    assets = currentEvidenceAssets(now, source.rawText, source.type),
                    sourceLabel = sourceLabelFor(source.type),
                    providerProvenance = providerProvenanceFor(source.type),
                )
            }
        } ?: snapshot
        val inputType = when {
            publishSnapshot.lessonSource?.type == L3SourceType.OCR_IMAGE -> ToolInputType.IMAGE
            publishSnapshot.lessonSource?.type == L3SourceType.QUESTION_BANK -> ToolInputType.QUESTION_BANK
            ui.pdfPages.isNotEmpty() -> ToolInputType.PDF
            ui.asrLongJobs.isNotEmpty() || publishSnapshot.transcriptSegments.isNotEmpty() -> ToolInputType.AUDIO
            else -> ToolInputType.TEXT
        }
        val toolPlan = L3OfficialToolSeams.orchestrate("L3 learning package", publishSnapshot, ui.providerConfigSummary, now)
        val toolSteps = toolPlan.stepRecords.ifEmpty {
            ToolOrchestratorProductizationEngine.stepRecords(inputType, publishSnapshot, ui.providerConfigSummary, now)
        }
        val localSemanticRecords = LocalSemanticIndexEngine.records(publishSnapshot, ui.providerConfigSummary, now)
        val baseSnapshot = publishSnapshot.copy(
            inputArtifacts = ui.inputArtifacts,
            inputReports = ui.importReports,
            pdfDocuments = ui.pdfDocuments,
            pdfPages = ui.pdfPages,
            asrJobs = ui.asrLongJobs,
            toolOrchestrationPlan = toolPlan.copy(stepRecords = toolSteps),
            toolStepRecords = toolSteps,
            semanticIndexRecords = localSemanticRecords,
            translationResults = ui.l3TranslationResults,
            ttsPlaybackStates = listOfNotNull(ui.l3TtsPlaybackState),
            reviewDailyStats = ReviewStatsEngine.daily(publishSnapshot, now),
        )
        val runtimeSnapshot = OfficialRuntimeIntegrator.enrich(
            snapshot = baseSnapshot,
            summary = ui.providerConfigSummary,
            gateway = officialRuntimeGateway,
            inputType = inputType,
            now = now,
            localTtsAvailable = localTtsPlayer.canAttemptLocalPlayback(),
            edgeModelAvailable = onDeviceController.isAvailable(),
        )
        semanticIndexRepository.save(runtimeSnapshot.semanticIndexRecords)
        val reloadedSemanticRecords = semanticIndexRepository.load().ifEmpty { runtimeSnapshot.semanticIndexRecords }
        val searchQuery = runtimeSnapshot.semanticSearchResults.firstOrNull()?.query
            ?: runtimeSnapshot.knowledgePoints.firstOrNull()?.title
            ?: runtimeSnapshot.summary
        val enrichedBase = runtimeSnapshot.copy(
            semanticIndexRecords = reloadedSemanticRecords,
            semanticSearchResults = if (searchQuery.isBlank()) emptyList() else listOf(LocalSemanticIndexEngine.search(reloadedSemanticRecords, searchQuery)),
            reviewDailyStats = ReviewStatsEngine.daily(runtimeSnapshot, now),
        )
        val guardedBase = enrichedBase.copy(
            safetyGuardResults = SafetyGuardEngine.results(enrichedBase, now),
            deviceReadinessResults = DeviceReadinessEngine.results(ui.providerConfigSummary, now),
        )
        val plan = LearningLoopCapabilityOrchestrator.plan(
            inputKind = learningInputKind(guardedBase.lessonSource?.type ?: L3SourceType.TEXT),
            sourceType = guardedBase.lessonSource?.type ?: L3SourceType.TEXT,
            snapshot = guardedBase,
            summary = ui.providerConfigSummary,
            now = now,
            dialectMode = ui.audioDialectMode,
            enableExperimentalImageGeneration = ui.enableExperimentalImageGeneration,
            enableExperimentalVideoGeneration = ui.enableExperimentalVideoGeneration,
            enableExperimentalSimultaneousInterpretation = ui.enableExperimentalSimultaneousInterpretation,
        )
        val withCapabilityLayer = guardedBase.copy(
            capabilityPlans = (guardedBase.capabilityPlans + plan).takeLast(5),
            officialCapabilityContributions = OfficialCapabilityRegistry.officialMatrix(guardedBase, ui.providerConfigSummary),
            qualityWarnings = LearningLoopCapabilityOrchestrator.qualityWarnings(guardedBase),
        )
        val enrichedSnapshot = withCapabilityLayer.copy(
            learningDiagnosis = LearningDiagnosisEngine.build(withCapabilityLayer, now),
        )
        val artifacts = l3Pipeline.toCourseArtifacts(enrichedSnapshot, now)
        if (artifacts == null) {
            // P0-4: honest "insufficient material" — never a silent no-quiz; the user is told to add material
            // or edit the OCR text rather than left wondering why there are no 微测.
            ui = ui.copy(l3Pipeline = enrichedSnapshot, toast = "资料不足，暂不能生成微测，请补充资料或手动编辑 OCR 文本后重试。")
            persistL3(enrichedSnapshot)
            return false
        }
        val outcome = AnalysisOutcome.Success(
            result = artifacts.result,
            report = com.classmate.core.validation.ValidationReport.PASS,
            logs = listOf(RedactedLogEntry("L3_PIPELINE", "OK", 0, "PASS", false, null)),
        )
        learningStore.addTasksFromAnalysis(
            result = artifacts.result,
            courseTitle = artifacts.session.title.ifBlank { "未命名课程" },
            sourceProvider = "L3_PIPELINE",
            sourceProfile = ui.providerConfigSummary.profileLabel,
            sourceModel = "local-learning-pipeline",
        )
        val updatedHistory = (listOf(buildHistoryRecord(artifacts.session, outcome, now)) + ui.history).take(MAX_HISTORY)
        ui = ui.copy(
            session = artifacts.session,
            result = artifacts.result,
            l3Pipeline = enrichedSnapshot,
            l3ToolOrchestrationPlan = enrichedSnapshot.toolOrchestrationPlan,
            l3ToolStepRecords = enrichedSnapshot.toolStepRecords,
            l3SemanticSearchResults = enrichedSnapshot.semanticSearchResults,
            l3EdgeStudySeam = L3OfficialToolSeams.edgeStudyFallback(enrichedSnapshot.summary),
            learningState = LearningState.seed(artifacts.result.sessionId, artifacts.result.knowledgePoints, now),
            learningSnapshot = learningStore.snapshot(),
            history = updatedHistory,
            answers = emptyMap(),
            revealedQuestionIds = emptySet(),
            currentQuestionIndex = 0,
            feedbackEvents = emptyList(),
            reviewPlan = null,
            analysisStatus = AnalysisStatus.SUCCESS,
            analysisError = null,
            selectedCourseKey = CourseLibraryBuilder.normalizeCourseName(artifacts.session.title).lowercase(),
            toast = toast,
        )
        persistHistory(updatedHistory)
        persistL3(enrichedSnapshot)
        // Replace the loading screen so system back from the course never returns to the analyze/loading
        // screen (P0-1). From any other entry, just push the course.
        if (currentScreen == Screen.ANALYZE) navigateReplacing(Screen.COURSE_DETAIL) else navigateTo(Screen.COURSE_DETAIL)
        return true
    }

    private fun persistL3(snapshot: L3PipelineSnapshot) {
        l3PersistenceRepository.saveSnapshot(snapshot)
    }

    fun prepareL3Translation(targetLanguage: String = "zh-CHS"): Boolean {
        val evidence = ui.l3Pipeline.evidence.firstOrNull()
        val text = evidence?.text ?: ui.courseText
        if (text.isBlank()) {
            ui = ui.copy(toast = "No lesson evidence is available for translation.")
            return false
        }
        val result = L3OfficialToolSeams.translate(
            sourceText = text,
            sourceLanguage = "auto",
            targetLanguage = targetLanguage,
            summary = ui.providerConfigSummary,
        )
        val runtime = officialRuntimeGateway.translate(
            sourceText = text,
            sourceLanguage = "auto",
            targetLanguage = targetLanguage,
            summary = ui.providerConfigSummary,
            now = System.currentTimeMillis(),
        )
        val productResult = TranslationProductEngine.prepare(
            sourceText = text,
            targetLanguage = targetLanguageFrom(targetLanguage),
            evidenceId = evidence?.id,
            summary = ui.providerConfigSummary,
            now = System.currentTimeMillis(),
        ).let { prepared ->
            if (runtime.status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED && runtime.output.orEmpty().isNotBlank()) {
                prepared.copy(
                    status = TranslationProductStatus.OFFICIAL_TRANSLATION_READY,
                    translatedText = runtime.output.orEmpty(),
                    errorMessage = null,
                )
            } else {
                prepared.copy(errorMessage = runtime.errorMessage ?: prepared.errorMessage ?: runtime.errorCode)
            }
        }
        ui = ui.copy(
            l3TranslationSeams = (ui.l3TranslationSeams + result).takeLast(10),
            l3TranslationResults = (ui.l3TranslationResults + productResult).takeLast(10),
            l3Pipeline = ui.l3Pipeline.copy(translationResults = (ui.l3Pipeline.translationResults + productResult).takeLast(10)),
            toast = if (runtime.status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED) {
                "Official translation runtime produced a derived translation artifact."
            } else {
                result.message
            },
        )
        return runtime.status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED ||
            result.status != com.classmate.app.l3.OfficialToolSeamStatus.NOT_CONFIGURED
    }

    fun prepareL3ListenReview(sourceType: TtsPlaybackSourceType = TtsPlaybackSourceType.SUMMARY): Boolean {
        val text = ui.l3Pipeline.summary.ifBlank { ui.courseEssenceScript?.toPlainText().orEmpty() }.ifBlank { ui.courseText.take(300) }
        if (text.isBlank()) {
            ui = ui.copy(toast = "No summary is available for listen-review.")
            return false
        }
        val result = L3OfficialToolSeams.prepareListenReview(text, ui.providerConfigSummary)
        val runtime = officialRuntimeGateway.prepareTts(
            text = text,
            summary = ui.providerConfigSummary,
            now = System.currentTimeMillis(),
            localTtsAvailable = localTtsPlayer.canAttemptLocalPlayback(),
        )
        val playback = TtsPlaybackEngine.prepare(
            text = text,
            sourceType = sourceType,
            summary = ui.providerConfigSummary,
            now = System.currentTimeMillis(),
            localTtsAvailable = localTtsPlayer.canAttemptLocalPlayback(),
        )
        val playbackAfterAttempt = if (runtime.status == OfficialRuntimeStatus.OFFICIAL_RUNTIME_USED) {
            playback.copy(
                provider = TtsPlaybackProvider.OFFICIAL_TTS,
                status = TtsPlaybackStatus.OFFICIAL_TTS_READY,
                message = "Official TTS runtime produced an audio artifact; local fallback remains available if playback fails.",
            )
        } else if (playback.provider == com.classmate.app.l3.TtsPlaybackProvider.ANDROID_LOCAL_TTS) {
            if (localTtsPlayer.speak(playback.id, text)) {
                playback.copy(status = com.classmate.app.l3.TtsPlaybackStatus.PLAYING, message = "Android local TextToSpeech playback started.")
            } else {
                playback.copy(message = "${playback.message} Runtime=${runtime.status.name}; playback may require device initialization.")
            }
        } else {
            playback.copy(message = "${playback.message} Runtime=${runtime.status.name}; ${runtime.errorCode.orEmpty()}")
        }
        ui = ui.copy(
            l3TtsReviewSeam = result,
            l3TtsPlaybackState = playbackAfterAttempt,
            l3Pipeline = ui.l3Pipeline.copy(ttsPlaybackStates = (ui.l3Pipeline.ttsPlaybackStates + playbackAfterAttempt).takeLast(10)),
            toast = playbackAfterAttempt.message,
        )
        return playbackAfterAttempt.status == com.classmate.app.l3.TtsPlaybackStatus.OFFICIAL_TTS_READY ||
            playbackAfterAttempt.status == com.classmate.app.l3.TtsPlaybackStatus.LOCAL_TTS_AVAILABLE ||
            playbackAfterAttempt.status == com.classmate.app.l3.TtsPlaybackStatus.PLAYING
    }

    fun refreshL3ToolOrchestration(now: Long = System.currentTimeMillis()) {
        val plan = L3OfficialToolSeams.orchestrate("L3 diagnostics refresh", ui.l3Pipeline, ui.providerConfigSummary, now)
        ui = ui.copy(
            l3ToolOrchestrationPlan = plan,
            l3ToolStepRecords = plan.stepRecords,
            l3Pipeline = ui.l3Pipeline.copy(toolOrchestrationPlan = plan, toolStepRecords = plan.stepRecords),
            toast = "L3 tool orchestration plan refreshed.",
        )
    }

    private fun targetLanguageFrom(value: String): TranslationTargetLanguage =
        when (value.lowercase(Locale.US)) {
            "en", "english" -> TranslationTargetLanguage.ENGLISH
            "ja", "jp", "japanese" -> TranslationTargetLanguage.JAPANESE
            "ko", "kr", "korean" -> TranslationTargetLanguage.KOREAN
            else -> TranslationTargetLanguage.CHINESE
        }

    // --- live companion ---
    fun updateLiveTitle(value: String) { ui = ui.copy(liveTitleDraft = value) }
    fun updateLiveSegment(value: String) { ui = ui.copy(liveSegmentDraft = value) }

    /** Current speaker tag applied to the next Live segment (manual; no voiceprint / ASR identity). */
    fun setLiveSpeaker(speaker: SpeakerLabel) { ui = ui.copy(liveSpeakerDraft = speaker) }

    fun startLiveClass() {
        val now = System.currentTimeMillis()
        ui = ui.copy(
            liveTranscript = TranscriptSession.start(
                id = "live_$now",
                title = ui.liveTitleDraft.ifBlank { "课堂伴学" },
                now = now,
            ),
            liveAnalyzed = false,
            liveSpeakerDraft = SpeakerLabel.UNKNOWN,
            asrSession = AsrSession(),
            toast = "课堂伴学已开始（手动转写模式）。",
        )
    }

    fun pauseLiveClass() {
        ui.liveTranscript?.let { ui = ui.copy(liveTranscript = TranscriptController.pause(it, System.currentTimeMillis())) }
    }

    fun resumeLiveClass() {
        ui.liveTranscript?.let { ui = ui.copy(liveTranscript = TranscriptController.resume(it, System.currentTimeMillis())) }
    }

    fun appendLiveSegment() {
        val session = ui.liveTranscript ?: return
        val next = session.append(ui.liveSegmentDraft, System.currentTimeMillis(), speaker = ui.liveSpeakerDraft)
        ui = ui.copy(liveTranscript = next, liveSegmentDraft = "")
    }

    /** Fold any text still in the segment field into a segment, so typed-but-not-added notes survive. */
    private fun flushLiveDraft(session: TranscriptSession): TranscriptSession =
        if (ui.liveSegmentDraft.isNotBlank()) {
            session.append(ui.liveSegmentDraft, System.currentTimeMillis(), speaker = ui.liveSpeakerDraft)
        } else {
            session
        }

    // --- experimental live ASR (system SpeechRecognizer; no raw audio saved, no upload, no background) ---

    /**
     * Begin the experimental ASR session. The screen passes the system recognizer's availability and
     * whether RECORD_AUDIO is granted; the returned [AsrState] tells the screen whether to actually
     * start the engine (only when LISTENING). Never auto-starts — only on the user's tap.
     */
    fun asrBegin(available: Boolean, permissionGranted: Boolean, now: Long = System.currentTimeMillis()): AsrState {
        val next = AsrSessionController.begin(ui.asrSession, available, permissionGranted, now)
        // Honest, actionable next-step wording from the testable readiness helper.
        val readiness = SpeechRecognitionReadiness(recordAudioGranted = permissionGranted, recognizerAvailable = available)
        ui = ui.copy(
            asrSession = next,
            toast = when (next.state) {
                AsrState.UNSUPPORTED, AsrState.PERMISSION_REQUIRED -> readiness.userGuidance()
                AsrState.LISTENING -> "实时转写已开始（系统语音识别，不保存原始音频）。"
                else -> ui.toast
            },
        )
        return next.state
    }

    fun asrOnListening() { ui = ui.copy(asrSession = AsrSessionController.onListening(ui.asrSession)) }
    fun asrOnEndOfSpeech() { ui = ui.copy(asrSession = AsrSessionController.onEndOfSpeech(ui.asrSession)) }
    fun asrOnPartial(text: String) { ui = ui.copy(asrSession = AsrSessionController.onPartial(ui.asrSession, text)) }

    fun asrOnFinal(text: String, confidence: Double? = null) {
        ui = ui.copy(
            asrSession = AsrSessionController.onFinal(ui.asrSession, text, confidence, ui.liveSpeakerDraft, System.currentTimeMillis()),
        )
    }

    fun asrOnError(message: String) {
        ui = ui.copy(asrSession = AsrSessionController.onError(ui.asrSession, message), toast = message)
    }

    fun pauseAsr() { ui = ui.copy(asrSession = AsrSessionController.pause(ui.asrSession, System.currentTimeMillis())) }
    fun resumeAsr() { ui = ui.copy(asrSession = AsrSessionController.resume(ui.asrSession, System.currentTimeMillis())) }

    /** Stop listening, keep confirmed segments, and fold them into the material tray for analysis. */
    fun stopAsr() {
        ui = ui.copy(asrSession = AsrSessionController.stop(ui.asrSession, System.currentTimeMillis()))
        commitAsr()
    }

    /** Hand the confirmed ASR segments to the Transcript Editor for manual cleanup. */
    fun asrToManualEdit() {
        val s = ui.asrSession
        if (s.segments.isEmpty()) {
            ui = ui.copy(toast = "还没有已确认的转写片段。")
            return
        }
        val id = asrDraftId(s)
        val draft = AsrTranscriptMapper.toDraft(s, ui.liveTitleDraft, System.currentTimeMillis(), id)
        ui = ui.copy(
            transcriptDraft = draft,
            transcripts = ui.transcripts.filterNot { it.id == id }, // editor will re-save into the tray
            asrSession = AsrSession(),
            toast = "已转入转写编辑器，可修改文本/说话人/时间。",
        )
        navigateTo(Screen.TRANSCRIPT_EDITOR)
    }

    /** Idempotently fold confirmed ASR segments into [ui.transcripts] as a LIVE_ASR transcript source. */
    private fun commitAsr() {
        val s = ui.asrSession
        if (s.segments.none { it.text.isNotBlank() }) return
        val id = asrDraftId(s)
        val draft = AsrTranscriptMapper.toDraft(s, ui.liveTitleDraft, System.currentTimeMillis(), id)
        ui = ui.copy(transcripts = ui.transcripts.filterNot { it.id == id } + draft)
    }

    private fun asrDraftId(session: AsrSession): String = "asr_${session.startedAtMs}"

    fun endLiveClass() {
        commitAsr() // fold any confirmed ASR segments into the tray even with no manual session
        val session = ui.liveTranscript ?: return
        val flushed = flushLiveDraft(session)
        val ended = if (flushed.status == TranscriptStatus.ENDED) flushed else flushed.end(System.currentTimeMillis())
        ui = ui.copy(
            liveTranscript = ended,
            liveSegmentDraft = "",
            courseTitle = ended.title,
            courseText = ended.courseText(),
            toast = "课堂已结束，转写已就绪，可生成知识时间线。",
        )
    }

    /** True when there is real content to analyze: manual segment, pending draft, ASR, or transcript. */
    fun canGenerateLiveTimeline(): Boolean {
        if (ui.asrSession.segments.any { it.text.isNotBlank() }) return true
        if (ui.transcripts.any { t -> t.segments.any { it.text.isNotBlank() } }) return true
        val s = ui.liveTranscript ?: return false
        return s.segments.isNotEmpty() || ui.liveSegmentDraft.isNotBlank()
    }

    fun analyzeLiveTranscript() {
        commitAsr() // experimental ASR segments become a LIVE_ASR transcript source
        val hasManualSession = ui.liveTranscript != null
        if (hasManualSession) endLiveClass() // flushes the pending draft, ends the session, fills courseText
        val hasTranscript = ui.transcripts.any { t -> t.segments.any { it.text.isNotBlank() } }
        val hasContent = ui.courseText.isNotBlank() || hasTranscript || ui.ocrImports.any { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() }
        if (!hasContent) {
            ui = ui.copy(toast = "还没有任何课堂片段或转写，无法生成时间线。")
            return
        }
        if (!hasManualSession && ui.courseTitle.isBlank()) {
            ui = ui.copy(courseTitle = ui.liveTitleDraft.ifBlank { "课堂伴学" })
        }
        // Manual session -> fromLiveWithOcr (maps session segments); ASR/transcript-only -> fromImportWithOcr.
        ui = ui.copy(liveAnalyzed = hasManualSession)
        startAnalysis()
    }

    // --- transcript / subtitle intake (Stage 5B) ---
    // Parses user-supplied subtitle/transcript TEXT into an editable draft. Never decodes audio/video
    // bytes and never hits the network; the media itself is only ever recorded as safe metadata.

    fun transcribeAudioFile(audioBytes: ByteArray, fileName: String, mimeType: String? = null) {
        if (audioBytes.isEmpty()) {
            ui = ui.copy(audioCaptureMessage = "音频文件为空，请重新选择或粘贴转写文本。", toast = "音频文件为空。")
            return
        }
        maybePromptMissingCloudConfig("官方 ASR")
        val title = ui.courseTitle.ifBlank { fileName.substringBeforeLast('.').ifBlank { "音频转写草稿" } }
        ui = ui.copy(
            transcriptSourceType = TranscriptSourceType.AUDIO_TRANSCRIPT,
            audioCaptureRunning = true,
            audioCaptureProgress = 0,
            audioCaptureMessage = "正在转写课堂音频",
            aiProcessing = AiProcessingUiState(
                visible = true,
                title = "正在转写课堂音频",
                steps = listOf("准备资料", "上传音频", "任务处理中", "生成转写草稿"),
                activeStep = 1,
                source = "云端",
                canCancel = true,
                canRetry = true,
                canContinueManual = true,
            ),
        )
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                captureGateway.transcribeAudio(
                    audioBytes = audioBytes,
                    fileName = fileName,
                    audioFormat = audioFormatFor(fileName, mimeType),
                    title = title,
                    onProgress = { progress ->
                        viewModelScope.launch {
                            ui = ui.copy(audioCaptureProgress = progress.coerceIn(0, 100))
                        }
                    },
                )
            }
            applyAudioTranscriptResult(result, fileName)
        }
    }

    internal fun applyAudioTranscriptResult(result: CaptureResult<TranscriptDraft>, fileName: String? = null): Boolean {
        return when (result) {
            is CaptureResult.Success -> {
                ui = ui.copy(
                    transcriptDraft = result.value,
                    transcriptSourceType = TranscriptSourceType.AUDIO_TRANSCRIPT,
                    audioCaptureRunning = false,
                    audioCaptureProgress = 100,
                    audioCaptureMessage = "已生成可编辑转写草稿，请确认后加入资料篮。",
                    aiProcessing = AiProcessingUiState.hidden(),
                    toast = "已生成转写草稿，可进入编辑器确认。",
                )
                true
            }
            is CaptureResult.Failure -> {
                val message = if (result.failure.error == CaptureError.ConfigMissing) {
                    "官方 ASR 未配置，可粘贴转写文本继续。"
                } else {
                    "官方 ASR 未成功，可粘贴转写文本继续。"
                }
                ui = ui.copy(
                    audioCaptureRunning = false,
                    audioCaptureMessage = message,
                    transcriptFileMetadata = ui.transcriptFileMetadata,
                    aiProcessing = AiProcessingUiState(
                        visible = true,
                        title = "等待粘贴转写文本",
                        steps = listOf("准备资料", "上传音频", "任务处理中", "手动编辑"),
                        activeStep = 3,
                        source = "手动",
                        fallbackMessage = message,
                        canCancel = true,
                        canRetry = true,
                        canContinueManual = true,
                    ),
                    toast = message,
                )
                false
            }
        }
    }

    fun createManualTranscriptDraftFromPaste(now: Long = System.currentTimeMillis()): Boolean {
        val raw = ui.transcriptPasteDraft
        if (raw.isBlank()) {
            ui = ui.copy(toast = "请先粘贴转写文本。")
            return false
        }
        val draft = captureGateway.draftFromPastedText(raw, ui.courseTitle.ifBlank { "手动转写草稿" })
            .copy(createdAt = now, updatedAt = now)
        ui = ui.copy(
            transcriptDraft = draft,
            transcriptSourceType = TranscriptSourceType.PASTED_TRANSCRIPT,
            transcriptParseWarnings = emptyList(),
            audioCaptureRunning = false,
            asrLongStatus = L3AsrStatus.MANUAL_TRANSCRIPT_FALLBACK,
            audioCaptureMessage = "已生成手动转写草稿，请确认后加入资料篮。",
            aiProcessing = AiProcessingUiState.hidden(),
            toast = "已生成手动转写草稿。",
        )
        return draft.segments.isNotEmpty()
    }

    fun hideAiProcessing() {
        ui = ui.copy(aiProcessing = AiProcessingUiState.hidden(), audioCaptureRunning = false, imageDraftRunning = false)
    }

    fun retryCurrentCapture() {
        ui = ui.copy(toast = "请重新选择文件或继续手动编辑。", aiProcessing = AiProcessingUiState.hidden())
    }

    private fun audioFormatFor(fileName: String, mimeType: String?): String {
        val hint = listOfNotNull(mimeType, fileName.substringAfterLast('.', missingDelimiterValue = ""))
            .joinToString(" ")
            .lowercase(Locale.US)
        return if (hint.contains("pcm")) "pcm" else "auto"
    }

    fun updateTranscriptPaste(value: String) { ui = ui.copy(transcriptPasteDraft = value) }

    fun selectTranscriptSourceType(type: TranscriptSourceType) { ui = ui.copy(transcriptSourceType = type) }

    fun recordTranscriptFileMetadata(metadata: SelectedLocalFileMetadata) {
        ui = ui.copy(transcriptFileMetadata = metadata, toast = metadata.summary())
    }

    /** Parse the pasted/loaded transcript text into an editable [TranscriptDraft]; warnings, no crash. */
    /** P0-3: feed a video's REAL embedded subtitle track (from VideoSubtitleExtractor) through the parser. */
    fun importVideoEmbeddedSubtitle(rawSubtitle: String, now: Long = System.currentTimeMillis()): Boolean {
        if (rawSubtitle.isBlank()) {
            ui = ui.copy(toast = "未检测到可提取的内嵌字幕，请导入字幕文件或粘贴转写文本。")
            return false
        }
        ui = ui.copy(transcriptPasteDraft = rawSubtitle, transcriptSourceType = TranscriptParser.autoDetect(rawSubtitle))
        val ok = parseTranscript(now)
        if (ok) ui = ui.copy(toast = "已从视频内嵌字幕轨提取并解析字幕，可编辑确认。")
        return ok
    }

    fun parseTranscript(now: Long = System.currentTimeMillis()): Boolean {
        val raw = ui.transcriptPasteDraft
        if (raw.isBlank()) {
            ui = ui.copy(toast = "请先粘贴字幕或转写稿文本。")
            return false
        }
        val parsed = TranscriptParser.parse(raw, ui.transcriptSourceType)
        val meta = ui.transcriptFileMetadata
        val draft = TranscriptDraft(
            id = "transcript_${now}",
            sourceType = ui.transcriptSourceType,
            title = meta?.fileName?.substringBeforeLast('.').orEmpty(),
            fileName = meta?.fileName,
            mimeType = meta?.mimeType?.takeIf { it.isNotBlank() },
            sizeBytes = meta?.sizeBytes,
            segments = parsed.segments,
            createdAt = now,
            updatedAt = now,
        )
        ui = ui.copy(
            transcriptDraft = draft,
            transcriptParseWarnings = parsed.warnings,
            toast = if (parsed.segments.isEmpty()) "未解析出段落，请检查格式。" else "已解析 ${parsed.segments.size} 个段落。",
        )
        return parsed.segments.isNotEmpty()
    }

    private fun updateDraftSegments(transform: (List<TranscriptSegmentDraft>) -> List<TranscriptSegmentDraft>) {
        val draft = ui.transcriptDraft ?: return
        val updated = transform(draft.segments)
        ui = ui.copy(transcriptDraft = draft.copy(segments = updated, updatedAt = System.currentTimeMillis()))
    }

    fun editTranscriptSegmentText(id: String, text: String) =
        updateDraftSegments { segs -> segs.map { if (it.id == id) it.copy(text = text) else it } }

    fun setTranscriptSpeaker(id: String, speaker: SpeakerLabel) =
        updateDraftSegments { segs -> segs.map { if (it.id == id) it.copy(speaker = speaker) else it } }

    /** Set start/end from a clock string ("MM:SS"/"HH:MM:SS"); blank clears that bound. */
    fun setTranscriptSegmentStart(id: String, clock: String) =
        updateDraftSegments { segs -> segs.map { if (it.id == id) it.copy(startMs = parseClockOrNull(clock)) else it } }

    fun setTranscriptSegmentEnd(id: String, clock: String) =
        updateDraftSegments { segs -> segs.map { if (it.id == id) it.copy(endMs = parseClockOrNull(clock)) else it } }

    fun clearTranscriptSegmentTime(id: String) =
        updateDraftSegments { segs -> segs.map { if (it.id == id) it.copy(startMs = null, endMs = null) else it } }

    fun deleteTranscriptSegment(id: String) =
        updateDraftSegments { segs -> segs.filterNot { it.id == id } }

    /** Merge a segment into the following one (text joined; earliest start / latest end kept). */
    fun mergeTranscriptSegmentDown(id: String) = updateDraftSegments { segs ->
        val i = segs.indexOfFirst { it.id == id }
        if (i < 0 || i >= segs.lastIndex) return@updateDraftSegments segs
        val a = segs[i]
        val b = segs[i + 1]
        val merged = a.copy(
            text = listOf(a.text, b.text).filter { it.isNotBlank() }.joinToString(" "),
            startMs = listOfNotNull(a.startMs, b.startMs).minOrNull(),
            endMs = listOfNotNull(a.endMs, b.endMs).maxOrNull(),
            speaker = if (a.speaker != SpeakerLabel.UNKNOWN) a.speaker else b.speaker,
        )
        segs.toMutableList().apply { set(i, merged); removeAt(i + 1) }
    }

    fun moveTranscriptSegment(id: String, up: Boolean) = updateDraftSegments { segs ->
        val i = segs.indexOfFirst { it.id == id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j < 0 || j > segs.lastIndex) return@updateDraftSegments segs
        segs.toMutableList().apply { val tmp = this[i]; this[i] = this[j]; this[j] = tmp }
    }

    fun addTranscriptSegment(now: Long = System.currentTimeMillis()) =
        updateDraftSegments { segs -> segs + TranscriptSegmentDraft(id = "seg_new_$now", text = "") }

    /** Commit the working draft into the material tray; it then fuses into analysis like any source. */
    fun saveTranscriptToTray(): Boolean {
        val draft = ui.transcriptDraft ?: return false
        val usable = draft.copy(segments = draft.segments.filter { it.text.isNotBlank() })
        if (usable.segments.isEmpty()) {
            ui = ui.copy(toast = "转写稿为空，未加入资料篮。")
            return false
        }
        val others = ui.transcripts.filterNot { it.id == usable.id }
        ui = ui.copy(
            transcripts = others + usable,
            transcriptDraft = null,
            transcriptPasteDraft = "",
            transcriptFileMetadata = null,
            transcriptParseWarnings = emptyList(),
            asrLongStatus = if (usable.sourceType == TranscriptSourceType.AUDIO_TRANSCRIPT || usable.sourceType == TranscriptSourceType.LIVE_ASR) {
                L3AsrStatus.TRANSCRIPT_READY
            } else {
                L3AsrStatus.MANUAL_TRANSCRIPT_FALLBACK
            },
            toast = "已加入资料篮：${com.classmate.core.transcript.TranscriptLabels.of(usable.sourceType)} · ${usable.segments.size} 段",
        )
        return true
    }

    fun saveTranscriptToTrayAndGenerateLearningLoop(now: Long = System.currentTimeMillis()): Boolean {
        if (!saveTranscriptToTray()) return false
        val generated = generateL3PipelineFromCurrentMaterial(now + 1)
        if (generated) navigateTo(Screen.COURSE_DETAIL)
        return generated
    }

    fun removeTranscript(id: String) {
        ui = ui.copy(transcripts = ui.transcripts.filterNot { it.id == id }, toast = "已移除转写稿。")
    }

    /** Bounded (<=20), display-only glossary terms appearing in the transcript text. Never evidence. */
    fun transcriptGlossaryHints(): List<String> {
        val text = (ui.transcriptDraft?.segments?.joinToString(" ") { it.text } ?: ui.transcriptPasteDraft)
        return CourseGlossary.matchingTerms(ui.selectedSubject, text, max = 20)
    }

    private fun parseClockOrNull(clock: String): Long? =
        clock.trim().takeIf { it.isNotBlank() }?.let { com.classmate.core.transcript.TranscriptClock.parseMs(it) }

    // --- analysis ---
    // The in-flight analysis coroutine, so a long cloud wait can be cancelled to switch to local rule.
    private var analysisJob: Job? = null

    fun startAnalysis() {
        val text = ui.courseText
        val hasOcrText = ui.ocrImports.any { it.status == OcrImportStatus.OK && it.pastedText.isNotBlank() }
        val hasTranscript = ui.transcripts.any { t -> t.segments.any { it.text.isNotBlank() } }
        if (text.isBlank() && !hasOcrText && !hasTranscript) {
            ui = ui.copy(toast = "请先粘贴课堂文本，或加入 OCR / 转写稿资料。")
            return
        }
        maybePromptMissingCloudConfig("CourseAnalysis 云端增强")
        val now = System.currentTimeMillis()
        val isSample = text.trim() == SampleCourses.SERIES_TEXT.trim() && ui.ocrImports.isEmpty() && ui.transcripts.isEmpty()
        val title = ui.courseTitle.ifBlank { "未命名课程" }

        // Stage 4B: unify text-class inputs through a LessonMaterialBundle, then feed the EXISTING
        // CourseAnalyzer with bundle.plainText() (plus a bounded, evidence-safe glossary hint). The
        // sample path keeps its curated, pre-bound analysis untouched.
        val bundle = if (ui.liveAnalyzed && ui.liveTranscript != null) {
            LessonMaterialAssembler.fromLiveWithOcr(ui.liveTranscript!!, ui.ocrImports, ui.transcripts, now)
        } else {
            LessonMaterialAssembler.fromImportWithOcr(
                title = title,
                text = text,
                importType = ui.importSourceType,
                ocrImports = ui.ocrImports,
                subject = ui.selectedSubject,
                transcripts = ui.transcripts,
                now = now,
            )
        }
        // Detect the course domain from the ACTUAL material (not just the picker default) so the prompt
        // hint carries the right vocabulary — mechanical content no longer gets 大学物理 terms — and build
        // a dynamic glossary for ANY subject, merged with the selected built-in starter pack.
        val bodyText = bundle.plainText()
        val domainResult = CourseDomainDetector.detect(
            title = title,
            manualCourseName = ui.selectedSubject,
            documentText = bodyText,
        )
        val dynamicTerms = DynamicGlossaryExtractor.extract(
            domain = domainResult.displayName,
            sources = listOf(GlossarySource("import_material", bodyText)),
            seedTerms = CourseDomainDetector.seedTermsFor(domainResult.domain),
            max = LessonContextHints.MAX_TERMS,
        ).map { it.term }
        val builtinTerms = CourseGlossary.termsFor(ui.selectedSubject).map { it.term }
        val hintSubject = if (!domainResult.requiresUserConfirmation) domainResult.displayName else ui.selectedSubject
        val glossaryHint = LessonContextHints.glossaryHint(
            subject = hintSubject,
            terms = (dynamicTerms + builtinTerms).distinct(),
        )
        val analyzerText = listOf(glossaryHint, bodyText).filter { it.isNotBlank() }.joinToString("\n\n")
        val materialSummary = LessonMaterialAssembler.summarize(bundle)

        val session = if (isSample) {
            SampleCourses.seriesSession(now)
        } else {
            CourseSegmenter.buildSession(
                id = "session_$now",
                title = title,
                rawText = analyzerText,
                nowMs = now,
                sourceKind = SourceKind.PASTED_TEXT,
            )
        }

        val intensity = ui.analysisIntensity
        // Content-aware time estimate (P0-4): scales with text length, image / OCR count and cloud-vs-local
        // — replaces the old fixed "60～90 秒" hint. Computed from the ACTUAL assembled material.
        val analysisEstimate = AnalysisTimeEstimator.estimate(
            AnalysisEstimateInput(
                chineseChars = bodyText.count { it.code in 0x4E00..0x9FFF },
                englishWords = Regex("[A-Za-z]+").findAll(bodyText).count(),
                imageCount = ui.ocrImports.count { it.status == OcrImportStatus.OK },
                ocrBatches = if (ui.ocrImports.any { it.status == OcrImportStatus.OK }) 1 else 0,
                usesCloudModel = hasConfiguredCloudModel(),
                hasAudioOrSubtitle = hasTranscript,
                localFallback = !hasConfiguredCloudModel(),
                intensity = intensity,
            ),
        )
        // Honest long-text record: the FULL original stays as Evidence (session segments); this only
        // documents the prompt view. analyzedLength >= originalLength because nothing is truncated.
        val longTextInfo = LongTextAnalysisInfo(
            originalLength = bodyText.length,
            analyzedLength = analyzerText.length,
            chunkCount = session.segments.size,
            strategy = if (session.segments.size > 1) "按段落切分（原文完整保留为证据）" else "整篇分析",
        )

        ui = ui.copy(
            session = session,
            lastMaterialSummary = materialSummary,
            analysisStatus = AnalysisStatus.RUNNING,
            analysisStageIndex = 0,
            analysisElapsedMs = 0L,
            analysisSlowNotice = false,
            analysisEstimateText = analysisEstimate.displayText,
            longTextInfo = longTextInfo,
            result = null,
            reviewPlan = null,
            answers = emptyMap(),
            revealedQuestionIds = emptySet(),
            currentQuestionIndex = 0,
            feedbackEvents = emptyList(),
            analysisError = null,
            detectedDomainLabel = domainResult.displayName,
            detectedDomainConfidence = domainResult.confidence,
            detectedDomainNeedsConfirm = domainResult.requiresUserConfirmation,
        )
        navigateTo(Screen.ANALYZE)

        analysisJob = viewModelScope.launch {
            // Live elapsed-time ticker — the analyze page must never look frozen during a slow cloud
            // READ. It also flips the "深度分析耗时较长" notice once past the intensity threshold.
            val started = System.currentTimeMillis()
            val ticker = launch {
                while (isActive) {
                    delay(1_000)
                    val elapsed = System.currentTimeMillis() - started
                    ui = ui.copy(
                        analysisElapsedMs = elapsed,
                        analysisSlowNotice = ui.analysisSlowNotice || elapsed >= intensity.slowNoticeThresholdMs,
                    )
                }
            }
            // The real pipeline runs off the main thread. For the bundled sample we show the
            // curated, evidence-bound analysis (clearly labelled demo data); for pasted text we
            // run the resolver, which falls back to the local heuristic when BlueLM is absent. The
            // intensity drives the cloud profile, read timeout, retry budget, and KP/quiz count.
            val cloudOutcome: AnalysisOutcome = try {
                withContext(Dispatchers.Default) {
                    if (isSample) {
                        AnalysisOutcome.Success(
                            result = SampleCourses.seriesAnalysis(now),
                            report = com.classmate.core.validation.ValidationReport.PASS,
                            logs = listOf(RedactedLogEntry("BLUELM", "OK", 0, "PASS", false, null)),
                        )
                    } else {
                        analyzer().analyze(
                            AnalysisRequest(
                                session = session,
                                maxKnowledgePoints = intensity.maxKnowledgePoints,
                                questionsPerKnowledgePoint = intensity.questionsPerKnowledgePoint,
                                intensity = intensity,
                            ),
                        )
                    }
                }
            } finally {
                ticker.cancel()
            }
            val totalElapsedMs = System.currentTimeMillis() - started
            val cloudSucceededSlowly = !isSample &&
                cloudOutcome is AnalysisOutcome.Success &&
                totalElapsedMs >= intensity.slowNoticeThresholdMs
            // Cloud success → use it. Cloud failed (or cloud-only chain produced nothing) → try the
            // on-device BlueLM 3B structured-analysis seam, which must pass the SAME validators to land.
            val cloudStatus = if (isSample) "OK" else (cloudOutcome as? AnalysisOutcome.Failure)?.lastError?.shortCode ?: "OK"
            val onDeviceAttempted = cloudOutcome !is AnalysisOutcome.Success
            val fallback = if (cloudOutcome is AnalysisOutcome.Success) null
            else onDeviceAnalysisFallback(session, cloudOutcome as AnalysisOutcome.Failure)
            val onDeviceReason = fallback?.second
            val onDeviceDiag = fallback?.third
            val baseOutcome = fallback?.first ?: cloudOutcome
            // Task 1 — terminal fallback: cloud + on-device both failed but we HAVE usable input → generate
            // a REAL local baseline (本地基础整理) instead of dropping to an empty 安全占位. 安全占位 is reserved
            // for empty input / safety-blocked / local-also-failed / hard error.
            val hasUsableInput = !isSample && session.segments.any { it.text.isNotBlank() }
            val localFallback = if (baseOutcome is AnalysisOutcome.Failure && hasUsableInput) {
                withContext(Dispatchers.Default) { runLocalRuleAnalysis(session) } as? AnalysisOutcome.Success
            } else {
                null
            }
            val outcome: AnalysisOutcome = localFallback ?: baseOutcome
            val usedLocalRule = localFallback != null
            // The unified AiCapabilityRouter is the authority for the cloud→on-device decision; when both
            // fail and local-rule produced a usable result the final source is 本地基础整理 (not 安全占位).
            val analysisRoute = CourseAnalysisRouting.decide(
                cloudSucceeded = cloudOutcome is AnalysisOutcome.Success,
                cloudStatusCode = cloudStatus,
                onDeviceAttempted = onDeviceAttempted,
                onDeviceAccepted = onDeviceReason == "ACCEPTED",
            )
            val sourceReport = AnalysisSourceReport(
                cloudStatus = cloudStatus,
                onDeviceAttempted = onDeviceAttempted,
                onDeviceReason = onDeviceReason,
                finalSource = if (usedLocalRule) "本地基础整理" else CourseAnalysisRouting.finalSourceZh(analysisRoute.source),
            )

            // Staged reveal for product feel — these are the real conceptual phases.
            for (stage in 1..TOTAL_STAGES) {
                ui = ui.copy(analysisStageIndex = stage)
                delay(STAGE_DELAY_MS)
            }

            when (outcome) {
                is AnalysisOutcome.Success -> {
                    val l3Snapshot = l3Pipeline.buildFromAnalysis(
                        session = session,
                        result = outcome.result,
                        sourceType = currentL3SourceType(),
                        providerSummary = ui.providerConfigSummary,
                        now = now,
                    )
                    publishL3Snapshot(l3Snapshot, now, "BlueLM analysis entered the unified L3 learning loop.")
                    ui = ui.copy(
                        logs = outcome.logs,
                        analysisSourceReport = sourceReport,
                        lastAnalysisLatencyMs = totalElapsedMs,
                        analysisSlowNotice = cloudSucceededSlowly,
                        onDeviceAnalysisReason = onDeviceReason ?: ui.onDeviceAnalysisReason,
                        onDeviceAnalysisDiagnostic = onDeviceDiag ?: ui.onDeviceAnalysisDiagnostic,
                        toast = when {
                            usedLocalRule && onDeviceReason == "PERMISSION_MISSING" ->
                                "端侧模型需要模型目录权限；已先用本地基础整理，不影响继续学习。"
                            usedLocalRule -> "已使用本地基础整理生成可用学习结果，云端可稍后重试。"
                            cloudSucceededSlowly -> "云端深度分析耗时较长（${totalElapsedMs / 1000}s），但已完成。"
                            else -> ui.toast
                        },
                    )
                }

                is AnalysisOutcome.Failure -> {
                    // True last resort: cloud + on-device failed AND local-rule could not produce a usable
                    // result (or input was empty / safety-blocked / sample). Only here do we land on 安全占位.
                    // We never persist a fake CourseAnalysis or invent knowledge points.
                    ui = ui.copy(
                        analysisStatus = AnalysisStatus.FAILED,
                        logs = outcome.logs,
                        analysisSourceReport = sourceReport,
                        onDeviceAnalysisReason = onDeviceReason ?: ui.onDeviceAnalysisReason,
                        onDeviceAnalysisDiagnostic = onDeviceDiag ?: ui.onDeviceAnalysisDiagnostic,
                        analysisError = buildString {
                            append("云端蓝心：").append(sourceReport.cloudStatus)
                            append("\n端侧蓝心：").append(if (sourceReport.onDeviceAttempted) "已尝试" else "未尝试")
                            append("\n端侧结果：").append(AnalysisSourceReport.onDeviceReasonZh(sourceReport.onDeviceReason))
                            append("\n本地基础整理：").append(if (hasUsableInput) "已尝试但未生成可用结果" else "无可用输入")
                            append("\n最终结果：安全占位（不生成假知识点，可重试或返回手动整理资料）")
                        },
                    )
                }
            }
        }
    }

    /**
     * Stage 8C Phase B: after the cloud analysis fails, try the on-device BlueLM 3B structured-analysis
     * seam. Only an [OnDeviceCourseAnalysis.Outcome.Accepted] (validators passed) becomes a Success;
     * otherwise the original cloud failure stands (caller shows the safety placeholder). No fake
     * knowledge points are ever produced, and validators are never relaxed.
     */
    /**
     * The live all-files-access signal for CLASSIFYING on-device availability failures. A successful
     * text init/generate is a positive signal that storage access is sufficient — it can never be
     * overridden back to "permission missing" by a stale snapshot (the Stage 8D-2 false negative).
     */
    private fun currentAllFilesAccess(): Boolean =
        allFilesAccessProvider?.invoke()
            ?: (ui.onDevicePermissions.allFilesAccess || onDeviceController.lastTextInitSucceeded())

    /** Returns the merged outcome, the on-device result code, AND the detailed on-device diagnostic. */
    private suspend fun onDeviceAnalysisFallback(
        session: CourseSession,
        cloudFailure: AnalysisOutcome.Failure,
    ): Triple<AnalysisOutcome, String, OnDeviceAnalysisDiagnostic> {
        val run = onDeviceController.analyzeCourse(session, currentAllFilesAccess())
        return when (val od = run.outcome) {
            is OnDeviceCourseAnalysis.Outcome.Accepted -> Triple(
                AnalysisOutcome.Success(
                    od.result,
                    od.report,
                    cloudFailure.logs + RedactedLogEntry("ONDEVICE_BLUELM", "OK", 0, "PASS", true, null),
                ),
                "ACCEPTED",
                run.diagnostic,
            )
            is OnDeviceCourseAnalysis.Outcome.Rejected -> Triple(
                AnalysisOutcome.Failure(
                    cloudFailure.lastError,
                    cloudFailure.report,
                    cloudFailure.logs + RedactedLogEntry("ONDEVICE_BLUELM", "FAIL", 0, "SKIPPED", true, od.reason),
                ),
                od.reason,
                run.diagnostic,
            )
        }
    }

    fun retryAnalysis() {
        if (backStack.last() == Screen.ANALYZE) backStack.removeAt(backStack.lastIndex)
        startAnalysis()
    }

    /** Set the thinking strength (快速/标准/深度). Affects timeout, prompt budget, quiz count, retries. */
    fun setAnalysisIntensity(intensity: AnalysisIntensity) {
        if (ui.analysisIntensity != intensity) ui = ui.copy(analysisIntensity = intensity)
    }

    /** Waiting-screen action: stop waiting on the cloud and produce a local baseline now. */
    fun switchToLocalRuleNow() {
        analysisJob?.cancel()
        generateWithLocalRuleAnalysis()
    }

    /** Waiting/failure action: cancel the current run, drop to 快速, and retry the cloud chain. */
    fun retryFast() {
        analysisJob?.cancel()
        setAnalysisIntensity(AnalysisIntensity.FAST)
        retryAnalysis()
    }

    /** Jump to Settings · 能力中心 so the user can grant model-dir access / re-check the on-device model. */
    fun goToOnDeviceSettings() {
        selectTab(Tab.SETTINGS)
    }

    /**
     * Synchronous local-rule analysis (no cloud/AI). LOCAL_ONLY resolver → the deterministic
     * LocalFallbackProvider, run through the SAME validators. Returns a real, usable [AnalysisOutcome]
     * so a permission-blocked / offline device is never stuck at an empty placeholder. Test seam.
     */
    internal fun runLocalRuleAnalysis(session: CourseSession): AnalysisOutcome {
        val localBundle = ProviderConfigBundle.forProfile(
            profile = LearnerProfile.LOCAL_ONLY,
            configs = configBundle.configs,
            policy = configBundle.policy,
        )
        return CourseAnalyzer(ProviderResolver(localBundle, promptBuilder, transport, blueLmSigner))
            .analyze(AnalysisRequest(session, intensity = ui.analysisIntensity))
    }

    /**
     * Explicit user choice from the failure screen: generate a baseline learning result LOCALLY, with no
     * cloud / AI call. It runs the deterministic [com.classmate.core.provider.LocalFallbackProvider] through
     * the SAME validators and pipeline, and is labelled honestly ("本地基础整理") — never dressed up as cloud.
     * This is why an offline / permission-blocked device is not stuck at an empty safety placeholder.
     */
    fun generateWithLocalRuleAnalysis() {
        val session = ui.session
        if (session == null) {
            ui = ui.copy(toast = "没有可整理的课程内容，请先返回导入。")
            return
        }
        val now = System.currentTimeMillis()
        ui = ui.copy(analysisStatus = AnalysisStatus.RUNNING, analysisStageIndex = 0, analysisError = null)
        analysisJob = viewModelScope.launch {
            val outcome = withContext(Dispatchers.Default) { runLocalRuleAnalysis(session) }
            for (stage in 1..TOTAL_STAGES) {
                ui = ui.copy(analysisStageIndex = stage)
                delay(STAGE_DELAY_MS)
            }
            when (outcome) {
                is AnalysisOutcome.Success -> {
                    val l3Snapshot = l3Pipeline.buildFromAnalysis(
                        session = session,
                        result = outcome.result,
                        sourceType = currentL3SourceType(),
                        providerSummary = ui.providerConfigSummary,
                        now = now,
                    )
                    publishL3Snapshot(l3Snapshot, now, "已用本地基础整理生成学习结果（未调用大模型）。")
                    ui = ui.copy(
                        logs = outcome.logs,
                        analysisSourceReport = AnalysisSourceReport(
                            cloudStatus = "LOCAL_RULE",
                            onDeviceAttempted = false,
                            onDeviceReason = null,
                            finalSource = "本地基础整理",
                        ),
                        toast = "已用本地基础整理生成学习结果（未调用大模型，可稍后用云端深度分析覆盖）。",
                    )
                }
                is AnalysisOutcome.Failure -> {
                    ui = ui.copy(
                        analysisStatus = AnalysisStatus.FAILED,
                        toast = "本地基础整理未能生成结果，可返回手动整理资料。",
                    )
                }
            }
        }
    }

    // --- knowledge / evidence ---
    fun openEvidence(knowledgePointId: String) {
        ui = ui.copy(selectedKnowledgePointId = knowledgePointId, selectedEvidenceId = null)
        ui.result?.sessionId?.let { sessionId ->
            learningStore.recordTracebackOpen(sessionId, knowledgePointId)
            ui = ui.copy(learningSnapshot = learningStore.snapshot())
        }
        navigateTo(Screen.EVIDENCE)
    }

    fun openEvidenceById(evidenceId: String) {
        if (evidenceId.isBlank() || evidenceOwnershipLevel(evidenceId) == EvidenceRelationLevel.MISSING) {
            ui = ui.copy(toast = "该内容暂无可回溯证据。")
            return
        }
        ui = ui.copy(selectedEvidenceId = evidenceId, selectedKnowledgePointId = null)
        navigateTo(Screen.EVIDENCE)
    }

    fun openEvidenceForQuestion(questionId: String) {
        val evidenceId = ui.l3Pipeline.questions.firstOrNull { it.id == questionId }
            ?.evidenceIds
            ?.firstOrNull()
        openEvidenceById(evidenceId.orEmpty())
    }

    /**
     * P0-2: resolve the evidence behind a quiz question so the practice screen can show its image / OCR /
     * source WHILE answering. Returns null when the question has no resolvable evidence. No raw ids leak —
     * only a local image file path + quote + source label.
     */
    fun practiceQuestionEvidence(quizId: String?): PracticeEvidenceContext? {
        val qId = quizId?.takeIf { it.isNotBlank() } ?: return null
        val evidenceId = ui.l3Pipeline.questions.firstOrNull { it.id == qId }?.evidenceIds?.firstOrNull()
            ?: return null
        val evidence = ui.l3Pipeline.evidence.firstOrNull { it.id == evidenceId } ?: return null
        val asset = evidence.assetId?.let { id -> ui.l3Pipeline.evidenceAssets.firstOrNull { it.id == id } }
        val imagePath = evidence.thumbnailRef.ifBlank { evidence.imageRef }
            .ifBlank { asset?.thumbnailRef.orEmpty() }
            .ifBlank { asset?.imageRef.orEmpty() }
        val quote = evidence.text.ifBlank { asset?.text.orEmpty() }
        val isImage = evidence.sourceType == L3SourceType.OCR_IMAGE || imagePath.isNotBlank()
        if (quote.isBlank() && imagePath.isBlank()) return null
        return PracticeEvidenceContext(
            evidenceId = evidenceId,
            quote = quote,
            imagePath = imagePath,
            sourceLabel = evidence.sourceLabel.ifBlank { asset?.sourceLabel.orEmpty() },
            isImage = isImage,
        )
    }

    fun reviewEvidenceIdForKnowledgePoint(knowledgePointId: String): String? =
        ui.l3Pipeline.knowledgePoints.firstOrNull { it.id == knowledgePointId }
            ?.sourceEvidenceIds
            ?.firstOrNull()
            ?: ui.l3Pipeline.questions.firstOrNull { it.knowledgePointId == knowledgePointId }
                ?.evidenceIds
                ?.firstOrNull()

    fun openEvidenceForKnowledgePoint(knowledgePointId: String) {
        openEvidenceById(reviewEvidenceIdForKnowledgePoint(knowledgePointId).orEmpty())
    }

    fun openEvidenceForReviewTask(task: ReviewTask) {
        openEvidenceForKnowledgePoint(task.knowledgePointId)
    }

    /**
     * True only when [evidenceId] resolves to evidence that has a retraceable excerpt — not merely a
     * dangling id. UI must gate "查看证据" on this so a mis-bound or empty evidence never claims to be
     * traceable; when it's false the surface shows an honest "暂无可回溯证据" instead.
     */
    /** The real, user-facing excerpt for an evidence id within the CURRENT pipeline (blank if none). */
    fun evidenceExcerptFor(evidenceId: String?): String {
        if (evidenceId.isNullOrBlank()) return ""
        if (evidenceOwnershipLevel(evidenceId) == EvidenceRelationLevel.MISSING) return ""
        val evidence = ui.l3Pipeline.evidence.firstOrNull { it.id == evidenceId } ?: return ""
        val asset = evidence.assetId?.let { id -> ui.l3Pipeline.evidenceAssets.firstOrNull { it.id == id } }
        return evidence.text
            .ifBlank { evidence.snippet }
            .ifBlank { evidence.transcriptSegment }
            .ifBlank { asset?.text.orEmpty() }
    }

    fun hasRetraceableEvidence(evidenceId: String?): Boolean = evidenceExcerptFor(evidenceId).isNotBlank()

    /**
     * Whether the bound evidence actually relates to the thing it supports. MISSING when there is no
     * retraceable excerpt; WEAK when the excerpt shares no keywords with [context] (likely mis-bound);
     * STRONG otherwise. Surfaces use this to show "查看证据" vs "证据待核对" vs "暂无可回溯证据".
     */
    fun evidenceRelationLevel(evidenceId: String?, context: String): EvidenceRelationLevel {
        val excerpt = evidenceExcerptFor(evidenceId)
        if (excerpt.isBlank()) return EvidenceRelationLevel.MISSING
        val ownership = evidenceOwnershipLevel(evidenceId)
        if (ownership == EvidenceRelationLevel.WEAK) return EvidenceRelationLevel.WEAK
        return EvidenceRelation.assess(excerpt, context)
    }

    private fun evidenceOwnershipLevel(
        evidenceId: String?,
        expectedLessonId: String? = null,
        expectedSourceLessonId: String? = null,
    ): EvidenceRelationLevel =
        EvidenceOwnership.assess(evidenceOwnershipSnapshot(), evidenceId, expectedLessonId, expectedSourceLessonId)

    private fun evidenceOwnershipSnapshot(snapshot: L3PipelineSnapshot = ui.l3Pipeline): EvidenceOwnership.Snapshot {
        val lesson = snapshot.lessonSource
        val sourceKind = ui.session?.sourceKind
        return EvidenceOwnership.Snapshot(
            snapshotId = lesson?.id.orEmpty(),
            lessonSourceId = lesson?.id.orEmpty(),
            lessonTitle = lesson?.title.orEmpty(),
            lessonSourceType = lesson?.type?.name.orEmpty(),
            isSampleCourse = sourceKind == SourceKind.SAMPLE || lesson?.status.equals("SAMPLE", ignoreCase = true),
            evidence = snapshot.evidence.map {
                EvidenceOwnership.EvidenceRecord(
                    id = it.id,
                    sourceId = it.sourceId,
                    sourceType = it.sourceType.name,
                    assetId = it.assetId,
                    sourceLabel = it.sourceLabel,
                    fileName = it.fileName,
                    imageRef = it.imageRef,
                    audioRef = it.audioRef,
                    excerpt = it.text.ifBlank { it.snippet }.ifBlank { it.transcriptSegment },
                )
            },
            assets = snapshot.evidenceAssets.map {
                EvidenceOwnership.AssetRecord(
                    id = it.id,
                    sourceType = it.sourceType.name,
                    sourceLabel = it.sourceLabel,
                    fileName = it.fileName,
                    imageRef = it.imageRef,
                    audioRef = it.audioRef,
                    createdAt = it.createdAt,
                )
            },
        )
    }

    // --- quiz ---
    fun answer(question: QuizQuestion, optionId: String) {
        if (question.id in ui.revealedQuestionIds) return
        val now = System.currentTimeMillis()
        val correct = question.isCorrect(listOf(optionId))
        val baseState = ui.learningState ?: LearningState(ui.result?.sessionId ?: "")
        val attempt = QuizAttempt(
            id = Ids.attempt(now, question.id),
            questionId = question.id,
            testedKnowledgePointIds = question.testedKnowledgePointIds,
            selectedOptionIds = listOf(optionId),
            isCorrect = correct,
            answeredAtEpochMs = now,
        )
        val updated = LearningStateUpdater().applyAttempt(baseState, attempt)
        val updatedL3 = l3Pipeline.submitAnswer(ui.l3Pipeline, question.id, optionId, now)
        ui = ui.copy(
            answers = ui.answers + (question.id to optionId),
            revealedQuestionIds = ui.revealedQuestionIds + question.id,
            learningState = updated,
            l3Pipeline = updatedL3,
            reviewPlan = null, // answers changed -> plan must regenerate
        )
        // Feed the cross-course review queue: records the attempt + WRONG/CORRECT_ANSWER rule.
        val sessionId = ui.result?.sessionId
        val kpId = question.testedKnowledgePointIds.firstOrNull()
        if (sessionId != null && kpId != null) {
            learningStore.recordQuizAttempt(
                courseSessionId = sessionId,
                knowledgePointId = kpId,
                quizId = question.id,
                selectedAnswer = optionId,
                correctAnswer = question.options.firstOrNull { it.isCorrect }?.id.orEmpty(),
                isCorrect = correct,
            )
            ui = ui.copy(learningSnapshot = learningStore.snapshot())
        }
        persistL3(updatedL3)
    }

    fun goToQuestion(index: Int) { ui = ui.copy(currentQuestionIndex = index) }

    // --- feedback ---
    fun submitFeedback(type: FeedbackType, targetKind: FeedbackTargetKind, targetId: String?, note: String = "") {
        val now = System.currentTimeMillis()
        val event = FeedbackEvent(
            id = Ids.feedback(now, ui.feedbackEvents.size),
            type = type,
            targetKind = targetKind,
            targetId = targetId,
            note = note,
            createdAtEpochMs = now,
        )
        val updated = ui.learningState?.let { LearningStateUpdater().applyFeedback(it, event) }
        // P0-3: a "this is inaccurate / evidence is wrong" feedback produces a VISIBLE effect — the target
        // knowledge point gets a 需复核 marker on the timeline, and a flagged question leaves random practice.
        val inaccurate = type == FeedbackType.NOT_ACCURATE || type == FeedbackType.EVIDENCE_WRONG || type == FeedbackType.MISSING_KEY_POINT
        val flaggedKpId = when (targetKind) {
            FeedbackTargetKind.KNOWLEDGE_POINT -> targetId
            FeedbackTargetKind.QUIZ_QUESTION -> ui.result?.quizQuestions?.firstOrNull { it.id == targetId }?.testedKnowledgePointIds?.firstOrNull()
            else -> null
        }?.takeIf { inaccurate }
        val flaggedQId = targetId.takeIf { inaccurate && targetKind == FeedbackTargetKind.QUIZ_QUESTION }
        val oldCoreQuestion = ui.result?.quizQuestions?.firstOrNull { it.id == targetId }
        val optimization = if (ui.l3Pipeline.lessonSource != null) {
            FeedbackLearningOptimizer.optimize(ui.l3Pipeline, type, targetKind, targetId, note, now)
        } else {
            null
        }
        val optimizedSnapshot = optimization?.snapshot ?: ui.l3Pipeline
        val optimizedResult = syncFeedbackOptimizationToResult(ui.result, oldCoreQuestion, optimization)
        ui = ui.copy(
            feedbackEvents = ui.feedbackEvents + event,
            learningState = updated ?: ui.learningState,
            result = optimizedResult,
            l3Pipeline = optimizedSnapshot,
            reviewPlan = null,
            flaggedKnowledgePointIds = if (flaggedKpId != null) ui.flaggedKnowledgePointIds + flaggedKpId else ui.flaggedKnowledgePointIds,
            flaggedQuestionIds = if (flaggedQId != null) ui.flaggedQuestionIds + flaggedQId else ui.flaggedQuestionIds,
            toast = optimization?.result?.message ?: if (inaccurate) "已加入待复核，复习计划已更新。" else "已记录反馈：${type.displayZh}",
        )
        optimization?.snapshot?.let { persistL3(it) }
        // Mirror into the cross-course review queue (when the feedback maps to a known kp).
        val sessionId = ui.result?.sessionId
        val kpId = when (targetKind) {
            FeedbackTargetKind.KNOWLEDGE_POINT -> targetId
            FeedbackTargetKind.QUIZ_QUESTION -> ui.result?.quizQuestions?.firstOrNull { it.id == targetId }?.testedKnowledgePointIds?.firstOrNull()
            else -> null
        }
        val reviewType = type.toReviewEventType()
        if (sessionId != null && kpId != null && reviewType != null) {
            learningStore.recordFeedback(sessionId, kpId, reviewType)
            ui = ui.copy(learningSnapshot = learningStore.snapshot())
        }
    }

    private fun syncFeedbackOptimizationToResult(
        result: CourseAnalysisResult?,
        oldCoreQuestion: QuizQuestion?,
        optimization: FeedbackOptimizationOutcome?,
    ): CourseAnalysisResult? {
        if (result == null || optimization == null) return result
        val snapshot = optimization.snapshot
        val updatedKnowledge = optimization.result.updatedKnowledgePointId?.let { kpId ->
            val updatedL3 = snapshot.knowledgePoints.firstOrNull { it.id == kpId }
            if (updatedL3 == null) result.knowledgePoints else {
                result.knowledgePoints.map { kp ->
                    if (kp.id == kpId) kp.copy(summary = updatedL3.explanation) else kp
                }
            }
        } ?: result.knowledgePoints
        val replacement = optimization.result.createdQuestionId?.let { id ->
            val l3Question = snapshot.questions.firstOrNull { it.id == id }
            if (l3Question != null) {
                val fallbackEvidence = oldCoreQuestion?.evidence.orEmpty()
                QuizQuestion(
                    id = l3Question.id,
                    type = oldCoreQuestion?.type ?: QuestionType.CONCEPT_UNDERSTANDING,
                    stem = l3Question.stem,
                    options = l3Question.options.mapIndexed { index, option ->
                        QuizOption(
                            id = QuizOptionIds.letterId(index),
                            text = QuizOptionIds.cleanText(option),
                            isCorrect = QuizOptionIds.isAnswer(index, option, l3Question.correctAnswer),
                            rationale = if (QuizOptionIds.isAnswer(index, option, l3Question.correctAnswer)) {
                                l3Question.explanation
                            } else {
                                "请回到课堂证据核对，该选项没有直接支撑。"
                            },
                        )
                    },
                    testedKnowledgePointIds = listOf(l3Question.knowledgePointId).filter { it.isNotBlank() },
                    evidence = fallbackEvidence,
                    explanation = l3Question.explanation,
                    difficulty = l3Question.difficulty,
                )
            } else {
                null
            }
        }
        val updatedQuestions = if (replacement != null && result.quizQuestions.none { it.id == replacement.id }) {
            result.quizQuestions + replacement
        } else {
            result.quizQuestions
        }
        return result.copy(knowledgePoints = updatedKnowledge, quizQuestions = updatedQuestions)
    }

    private fun FeedbackType.toReviewEventType(): ReviewEventType? = when (this) {
        FeedbackType.ALREADY_MASTERED, FeedbackType.TOO_EASY -> ReviewEventType.MASTERED
        FeedbackType.TOO_HARD -> ReviewEventType.TOO_HARD
        FeedbackType.NEED_MORE_EXAMPLES -> ReviewEventType.NEED_EXAMPLE
        FeedbackType.EVIDENCE_WRONG, FeedbackType.NOT_ACCURATE -> ReviewEventType.EVIDENCE_WRONG
        FeedbackType.MISSING_KEY_POINT -> null
    }

    // --- cross-course review queue (Review tab) ---
    fun reviewMarkDone(taskId: String) { learningStore.markTaskDone(taskId); syncLearning("已标记完成") }
    fun reviewSetPriority(taskId: String, level: ReviewPriorityLevel) { learningStore.updateTaskPriority(taskId, level); syncLearning() }
    fun reviewMoveUp(taskId: String) { learningStore.moveTaskUp(taskId); syncLearning() }
    fun reviewMoveDown(taskId: String) { learningStore.moveTaskDown(taskId); syncLearning() }
    fun reviewSetPinned(taskId: String, pinned: Boolean) { learningStore.setPinned(taskId, pinned); syncLearning() }
    fun reviewRemove(taskId: String) { learningStore.removeTaskFromPlan(taskId); syncLearning("已移除复习任务") }
    fun reviewRestore(taskId: String) { learningStore.restoreRemovedTask(taskId); syncLearning("已恢复复习任务") }
    fun reviewTaskFeedback(taskId: String, type: ReviewEventType) { learningStore.recordFeedbackForTask(taskId, type); syncLearning("已记录反馈") }

    // --- export ---
    fun exportLearningReport(format: ExportFormat = ExportFormat.MARKDOWN) {
        val record = ui.history.firstOrNull()
        val session = ui.session ?: record?.session
        val result = ui.result ?: record?.result
        if (session == null || result == null) {
            ui = ui.copy(toast = "No learning report is available yet.")
            return
        }
        saveExport(buildFullReport(session, result, ui.reviewPlan, ui.learningState, format))
    }

    fun exportHistoryReport(record: HistoryRecord, format: ExportFormat = ExportFormat.MARKDOWN) {
        saveExport(buildFullReport(record.session, record.result, reviewPlan = null, learningState = null, format = format))
    }

    fun exportReviewReport(format: ExportFormat = ExportFormat.MARKDOWN) {
        val session = ui.session
        val result = ui.result
        if (session != null && result != null) {
            ensureReviewPlan()
            saveExport(buildFullReport(session, result, ui.reviewPlan, ui.learningState, format))
        } else {
            saveExport(contentExporter.exportReviewQueue(ui.learningSnapshot, format))
        }
    }

    fun prepareRefinedExportDraft(): Boolean {
        val hasCourseReport = ui.session != null && ui.result != null
        val hasHistoryReport = ui.history.isNotEmpty()
        val hasReviewReport = ui.learningSnapshot.tasks.isNotEmpty() || ui.learningSnapshot.practiceHistory.isNotEmpty()
        if (!hasCourseReport && !hasHistoryReport && !hasReviewReport) {
            ui = ui.copy(toast = "暂无可生成的学习报告草稿。")
            return false
        }
        maybePromptMissingCloudConfig("Export AI 精炼报告")
        val source = refinedExportSourceLabel()
        ui = ui.copy(
            exportDraftReady = true,
            exportDraftSource = source,
            exportDraftMessage = "已生成学习报告草稿，可选择 PDF、DOCX、HTML、Markdown、Text 或课程精华音频脚本。",
            aiProcessing = AiProcessingUiState(
                visible = true,
                title = "正在提炼课堂精华",
                steps = listOf("整理课程资料", "整理知识结构", "汇总练习与薄弱点", "生成学习报告草稿", "等待选择导出格式"),
                activeStep = 3,
                source = source,
                fallbackMessage = "云端或端侧不可用时，使用本地模板整理；DOCX/PDF 失败时可改导出 HTML 或 Text。",
                canCancel = true,
                canRetry = true,
                canContinueManual = true,
            ),
            toast = "学习报告草稿已准备好，请选择导出格式。",
        )
        return true
    }

    fun buildLearningReportArtifact(format: ExportFileFormat): ExportArtifact? {
        val record = ui.history.firstOrNull()
        val session = ui.session ?: record?.session
        val result = ui.result ?: record?.result
        if (session == null || result == null) {
            ui = ui.copy(toast = "暂无可导出的学习报告。")
            return null
        }
        return buildReportArtifact(session, result, ui.reviewPlan, ui.learningState, format)
    }

    fun buildCurrentReportArtifact(format: ExportFileFormat): ExportArtifact? {
        val session = ui.session
        val result = ui.result
        if (session == null || result == null) {
            ui = ui.copy(toast = "暂无当前课程报告。")
            return null
        }
        return buildReportArtifact(session, result, ui.reviewPlan, ui.learningState, format)
    }

    fun buildLearningStudyPackArtifact(format: ExportFileFormat): ExportArtifact? {
        val snapshot = ui.l3Pipeline
        val title = snapshot.lessonSource?.title ?: ui.session?.title ?: ui.history.firstOrNull()?.session?.title
        if (snapshot.lessonSource == null && snapshot.knowledgePoints.isEmpty() && snapshot.questions.isEmpty()) {
            ui = ui.copy(toast = "暂无完整学习包，已导出空态学习文档。")
        }
        val markdown = LearningExportEngine.buildStudyPackMarkdown(snapshot)
        return ExportCenter.artifactFromMarkdown(
            courseTitle = title ?: "ClassMate study pack",
            markdown = markdown,
            format = format,
        )
    }

    /** P0-3: export the AI-organized study material (CourseDetail's 'AI 复习材料' result) into the export
     *  chain. The source label is carried into the document; SafeExportText strips any ids/debug tokens. */
    fun buildAiStudyMaterialArtifact(format: ExportFileFormat): ExportArtifact? {
        val state = ui.studyPackEnhancement
        if (!state.hasResult) {
            ui = ui.copy(toast = "请先生成 AI 整理版材料，再导出。")
            return null
        }
        val title = ui.session?.title?.ifBlank { "未命名课程" } ?: "ClassMate AI 整理版"
        val formLabel = if (state.type == AiEnhancementType.EXAM_CRAM_SHEET) "考前速记版" else "讲义版"
        val markdown = buildString {
            append("# 《$title》· AI $formLabel\n\n")
            append("> 来源：${state.sourceZh}\n\n")
            append(SafeExportText.redact(state.text))
        }
        return ExportCenter.artifactFromMarkdown(courseTitle = "$title-AI-$formLabel", markdown = markdown, format = format)
    }

    // ---- P0-1/P0-2: user-initiated AI 精修导出 / 导出资料升级 ----
    // A deliberate LONG task (NOT the 30s secondary-enhancement path): cloud 蓝心 deep pass (reusing the
    // user's analysis intensity timeout, 120–240s) → on-device → honest local organize. The default fast
    // export stays untouched/instant; a failure never overwrites it. PDF/HTML/Word all render from the
    // SAME PolishedStudyPack.
    private var polishExportJob: Job? = null

    /** True when there is real course material to upgrade (drives the 精修 entry's visibility). */
    fun hasPolishableMaterial(): Boolean {
        val s = ui.l3Pipeline
        return s.knowledgePoints.isNotEmpty() || s.summary.isNotBlank()
    }

    private fun polishedExportInput(): PolishedStudyPackInput? {
        val snapshot = ui.l3Pipeline
        val title = snapshot.lessonSource?.title?.ifBlank { null }
            ?: ui.session?.title ?: ui.history.firstOrNull()?.session?.title ?: return null
        val evidenceById = snapshot.evidence.associateBy { it.id }
        val materials = snapshot.knowledgePoints.map { kp ->
            val quote = kp.sourceEvidenceIds
                .firstNotNullOfOrNull { id -> evidenceById[id]?.text?.takeIf { it.isNotBlank() } }
                .orEmpty()
            PolishedStudyMaterial(kp.title, kp.explanation, quote)
        }
        if (materials.isEmpty() && snapshot.summary.isBlank()) return null
        val sources = snapshot.evidence.mapNotNull { it.sourceLabel.ifBlank { null } }.distinct().take(4)
        val hasImage = snapshot.evidence.any { it.imageRef.isNotBlank() } ||
            snapshot.evidenceAssets.any { it.imageRef.isNotBlank() }
        val lowConf = snapshot.qualityWarnings.map { it.message } +
            snapshot.asrJobs.flatMap { j -> j.transcriptSegments.filter { it.lowConfidence }.map { "低置信转写：${it.text.take(60)}" } }
        return PolishedStudyPackInput(
            courseTitle = title,
            sourceSummary = sources.joinToString("、").ifBlank { "课堂资料" },
            knowledgePoints = materials,
            quizStems = snapshot.questions.map { it.stem },
            weakPoints = snapshot.learningDiagnosis.weakKnowledgePoints.map { it.title },
            lowConfidenceNotes = lowConf,
            hasImageMaterial = hasImage,
        )
    }

    /** The honest local organize used when no cloud/on-device model is available (labelled 本地整理版). */
    private fun localPolishedMarkdown(): String = LearningExportEngine.buildStudyPackMarkdown(ui.l3Pipeline)

    /** Start the polished export. Shows staged progress + a >30s slow notice; cancelable; never blocks the
     *  fast default export and never overwrites it on failure. */
    fun startPolishedExport() {
        val input = polishedExportInput() ?: run {
            ui = ui.copy(toast = "暂无可精修的课程资料，请先生成学习包。")
            return
        }
        maybePromptMissingCloudConfig("AI 精修导出")
        polishExportJob?.cancel()
        val startedAt = System.currentTimeMillis()
        ui = ui.copy(
            polishedExport = PolishedExportUiState(
                status = PolishedExportStatus.RUNNING,
                stageIndex = 0,
                startedAtMs = startedAt,
                message = "正在准备课程资料……",
                pack = ui.polishedExport.pack, // keep any previous polished version until the new one is ready
            ),
        )
        val bundle = configBundle
        val tx = transport
        val timeouts = PolishedExportPlan.timeouts(ui.analysisIntensity)
        val prompt = PolishedStudyPackPromptBuilder.build(input)
        val title = input.courseTitle
        val generatedLabel = formatExportTime(startedAt)
        polishExportJob = viewModelScope.launch {
            ui = ui.copy(polishedExport = ui.polishedExport.copy(stageIndex = 1, message = "正在调用蓝心深度整理，可能需要更久……"))
            val slowJob = launch {
                delay(30_000)
                if (isActive && ui.polishedExport.running) {
                    ui = ui.copy(polishedExport = ui.polishedExport.copy(slowNotice = true, message = "蓝心正在深度整理，可继续等待或改用普通导出。"))
                }
            }
            val (markdown, source) = withContext(Dispatchers.IO) {
                val cloud = runCatching { ProviderAskChatClient(bundle, tx, timeouts = timeouts).chat(prompt, null) }.getOrNull()
                if (cloud != null && cloud.text.isNotBlank()) return@withContext cloud.text to AiExecutionSource.CLOUD
                val device = runCatching { onDeviceController.askSeam().chat(prompt, null) }.getOrNull()
                if (device != null && device.text.isNotBlank()) return@withContext device.text to AiExecutionSource.ON_DEVICE
                "" to AiExecutionSource.SAFE_PLACEHOLDER
            }
            slowJob.cancel()
            ui = ui.copy(polishedExport = ui.polishedExport.copy(stageIndex = 3, message = "正在生成复习资料……"))
            val resolved = if (markdown.isNotBlank()) markdown else localPolishedMarkdown()
            val sourceZh = PolishedExportPlan.sourceLabel(source)
            val pack = PolishedStudyPack(
                courseTitle = title,
                sourceLabel = sourceZh,
                generatedAtLabel = generatedLabel,
                markdown = SafeExportText.redact(resolved),
            )
            val ok = !pack.isBlank
            ui = ui.copy(
                polishedExport = PolishedExportUiState(
                    status = if (ok) PolishedExportStatus.READY else PolishedExportStatus.FAILED,
                    stageIndex = PolishedExportUiState.POLISH_STAGES.size,
                    startedAtMs = startedAt,
                    sourceZh = sourceZh,
                    message = when {
                        !ok -> "精修生成失败，请重试，或继续使用普通导出。"
                        source == AiExecutionSource.SAFE_PLACEHOLDER -> "未使用云端蓝心，已用本地整理版，可重试获取蓝心精修版。"
                        else -> "精修学习包已生成（$sourceZh），可选择 PDF / HTML / Word 等格式导出。"
                    },
                    pack = if (ok) pack else ui.polishedExport.pack,
                ),
                toast = if (ok) "精修学习包已生成（$sourceZh）。" else "精修生成失败，请重试或使用普通导出。",
            )
        }
    }

    /** Cancel an in-flight polished pass. Keeps any previously-generated polished version; never touches the
     *  fast default export. */
    fun cancelPolishedExport() {
        polishExportJob?.cancel()
        polishExportJob = null
        val prev = ui.polishedExport
        ui = ui.copy(
            polishedExport = if (prev.pack != null) {
                prev.copy(status = PolishedExportStatus.READY, slowNotice = false, message = "已取消重新精修，仍可导出上一版（${prev.sourceZh}）。")
            } else {
                PolishedExportUiState()
            },
            toast = "已取消精修，可继续使用普通导出。",
        )
    }

    /** Build an export artifact from the polished pack (PDF/HTML/Word all share the SAME content). */
    fun buildPolishedArtifact(format: ExportFileFormat): ExportArtifact? {
        val pack = ui.polishedExport.pack ?: run {
            ui = ui.copy(toast = "请先点击「AI 精修导出」生成精修版。")
            return null
        }
        return ExportCenter.artifactFromPolishedPack(pack, format)
    }

    private fun formatExportTime(epochMs: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(epochMs))

    // ---- P1-2: Flow background music (lifecycle owned here, survives leaving the Flow page) ----
    fun flowMusicPlay(sound: AmbientSound) {
        flowAudioController.play(sound, ui.flowMusic.volume)
        ui = ui.copy(flowMusic = ui.flowMusic.copy(status = FlowMusicStatus.PLAYING, sceneId = sound.id, soundName = sound.displayName))
    }

    fun flowMusicPause() {
        flowAudioController.pause()
        ui = ui.copy(flowMusic = ui.flowMusic.copy(status = FlowMusicStatus.PAUSED))
    }

    fun flowMusicStop() {
        flowAudioController.stop()
        ui = ui.copy(flowMusic = ui.flowMusic.copy(status = FlowMusicStatus.STOPPED))
    }

    fun flowMusicSetVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        flowAudioController.setVolume(v)
        ui = ui.copy(flowMusic = ui.flowMusic.copy(volume = v))
    }

    fun buildHistoryReportArtifact(record: HistoryRecord, format: ExportFileFormat): ExportArtifact =
        buildReportArtifact(record.session, record.result, reviewPlan = null, learningState = null, format = format)

    fun buildReviewReportArtifact(format: ExportFileFormat): ExportArtifact? {
        val session = ui.session
        val result = ui.result
        return if (session != null && result != null) {
            ensureReviewPlan()
            buildReportArtifact(session, result, ui.reviewPlan, ui.learningState, format)
        } else {
            // No current course open: render a printable review-queue-only StudyReport (same source).
            val report = StudyReportBuilder.reviewQueueOnly(
                snapshot = ui.learningSnapshot,
                providerLabel = ui.providerConfigSummary.profileLabel,
                generatedAtEpochMs = System.currentTimeMillis(),
            )
            ExportCenter.artifactFromStudyReport(report, format)
        }
    }

    private fun refinedExportSourceLabel(): String {
        val result = ui.result ?: ui.history.firstOrNull()?.result
        return when {
            result == null -> "本地模板整理"
            OnDeviceCourseAnalysis.isOnDevice(result.provenance) -> "端侧蓝心"
            result.provenance.provider == ProviderKind.BLUELM -> "云端蓝心"
            else -> "本地模板整理"
        }
    }

    fun recordExportAction(artifact: ExportArtifact, action: ExportActionStatus, message: String) {
        ui = ui.copy(
            lastExportReceipt = ExportReceipt(
                fileName = artifact.fileName,
                mimeType = artifact.mimeType,
                length = artifact.bytes.size,
                pathSummary = when (action) {
                    ExportActionStatus.SAVED_AS -> "用户选择的位置"
                    ExportActionStatus.SAVED_DOWNLOADS -> "系统下载目录 / Downloads"
                    ExportActionStatus.SHARED -> "系统分享面板"
                    ExportActionStatus.INTERNAL_ONLY -> "应用内部备份目录（legacy）"
                    ExportActionStatus.FAILED -> "失败"
                    ExportActionStatus.CANCELED -> "已取消"
                },
                format = artifact.format.displayName,
                createdAt = artifact.createdAt,
                lastAction = action,
                message = message,
            ),
            toast = message,
        )
    }

    private fun buildReportArtifact(
        session: CourseSession,
        result: CourseAnalysisResult,
        reviewPlan: com.classmate.core.model.ReviewPlan?,
        learningState: LearningState?,
        format: ExportFileFormat,
    ): ExportArtifact {
        // Mind map keeps its own structure; every other format renders from one printable StudyReport.
        if (format == ExportFileFormat.MINDMAP_MARKDOWN || format == ExportFileFormat.MINDMAP_HTML) {
            val mindMap = MindMapBuilder.fromAnalysis(
                result = result,
                courseTitle = session.title,
                learningState = learningState,
                learningSnapshot = ui.learningSnapshot,
            )
            return ExportCenter.artifactFromMarkdown(session.title, contentExporter.markdownMindMap(mindMap), format)
        }
        val baseReport = buildStudyReport(session, result)
        val safety = TextSafetyGate.checkForExport(
            StudyReportRenderer.renderPlainText(baseReport),
            BasicTextSafetyProvider,
        )
        ui = ui.copy(textSafetyResult = safety)
        val report = buildStudyReport(session, result)
        val routedReport = routeStudyReport(report, result)
        internalTools.execute(
            InternalToolCall(name = InternalToolName.EXPORT_STUDY_REPORT),
            result,
            ui.learningSnapshot,
            routedReport.value ?: report,
        )
        ui = ui.copy(
            exportDraftSource = routedReport.sourceLabelZh,
            exportDraftMessage = when (routedReport.source) {
                AiExecutionSource.CLOUD -> "学习报告草稿由云端蓝心分析结果整理，可继续选择格式导出。"
                AiExecutionSource.ON_DEVICE -> "学习报告草稿由端侧蓝心结果整理，可继续选择格式导出。"
                AiExecutionSource.MANUAL -> "学习报告草稿由手动确认资料整理，可继续选择格式导出。"
                AiExecutionSource.SAFE_PLACEHOLDER -> "学习报告草稿由本地模板整理，可继续选择格式导出。"
            },
        )
        return ExportCenter.artifactFromStudyReport(routedReport.value ?: report, format)
    }

    /** Builds the single printable StudyReport that all non-mindmap export formats render from. */
    private fun buildStudyReport(session: CourseSession, result: CourseAnalysisResult): StudyReport {
        val summary = ui.lastMaterialSummary
        return StudyReportBuilder.build(
            courseTitle = session.title,
            result = result,
            session = session,
            snapshot = ui.learningSnapshot,
            askAnswers = ui.askLessonAnswers,
            sourceSummaryLine = summary?.exportLine(),
            transcriptSummaryLine = summary?.transcriptLine(),
            sourceTypeLabels = summary?.sourceTypeLabels() ?: emptyList(),
            providerLabel = safeProviderLabel(result),
            generatedAtEpochMs = System.currentTimeMillis(),
            practiceHistory = ui.learningSnapshot.practiceHistory,
            lastPractice = ui.practiceResult?.takeIf { it.courseSessionId == result.sessionId },
            // Phase C: embed the on-device local-intelligence study advice (or placeholder) when present.
            localSuggestion = ui.onDeviceReportSuggestion,
            translationNotes = ui.translationNotes,
            safetyResult = ui.textSafetyResult,
            courseEssenceScript = ui.courseEssenceScript,
        )
    }

    private fun routeStudyReport(report: StudyReport, result: CourseAnalysisResult): AiCapabilityResult<StudyReport> =
        AiCapabilityRouter().route(
            capability = AiCapability.EXPORT_REPORT,
            stages = listOf(
                AiStage(AiExecutionSource.CLOUD) {
                    if (result.provenance.provider == ProviderKind.BLUELM && !OnDeviceCourseAnalysis.isOnDevice(result.provenance)) {
                        StageOutcome.Produced(report.copy(providerLabel = "云端蓝心 / ${report.providerLabel}"))
                    } else {
                        StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING)
                    }
                },
                AiStage(AiExecutionSource.ON_DEVICE) {
                    if (OnDeviceCourseAnalysis.isOnDevice(result.provenance) || ui.onDeviceReportSuggestion != null) {
                        StageOutcome.Produced(report.copy(providerLabel = "端侧蓝心 / ${report.providerLabel}"))
                    } else {
                        StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING)
                    }
                },
                AiStage(AiExecutionSource.MANUAL) {
                    if (report.overview.isNotEmpty() || report.knowledgePoints.isNotEmpty()) {
                        StageOutcome.Produced(report.copy(providerLabel = "手动确认资料 / ${report.providerLabel}"), confidence = 0.8)
                    } else {
                        StageOutcome.Unavailable(AiExecutionStatus.LOW_CONFIDENCE)
                    }
                },
            ),
            terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) {
                StageOutcome.Produced(report.copy(providerLabel = "本地模板整理 / ${report.providerLabel}"))
            },
        )

    fun generateCourseEssenceAudioScript() {
        val session = ui.session ?: run {
            ui = ui.copy(toast = "请先打开一节已分析课程。")
            return
        }
        val result = ui.result ?: run {
            ui = ui.copy(toast = "请先完成课程分析。")
            return
        }
        maybePromptMissingCloudConfig("课程精华音频脚本")
        ui = ui.copy(
            aiProcessing = AiProcessingUiState(
                visible = true,
                title = "正在生成音频脚本",
                steps = listOf("提炼课程概览", "整理核心知识点", "整理易错点", "生成可朗读稿"),
                activeStep = 2,
                source = "手动",
                fallbackMessage = "TTS 未配置时，可先导出课程精华音频脚本文本。",
                canCancel = true,
                canRetry = true,
                canContinueManual = true,
            ),
        )
        val report = buildStudyReport(session, result)
        internalTools.execute(
            InternalToolCall(name = InternalToolName.CREATE_ESSENCE_AUDIO_SCRIPT),
            result,
            ui.learningSnapshot,
            report,
        )
        val audio = CourseEssenceAudioExporter.generate(report, ConfigMissingTtsProvider())
        ui = ui.copy(
            courseEssenceScript = audio.script,
            courseEssenceAudioResult = audio,
            aiProcessing = AiProcessingUiState.hidden(),
            toast = "已生成课程精华音频脚本，可在导出中心选择脚本格式。",
        )
    }

    /** Safe, short provider label for the report cover — never a credential or raw body. */
    private fun safeProviderLabel(result: CourseAnalysisResult): String {
        // On-device analysis keeps provider=BLUELM but is marked 端侧蓝心 via modelLabel (Stage 8C).
        if (OnDeviceCourseAnalysis.isOnDevice(result.provenance)) return "端侧蓝心 / BlueLM 3B"
        val base = when (result.provenance.provider) {
            ProviderKind.BLUELM -> "云端蓝心"
            ProviderKind.COMPATIBLE -> "云端兼容模型"
            // LocalRule is no longer presented as an intelligent capability — only a safety placeholder.
            ProviderKind.LOCAL_FALLBACK -> "安全占位"
        }
        val model = displayModelForReport(configBundle.configOf(result.provenance.provider)?.model.orEmpty())
        return if (model.isNotBlank()) "$base / $model" else base
    }

    private fun displayModelForReport(raw: String): String {
        val model = raw.trim()
        return when {
            model.isBlank() -> ""
            model.contains("Seed-2.0", ignoreCase = true) -> "qwen3.5-plus"
            else -> model
        }
    }

    private fun buildFullReport(
        session: CourseSession,
        result: CourseAnalysisResult,
        reviewPlan: com.classmate.core.model.ReviewPlan?,
        learningState: LearningState?,
        format: ExportFormat,
    ): ExportDocument {
        val plan = reviewPlan ?: ReviewPlanner().plan(
            result = result,
            state = learningState ?: LearningState.seed(result.sessionId, result.knowledgePoints, System.currentTimeMillis()),
            feedback = ui.feedbackEvents,
        )
        val mindMap = MindMapBuilder.fromAnalysis(
            result = result,
            courseTitle = session.title,
            learningState = learningState,
            learningSnapshot = ui.learningSnapshot,
        )
        val videos = ui.learningSnapshot.tasks
            .filter { it.courseSessionId == result.sessionId }
            .flatMap { VideoRecommendationEngine.recommendationsForTask(it) }
        val document = contentExporter.exportFullReport(
            session = session,
            result = result,
            reviewPlan = plan,
            mindMap = mindMap,
            videoRecommendations = videos,
            format = format,
            courseSummaries = courseSummaries(),
            learningSnapshot = ui.learningSnapshot,
            weaknesses = weaknessItems(),
            askAnswers = ui.askLessonAnswers,
        )
        // Stage 4B/5B: append a safe "material source summary" line (type/counts only; no raw content,
        // no credentials). Stage 5B adds a transcript roll-up (segment / timestamp / speaker counts).
        // Best-effort; absent when this report wasn't produced via a fused bundle.
        val summary = ui.lastMaterialSummary ?: return document
        val transcriptLine = summary.transcriptLine()?.let { "\n$it" }.orEmpty()
        return document.copy(content = document.content + "\n\n## 资料来源摘要\n" + summary.exportLine() + transcriptLine + "\n")
    }

    private fun saveExport(document: ExportDocument) {
        val receipt = try {
            exportStore.save(document)
        } catch (e: Exception) {
            ui = ui.copy(toast = "Export failed.")
            return
        }
        ui = ui.copy(lastExportReceipt = receipt, toast = "Exported ${receipt.fileName} to ${receipt.pathSummary}")
    }

    fun courseSummaries() = CourseLibraryBuilder.build(
        records = ui.history.map { it.toCourseRecordSnapshot() },
        learningSnapshot = ui.learningSnapshot,
    )

    fun selectCourse(courseKey: String?) {
        ui = ui.copy(selectedCourseKey = courseKey)
    }

    fun openCourse(courseKey: String) {
        ui = ui.copy(selectedCourseKey = courseKey)
        val record = recordsForCourse(courseKey).firstOrNull()
        if (record != null && ui.session?.id != record.session.id) {
            loadHistoryRecord(record)
        }
        navigateTo(Screen.COURSE_DETAIL)
    }

    fun openLatestKnowledgeFromHome() {
        val record = ui.history.firstOrNull()
        if (record == null) {
            ui = ui.copy(toast = "暂无知识点，请先导入课堂资料。")
            selectTab(Tab.IMPORT)
            return
        }
        openHistoryTimeline(record)
    }

    fun recordsForCourse(courseKey: String): List<HistoryRecord> =
        ui.history.filter { CourseLibraryBuilder.normalizeCourseName(it.title).lowercase() == courseKey }
            .sortedByDescending { it.createdAtEpochMs }

    fun reviewTasksForCourse(courseKey: String): List<ReviewTask> =
        ui.learningSnapshot.tasks.filter { CourseLibraryBuilder.normalizeCourseName(it.courseTitle).lowercase() == courseKey }

    fun weaknessItems() = WeaknessHub.fromSnapshot(ui.learningSnapshot)

    /**
     * P1-3: honest browser-search options for a knowledge point. Gated on confidence — a point the user
     * flagged 需复核 or one with no evidence returns [KnowledgePointSearch.Result.NeedsReview] so the UI shows
     * a "先完善资料" hint instead of a search button. Never an in-app API search.
     */
    fun knowledgePointSearch(knowledgePointId: String): KnowledgePointSearch.Result {
        val result = ui.result ?: return KnowledgePointSearch.Result.NeedsReview
        val kp = result.knowledgePoints.firstOrNull { it.id == knowledgePointId }
            ?: return KnowledgePointSearch.Result.NeedsReview
        val course = ui.session?.title ?: ui.history.firstOrNull()?.session?.title ?: ""
        val highConfidence = knowledgePointId !in ui.flaggedKnowledgePointIds && kp.evidence.isNotEmpty()
        return KnowledgePointSearch.forKnowledgePoint(course, kp.title, highConfidence)
    }

    // --- adaptive practice (Stage 7C): in-app drill that writes back through ReviewEngine rules ---

    /** Start a real quiz session in [mode] for the current course (or load a course if none is open). */
    fun startPractice(mode: PracticeMode) {
        startPracticeInternal(mode, PracticeQuestionMode.REAL_QUIZ)
    }

    fun startSelfAssessment(mode: PracticeMode = PracticeMode.EVIDENCE_RECALL) {
        startPracticeInternal(mode, PracticeQuestionMode.SELF_ASSESSMENT)
    }

    fun startExam(mode: PracticeMode = PracticeMode.QUICK_REVIEW) {
        startPracticeInternal(mode, PracticeQuestionMode.EXAM)
    }

    // ---- Adaptive AI Learning Layer: real second-pass enhancements at high-value nodes ----
    // Routes Cloud (云端蓝心 via the SAME ProviderAskChatClient the analyzer/Ask use) → On-device (端侧蓝心)
    // → real local template, off the main thread. Every result carries an honest source label; the local
    // path is genuine grounded Chinese, never presented as 蓝心.
    private val enhancementUseCase = RoutedEnhancementUseCase()

    private fun enhancementSourceZh(source: AiExecutionSource): String = when (source) {
        AiExecutionSource.CLOUD -> "蓝心整理版"
        AiExecutionSource.ON_DEVICE -> "端侧模型草稿"
        else -> "本地整理版"
    }

    private suspend fun routeEnhancement(
        type: AiEnhancementType,
        prompt: Prompt,
        localTemplate: () -> String,
    ): AiCapabilityResult<AiEnhancement> {
        val bundle = configBundle
        val tx = transport
        return withContext(Dispatchers.IO) {
            enhancementUseCase.enhance(
                type = type,
                cloud = {
                    val reply = runCatching { ProviderAskChatClient(bundle, tx, timeouts = HttpTimeouts.BLUE_LM_DIAGNOSTIC).chat(prompt, null) }.getOrNull()
                    if (reply != null && reply.text.isNotBlank()) StageOutcome.Produced(reply.text)
                    else StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING)
                },
                onDevice = {
                    val reply = runCatching { onDeviceController.askSeam().chat(prompt, null) }.getOrNull()
                    if (reply != null && reply.text.isNotBlank()) StageOutcome.Produced(reply.text)
                    else StageOutcome.Unavailable(AiExecutionStatus.UNSUPPORTED_MODALITY)
                },
                localTemplate = localTemplate,
            )
        }
    }

    private fun enhancementResultState(type: AiEnhancementType, result: AiCapabilityResult<AiEnhancement>): EnhancementUiState {
        val text = result.value?.text.orEmpty()
        return EnhancementUiState(
            type = type,
            running = false,
            text = text,
            sourceZh = if (text.isBlank()) "AI 整理失败，已保留基础版" else enhancementSourceZh(result.source),
            failed = text.isBlank(),
        )
    }

    /** P0-1: generate an AI-organized, review/print-ready study material for the CURRENT course. */
    fun generateStudyPackEnhancement(form: AiEnhancementType = AiEnhancementType.EXPORT_HANDOUT) {
        val result = ui.result
        val session = ui.session
        if (result == null || session == null) {
            ui = ui.copy(toast = "请先生成学习包，再整理 AI 复习材料。")
            return
        }
        maybePromptMissingCloudConfig("学习材料 AI 整理")
        val courseTitle = session.title.ifBlank { "未命名课程" }
        val points = result.knowledgePoints.map {
            EnhancementPoint(it.title, it.summary, it.evidence.firstOrNull()?.quote.orEmpty())
        }
        val prompt = when (form) {
            AiEnhancementType.EXAM_CRAM_SHEET -> EnhancementPromptBuilder.examCramSheet(courseTitle, points)
            else -> EnhancementPromptBuilder.studyPackHandout(courseTitle, points)
        }
        val local: () -> String = {
            when (form) {
                AiEnhancementType.EXAM_CRAM_SHEET -> LocalEnhancementTemplates.examCramSheet(courseTitle, points)
                else -> LocalEnhancementTemplates.studyPackHandout(courseTitle, points)
            }
        }
        ui = ui.copy(studyPackEnhancement = EnhancementUiState(type = form, running = true))
        viewModelScope.launch {
            val routed = routeEnhancement(form, prompt, local)
            ui = ui.copy(studyPackEnhancement = enhancementResultState(form, routed))
        }
    }

    fun clearStudyPackEnhancement() { ui = ui.copy(studyPackEnhancement = EnhancementUiState.idle()) }

    /** P0-2: after a practice/quiz attempt, generate targeted feedback grounded in THIS attempt. */
    fun generateQuizFeedbackEnhancement() {
        val session = ui.practiceSession
        val attempts = ui.practiceAttempts
        if (session == null || attempts.isEmpty()) {
            ui = ui.copy(toast = "完成一组练习后，再生成 AI 学习反馈。")
            return
        }
        val total = attempts.size
        val correct = attempts.count { it.outcome == PracticeOutcome.CORRECT || it.outcome == PracticeOutcome.MASTERED }
        val wrongSummaries = attempts.filter { it.outcome == PracticeOutcome.WRONG }.mapNotNull { att ->
            val item = session.items.firstOrNull { it.id == att.itemId } ?: return@mapNotNull null
            val correctText = item.options.firstOrNull { it.correct }?.text.orEmpty()
            "${item.question} / 正确答案：$correctText / 知识点：${item.knowledgePointTitle}"
        }
        val weakTitles = ui.l3Pipeline.learningDiagnosis.weakKnowledgePoints.map { it.title }.ifEmpty {
            attempts.filter { it.outcome == PracticeOutcome.WRONG }
                .mapNotNull { att -> session.items.firstOrNull { it.id == att.itemId }?.knowledgePointTitle }
                .distinct()
        }
        val type = AiEnhancementType.POST_QUIZ_FEEDBACK
        val prompt = EnhancementPromptBuilder.quizFeedback(total, correct, wrongSummaries, weakTitles)
        val local: () -> String = { LocalEnhancementTemplates.postQuizFeedback(total, correct, weakTitles) }
        ui = ui.copy(quizFeedbackEnhancement = EnhancementUiState(type = type, running = true))
        viewModelScope.launch {
            val routed = routeEnhancement(type, prompt, local)
            ui = ui.copy(quizFeedbackEnhancement = enhancementResultState(type, routed))
        }
    }

    fun clearQuizFeedbackEnhancement() { ui = ui.copy(quizFeedbackEnhancement = EnhancementUiState.idle()) }

    /** P0-1: explain how the CURRENTLY-open evidence supports its knowledge point; hedge weak links. */
    fun generateEvidenceExplanation() {
        val evidenceId = ui.selectedEvidenceId ?: run {
            ui = ui.copy(toast = "请先打开一条证据。")
            return
        }
        val evidence = ui.l3Pipeline.evidence.firstOrNull { it.id == evidenceId } ?: run {
            ui = ui.copy(toast = "这条证据已不可用。")
            return
        }
        val asset = evidence.assetId?.let { id -> ui.l3Pipeline.evidenceAssets.firstOrNull { it.id == id } }
        val excerpt = evidence.text.ifBlank { asset?.text.orEmpty() }
        if (excerpt.isBlank()) {
            ui = ui.copy(toast = "这条证据暂无可解释的原文片段。")
            return
        }
        val kpTitle = ui.l3Pipeline.knowledgePoints.firstOrNull { evidenceId in it.sourceEvidenceIds }?.title.orEmpty()
        val questionStem = ui.l3Pipeline.questions.firstOrNull { evidenceId in it.evidenceIds }?.stem
        val boundContext = (
            ui.l3Pipeline.knowledgePoints.filter { evidenceId in it.sourceEvidenceIds }.joinToString(" ") { it.title } + " " +
                ui.l3Pipeline.questions.filter { evidenceId in it.evidenceIds }.joinToString(" ") { it.stem }
            ).trim()
        val weak = boundContext.isNotBlank() && evidenceRelationLevel(evidenceId, boundContext) == EvidenceRelationLevel.WEAK
        val type = AiEnhancementType.EVIDENCE_EXPLANATION
        val prompt = EnhancementPromptBuilder.evidenceExplanation(kpTitle.ifBlank { "本条证据" }, excerpt, questionStem, weak)
        val local: () -> String = { LocalEnhancementTemplates.evidenceExplanation(kpTitle.ifBlank { "本条证据" }, excerpt, weak) }
        ui = ui.copy(evidenceEnhancement = EnhancementUiState(type = type, running = true))
        viewModelScope.launch {
            val routed = routeEnhancement(type, prompt, local)
            ui = ui.copy(evidenceEnhancement = enhancementResultState(type, routed))
        }
    }

    fun clearEvidenceEnhancement() { ui = ui.copy(evidenceEnhancement = EnhancementUiState.idle()) }

    /** P0-2: AI remediation plan for the top weak knowledge point. Pairs with the gated 薄弱点专项 practice
     *  (startPractice(WEAKNESS_DRILL)) which only admits answerable questions. */
    fun generateWeaknessRemediation() {
        val weak = ui.l3Pipeline.learningDiagnosis.weakKnowledgePoints.firstOrNull() ?: run {
            ui = ui.copy(toast = "暂无薄弱知识点，继续练习后再生成加练方案。")
            return
        }
        val type = AiEnhancementType.WEAKNESS_VARIANTS
        val prompt = EnhancementPromptBuilder.weaknessRemediation(weak.title, weak.reason, weak.wrongCount)
        val local: () -> String = { LocalEnhancementTemplates.weaknessRemediation(weak.title, weak.wrongCount) }
        ui = ui.copy(weaknessEnhancement = EnhancementUiState(type = type, running = true))
        viewModelScope.launch {
            val routed = routeEnhancement(type, prompt, local)
            ui = ui.copy(weaknessEnhancement = enhancementResultState(type, routed))
        }
    }

    fun clearWeaknessRemediation() { ui = ui.copy(weaknessEnhancement = EnhancementUiState.idle()) }

    /**
     * P0-4: ask BlueLM (云端蓝心) / on-device for BRAND-NEW variant questions for the top weak knowledge
     * point, parse the strict JSON, keep only answerable questions, and enter practice. Cloud → on-device
     * → local (existing course questions for this KP, labelled 本地基础变式). Empty → no practice + error.
     */
    fun generateWeakPointVariants() {
        val result = ui.result ?: run { ui = ui.copy(toast = "请先打开一门已分析的课程。"); return }
        val weak = ui.l3Pipeline.learningDiagnosis.weakKnowledgePoints.firstOrNull() ?: run {
            ui = ui.copy(toast = "暂无薄弱知识点，继续练习后再生成变式题。")
            return
        }
        val courseId = ui.session?.id ?: result.sessionId
        val courseTitle = ui.session?.title?.ifBlank { weak.title } ?: weak.title
        val kpId = weak.knowledgePointId
        val wrongStems = ui.l3Pipeline.wrongBook
            .filter { it.knowledgePointId == kpId }
            .mapNotNull { wb -> ui.l3Pipeline.questions.firstOrNull { it.id == wb.questionId }?.stem }
            .take(3)
        val evidenceQuotes = weak.evidenceIds
            .mapNotNull { id -> ui.l3Pipeline.evidence.firstOrNull { it.id == id }?.text?.takeIf { it.isNotBlank() } }
            .take(3)
        val kpResolver: (String) -> String = { title ->
            result.knowledgePoints.firstOrNull { it.title == title }?.id ?: kpId
        }
        val prompt = EnhancementPromptBuilder.weakPointVariants(weak.title, weak.reason, wrongStems, evidenceQuotes)
        val bundle = configBundle
        val tx = transport
        val now = System.currentTimeMillis()
        val idPrefix = "var_${courseId}_$now"
        ui = ui.copy(weakVariantStatus = EnhancementUiState(type = AiEnhancementType.WEAKNESS_VARIANTS, running = true))
        viewModelScope.launch {
            val (items, source) = withContext(Dispatchers.IO) {
                val cloud = runCatching { ProviderAskChatClient(bundle, tx, timeouts = HttpTimeouts.BLUE_LM_DIAGNOSTIC).chat(prompt, null) }.getOrNull()
                val cloudItems = cloud?.text?.let { VariantQuizParser.parse(it, AiExecutionSource.CLOUD, idPrefix, kpResolver) }.orEmpty()
                if (cloudItems.isNotEmpty()) return@withContext cloudItems to AiExecutionSource.CLOUD
                val device = runCatching { onDeviceController.askSeam().chat(prompt, null) }.getOrNull()
                val deviceItems = device?.text?.let { VariantQuizParser.parse(it, AiExecutionSource.ON_DEVICE, idPrefix, kpResolver) }.orEmpty()
                if (deviceItems.isNotEmpty()) return@withContext deviceItems to AiExecutionSource.ON_DEVICE
                // Honest local fallback: reuse THIS course's existing questions for the weak KP (no fabrication).
                existingQuestionsAsVariants(kpId, idPrefix) to AiExecutionSource.SAFE_PLACEHOLDER
            }
            if (items.isEmpty()) {
                ui = ui.copy(
                    weakVariantStatus = EnhancementUiState(
                        type = AiEnhancementType.WEAKNESS_VARIANTS,
                        running = false,
                        failed = true,
                        sourceZh = "暂无可用变式题",
                    ),
                )
                return@launch
            }
            val sourceZh = when (source) {
                AiExecutionSource.CLOUD -> "蓝心生成变式题"
                AiExecutionSource.ON_DEVICE -> "端侧模型草稿"
                else -> "本地基础变式"
            }
            val session = PracticeSession(
                id = "variant_$now",
                courseSessionId = courseId,
                courseTitle = courseTitle,
                mode = PracticeMode.WEAKNESS_DRILL,
                items = items,
                createdAt = now,
                source = source,
                routeReason = "weak-point variants · $sourceZh",
            )
            ui = ui.copy(
                weakVariantStatus = EnhancementUiState.idle(),
                practiceSession = session,
                practiceIndex = 0,
                practiceAttempts = emptyList(),
                practiceResult = null,
                practiceSelectedAnswers = emptyMap(),
                practiceSubmittedAnswers = emptyMap(),
                practiceQuestionMode = PracticeQuestionMode.REAL_QUIZ,
                practiceStartedAt = now,
                practiceRevealed = false,
                toast = "已生成 ${items.size} 道变式题（$sourceZh）。",
            )
            navigateTo(Screen.PRACTICE)
        }
    }

    /** Local fallback for variants: this course's existing answerable questions for the weak KP. */
    private fun existingQuestionsAsVariants(knowledgePointId: String, idPrefix: String): List<PracticeItem> {
        val kp = ui.result?.knowledgePoints?.firstOrNull { it.id == knowledgePointId }
        return ui.l3Pipeline.questions
            .filter { it.knowledgePointId == knowledgePointId }
            .mapIndexed { index, q ->
                val quizOptions = q.options.mapIndexed { i, text ->
                    com.classmate.core.model.QuizOption(id = ('A' + i).toString(), text = text, isCorrect = false)
                }
                val correctIds = com.classmate.core.model.QuizAnswerNormalizer
                    .resolveCorrectIds(quizOptions, q.correctAnswer).toSet()
                PracticeItem(
                    id = "${idPrefix}_local_$index",
                    type = PracticeItemType.QUIZ_RETRY,
                    knowledgePointId = knowledgePointId,
                    knowledgePointTitle = kp?.title.orEmpty(),
                    question = q.stem,
                    answer = q.explanation.ifBlank { kp?.summary.orEmpty() },
                    options = quizOptions.map { PracticeOption(it.id, it.text, it.id in correctIds) },
                    source = AiExecutionSource.SAFE_PLACEHOLDER,
                )
            }
            .filter { it.isAnswerableQuiz() }
    }

    fun clearWeakVariantStatus() { ui = ui.copy(weakVariantStatus = EnhancementUiState.idle()) }

    fun startRandomQuiz(questionCount: Int = 5, now: Long = System.currentTimeMillis()) {
        startPracticeInternal(PracticeMode.QUICK_REVIEW, PracticeQuestionMode.REAL_QUIZ)
        val session = ui.practiceSession ?: return
        // Only questions that actually have a correct answer may enter the random quiz (shared gate).
        val answerable = session.items.filter { it.isAnswerableQuiz() && it.quizId !in ui.flaggedQuestionIds }
        if (answerable.isEmpty()) {
            ui = ui.copy(
                practiceSession = session.copy(items = emptyList()),
                toast = "暂时没有带正确答案的题目，请先生成微测题或重新分析课程。",
            )
            return
        }
        val picked = answerable.shuffled(Random(now)).take(questionCount.coerceIn(1, 10))
        ui = ui.copy(
            practiceSession = session.copy(items = picked, routeReason = "random quiz from current lesson/question bank"),
            practiceIndex = 0,
            practiceSelectedAnswers = emptyMap(),
            practiceSubmittedAnswers = emptyMap(),
            practiceAttempts = emptyList(),
            practiceResult = null,
            toast = "已生成 ${picked.size} 题随机小测。",
        )
    }

    fun retryWrongQuestion(wrongRecordId: String, now: Long = System.currentTimeMillis()): Boolean {
        val wrong = ui.l3Pipeline.wrongBook.firstOrNull { it.id == wrongRecordId } ?: run {
            ui = ui.copy(toast = "没有找到这条错题记录。")
            return false
        }
        val question = ui.l3Pipeline.questions.firstOrNull { it.id == wrong.questionId } ?: run {
            ui = ui.copy(toast = "错题原题已不可用，先回到课程重新生成练习。")
            return false
        }
        val source = ui.l3Pipeline.lessonSource
        val courseSessionId = ui.session?.id ?: source?.id ?: question.lessonId
        val courseTitle = ui.session?.title ?: source?.title ?: "错题重练"
        val kpTitle = ui.l3Pipeline.knowledgePoints.firstOrNull { it.id == question.knowledgePointId }?.title
            ?: question.knowledgePointId
        val item = PracticeItem(
            id = "retry_${wrong.id}_$now",
            type = PracticeItemType.QUIZ_RETRY,
            knowledgePointId = question.knowledgePointId,
            knowledgePointTitle = kpTitle,
            taskId = wrong.id,
            question = question.stem,
            answer = question.explanation,
            evidenceQuote = ui.l3Pipeline.evidence.firstOrNull { it.id in wrong.evidenceIds }?.text,
            quizId = question.id,
            options = question.options.mapIndexed { index, option ->
                // Position-based id (A/B/C/D): unique, so a single-choice tap selects exactly one option.
                PracticeOption(
                    id = QuizOptionIds.letterId(index),
                    text = QuizOptionIds.cleanText(option),
                    correct = QuizOptionIds.isAnswer(index, option, question.correctAnswer),
                )
            },
            whyThisQuestionMatters = wrong.mistakeReason.ifBlank { "这道错题会影响关联知识点掌握度。" },
        )
        ui = ui.copy(
            practiceSession = PracticeSession(
                id = "wrong_retry_$now",
                courseSessionId = courseSessionId,
                courseTitle = courseTitle,
                mode = PracticeMode.WRONG_ANSWER_RETRY,
                items = listOf(item),
                createdAt = now,
                source = AiExecutionSource.SAFE_PLACEHOLDER,
                routeReason = "single wrong question retry from WrongBook",
            ),
            practiceIndex = 0,
            practiceAttempts = emptyList(),
            practiceResult = null,
            practiceStartedAt = now,
            practiceRevealed = false,
            practiceQuestionMode = PracticeQuestionMode.REAL_QUIZ,
            practiceSelectedAnswers = emptyMap(),
            practiceSubmittedAnswers = emptyMap(),
            examSession = null,
            toast = "已进入这道错题的重练。",
        )
        navigateTo(Screen.PRACTICE)
        return true
    }

    private fun startPracticeInternal(mode: PracticeMode, questionMode: PracticeQuestionMode) {
        var result = ui.result
        var session = ui.session
        if (result == null || session == null) {
            val task = learningStore.listDueTasks().firstOrNull() ?: ui.learningSnapshot.tasks.firstOrNull { !it.manuallyRemoved }
            val record = task?.let { t -> ui.history.firstOrNull { it.session.id == t.courseSessionId } }
            if (record != null) { loadHistoryRecord(record); result = record.result; session = record.session }
        }
        val r = result
        val s = session
        if (r == null || s == null) {
            ui = ui.copy(toast = "请先打开一门已分析的课程再开始练习。")
            return
        }
        maybePromptMissingCloudConfig("Practice generation")
        val now = System.currentTimeMillis()
        ui = ui.copy(
            aiProcessing = AiProcessingUiState(
                visible = true,
                title = "正在基于课程证据出题",
                steps = listOf("查找证据", "生成练习题", "整理题目", "等待作答"),
                activeStep = 2,
                source = "云端优先 / 端侧兜底",
                fallbackMessage = "模型不可用时会使用本节课证据生成可编辑练习。",
                canCancel = true,
                canRetry = true,
                canContinueManual = true,
            ),
        )
        internalTools.execute(
            InternalToolCall(
                name = InternalToolName.CREATE_PRACTICE,
                courseTitle = s.title,
                now = now,
            ),
            r,
            ui.learningSnapshot,
        )
        val generated = practiceGeneration.generate(
            PracticeGenerationRequest(
                result = r,
                snapshot = ui.learningSnapshot,
                mode = mode,
                now = now,
                courseTitle = s.title,
            ),
        )
        val generatedPractice = generated.value?.session ?: PracticeSessionEngine.build(r, ui.learningSnapshot, mode, now, courseTitle = s.title)
        val practice = when (questionMode) {
            // Graded quizzes (real quiz / exam) only keep questions that actually have a correct answer
            // — see PracticeItem.isAnswerableQuiz(). Self-assessment cards have no graded answer.
            PracticeQuestionMode.REAL_QUIZ, PracticeQuestionMode.EXAM -> {
                // P0-3: flagged-as-wrong questions are excluded from graded practice.
                val quizItems = generatedPractice.items.filter { it.isAnswerableQuiz() && it.quizId !in ui.flaggedQuestionIds }
                generatedPractice.copy(items = quizItems)
            }
            PracticeQuestionMode.SELF_ASSESSMENT -> generatedPractice
        }
        if (practice.items.isEmpty()) {
            val message = if (questionMode == PracticeQuestionMode.SELF_ASSESSMENT) {
                "暂时没有可复盘的内容。"
            } else {
                "暂时没有可答题的题目，请先生成微测题或导入题库。"
            }
            ui = ui.copy(toast = message, aiProcessing = AiProcessingUiState.hidden())
            return
        }
        val exam = if (questionMode == PracticeQuestionMode.EXAM) {
            ExamSession(
                id = "exam_$now",
                sourceLessonId = s.id,
                questionBankId = ui.l3Pipeline.questionBank?.id,
                questionIds = practice.items.mapNotNull { it.quizId ?: it.id },
                startedAt = now,
                status = ExamStatus.IN_PROGRESS,
            )
        } else {
            null
        }
        ui = ui.copy(
            practiceSession = practice,
            practiceIndex = 0,
            practiceAttempts = emptyList(),
            practiceResult = null,
            practiceStartedAt = now,
            practiceRevealed = false,
            practiceQuestionMode = questionMode,
            practiceSelectedAnswers = emptyMap(),
            practiceSubmittedAnswers = emptyMap(),
            examSession = exam,
            aiProcessing = AiProcessingUiState.hidden(),
        )
        navigateTo(Screen.PRACTICE)
    }

    /** Start practice focused on a review task's course, picking a mode from its counters. */
    fun startPracticeForTask(task: ReviewTask) {
        if (ui.result?.sessionId != task.courseSessionId) {
            val record = ui.history.firstOrNull { it.session.id == task.courseSessionId }
            if (record == null) {
                ui = ui.copy(toast = "该课程不在历史记录中，无法练习。")
                return
            }
            loadHistoryRecord(record)
        }
        val mode = when {
            task.counters.wrongAnswer > 0 -> PracticeMode.WRONG_ANSWER_RETRY
            task.counters.needExample > 0 -> PracticeMode.NEED_MORE_PRACTICE
            task.counters.tooHard > 0 -> PracticeMode.WEAKNESS_DRILL
            else -> PracticeMode.QUICK_REVIEW
        }
        startPractice(mode)
    }

    fun revealPracticeAnswer() { ui = ui.copy(practiceRevealed = true) }

    fun selectPracticeAnswer(optionId: String) {
        val session = ui.practiceSession ?: return
        val item = session.items.getOrNull(ui.practiceIndex) ?: return
        if (item.id in ui.practiceSubmittedAnswers) return
        val correctCount = item.correctOptionIds.size
        val previous = ui.practiceSelectedAnswers[item.id].orEmpty()
        val next = if (correctCount > 1) {
            if (optionId in previous) previous - optionId else previous + optionId
        } else {
            setOf(optionId)
        }
        ui = ui.copy(practiceSelectedAnswers = ui.practiceSelectedAnswers + (item.id to next))
    }

    fun submitPracticeAnswer(now: Long = System.currentTimeMillis()): Boolean {
        val session = ui.practiceSession ?: return false
        val item = session.items.getOrNull(ui.practiceIndex) ?: return false
        if (item.id in ui.practiceSubmittedAnswers) return true
        val selected = ui.practiceSelectedAnswers[item.id].orEmpty().toList().sorted()
        if (selected.isEmpty()) {
            ui = ui.copy(toast = "请先选择一个答案。")
            return false
        }
        val grade = PracticeGradingEngine.grade(item, selected)
        val correctAnswers = grade.correctAnswers
        val correct = grade.correct
        val mode = ui.practiceQuestionMode
        val elapsedMs = (now - ui.practiceStartedAt).coerceAtLeast(0L)
        val outcome = if (correct) PracticeOutcome.CORRECT else PracticeOutcome.WRONG
        val feedback = PracticeFeedbackEngine.evaluateSelfReported(item, outcome)
        val attempt = PracticeAttempt(
            itemId = item.id,
            knowledgePointId = item.knowledgePointId,
            taskId = item.taskId,
            outcome = outcome,
            submittedAnswer = selected.joinToString(","),
            feedback = feedback,
        )
        val quizId = item.quizId
        val submission = PracticeAnswerSubmission(
            itemId = item.id,
            questionId = quizId ?: item.id,
            selectedAnswers = selected,
            correct = correct,
            submittedAt = now,
            elapsedMs = elapsedMs,
            mode = mode,
            state = if (correct) PracticeAnswerState.SUBMITTED_CORRECT else PracticeAnswerState.SUBMITTED_WRONG,
        )
        val updatedL3 = quizId?.let { questionId ->
            l3Pipeline.submitAnswer(
                snapshot = ui.l3Pipeline,
                questionId = questionId,
                userAnswer = selected.joinToString(","),
                now = now,
                selectedAnswers = selected,
                elapsedMs = elapsedMs,
                mode = mode,
            )
        } ?: ui.l3Pipeline
        if (quizId != null && item.knowledgePointId.isNotBlank()) {
            learningStore.recordQuizAttempt(
                courseSessionId = session.courseSessionId,
                knowledgePointId = item.knowledgePointId,
                quizId = quizId,
                selectedAnswer = selected.joinToString(","),
                correctAnswer = correctAnswers.joinToString(","),
                isCorrect = correct,
            )
        }
        ui = ui.copy(
            practiceAttempts = ui.practiceAttempts + attempt,
            practiceSubmittedAnswers = ui.practiceSubmittedAnswers + (item.id to submission),
            l3Pipeline = updatedL3,
            learningSnapshot = learningStore.snapshot(),
            toast = if (correct) "回答正确，已更新掌握度。" else "回答错误，已加入错题本和今日复习。",
        )
        persistL3(updatedL3)
        return true
    }

    fun nextPracticeQuestion() {
        val session = ui.practiceSession ?: return
        val item = session.items.getOrNull(ui.practiceIndex) ?: return
        if (ui.practiceQuestionMode != PracticeQuestionMode.SELF_ASSESSMENT && item.id !in ui.practiceSubmittedAnswers) {
            ui = ui.copy(toast = "请先提交当前题。")
            return
        }
        val nextIndex = ui.practiceIndex + 1
        if (nextIndex >= session.items.size) {
            finishPractice()
        } else {
            ui = ui.copy(practiceIndex = nextIndex, practiceRevealed = false)
        }
    }

    fun answerPractice(outcome: PracticeOutcome) {
        if (ui.practiceQuestionMode != PracticeQuestionMode.SELF_ASSESSMENT) {
            ui = ui.copy(toast = "专项练习请先选择答案并提交；自评请进入回忆复盘。")
            return
        }
        val session = ui.practiceSession ?: return
        val item = session.items.getOrNull(ui.practiceIndex) ?: return
        val feedback = PracticeFeedbackEngine.evaluateSelfReported(item, outcome)
        val attempts = ui.practiceAttempts + PracticeAttempt(
            itemId = item.id,
            knowledgePointId = item.knowledgePointId,
            taskId = item.taskId,
            outcome = outcome,
            feedback = feedback,
        )
        val nextIndex = ui.practiceIndex + 1
        ui = ui.copy(practiceAttempts = attempts, practiceIndex = nextIndex, practiceRevealed = false)
        if (nextIndex >= session.items.size) finishPractice()
    }

    private fun finishPractice() {
        val session = ui.practiceSession ?: return
        val now = System.currentTimeMillis()
        val result = PracticeSessionEngine.summarize(session, ui.practiceAttempts, now - ui.practiceStartedAt)
        // Reuse the existing ReviewEngine rules: correct lowers priority, wrong raises + re-queues,
        // mastered defers, need-more-practice keeps it due. No schema or provider changes.
        val after = if (ui.practiceQuestionMode == PracticeQuestionMode.SELF_ASSESSMENT) {
            PracticeSessionEngine.writeBack(learningStore.snapshot(), session.courseSessionId, ui.practiceAttempts, now)
        } else {
            learningStore.snapshot()
        }
        val record = PracticeHistoryRecord(
            id = "ph_$now",
            courseSessionId = session.courseSessionId,
            courseTitle = session.courseTitle,
            createdAt = now,
            mode = session.mode.name,
            itemCount = result.itemCount,
            correctCount = result.correctCount,
            wrongCount = result.wrongCount,
            masteredCount = result.masteredCount,
            needMorePracticeCount = result.needMorePracticeCount,
            relatedKnowledgePointTitles = result.relatedKnowledgePointTitles.take(10),
        )
        learningStore.recordPracticeSession(after, record)
        val submittedExam = ui.examSession?.takeIf { ui.practiceQuestionMode == PracticeQuestionMode.EXAM }?.copy(
            submittedAt = now,
            status = ExamStatus.SUBMITTED,
            score = if (result.itemCount == 0) 0 else (result.correctCount * 100 / result.itemCount),
            correctCount = result.correctCount,
            wrongCount = result.wrongCount,
        )
        val examReport = submittedExam?.let { ExamReportEngine.build(it, ui.l3Pipeline, ui.practiceSubmittedAnswers) }
        val nextL3 = if (examReport != null) {
            ui.l3Pipeline.copy(examReports = (ui.l3Pipeline.examReports + examReport).takeLast(10))
        } else {
            ui.l3Pipeline
        }
        if (examReport != null) {
            ui = ui.copy(l3Pipeline = nextL3)
            persistL3(nextL3)
        }
        ui = ui.copy(
            practiceResult = result,
            learningSnapshot = learningStore.snapshot(),
            examSession = submittedExam ?: ui.examSession,
            toast = if (ui.practiceQuestionMode == PracticeQuestionMode.EXAM) "模拟考试已提交。" else "本轮练习完成。",
        )
    }

    fun currentPracticeItem() = ui.practiceSession?.items?.getOrNull(ui.practiceIndex)
    fun isPracticeComplete(): Boolean = ui.practiceResult != null

    fun exitPractice() {
        ui = ui.copy(
            practiceSession = null,
            practiceIndex = 0,
            practiceAttempts = emptyList(),
            practiceResult = null,
            practiceRevealed = false,
            practiceQuestionMode = PracticeQuestionMode.REAL_QUIZ,
            practiceSelectedAnswers = emptyMap(),
            practiceSubmittedAnswers = emptyMap(),
            examSession = null,
        )
        goBack()
    }

    fun practiceHistoryForCourse(courseSessionId: String): List<PracticeHistoryRecord> =
        ui.learningSnapshot.practiceHistory.filter { it.courseSessionId == courseSessionId }.takeLast(5).reversed()

    fun updateAskLessonQuestion(value: String) {
        ui = ui.copy(askLessonQuestion = value)
    }

    fun askThisLesson() {
        val question = ui.askLessonQuestion.trim()
        val session = ui.session
        val result = ui.result
        if (question.isBlank() || session == null || result == null) {
            ui = ui.copy(toast = "请先打开已分析的课程并输入问题。")
            return
        }
        maybePromptMissingCloudConfig("Ask 云端回答")
        // Grounded QA routes through the SAME profile order as the analyzer (BlueLM/Compatible/Local).
        // The network call runs off the main thread; with no real credentials the seam returns null
        // and the engine answers from local evidence — never inventing course-external facts.
        val bundle = configBundle
        val tx = transport
        ui = ui.copy(
            askLessonPending = true,
            askLessonQuestion = "",
            aiProcessing = AiProcessingUiState(
                visible = true,
                title = "正在基于证据回答",
                steps = listOf("检索本节课证据", "云端处理中", "端侧兜底", "整理答案与追问"),
                activeStep = 1,
                source = "云端优先 / 端侧兜底",
                fallbackMessage = "没有证据时会温和提示，不编造课程外内容。",
                canCancel = true,
                canRetry = true,
                canContinueManual = false,
            ),
        )
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                // Honest order: cloud BlueLM → on-device BlueLM 3B → (engine LocalRule evidence).
                // The engine still enforces anti-fabrication on whichever seam answers.
                val seam = CompositeAskChatSeam(
                    listOf(ProviderAskChatClient(bundle, tx), onDeviceController.askSeam()),
                )
                GroundedAskLessonEngine.answer(question, session, result, seam)
            }
            // Only safe, enum/count telemetry is ever surfaced — never the prompt, body, or answer text.
            // Honest source: cloud 云端蓝心 / on-device 端侧蓝心 / 安全占位 (no real model answered).
            val answer = outcome.answer
            // P0: route the Ask answer's source through the unified AiExecutionSource vocabulary (CLOUD /
            // ON_DEVICE / SAFE_PLACEHOLDER). GroundedAskLessonEngine still enforces evidence/citation rules.
            val servedByModel = com.classmate.core.ai.AskRouting.servedByModel(answer.providerName)
            val sourceZh = com.classmate.core.ai.AskRouting.sourceOf(answer.providerName).displayZh
            ui = ui.copy(
                askLessonAnswers = ui.askLessonAnswers + answer,
                askLessonPending = false,
                aiProcessing = AiProcessingUiState.hidden(),
                toast = when {
                    !servedByModel ->
                        "当前云端与端侧模型均不可用，已用安全占位并附本节课证据；请检查网络、模型目录授权或稍后重试。"
                    answer.groundedness == "not_found" -> "本节课资料中没有找到明确依据。（$sourceZh）"
                    answer.groundedness == "grounded" -> "已根据本节课证据回答。（$sourceZh）"
                    else -> "已根据部分证据回答。（$sourceZh）"
                },
            )
        }
    }

    fun addAskAnswerToReview(answerIndex: Int) {
        val answer = ui.askLessonAnswers.getOrNull(answerIndex) ?: return
        val result = ui.result ?: return
        val session = ui.session ?: return
        val kp = answer.relatedKnowledgePointTitles.firstNotNullOfOrNull { title ->
            result.knowledgePoints.firstOrNull { it.title == title }
        } ?: answer.evidenceRefs.firstNotNullOfOrNull { ref ->
            ref.knowledgePointTitle?.let { title -> result.knowledgePoints.firstOrNull { it.title == title } }
        } ?: result.knowledgePoints.firstOrNull()

        if (kp == null) {
            ui = ui.copy(toast = "这条问答没有可加入复习的知识点。")
            return
        }

        internalTools.execute(
            InternalToolCall(
                name = InternalToolName.CREATE_REVIEW_TASK,
                courseTitle = session.title,
                knowledgePointId = kp.id,
                now = System.currentTimeMillis(),
            ),
            result,
            ui.learningSnapshot,
        )
        learningStore.addManualTask(
            courseSessionId = result.sessionId,
            courseTitle = session.title.ifBlank { "未命名课程" },
            knowledgePointId = kp.id,
            title = "问答复习：${kp.title}",
            difficultyName = kp.difficulty.name,
            sourceProvider = "ASK",
            sourceProfile = ui.providerConfigSummary.profileLabel,
            sourceModel = answer.modelName.orEmpty(),
        )
        syncLearning("已把这条问答加入复习：${kp.title}")
    }

    private fun HistoryRecord.toCourseRecordSnapshot(): CourseRecordSnapshot =
        CourseRecordSnapshot(
            id = id,
            title = title,
            createdAtEpochMs = createdAtEpochMs,
            providerName = providerName,
            profileLabel = profileLabel,
            knowledgePointCount = knowledgePointCount,
            quizCount = quizCount,
            fallbackUsed = fallbackUsed,
        )

    /** Add a manual review task from a knowledge point of the current analysis. */
    fun reviewAddManual(knowledgePointId: String) {
        val result = ui.result ?: return
        val session = ui.session ?: return
        val kp = result.knowledgePoint(knowledgePointId) ?: return
        learningStore.addManualTask(
            courseSessionId = result.sessionId,
            courseTitle = session.title.ifBlank { "未命名课程" },
            knowledgePointId = kp.id,
            title = kp.title,
            difficultyName = kp.difficulty.name,
            sourceProvider = "MANUAL",
            sourceProfile = ui.providerConfigSummary.profileLabel,
            sourceModel = "",
        )
        syncLearning("已加入复习：${kp.title}")
    }

    /** Open the course behind a review task (loads it from history if needed) at its timeline. */
    fun openTaskCourse(task: ReviewTask) {
        if (ui.result?.sessionId == task.courseSessionId && ui.session != null) {
            navigateTo(Screen.COURSE_DETAIL)
            return
        }
        val record = ui.history.firstOrNull { it.session.id == task.courseSessionId }
        if (record == null) {
            ui = ui.copy(toast = "该课程不在历史记录中，无法打开")
            return
        }
        openHistory(record)
    }

    // --- review (legacy single-session plan; kept for compatibility, unused by the new Review tab) ---
    fun ensureReviewPlan() {
        val result = ui.result ?: return
        if (ui.reviewPlan != null) return
        val now = System.currentTimeMillis()
        val learning = ui.learningState ?: LearningState.seed(result.sessionId, result.knowledgePoints, now)
        val plan = ReviewPlanner().plan(result, learning, ui.feedbackEvents)
        ui = ui.copy(reviewPlan = plan, learningState = learning)
    }

    fun regenerateReviewPlan() {
        ui = ui.copy(reviewPlan = null)
        ensureReviewPlan()
    }

    fun toast(message: String) { ui = ui.copy(toast = message) }
    fun consumeToast() { ui = ui.copy(toast = null) }

    /** Provenance provider name for history/learning, marking on-device analysis as ONDEVICE_BLUELM. */
    private fun analysisProviderName(result: CourseAnalysisResult): String =
        if (OnDeviceCourseAnalysis.isOnDevice(result.provenance)) "ONDEVICE_BLUELM"
        else result.provenance.provider.name

    private fun analysisModelLabel(result: CourseAnalysisResult): String =
        if (OnDeviceCourseAnalysis.isOnDevice(result.provenance)) "BlueLM 3B (端侧蓝心)"
        else configBundle.configOf(result.provenance.provider)?.model.orEmpty()

    // --- history ---
    private fun buildHistoryRecord(session: CourseSession, outcome: AnalysisOutcome.Success, now: Long): HistoryRecord {
        val result = outcome.result
        return HistoryRecord(
            id = "hist_$now",
            title = session.title.ifBlank { "未命名课程" },
            createdAtEpochMs = now,
            providerName = analysisProviderName(result),
            profileLabel = ui.providerConfigSummary.profileLabel,
            model = analysisModelLabel(result),
            knowledgePointCount = result.knowledgePoints.size,
            quizCount = result.quizQuestions.size,
            fallbackUsed = result.provenance.fallbackUsed,
            validationStatus = if (outcome.report.ok) "PASS" else "FAIL",
            session = session,
            result = result,
        )
    }

    private fun persistHistory(records: List<HistoryRecord>) {
        // Best-effort, synchronous: the file is tiny and FileHistoryStore swallows I/O errors.
        // Synchronous keeps the ViewModel unit-testable (no Main dispatcher required).
        historyStore.save(records)
    }

    /** Re-open a saved analysis into the knowledge timeline. Deleting history never touches config. */
    fun openHistory(record: HistoryRecord) {
        loadHistoryRecord(record)
        navigateTo(Screen.COURSE_DETAIL)
    }

    fun openHistoryTimeline(record: HistoryRecord) {
        loadHistoryRecord(record)
        navigateTo(Screen.KNOWLEDGE)
    }

    private fun loadHistoryRecord(record: HistoryRecord) {
        val now = System.currentTimeMillis()
        val l3Snapshot = l3Pipeline.buildFromAnalysis(
            session = record.session,
            result = record.result,
            sourceType = L3SourceType.TEXT,
            providerSummary = ui.providerConfigSummary,
            now = now,
        )
        ui = ui.copy(
            selectedCourseKey = CourseLibraryBuilder.normalizeCourseName(record.title).lowercase(),
            session = record.session,
            result = record.result,
            l3Pipeline = l3Snapshot,
            learningState = LearningState.seed(record.result.sessionId, record.result.knowledgePoints, now),
            reviewPlan = null,
            answers = emptyMap(),
            revealedQuestionIds = emptySet(),
            currentQuestionIndex = 0,
            feedbackEvents = emptyList(),
        )
    }

    fun deleteCourse(courseKey: String): Boolean {
        val records = recordsForCourse(courseKey)
        if (records.isEmpty()) {
            ui = ui.copy(toast = "课程不存在或已删除。")
            return false
        }
        return deleteHistoryRecords(records, "已删除课程及相关学习记录。")
    }

    fun deleteHistory(id: String): Boolean {
        val record = ui.history.firstOrNull { it.id == id }
        if (record == null) {
            ui = ui.copy(toast = "课程不存在或已删除。")
            return false
        }
        return deleteHistoryRecords(listOf(record), "已删除该历史记录及相关复习数据。")
    }

    fun deleteCurrentCourse(): Boolean {
        val activeId = ui.session?.id
        val record = activeId?.let { id -> ui.history.firstOrNull { it.session.id == id } }
        if (record != null) return deleteHistory(record.id)
        if (ui.session == null && ui.l3Pipeline.lessonSource == null) {
            ui = ui.copy(toast = "当前没有可删除的课程。")
            return false
        }
        ui = ui.copy(
            selectedCourseKey = null,
            session = null,
            result = null,
            l3Pipeline = L3PipelineSnapshot.Empty,
            learningState = null,
            reviewPlan = null,
            answers = emptyMap(),
            revealedQuestionIds = emptySet(),
            selectedKnowledgePointId = null,
            selectedEvidenceId = null,
            toast = "已删除当前未保存课程。",
        )
        l3PersistenceRepository.saveSnapshot(L3PipelineSnapshot.Empty)
        selectTab(Tab.HISTORY)
        return true
    }

    private fun deleteHistoryRecords(records: List<HistoryRecord>, successToast: String): Boolean {
        if (records.isEmpty()) return false
        val recordIds = records.map { it.id }.toSet()
        val sessionIds = records.map { it.session.id }.toSet()
        val courseTitles = records.map { it.title }.toSet()
        val courseKeys = courseTitles.map { CourseLibraryBuilder.normalizeCourseName(it).lowercase() }.toSet()
        val updated = ui.history.filterNot { it.id in recordIds }
        val activeDeleted = ui.session?.id in sessionIds || ui.selectedCourseKey in courseKeys
        val recordingRecordsToDelete = ui.recordingRecords.filter { it.belongsToDeletedCourse(sessionIds, courseKeys) }

        val privateEvidenceRefs = if (activeDeleted) ui.l3Pipeline.privateEvidenceRefs() else emptySet()
        val evidenceCleanup = evidenceAssetStore.deleteRefs(privateEvidenceRefs)
        val evidenceSessionCleanup = sessionIds
            .map { evidenceAssetStore.deleteForSession(it) }
            .firstOrNull { !it.success }
        val exportCleanup = exportStore.deleteForCourse(courseTitles, sessionIds)
        val recordingCleanup = recordingFileManager.deleteForRecords(recordingRecordsToDelete)
        val persistenceCleanupOk = l3PersistenceRepository.clearIfMatches(sessionIds, courseTitles)

        if (!evidenceCleanup.success || evidenceSessionCleanup != null || !exportCleanup.success || !recordingCleanup.success || !persistenceCleanupOk) {
            ui = ui.copy(toast = "删除课程失败，已保留现有学习记录，请稍后重试。")
            return false
        }

        learningStore.deleteCourseSessions(sessionIds)
        val nextL3 = if (activeDeleted) L3PipelineSnapshot.Empty else ui.l3Pipeline
        if (activeDeleted) l3PersistenceRepository.saveSnapshot(nextL3)
        ui = ui.copy(
            history = updated,
            learningSnapshot = learningStore.snapshot(),
            selectedCourseKey = if (activeDeleted) null else ui.selectedCourseKey,
            session = if (activeDeleted) null else ui.session,
            result = if (activeDeleted) null else ui.result,
            l3Pipeline = nextL3,
            learningState = if (activeDeleted) null else ui.learningState,
            reviewPlan = if (activeDeleted) null else ui.reviewPlan,
            answers = if (activeDeleted) emptyMap() else ui.answers,
            revealedQuestionIds = if (activeDeleted) emptySet() else ui.revealedQuestionIds,
            selectedKnowledgePointId = if (activeDeleted) null else ui.selectedKnowledgePointId,
            selectedEvidenceId = if (activeDeleted) null else ui.selectedEvidenceId,
            inputArtifacts = if (activeDeleted) emptyList() else ui.inputArtifacts,
            importReports = if (activeDeleted) emptyList() else ui.importReports,
            pdfDocuments = if (activeDeleted) emptyList() else ui.pdfDocuments,
            pdfPages = if (activeDeleted) emptyList() else ui.pdfPages,
            asrLongJobs = if (activeDeleted) emptyList() else ui.asrLongJobs,
            recordingRecords = ui.recordingRecords.filterNot { it in recordingRecordsToDelete },
            currentRecording = ui.currentRecording?.takeUnless { current -> recordingRecordsToDelete.any { it.id == current.id } },
            toast = successToast,
        )
        persistHistory(updated)
        if (activeDeleted) {
            // P0-2: clean the current course's generated TTS audio file so it doesn't orphan on disk.
            deleteTtsAudioFileOnly()
            ui = ui.copy(ttsAudio = TtsAudioUiState())
        }
        if (activeDeleted && currentScreen == Screen.COURSE_DETAIL) selectTab(Tab.HISTORY)
        return true
    }

    private fun L3PipelineSnapshot.privateEvidenceRefs(): Set<String> =
        buildSet {
            evidenceAssets.forEach { asset ->
                listOf(asset.localUri, asset.thumbnailRef, asset.imageRef, asset.audioRef)
                    .filterTo(this) { it.isNotBlank() }
            }
            evidence.forEach { ev ->
                listOf(ev.localUri, ev.thumbnailRef, ev.imageRef, ev.audioRef)
                    .filterTo(this) { it.isNotBlank() }
            }
        }

    private fun ClassroomRecordingRecord.belongsToDeletedCourse(sessionIds: Set<String>, courseKeys: Set<String>): Boolean {
        val titleKey = CourseLibraryBuilder.normalizeCourseName(title).lowercase()
        if (titleKey in courseKeys) return true
        val haystack = listOf(id, artifactFileName.orEmpty(), artifactPath.orEmpty()).joinToString(" ").lowercase()
        return sessionIds.any { it.isNotBlank() && haystack.contains(it.lowercase()) }
    }

    /** Official SDK requirement: release native resources when the owner is destroyed. */
    override fun onCleared() {
        runCatching { localTtsPlayer.release() }
        runCatching { onDeviceController.release() }
        // P1-2: release the Flow background-music player so playback never outlives the app.
        runCatching { flowAudioController.release() }
        super.onCleared()
    }

    private companion object {
        const val TOTAL_STAGES = 6
        const val STAGE_DELAY_MS = 430L
        const val MAX_HISTORY = 50

        /** Small built-in lesson text for the Settings "端侧独立模式检查" (no cloud config needed). */
        const val OFFLINE_CHECK_TEXT =
            "光合作用是绿色植物利用光能，把二氧化碳和水转化为有机物并释放氧气的过程。" +
                "光反应发生在类囊体薄膜上，将光能转化为 ATP 和 NADPH。" +
                "暗反应发生在叶绿体基质中，利用 ATP 和 NADPH 把二氧化碳固定为葡萄糖。" +
                "影响光合作用的主要因素包括光照强度、二氧化碳浓度和温度。"
    }
}
