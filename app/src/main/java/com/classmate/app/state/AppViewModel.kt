package com.classmate.app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classmate.app.data.BlueLMHttpTransport
import com.classmate.app.platform.ConfigImportPreview
import com.classmate.app.platform.ConfigRepository
import com.classmate.app.platform.DebugConfigImporter
import com.classmate.app.platform.ProviderConfigSummary
import com.classmate.app.ui.theme.ThemeOption
import com.classmate.core.analysis.AnalysisOutcome
import com.classmate.core.analysis.CourseAnalyzer
import com.classmate.core.analysis.CourseSegmenter
import com.classmate.core.feedback.LearningStateUpdater
import com.classmate.core.logging.RedactedLogEntry
import com.classmate.core.model.FeedbackEvent
import com.classmate.core.model.FeedbackTargetKind
import com.classmate.core.model.FeedbackType
import com.classmate.core.model.Ids
import com.classmate.core.model.LearningState
import com.classmate.core.model.QuizAttempt
import com.classmate.core.model.QuizQuestion
import com.classmate.core.model.SourceKind
import com.classmate.core.prompt.PromptBuilder
import com.classmate.core.provider.AnalysisRequest
import com.classmate.core.provider.BlueLMDiagnosticReport
import com.classmate.core.provider.BlueLMDiagnosticRunner
import com.classmate.core.provider.BlueLmSigner
import com.classmate.core.provider.HttpTransport
import com.classmate.core.provider.ProviderConfigBundle
import com.classmate.core.provider.ProviderResolver
import com.classmate.core.provider.UnconfiguredBlueLmSigner
import com.classmate.core.model.ProviderKind
import com.classmate.core.review.ReviewPlanner
import com.classmate.core.sample.SampleCourses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
) : ViewModel() {

    private val promptBuilder = PromptBuilder()

    // THE single active provider config — one source of truth. The Settings model-config
    // summary, CourseAnalyzer (analyzer()) and the BlueLM diagnostic ALL read this exact field,
    // so they can never diverge. It changes ONLY through applyActiveConfig().
    // Default remains inert/fallback-safe; config.local.json may replace credentials locally.
    private val initialConfig = configRepository.loadLocalOrDefault()
    private var configBundle: ProviderConfigBundle = initialConfig.bundle

    /** The active bundle that both the analyzer and the diagnostic run against (also for tests). */
    internal fun activeConfigBundle(): ProviderConfigBundle = configBundle

    private fun analyzer(): CourseAnalyzer =
        CourseAnalyzer(ProviderResolver(configBundle, promptBuilder, transport, blueLmSigner))

    var ui by mutableStateOf(
        ClassMateUiState(providerConfigSummary = providerSummary(initialConfig.summary.source)),
    )
        private set

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
    fun setTheme(option: ThemeOption) { ui = ui.copy(theme = option) }
    fun setDarkMode(dark: Boolean?) { ui = ui.copy(darkMode = dark) }

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

    private fun providerSummary(source: String): ProviderConfigSummary =
        ProviderConfigSummary.fromBundle(
            source = source,
            bundle = configBundle,
            primaryReady = ProviderResolver(configBundle, promptBuilder, transport, blueLmSigner).isPrimaryReady(),
        )

    // --- import ---
    fun updateCourseTitle(value: String) { ui = ui.copy(courseTitle = value) }
    fun updateCourseText(value: String) { ui = ui.copy(courseText = value) }

    fun loadSample() {
        ui = ui.copy(courseTitle = SampleCourses.SERIES_TITLE, courseText = SampleCourses.SERIES_TEXT)
    }

    // --- analysis ---
    fun startAnalysis() {
        val text = ui.courseText
        if (text.isBlank()) {
            ui = ui.copy(toast = "请先粘贴课堂文本")
            return
        }
        val now = System.currentTimeMillis()
        val isSample = text.trim() == SampleCourses.SERIES_TEXT.trim()
        val session = if (isSample) {
            SampleCourses.seriesSession(now)
        } else {
            CourseSegmenter.buildSession(
                id = "session_$now",
                title = ui.courseTitle.ifBlank { "未命名课程" },
                rawText = text,
                nowMs = now,
                sourceKind = SourceKind.PASTED_TEXT,
            )
        }

        ui = ui.copy(
            session = session,
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
            val outcome: AnalysisOutcome = withContext(Dispatchers.Default) {
                if (isSample) {
                    AnalysisOutcome.Success(
                        result = SampleCourses.seriesAnalysis(now),
                        report = com.classmate.core.validation.ValidationReport.PASS,
                        logs = listOf(RedactedLogEntry("LOCAL_FALLBACK", "OK", 0, "PASS", true, null)),
                    )
                } else {
                    analyzer().analyze(AnalysisRequest(session))
                }
            }

            // Staged reveal for product feel — these are the real conceptual phases.
            for (stage in 1..TOTAL_STAGES) {
                ui = ui.copy(analysisStageIndex = stage)
                delay(STAGE_DELAY_MS)
            }

            when (outcome) {
                is AnalysisOutcome.Success -> {
                    val learning = LearningState.seed(outcome.result.sessionId, outcome.result.knowledgePoints, now)
                    ui = ui.copy(
                        analysisStatus = AnalysisStatus.SUCCESS,
                        result = outcome.result,
                        learningState = learning,
                        logs = outcome.logs,
                    )
                    navigateReplacing(Screen.KNOWLEDGE)
                }

                is AnalysisOutcome.Failure -> {
                    ui = ui.copy(
                        analysisStatus = AnalysisStatus.FAILED,
                        logs = outcome.logs,
                        analysisError = outcome.lastError?.shortCode ?: "分析失败",
                    )
                }
            }
        }
    }

    fun retryAnalysis() {
        if (backStack.last() == Screen.ANALYZE) backStack.removeAt(backStack.lastIndex)
        startAnalysis()
    }

    // --- knowledge / evidence ---
    fun openEvidence(knowledgePointId: String) {
        ui = ui.copy(selectedKnowledgePointId = knowledgePointId)
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
        ui = ui.copy(
            answers = ui.answers + (question.id to optionId),
            revealedQuestionIds = ui.revealedQuestionIds + question.id,
            learningState = updated,
            reviewPlan = null, // answers changed -> plan must regenerate
        )
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
    }

    // --- review ---
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

    fun consumeToast() { ui = ui.copy(toast = null) }

    private companion object {
        const val TOTAL_STAGES = 6
        const val STAGE_DELAY_MS = 430L
    }
}
