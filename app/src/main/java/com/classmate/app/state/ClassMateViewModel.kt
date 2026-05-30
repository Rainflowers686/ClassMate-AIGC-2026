package com.classmate.app.state

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classmate.app.data.ApiConfigRepository
import com.classmate.app.data.DemoInputRepository
import com.classmate.app.domain.AnalyzeCourseUseCase
import com.classmate.app.domain.ProviderResolver
import com.classmate.app.ui.theme.ThemeId
import com.classmate.core.adapter.ProviderConfig
import com.classmate.core.logging.ModelCallLog
import com.classmate.core.logging.RedactedLogger
import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.network.SimpleHttpEngine
import com.classmate.core.segmenter.Segmenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val LOG_TAG = "ClassMateLog"
private const val UI_LOG_TAG = "ClassMateVM"

/**
 * Owns v0.4 main-flow state.
 *
 * Delegates the heavy lifting to [AnalyzeCourseUseCase] + [ProviderResolver].
 * Responsibilities kept here:
 *  - navigation between sealed [ClassMateScreen]s
 *  - input pipeline (text / hotword / segmenter / demo loader)
 *  - quiz interaction state
 *  - log emission per analyze call
 *  - theme selection
 *
 * Provider routing (v0.4): see ProviderResolver. Fallback decisions:
 * see AnalyzeCourseUseCase. UI surfaces them via [ClassMateUiState].
 */
class ClassMateViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ClassMateUiState())
    val state: StateFlow<ClassMateUiState> = _state.asStateFlow()

    private val redactedLogger = RedactedLogger { line -> Log.i(LOG_TAG, line) }
    private val httpEngine = SimpleHttpEngine()
    private val providerConfig: ProviderConfig
    private val analyzeCourse: AnalyzeCourseUseCase

    init {
        val cfg = ApiConfigRepository.load(application)
        providerConfig = cfg.providerConfig
        val resolver = ProviderResolver(providerConfig, httpEngine)
        analyzeCourse = AnalyzeCourseUseCase(resolver)
        _state.update {
            it.copy(
                requestedProvider = resolver.requestedName,
                activeProvider = resolver.requestedName,
                configHint = resolver.requestedDisplayName + " · " +
                    if (cfg.loadedFromLocalFile) "已读取本机配置" else "使用示例配置"
            )
        }
    }

    // ---------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------

    fun navigateTo(target: ClassMateScreen) {
        _state.update { it.copy(screen = target, errorMessage = null) }
    }

    fun back() {
        val previous = when (_state.value.screen) {
            ClassMateScreen.Home -> ClassMateScreen.Home
            ClassMateScreen.CourseInput -> ClassMateScreen.Home
            ClassMateScreen.Hotword -> ClassMateScreen.CourseInput
            ClassMateScreen.Analyze -> ClassMateScreen.Hotword
            ClassMateScreen.Timeline -> ClassMateScreen.Analyze
            ClassMateScreen.Quiz -> ClassMateScreen.Timeline
            ClassMateScreen.ReviewPlan -> ClassMateScreen.Quiz
            ClassMateScreen.Settings -> ClassMateScreen.Home
        }
        navigateTo(previous)
    }

    // ---------------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------------

    fun setTheme(themeId: ThemeId) {
        _state.update { it.copy(themeId = themeId) }
    }

    // ---------------------------------------------------------------------
    // Input pipeline
    // ---------------------------------------------------------------------

    fun updateCourseTitle(title: String) {
        _state.update { it.copy(courseTitle = title) }
    }

    fun updateCourseText(text: String) {
        _state.update { it.copy(courseText = text) }
    }

    /**
     * Populates title + text + hotwords + SEGMENTS from the bundled demo
     * course. The key v0.4 fix: we copy demo_input.segments verbatim into
     * state.segments so the downstream evidence chain is anchored to the
     * same ids the Provider will emit. Segmenter is bypassed for the demo
     * path; user-typed text still flows through Segmenter via [runSegmentation].
     */
    fun loadDemoInput() {
        viewModelScope.launch {
            runCatching {
                val demoInput = DemoInputRepository.loadDemoInput(getApplication())
                _state.update {
                    it.copy(
                        courseTitle = demoInput.courseTitle,
                        courseText = demoInput.segments.joinToString("\n\n") { s -> s.text },
                        hotwords = demoInput.hotwords,
                        segments = demoInput.segments,
                        errorMessage = null
                    )
                }
            }.onFailure { t ->
                Log.w(UI_LOG_TAG, "loadDemoInput failed", t)
                _state.update { it.copy(errorMessage = "示例课程加载失败，请重试") }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Hotwords
    // ---------------------------------------------------------------------

    fun updatePendingHotword(text: String) {
        _state.update { it.copy(pendingHotword = text) }
    }

    fun addHotword() {
        val current = _state.value
        val cleaned = current.pendingHotword.trim()
        if (cleaned.isEmpty() || cleaned in current.hotwords) {
            _state.update { it.copy(pendingHotword = "") }
            return
        }
        if (current.hotwords.size >= 20) {
            _state.update { it.copy(pendingHotword = "") }
            return
        }
        _state.update {
            it.copy(
                hotwords = it.hotwords + cleaned,
                pendingHotword = ""
            )
        }
    }

    fun removeHotword(hotword: String) {
        _state.update { it.copy(hotwords = it.hotwords - hotword) }
    }

    // ---------------------------------------------------------------------
    // Analyze
    // ---------------------------------------------------------------------

    /**
     * Re-runs Segmenter on courseText. Only the user-input path needs this —
     * the demo loader already populated state.segments verbatim.
     */
    fun runSegmentation() {
        val current = _state.value
        // If segments already match the current text (e.g. demo loader populated
        // them verbatim), don't re-segment: that would scramble the ids the
        // downstream evidence chain depends on.
        val segs = Segmenter.segment(current.courseText)
        _state.update { it.copy(segments = segs) }
    }

    fun runAnalysis() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val started = System.currentTimeMillis()
            if (_state.value.segments.isEmpty()) {
                // Only run Segmenter when state has no segments yet (manual input flow).
                runSegmentation()
            }
            val current = _state.value
            val input = CourseAnalysisInput(
                courseTitle = current.courseTitle.ifBlank { "Untitled" },
                hotwords = current.hotwords,
                segments = current.segments
            )

            val outcome = analyzeCourse.run(input)
            val elapsed = System.currentTimeMillis() - started

            when (outcome) {
                is AnalyzeCourseUseCase.Outcome.Success -> {
                    val structureOk = outcome.structural.passed && outcome.evidence.schemaPassed
                    redactedLogger.courseAnalysisCall(
                        ModelCallLog(
                            timestamp = isoNow(),
                            provider = outcome.providerUsed,
                            task = "course_analysis",
                            inputSegmentCount = input.segments.size,
                            hotwordCount = input.hotwords.size,
                            success = true,
                            latencyMs = elapsed,
                            structureValid = structureOk,
                            strictEvidenceMatchRate = outcome.evidence.strictEvidenceMatchRate,
                            lenientEvidenceMatchRate = outcome.evidence.lenientEvidenceMatchRate,
                            fallbackUsed = outcome.fallbackUsed,
                            errorType = outcome.fallbackReason
                        )
                    )
                    _state.update {
                        it.copy(
                            analysisResult = outcome.result,
                            evidenceValidation = outcome.evidence,
                            validationIssues = outcome.structural.issues,
                            activeProvider = outcome.providerUsed,
                            structureValid = structureOk,
                            fallbackUsed = outcome.fallbackUsed,
                            lastProviderError = outcome.fallbackReason,
                            isLoading = false,
                            errorMessage = null,
                            currentQuizIndex = 0,
                            quizAnswers = emptyMap(),
                            wrongKnowledgePointIds = emptySet()
                        )
                    }
                }
                is AnalyzeCourseUseCase.Outcome.HardFailure -> {
                    redactedLogger.courseAnalysisCall(
                        ModelCallLog(
                            timestamp = isoNow(),
                            provider = outcome.providerUsed,
                            task = "course_analysis",
                            inputSegmentCount = input.segments.size,
                            hotwordCount = input.hotwords.size,
                            success = false,
                            latencyMs = elapsed,
                            structureValid = false,
                            strictEvidenceMatchRate = null,
                            lenientEvidenceMatchRate = null,
                            fallbackUsed = true,
                            errorType = outcome.errorType
                        )
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            fallbackUsed = true,
                            lastProviderError = outcome.errorType,
                            errorMessage = uiErrorMessage(outcome.errorType)
                        )
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Evidence + quiz interaction
    // ---------------------------------------------------------------------

    fun selectKnowledgePoint(kp: KnowledgePoint?) {
        _state.update {
            it.copy(
                selectedKnowledgePoint = kp,
                selectedEvidenceSegmentId = kp?.sourceSegmentId
            )
        }
    }

    fun showEvidenceFor(segmentId: String?) {
        _state.update { it.copy(selectedEvidenceSegmentId = segmentId) }
    }

    fun submitQuizAnswer(quizId: String, chosenIndex: Int) {
        val current = _state.value
        val quiz = current.quizzes.firstOrNull { it.quizId == quizId } ?: return
        val updated = current.quizAnswers + (quizId to chosenIndex)
        val wrong = current.wrongKnowledgePointIds.toMutableSet()
        if (chosenIndex == quiz.answerIndex) {
            wrong.remove(quiz.relatedKpId)
        } else {
            wrong.add(quiz.relatedKpId)
        }
        _state.update { it.copy(quizAnswers = updated, wrongKnowledgePointIds = wrong) }
    }

    fun nextQuiz() {
        val current = _state.value
        val next = (current.currentQuizIndex + 1).coerceAtMost(current.quizzes.lastIndex.coerceAtLeast(0))
        _state.update { it.copy(currentQuizIndex = next) }
    }

    fun previousQuiz() {
        val prev = (_state.value.currentQuizIndex - 1).coerceAtLeast(0)
        _state.update { it.copy(currentQuizIndex = prev) }
    }

    // ---------------------------------------------------------------------
    // Reset
    // ---------------------------------------------------------------------

    fun resetSession() {
        _state.update {
            ClassMateUiState(
                requestedProvider = it.requestedProvider,
                activeProvider = it.requestedProvider,
                themeId = it.themeId,
                configHint = it.configHint
            )
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return fmt.format(Date())
    }

    private fun uiErrorMessage(errorType: String): String = when (errorType) {
        "HTTP_ERROR" -> "云端分析失败，已切换到本地证据引擎"
        "CONFIG_MISSING" -> "云端配置不可用，已切换到本地证据引擎"
        "PROVIDER_NOT_IMPLEMENTED" -> "云端能力暂未启用，已切换到本地证据引擎"
        "VALIDATION_FAILED" -> "分析结果校验未通过，请重试"
        else -> "分析失败，请检查课程文本后重试"
    }
}
