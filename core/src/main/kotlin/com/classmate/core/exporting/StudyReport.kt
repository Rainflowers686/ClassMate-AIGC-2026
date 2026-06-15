package com.classmate.core.exporting

import com.classmate.core.audio.CourseEssenceScript

/**
 * The single, render-agnostic data source for ALL export formats (Markdown / HTML / TXT / PDF /
 * Slides). It is a "study handout", NOT a UI dump: it contains only learning content — never UI state,
 * debug fields, JSON, provider bodies, prompts, messages, reasoning_content, or credentials.
 *
 * [StudyReportBuilder] fills it from the analysis + learning state; [StudyReportRenderer] turns it into
 * each printable format. Keeping one source means the formats can never drift apart.
 */
data class StudyReport(
    val courseTitle: String,
    val generatedAtLabel: String,
    val providerLabel: String,
    val sourceSummaryLine: String?,
    val transcriptSummaryLine: String?,
    val sourceTypeLabels: List<String>,
    val overview: List<String>,
    val reviewTopics: List<String>,
    val knowledgePoints: List<StudyKnowledgePoint>,
    val quizzes: List<StudyQuiz>,
    val needPractice: List<StudyPracticeItem>,
    val review: StudyReviewBuckets,
    val askItems: List<StudyAskItem>,
    val practice: StudyPracticeSummary? = null,
    val weaknesses: List<StudyWeaknessSummary> = emptyList(),
    val translationNotes: List<StudyTranslationNoteSummary> = emptyList(),
    val safetySummary: StudySafetySummary? = null,
    val courseEssenceScript: CourseEssenceScript? = null,
    /**
     * Optional on-device "local intelligence" study advice (Phase C). Either genuine on-device
     * BlueLM output (prefixed "由端侧 BlueLM 生成：") or the fixed safety placeholder. Never the
     * deterministic rule path dressed as advice; null when not generated.
     */
    val localSuggestion: String? = null,
)

/** Section 十: in-app adaptive practice + wrong-answer notebook summary. */
data class StudyPracticeSummary(
    val modeZh: String,
    val itemCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val masteredCount: Int,
    val needMorePracticeCount: Int,
    val nextSuggestion: String,
    val practicedTopics: List<String>,
    val needMoreTopics: List<String>,
    val searchQueries: List<String>,
    val recentRecords: List<String>,
)

data class StudyEvidenceRef(val quote: String, val sourceLabel: String)

data class StudyKnowledgePoint(
    val index: Int,
    val title: String,
    val summary: String,
    val importanceZh: String,
    val difficultyZh: String,
    val evidence: List<StudyEvidenceRef>,
    val studyTip: String,
)

data class StudyQuizOption(val label: String, val text: String, val correct: Boolean)

data class StudyQuiz(
    val index: Int,
    val typeZh: String,
    val stem: String,
    val options: List<StudyQuizOption>,
    val correctLabels: List<String>,
    val explanation: String,
    val relatedKnowledgePoints: List<String>,
    val evidenceQuotes: List<String>,
)

data class StudyPracticeItem(
    val title: String,
    val reason: String,
    val direction: String,
    val keywords: String,
    val searchLinks: List<String>,
)

data class StudyReviewBuckets(
    val dueToday: List<String>,
    val upcoming: List<String>,
    val mastered: List<String>,
    val needsRecheck: List<String>,
    val estimatedMinutes: Int,
)

data class StudyAskItem(
    val topic: String,
    val statusZh: String,
    val answerSummary: String,
    val evidenceQuotes: List<String>,
)

data class StudyWeaknessSummary(
    val title: String,
    val courseTitle: String,
    val wrongCount: Int,
    val correctCount: Int,
    val reason: String,
    val recommendedAction: String,
)

data class StudyTranslationNoteSummary(
    val targetTitle: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val translatedText: String,
)

data class StudySafetySummary(
    val status: String,
    val source: String,
    val note: String,
)
