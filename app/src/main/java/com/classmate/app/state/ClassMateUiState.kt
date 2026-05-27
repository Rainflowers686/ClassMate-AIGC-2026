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
    val providerName: String = "demo",
    val analysisResult: CourseAnalysisResult? = null,
    val evidenceValidation: EvidenceValidationResult? = null,

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
