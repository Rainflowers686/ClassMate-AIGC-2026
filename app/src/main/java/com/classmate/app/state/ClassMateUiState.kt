package com.classmate.app.state

import com.classmate.core.evidence.EvidenceValidationResult
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.InputSegment
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.Quiz
import com.classmate.core.model.ReviewPlanItem

/**
 * Single source of truth for v0.3 main-flow UI.
 *
 * Kept as one immutable struct rather than ten separate StateFlows because
 * the screens mostly need cross-cutting reads (e.g. TimelineScreen wants
 * segments + analysisResult + wrongKnowledgePointIds together). Copy-on-write
 * is fine at this size; if it ever stops being fine, split per-screen.
 *
 * v0.3.5 adds three provider-call diagnostics:
 *   - [requestedProvider]: what config.local.json asked for
 *   - [activeProvider]: what actually ran (may differ if fallback fired)
 *   - [fallbackUsed], [structureValid] — surfaced on AnalyzeScreen so
 *     reviewers can tell a real call apart from a Demo response.
 */
data class ClassMateUiState(
    /** Currently visible screen — drives AppRoot's `when` dispatch. */
    val screen: ClassMateScreen = ClassMateScreen.Home,

    // Input pipeline ---------------------------------------------------------
    val courseTitle: String = "",
    val courseText: String = "",
    val hotwords: List<String> = emptyList(),
    val pendingHotword: String = "",
    val segments: List<InputSegment> = emptyList(),

    // Analysis pipeline ------------------------------------------------------
    /** Provider name from config.local.json. Stays stable across runs. */
    val requestedProvider: String = "demo",
    /** Provider that actually produced [analysisResult]. Differs from requested when fallback fires. */
    val activeProvider: String = "demo",
    val analysisResult: CourseAnalysisResult? = null,
    val evidenceValidation: EvidenceValidationResult? = null,
    /** Result of ResultValidator on the model output. Null until a call completes. */
    val structureValid: Boolean? = null,
    /** True iff the analysis pipeline fell back to a lower-priority provider. */
    val fallbackUsed: Boolean = false,
    /** Last provider-call failure reason (verbatim from ModelCallException) — null on success. */
    val lastProviderError: String? = null,

    // Interaction state ------------------------------------------------------
    val selectedKnowledgePoint: KnowledgePoint? = null,
    val selectedEvidenceSegmentId: String? = null,
    /** quizId -> chosen answer index. Absent key = unanswered. */
    val quizAnswers: Map<String, Int> = emptyMap(),
    val wrongKnowledgePointIds: Set<String> = emptySet(),
    val currentQuizIndex: Int = 0,

    // Status -----------------------------------------------------------------
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val configHint: String = ""
) {
    val knowledgePoints: List<KnowledgePoint>
        get() = analysisResult?.segments?.flatMap { it.knowledgePoints }.orEmpty()

    val quizzes: List<Quiz>
        get() = analysisResult?.quizzes.orEmpty()

    val reviewPlan: List<ReviewPlanItem>
        get() = analysisResult?.reviewPlan.orEmpty()

    /** Convenience for the AnalyzeScreen header — back-compat alias. */
    val providerName: String get() = activeProvider
}

/**
 * Screen identity. Sealed so AppRoot's `when` is exhaustive — adding a new
 * screen forces an update at the dispatch site.
 */
sealed interface ClassMateScreen {
    data object Home : ClassMateScreen
    data object CourseInput : ClassMateScreen
    data object Hotword : ClassMateScreen
    data object Analyze : ClassMateScreen
    data object Timeline : ClassMateScreen
    data object Quiz : ClassMateScreen
    data object ReviewPlan : ClassMateScreen
}
