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
import com.classmate.app.asr.AsrTranscriptMapper
import com.classmate.app.capture.CaptureGateway
import com.classmate.app.capture.CaptureGatewayPort
import com.classmate.app.data.BlueLMHttpTransport
import com.classmate.app.data.ExportActionStatus
import com.classmate.app.data.ExportReceipt
import com.classmate.app.data.ExportStore
import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.HistoryStore
import com.classmate.app.data.InMemoryExportStore
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.data.ThemePreferenceRepository
import com.classmate.app.exporting.ExportArtifact
import com.classmate.app.exporting.ExportCenter
import com.classmate.app.exporting.ExportFileFormat
import com.classmate.core.audio.ConfigMissingTtsProvider
import com.classmate.core.audio.CourseEssenceAudioExporter
import com.classmate.core.exporting.StudyReport
import com.classmate.core.exporting.StudyReportBuilder
import com.classmate.core.exporting.StudyReportRenderer
import com.classmate.app.glossary.CourseGlossary
import com.classmate.app.importing.OcrImportAssembler
import com.classmate.app.importing.OcrImportDraft
import com.classmate.app.importing.OcrImportFileMeta
import com.classmate.app.importing.OcrImportKind
import com.classmate.app.importing.SelectedLocalFileMetadata
import com.classmate.app.l3.ClassroomAudioRecorder
import com.classmate.app.l3.ClassroomRecordingRecord
import com.classmate.app.l3.L3AsrStatus
import com.classmate.app.l3.L3DemoSeeds
import com.classmate.app.l3.L3LearningPipeline
import com.classmate.app.l3.L3PipelineSnapshot
import com.classmate.app.l3.L3RecordingStatus
import com.classmate.app.l3.L3SourceType
import com.classmate.app.l3.NoOpClassroomAudioRecorder
import com.classmate.app.l3.QuestionBankParser
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
import com.classmate.core.practice.PracticeAttempt
import com.classmate.core.practice.PracticeFeedbackEngine
import com.classmate.core.practice.PracticeGenerationRequest
import com.classmate.core.practice.PracticeMode
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
import com.classmate.core.model.QuizQuestion
import com.classmate.core.model.SourceKind
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * The single source of truth for the UI. Holds the in-memory back stack and the learning-loop
 * state, and is the ONLY thing that touches the core pipeline — screens never see a provider.
 *
 * Round 1 keeps everything in memory (great for a demo; no persistence yet). Real BlueLM
 * credentials would be injected here by rebuilding [configBundle] from the debug import entry.
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
    // On-device BlueLM 3B owner. Defaults to the honest missing-SDK bridge until the AAR is bundled.
    private val onDeviceController: OnDeviceLlmController = OnDeviceLlmController(),
    // Lazy so constructing the VM never reads capture credentials; OCR/ASR config is loaded only when
    // the user actually invokes those capture paths. Tests inject a fake gateway.
    private val captureGatewayProvider: () -> CaptureGatewayPort = { CaptureGateway() },
    private val classroomAudioRecorder: ClassroomAudioRecorder = NoOpClassroomAudioRecorder,
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
    }

    init {
        // Small one-time read of local history + cross-course learning state. Synchronous on
        // purpose so construction needs no Main dispatcher (keeps the ViewModel unit-testable);
        // both files are tiny. Home/Review/History then share this one snapshot.
        ui = ui.copy(history = historyStore.load(), learningSnapshot = learningStore.snapshot())
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

    fun resetTo(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    private fun navigateReplacing(screen: Screen) {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        navigateTo(screen)
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
    fun setLanguage(language: AppLanguage) { ui = ui.copy(language = language) }

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
        ui = ui.copy(
            modelConfigMasked = masked,
            toast = when {
                masked?.credentialPresent == true -> "配置已保存：当前仅做 readiness 检查，未发送网络请求。"
                else -> "尚未配置云端凭据。未配置时仍可继续端侧处理或手动编辑。"
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
            imageDraftActive = true, imageDraftRunning = false, imageDraftText = "",
            imageDraftManualMode = false, imageDraftMessage = null,
            imageDraftOrigin = origin, imageDraftMeta = null,
        )
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
        val meta = buildString {
            if (originalWidth > 0 && originalHeight > 0) append("原图 ${originalWidth}x$originalHeight → ")
            append("处理 ${image.width}x${image.height} · RGB ${image.bytes.size} 字节")
        }
        ui = ui.copy(
            imageDraftActive = true, imageDraftRunning = true, imageDraftManualMode = false,
            imageDraftMessage = null, imageDraftMeta = meta, imageStudyDraft = null,
            imageDraftSource = null, imageDraftOcrError = null,
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

    /** Apply a draft result to UI state (separated for unit testing without a Main dispatcher). */
    internal fun applyImageDraftResult(result: OnDeviceImageDraftResult) {
        ui = when (result) {
            is OnDeviceImageDraftResult.Draft -> ui.copy(
                imageDraftActive = true,
                imageDraftRunning = false,
                imageDraftText = result.text.trim(),
                imageDraftManualMode = false,
                imageDraftSource = AiExecutionSource.ON_DEVICE,
                aiProcessing = AiProcessingUiState.hidden(),
                imageDraftMessage = "已由端侧蓝心生成图片学习文本草稿，请检查并编辑后确认。",
            )
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
        val text = draft.initialEditableText().trim()
        val needsManualText = text.isBlank()
        ui = ui.copy(
            imageDraftActive = true,
            imageDraftRunning = false,
            imageDraftText = text,
            imageDraftManualMode = needsManualText,
            imageDraftMessage = imageDraftStatusMessage(result.source, draft.ocrError),
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
        source == AiExecutionSource.CLOUD -> "官方 OCR 已生成可编辑文字；请确认后再生成知识地图。"
        source == AiExecutionSource.ON_DEVICE && ocrError == CaptureError.ConfigMissing ->
            "官方 OCR 未配置时，可继续使用端侧蓝心草稿；请确认后再生成知识地图。"
        source == AiExecutionSource.ON_DEVICE -> "官方 OCR 未成功，已保留端侧蓝心草稿；请确认后再生成知识地图。"
        else -> "请手动补充图片中的学习内容；确认后再生成知识地图。"
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
        val confirmed = ui.imageStudyDraft?.let { captureGateway.confirmImageDraft(it, text, ui.courseTitle.ifBlank { origin }) }
        ui = ui.copy(
            courseTitle = confirmed?.courseTitle?.takeIf { it.isNotBlank() } ?: ui.courseTitle,
            courseText = confirmed?.courseText ?: text,
            importSourceType = ImportSourceType.PASTE_TEXT,
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
            aiProcessing = AiProcessingUiState.hidden(),
            toast = "已确认（$origin · 端侧多模态理解草稿），用户确认后进入学习资料，可生成知识时间线。",
        )
    }

    /** Cancel the draft — nothing enters the course text or the knowledge base. */
    fun cancelImageDraft() {
        ui = ui.copy(
            imageDraftActive = false, imageDraftRunning = false, imageDraftText = "",
            imageDraftManualMode = false, imageDraftMessage = null,
            imageDraftOrigin = null, imageDraftMeta = null,
            imageStudyDraft = null, imageDraftSource = null, imageDraftOcrError = null,
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
        val clean = pastedText.trim()
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
            pageIndex = ordinal,
            blockIndex = ordinal,
            createdAt = now,
            updatedAt = now,
        )
        ui = ui.copy(
            ocrImports = ui.ocrImports + draft,
            toast = "已加入本节课资料：${OcrImportAssembler.sourceLabel(kind)}",
        )
        return true
    }

    fun updateOcrImportText(id: String, text: String, now: Long = System.currentTimeMillis()) {
        ui = ui.copy(
            ocrImports = ui.ocrImports.map { draft ->
                if (draft.id == id) draft.copy(pastedText = text, updatedAt = now) else draft
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
            asrStatus = if (ui.providerConfigSummary.officialProviders.asrLongConfigured) L3AsrStatus.PENDING_ASR_CONFIG else L3AsrStatus.ASR_NOT_CONFIGURED,
            message = artifact.safeMessage,
        )
        ui = ui.copy(
            currentRecording = null,
            recordingRecords = ui.recordingRecords + saved,
            asrLongStatus = saved.asrStatus,
            audioCaptureMessage = if (saved.asrStatus == L3AsrStatus.ASR_NOT_CONFIGURED) "官方 ASR Long 未配置，可粘贴转写文本继续。" else "录音已保存，等待 ASR Long 配置或手动转写。",
            toast = artifact.safeMessage,
        )
    }

    fun showImportPlaceholder(sourceType: ImportSourceType) {
        ui = ui.copy(toast = ImportHub.placeholderMessage(sourceType))
    }

    private fun l3MaterialText(): String =
        buildString {
            if (ui.courseText.isNotBlank()) appendLine(ui.courseText.trim())
            ui.ocrImports.filter { it.pastedText.isNotBlank() }.forEach { appendLine(it.pastedText.trim()) }
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
        ui.ocrImports.any { it.pastedText.isNotBlank() } -> L3SourceType.OCR_IMAGE
        else -> L3SourceType.TEXT
    }

    private fun publishL3Snapshot(snapshot: L3PipelineSnapshot, now: Long, toast: String): Boolean {
        val artifacts = l3Pipeline.toCourseArtifacts(snapshot, now)
        if (artifacts == null) {
            ui = ui.copy(l3Pipeline = snapshot, toast = "L3 资料不足，未生成完整闭环。")
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
            l3Pipeline = snapshot,
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
        navigateTo(Screen.COURSE_DETAIL)
        return true
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
        ui = ui.copy(
            asrSession = next,
            toast = when (next.state) {
                AsrState.UNSUPPORTED -> "本设备不支持系统语音识别，请使用手动转写或导入字幕。"
                AsrState.PERMISSION_REQUIRED -> "未授权麦克风，仍可手动记录或导入转写稿。"
                AsrState.LISTENING -> "实时转写实验已开始（使用系统语音识别，不保存原始音频）。"
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
        val hasContent = ui.courseText.isNotBlank() || hasTranscript || ui.ocrImports.any { it.pastedText.isNotBlank() }
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
    fun saveTranscriptToTray() {
        val draft = ui.transcriptDraft ?: return
        val usable = draft.copy(segments = draft.segments.filter { it.text.isNotBlank() })
        if (usable.segments.isEmpty()) {
            ui = ui.copy(toast = "转写稿为空，未加入资料篮。")
            return
        }
        val others = ui.transcripts.filterNot { it.id == usable.id }
        ui = ui.copy(
            transcripts = others + usable,
            transcriptDraft = null,
            transcriptPasteDraft = "",
            transcriptFileMetadata = null,
            transcriptParseWarnings = emptyList(),
            toast = "已加入资料篮：${com.classmate.core.transcript.TranscriptLabels.of(usable.sourceType)} · ${usable.segments.size} 段",
        )
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
    fun startAnalysis() {
        val text = ui.courseText
        val hasOcrText = ui.ocrImports.any { it.pastedText.isNotBlank() }
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
        val glossaryHint = LessonContextHints.glossaryHint(
            subject = ui.selectedSubject,
            terms = CourseGlossary.termsFor(ui.selectedSubject).map { it.term },
        )
        val analyzerText = listOf(glossaryHint, bundle.plainText()).filter { it.isNotBlank() }.joinToString("\n\n")
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

        ui = ui.copy(
            session = session,
            lastMaterialSummary = materialSummary,
            analysisStatus = AnalysisStatus.RUNNING,
            analysisStageIndex = 0,
            result = null,
            reviewPlan = null,
            answers = emptyMap(),
            revealedQuestionIds = emptySet(),
            currentQuestionIndex = 0,
            feedbackEvents = emptyList(),
            analysisError = null,
        )
        navigateTo(Screen.ANALYZE)

        viewModelScope.launch {
            // The real pipeline runs off the main thread. For the bundled sample we show the
            // curated, evidence-bound analysis (clearly labelled demo data); for pasted text we
            // run the resolver, which falls back to the local heuristic when BlueLM is absent.
            val cloudOutcome: AnalysisOutcome = withContext(Dispatchers.Default) {
                if (isSample) {
                    AnalysisOutcome.Success(
                        result = SampleCourses.seriesAnalysis(now),
                        report = com.classmate.core.validation.ValidationReport.PASS,
                        logs = listOf(RedactedLogEntry("BLUELM", "OK", 0, "PASS", false, null)),
                    )
                } else {
                    analyzer().analyze(AnalysisRequest(session))
                }
            }
            // Cloud success → use it. Cloud failed (or cloud-only chain produced nothing) → try the
            // on-device BlueLM 3B structured-analysis seam, which must pass the SAME validators to land.
            val cloudStatus = if (isSample) "OK" else (cloudOutcome as? AnalysisOutcome.Failure)?.lastError?.shortCode ?: "OK"
            val onDeviceAttempted = cloudOutcome !is AnalysisOutcome.Success
            val fallback = if (cloudOutcome is AnalysisOutcome.Success) null
            else onDeviceAnalysisFallback(session, cloudOutcome as AnalysisOutcome.Failure)
            val outcome = fallback?.first ?: cloudOutcome
            val onDeviceReason = fallback?.second
            val onDeviceDiag = fallback?.third
            // P0: the unified AiCapabilityRouter is the single authority for the CourseAnalysis route
            // decision (CLOUD → ON_DEVICE → SAFE_PLACEHOLDER). It drives the user-visible source label;
            // the validated outcome above is produced exactly as before (validators never relaxed).
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
                finalSource = CourseAnalysisRouting.finalSourceZh(analysisRoute.source),
            )

            // Staged reveal for product feel — these are the real conceptual phases.
            for (stage in 1..TOTAL_STAGES) {
                ui = ui.copy(analysisStageIndex = stage)
                delay(STAGE_DELAY_MS)
            }

            when (outcome) {
                is AnalysisOutcome.Success -> {
                    val learning = LearningState.seed(outcome.result.sessionId, outcome.result.knowledgePoints, now)
                    val l3Snapshot = l3Pipeline.buildFromAnalysis(
                        session = session,
                        result = outcome.result,
                        sourceType = currentL3SourceType(),
                        providerSummary = ui.providerConfigSummary,
                        now = now,
                    )
                    val updatedHistory = (listOf(buildHistoryRecord(session, outcome, now)) + ui.history).take(MAX_HISTORY)
                    // Cross-course review tasks: one per kept knowledge point (idempotent per session).
                    val provider = outcome.result.provenance.provider
                    learningStore.addTasksFromAnalysis(
                        result = outcome.result,
                        courseTitle = session.title.ifBlank { "未命名课程" },
                        sourceProvider = provider.name,
                        sourceProfile = ui.providerConfigSummary.profileLabel,
                        sourceModel = configBundle.configOf(provider)?.model.orEmpty(),
                    )
                    ui = ui.copy(
                        analysisStatus = AnalysisStatus.SUCCESS,
                        result = outcome.result,
                        learningState = learning,
                        logs = outcome.logs,
                        history = updatedHistory,
                        learningSnapshot = learningStore.snapshot(),
                        l3Pipeline = l3Snapshot,
                        analysisSourceReport = sourceReport,
                        onDeviceAnalysisReason = onDeviceReason ?: ui.onDeviceAnalysisReason,
                        onDeviceAnalysisDiagnostic = onDeviceDiag ?: ui.onDeviceAnalysisDiagnostic,
                    )
                    persistHistory(updatedHistory)
                    navigateReplacing(Screen.COURSE_DETAIL)
                }

                is AnalysisOutcome.Failure -> {
                    // Both cloud 云端蓝心 and on-device 端侧蓝心 failed/were rejected by validators →
                    // safety placeholder. We do NOT persist a fake CourseAnalysis or invent knowledge points.
                    // The message is STRUCTURED so an on-device failure is never hidden behind the cloud code.
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

    // --- knowledge / evidence ---
    fun openEvidence(knowledgePointId: String) {
        ui = ui.copy(selectedKnowledgePointId = knowledgePointId)
        ui.result?.sessionId?.let { sessionId ->
            learningStore.recordTracebackOpen(sessionId, knowledgePointId)
            ui = ui.copy(learningSnapshot = learningStore.snapshot())
        }
        navigateTo(Screen.EVIDENCE)
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
        ui = ui.copy(
            feedbackEvents = ui.feedbackEvents + event,
            learningState = updated ?: ui.learningState,
            reviewPlan = null,
            toast = "已记录反馈：${type.displayZh}",
        )
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
                steps = listOf("整理课程资料", "整理知识地图", "汇总练习与薄弱点", "生成学习报告草稿", "等待选择导出格式"),
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

    fun recordsForCourse(courseKey: String): List<HistoryRecord> =
        ui.history.filter { CourseLibraryBuilder.normalizeCourseName(it.title).lowercase() == courseKey }
            .sortedByDescending { it.createdAtEpochMs }

    fun reviewTasksForCourse(courseKey: String): List<ReviewTask> =
        ui.learningSnapshot.tasks.filter { CourseLibraryBuilder.normalizeCourseName(it.courseTitle).lowercase() == courseKey }

    fun weaknessItems() = WeaknessHub.fromSnapshot(ui.learningSnapshot)

    // --- adaptive practice (Stage 7C): in-app drill that writes back through ReviewEngine rules ---

    /** Start a practice session in [mode] for the current course (or load a course if none is open). */
    fun startPractice(mode: PracticeMode) {
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
        val practice = generated.value?.session ?: PracticeSessionEngine.build(r, ui.learningSnapshot, mode, now, courseTitle = s.title)
        if (practice.items.isEmpty()) {
            ui = ui.copy(toast = "暂时没有可练习的内容。", aiProcessing = AiProcessingUiState.hidden())
            return
        }
        ui = ui.copy(
            practiceSession = practice,
            practiceIndex = 0,
            practiceAttempts = emptyList(),
            practiceResult = null,
            practiceStartedAt = now,
            practiceRevealed = false,
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

    fun answerPractice(outcome: PracticeOutcome) {
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
        val after = PracticeSessionEngine.writeBack(learningStore.snapshot(), session.courseSessionId, ui.practiceAttempts, now)
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
        ui = ui.copy(practiceResult = result, learningSnapshot = learningStore.snapshot(), toast = "本轮练习完成。")
    }

    fun currentPracticeItem() = ui.practiceSession?.items?.getOrNull(ui.practiceIndex)
    fun isPracticeComplete(): Boolean = ui.practiceResult != null

    fun exitPractice() {
        ui = ui.copy(practiceSession = null, practiceIndex = 0, practiceAttempts = emptyList(), practiceResult = null, practiceRevealed = false)
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

    fun deleteHistory(id: String) {
        val updated = ui.history.filterNot { it.id == id }
        ui = ui.copy(history = updated)
        persistHistory(updated)
    }

    /** Official SDK requirement: release native resources when the owner is destroyed. */
    override fun onCleared() {
        runCatching { onDeviceController.release() }
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
