package com.classmate.app.state

import com.classmate.app.ui.theme.ThemeOption
import com.classmate.core.logging.RedactedLogEntry
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.FeedbackEvent
import com.classmate.core.model.LearningState
import com.classmate.core.model.ReviewPlan

/** All UI state in one immutable snapshot, updated via copy() from [AppViewModel]. */
data class ClassMateUiState(
    // appearance
    val theme: ThemeOption = ThemeOption.Default,
    val darkMode: Boolean? = null, // null = follow system

    // import inputs
    val courseTitle: String = "",
    val courseText: String = "",

    // analysis
    val session: CourseSession? = null,
    val analysisStatus: AnalysisStatus = AnalysisStatus.IDLE,
    val analysisStageIndex: Int = 0,
    val result: CourseAnalysisResult? = null,
    val logs: List<RedactedLogEntry> = emptyList(),
    val analysisError: String? = null,

    // learning loop
    val learningState: LearningState? = null,
    val answers: Map<String, String> = emptyMap(), // questionId -> selected optionId
    val revealedQuestionIds: Set<String> = emptySet(),
    val currentQuestionIndex: Int = 0,
    val feedbackEvents: List<FeedbackEvent> = emptyList(),
    val reviewPlan: ReviewPlan? = null,

    // navigation context
    val selectedKnowledgePointId: String? = null,

    // transient
    val toast: String? = null,
) {
    val answeredCount: Int get() = revealedQuestionIds.size
}
