package com.classmate.app.state

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classmate.app.data.ApiConfigRepository
import com.classmate.app.data.DemoInputRepository
import com.classmate.core.adapter.DemoProvider
import com.classmate.core.adapter.ModelProvider
import com.classmate.core.evidence.EvidenceValidator
import com.classmate.core.logging.ModelCallLog
import com.classmate.core.logging.RedactedLogger
import com.classmate.core.model.CourseAnalysisInput
import com.classmate.core.model.KnowledgePoint
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
 * Extends [AndroidViewModel] so we can reach [android.content.res.AssetManager]
 * via the application context — DemoInputRepository / ApiConfigRepository
 * both need it. A constructor-injected Application is intentional: the
 * Activity does NOT thread its context into every callback.
 */
class ClassMateViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ClassMateUiState())
    val state: StateFlow<ClassMateUiState> = _state.asStateFlow()

    private val redactedLogger = RedactedLogger { line -> Log.i(LOG_TAG, line) }

    init {
        // Surface which config file we found before the user touches anything.
        val cfg = ApiConfigRepository.load(application)
        _state.update {
            it.copy(
                providerName = cfg.provider,
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
        // Linear flow per spec §5.1; back walks the chain. Home stays Home.
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

    /**
     * Populates title + text + hotwords from the bundled demo course. This is
     * the entry point that backs both Home's "Load Demo" button and
     * CourseInputScreen's "Load demo_course" button.
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

    /** Runs the Segmenter on courseText and stores the result. */
    fun runSegmentation() {
        val text = _state.value.courseText
        val segs = Segmenter.segment(text)
        _state.update { it.copy(segments = segs) }
    }

    /**
     * v0.3 only wires DemoProvider. BlueLM/Compatible providers are still
     * placeholders — see [com.classmate.core.adapter.BlueLMProvider]. Using a
     * `when` keeps the UI's intent explicit so a reviewer can see exactly
     * what's wired vs. stubbed without grepping the core module.
     */
    fun runAnalysis() {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val started = System.currentTimeMillis()
            try {
                if (_state.value.segments.isEmpty()) {
                    runSegmentation()
                }
                val current = _state.value
                val input = CourseAnalysisInput(
                    courseTitle = current.courseTitle.ifBlank { "Untitled" },
                    hotwords = current.hotwords,
                    segments = current.segments
                )

                val provider: ModelProvider = buildProvider(current.providerName)
                val result = provider.analyzeCourse(input)

                val structural = ResultValidator.validate(result)
                if (!structural.passed) {
                    Log.w(UI_LOG_TAG, "ResultValidator issues: ${structural.issues}")
                }
                val evidence = EvidenceValidator.validate(input, result)
                val elapsed = System.currentTimeMillis() - started

                redactedLogger.courseAnalysisCall(
                    ModelCallLog(
                        timestamp = isoNow(),
                        provider = provider.name,
                        task = "course_analysis",
                        inputSegmentCount = input.segments.size,
                        hotwordCount = input.hotwords.size,
                        success = true,
                        latencyMs = elapsed,
                        schemaValid = structural.passed && evidence.schemaPassed,
                        evidenceMatchRate = evidence.evidenceMatchRate,
                        errorType = null
                    )
                )

                _state.update {
                    it.copy(
                        analysisResult = result,
                        evidenceValidation = evidence,
                        providerName = provider.name,
                        isLoading = false,
                        errorMessage = null,
                        currentQuizIndex = 0,
                        quizAnswers = emptyMap(),
                        wrongKnowledgePointIds = emptySet()
                    )
                }
            } catch (t: Throwable) {
                Log.w(UI_LOG_TAG, "analysis failed", t)
                val elapsed = System.currentTimeMillis() - started
                redactedLogger.courseAnalysisCall(
                    ModelCallLog(
                        timestamp = isoNow(),
                        provider = _state.value.providerName,
                        task = "course_analysis",
                        inputSegmentCount = _state.value.segments.size,
                        hotwordCount = _state.value.hotwords.size,
                        success = false,
                        latencyMs = elapsed,
                        schemaValid = false,
                        evidenceMatchRate = null,
                        errorType = t::class.simpleName ?: "Unknown"
                    )
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.message ?: t::class.simpleName ?: "unknown error"
                    )
                }
            }
        }
    }

    private suspend fun buildProvider(name: String): ModelProvider {
        // v0.3 scope: only DemoProvider is wired. The other two throw on call;
        // we intentionally fall back here so the demo-flow walkthrough never
        // depends on a key being present.
        return when (name) {
            "demo" -> DemoProvider(DemoInputRepository.loadDemoOutputRaw(getApplication()))
            "bluelm", "compatible" -> {
                _state.update { it.copy(providerName = "demo") }
                Log.w(
                    UI_LOG_TAG,
                    "provider=$name not wired in v0.3; falling back to demo. Set provider=demo in config to silence."
                )
                DemoProvider(DemoInputRepository.loadDemoOutputRaw(getApplication()))
            }
            else -> DemoProvider(DemoInputRepository.loadDemoOutputRaw(getApplication()))
        }
    }

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
        // Idempotent: a second submit with the same choice is fine.
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
                providerName = it.providerName,
                configHint = it.configHint
            )
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return fmt.format(Date())
    }
}
