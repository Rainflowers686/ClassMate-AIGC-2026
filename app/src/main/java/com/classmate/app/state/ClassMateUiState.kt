package com.classmate.app.state

import com.classmate.app.ui.theme.ThemeId
import com.classmate.core.evidence.EvidenceValidationResult
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.InputSegment
import com.classmate.core.model.KnowledgePoint
import com.classmate.core.model.Quiz
import com.classmate.core.model.ReviewPlanItem
import com.classmate.core.validation.ValidationIssue

/**
 * Single source of truth for v0.4 main-flow UI.
 *
 * v0.4 changes vs v0.3.5:
 *  - replaces single matchRate with strict + lenient
 *  - carries typed [ValidationIssue]s so the UI can render specific reasons
 *  - holds the current [themeId] so the user can switch from Settings
 *  - keeps fallback semantics (requested vs active vs fallbackUsed)
 */
data class ClassMateUiState(
    val screen: ClassMateScreen = ClassMateScreen.Home,

    // Theme ------------------------------------------------------------------
    val themeId: ThemeId = ThemeId.FocusGlass,

    // Input pipeline ---------------------------------------------------------
    val courseTitle: String = "",
    val courseText: String = "",
    val hotwords: List<String> = emptyList(),
    val pendingHotword: String = "",
    /** Segments fed to the provider. May be loaded from demo_input.json verbatim
     *  (preserving 1:1 ids/timings) or produced by Segmenter from courseText. */
    val segments: List<InputSegment> = emptyList(),

    // Analysis pipeline ------------------------------------------------------
    val requestedProvider: String = "local",
    val activeProvider: String = "local",
    val analysisResult: CourseAnalysisResult? = null,
    val evidenceValidation: EvidenceValidationResult? = null,
    val validationIssues: List<ValidationIssue> = emptyList(),
    val structureValid: Boolean? = null,
    val fallbackUsed: Boolean = false,
    val lastProviderError: String? = null,

    // Interaction state ------------------------------------------------------
    val selectedKnowledgePoint: KnowledgePoint? = null,
    val selectedEvidenceSegmentId: String? = null,
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

    val strictMatchRate: Double?
        get() = evidenceValidation?.strictEvidenceMatchRate

    val lenientMatchRate: Double?
        get() = evidenceValidation?.lenientEvidenceMatchRate
}

sealed interface ClassMateScreen {
    data object Home : ClassMateScreen
    data object CourseInput : ClassMateScreen
    data object Hotword : ClassMateScreen
    data object Analyze : ClassMateScreen
    data object Timeline : ClassMateScreen
    data object Quiz : ClassMateScreen
    data object ReviewPlan : ClassMateScreen
    data object Settings : ClassMateScreen
}
