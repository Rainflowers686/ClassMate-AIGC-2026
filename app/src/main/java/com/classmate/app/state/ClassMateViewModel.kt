package com.classmate.app.state

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classmate.app.data.ApiConfigRepository
import com.classmate.app.data.DemoInputRepository
import com.classmate.core.adapter.BlueLMProvider
import com.classmate.core.adapter.CompatibleProvider
import com.classmate.core.adapter.DemoProvider
import com.classmate.core.adapter.ModelCallException
import com.classmate.core.adapter.ModelProvider
import com.classmate.core.adapter.ProviderConfig
import com.classmate.core.evidence.EvidenceValidationResult
import com.classmate.core.evidence.EvidenceValidator
import com.classmate.core.logging.ModelCallLog
import com.classmate.core.logging.RedactedLogger
import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.network.SimpleHttpEngine
import com.classmate.core.segmenter.Segmenter
import com.classmate.core.validation.ResultValidator
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
 * Owns the v0.3 main-flow state.
 *
 * v0.3.5 fallback policy (task §7):
 *   1. Try the provider named in config.local.json (compatible / bluelm).
 *   2. If that provider throws ModelCallException, log the reason and fall
 *      back to DemoProvider so the demo flow still renders.
 *   3. Mark [ClassMateUiState.fallbackUsed]=true so the UI can warn that the
 *      result was canned, not a real model call.
 *
 * Provider = "demo" is treated as the intended path, not a fallback — its
 * fallbackUsed stays false.
 */
class ClassMateViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ClassMateUiState())
    val state: StateFlow<ClassMateUiState> = _state.asStateFlow()

    private val redactedLogger = RedactedLogger { line -> Log.i(LOG_TAG, line) }
    private val httpEngine = SimpleHttpEngine()
    private val providerConfig: ProviderConfig

    init {
        val cfg = ApiConfigRepository.load(application)
        providerConfig = cfg.providerConfig
        _state.update {
            it.copy(
                requestedProvider = cfg.provider,
                activeProvider = cfg.provider,
                configHint = "provider=${cfg.provider} (config.local.json " +
                    "${if (cfg.loadedFromLocalFile) "found" else "missing — using example defaults"})"
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
        }
        navigateTo(previous)
    }

    // ---------------------------------------------------------------------
    // Step 1: course input
    // ---------------------------------------------------------------------

    fun updateCourseTitle(title: String) {
        _state.update { it.copy(courseTitle = title) }
    }

    fun updateCourseText(text: String) {
        _state.update { it.copy(courseText = text) }
    }

    fun loadDemoInput() {
        viewModelScope.launch {
            runCatching {
                val demoInput = DemoInputRepository.loadDemoInput(getApplication())
                _state.update {
                    it.copy(
                        courseTitle = demoInput.courseTitle,
                        courseText = demoInput.segments.joinToString("\n\n") { s -> s.text },
                        hotwords = demoInput.hotwords,
                        errorMessage = null
                    )
                }
            }.onFailure { t ->
                Log.w(UI_LOG_TAG, "loadDemoInput failed", t)
                _state.update { it.copy(errorMessage = "Failed to load demo input: ${t.message}") }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Step 2: hotwords
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
    // Step 3: segmentation + analysis
    // ---------------------------------------------------------------------

    fun runSegmentation() {
        val text = _state.value.courseText
        val segs = Segmenter.segment(text)
        _state.update { it.copy(segments = segs) }
    }

    /**
     * Runs the configured provider. On any [ModelCallException], logs the
     * reason and falls back to DemoProvider so the demo flow always renders.
     * The UI sees both [ClassMateUiState.activeProvider] and
     * [ClassMateUiState.fallbackUsed] so reviewers can spot a canned result.
     */
    fun runAnalysis() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val started = System.currentTimeMillis()
            if (_state.value.segments.isEmpty()) {
                runSegmentation()
            }
            val current = _state.value
            val input = CourseAnalysisInput(
                courseTitle = current.courseTitle.ifBlank { "Untitled" },
                hotwords = current.hotwords,
                segments = current.segments
            )

            val requested = providerConfig.provider
            val outcome = runWithFallback(requested, input)
            val elapsed = System.currentTimeMillis() - started

            when (outcome) {
                is AnalysisOutcome.Success -> {
                    val structurePassed = outcome.structureValid && outcome.evidence.schemaPassed
                    redactedLogger.courseAnalysisCall(
                        ModelCallLog(
                            timestamp = isoNow(),
                            provider = outcome.providerUsed,
                            task = "course_analysis",
                            inputSegmentCount = input.segments.size,
                            hotwordCount = input.hotwords.size,
                            success = true,
                            latencyMs = elapsed,
                            structureValid = structurePassed,
                            evidenceMatchRate = outcome.evidence.evidenceMatchRate,
                            fallbackUsed = outcome.fallbackUsed,
                            errorType = outcome.fallbackReason
                        )
                    )
                    _state.update {
                        it.copy(
                            analysisResult = outcome.result,
                            evidenceValidation = outcome.evidence,
                            activeProvider = outcome.providerUsed,
                            structureValid = structurePassed,
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
                is AnalysisOutcome.HardFailure -> {
                    redactedLogger.courseAnalysisCall(
                        ModelCallLog(
                            timestamp = isoNow(),
                            provider = outcome.providerAttempted,
                            task = "course_analysis",
                            inputSegmentCount = input.segments.size,
                            hotwordCount = input.hotwords.size,
                            success = false,
                            latencyMs = elapsed,
                            structureValid = false,
                            evidenceMatchRate = null,
                            fallbackUsed = true,
                            errorType = outcome.errorType
                        )
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            fallbackUsed = true,
                            lastProviderError = outcome.message,
                            errorMessage = outcome.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Runs [requested] provider; on ModelCallException, drops to DemoProvider
     * and reports both providers + the reason. DemoProvider failure becomes
     * a hard error (nothing left to fall back to).
     */
    private suspend fun runWithFallback(
        requested: String,
        input: CourseAnalysisInput
    ): AnalysisOutcome {
        // DemoProvider intentionally has no fallback chain — it IS the floor.
        if (requested == "demo") {
            return runProviderAndValidate(
                provider = buildDemoProvider(),
                input = input,
                fallbackUsed = false,
                fallbackReason = null
            )
        }

        // Try the requested non-demo provider first.
        val primary = try {
            buildProvider(requested)
        } catch (e: ModelCallException) {
            Log.w(UI_LOG_TAG, "provider $requested unavailable (${e.reason}); falling back to demo")
            return demoFallback(input, "${e.reason}: ${e.message}")
        }

        return try {
            runProviderAndValidate(primary, input, fallbackUsed = false, fallbackReason = null)
        } catch (e: ModelCallException) {
            Log.w(UI_LOG_TAG, "provider ${primary.name} failed (${e.reason}); falling back to demo", e)
            demoFallback(input, "${e.reason}: ${e.message}")
        }
    }

    private suspend fun demoFallback(input: CourseAnalysisInput, reason: String): AnalysisOutcome {
        return try {
            runProviderAndValidate(buildDemoProvider(), input, fallbackUsed = true, fallbackReason = reason)
        } catch (e: Throwable) {
            AnalysisOutcome.HardFailure(
                providerAttempted = "demo",
                errorType = e::class.simpleName ?: "Unknown",
                message = "demo fallback also failed: ${e.message}"
            )
        }
    }

    /**
     * Calls [provider], then runs Result and Evidence validators. Wraps any
     * non-ModelCallException Throwable in a ModelCallException so the caller's
     * try/catch is uniform.
     */
    private suspend fun runProviderAndValidate(
        provider: ModelProvider,
        input: CourseAnalysisInput,
        fallbackUsed: Boolean,
        fallbackReason: String?
    ): AnalysisOutcome.Success {
        val result: CourseAnalysisResult = try {
            provider.analyzeCourse(input)
        } catch (e: ModelCallException) {
            throw e
        } catch (e: Throwable) {
            throw ModelCallException(
                ModelCallException.Reason.HTTP_ERROR,
                "unexpected error from provider ${provider.name}: ${e.message}",
                e
            )
        }

        val structural = ResultValidator.validate(result)
        if (!structural.passed) {
            Log.w(UI_LOG_TAG, "ResultValidator issues for ${provider.name}: ${structural.issues}")
        }
        val evidence: EvidenceValidationResult = EvidenceValidator.validate(input, result)
        // Per spec §11.2 we do NOT raise on imperfect span match — UI degrades.
        if (evidence.evidenceMatchRate < 1.0) {
            Log.i(UI_LOG_TAG, "evidence match rate ${evidence.evidenceMatchRate} for ${provider.name}; UI will downgrade highlights")
        }
        return AnalysisOutcome.Success(
            result = result,
            evidence = evidence,
            structureValid = structural.passed,
            providerUsed = provider.name,
            fallbackUsed = fallbackUsed,
            fallbackReason = fallbackReason
        )
    }

    private fun buildProvider(name: String): ModelProvider = when (name) {
        "demo" -> buildDemoProvider()
        "compatible" -> CompatibleProvider(providerConfig.compatible, httpEngine)
        "bluelm" -> BlueLMProvider(providerConfig.bluelm, httpEngine)
        else -> throw ModelCallException(
            ModelCallException.Reason.PROVIDER_NOT_IMPLEMENTED,
            "unknown provider '$name' (expected demo | compatible | bluelm)"
        )
    }

    private fun buildDemoProvider(): DemoProvider =
        DemoProvider(DemoInputRepository.loadDemoOutputRaw(getApplication()))

    // ---------------------------------------------------------------------
    // Step 4: evidence + quiz interaction
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
    // Step 5: reset
    // ---------------------------------------------------------------------

    fun resetSession() {
        _state.update {
            ClassMateUiState(
                requestedProvider = it.requestedProvider,
                activeProvider = it.requestedProvider,
                configHint = it.configHint
            )
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return fmt.format(Date())
    }

    /**
     * Internal discriminator for the runAnalysis pipeline. Lifted out of
     * runAnalysis so the fallback + logging path can be read top-to-bottom.
     */
    private sealed interface AnalysisOutcome {
        data class Success(
            val result: CourseAnalysisResult,
            val evidence: EvidenceValidationResult,
            val structureValid: Boolean,
            val providerUsed: String,
            val fallbackUsed: Boolean,
            val fallbackReason: String?
        ) : AnalysisOutcome

        data class HardFailure(
            val providerAttempted: String,
            val errorType: String,
            val message: String
        ) : AnalysisOutcome
    }
}
