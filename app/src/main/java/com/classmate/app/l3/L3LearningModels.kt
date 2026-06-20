package com.classmate.app.l3

import com.classmate.core.model.Difficulty

enum class L3SourceType {
    TEXT,
    OCR_IMAGE,
    AUDIO_TRANSCRIPT,
    MANUAL_TRANSCRIPT,
    RECORDING_ARTIFACT,
    QUESTION_BANK,
}

enum class L3MasteryState {
    UNKNOWN,
    LEARNING,
    REVIEWING,
    MASTERED,
    WEAK,
}

enum class L3AsrStatus {
    PENDING_ASR_CONFIG,
    ASR_NOT_CONFIGURED,
    QUEUED,
    PROCESSING,
    ASR_FAILED,
    TRANSCRIPT_READY,
    MANUAL_TRANSCRIPT_FALLBACK,
}

enum class KnowledgeGraphRelation {
    PREREQUISITE,
    RELATED,
    CONTRAST,
    EXAMPLE,
}

enum class L3RecordingStatus {
    IDLE,
    RECORDING,
    SAVED,
    FAILED,
}

enum class PracticeQuestionMode {
    REAL_QUIZ,
    SELF_ASSESSMENT,
    EXAM,
}

enum class PracticeQuestionType {
    SINGLE_CHOICE,
    TRUE_FALSE,
    MULTI_CHOICE,
    SHORT_ANSWER,
}

enum class PracticeAnswerState {
    NOT_ANSWERED,
    ANSWER_SELECTED,
    SUBMITTED_CORRECT,
    SUBMITTED_WRONG,
    REVEALED,
    SKIPPED,
}

enum class ExamStatus {
    NOT_STARTED,
    IN_PROGRESS,
    SUBMITTED,
}

enum class PdfPageStatus {
    PDF_ARTIFACT_READY,
    PDF_TEXT_PARSER_PENDING,
    PAGE_OCR_SEAM_READY,
    MANUAL_PAGE_TEXT_READY,
}

enum class PracticeGradingStatus {
    CORRECT,
    WRONG,
    PARTIAL,
    SELF_ASSESSMENT_REQUIRED,
    AI_GRADING_SEAM_ONLY,
}

data class LessonSource(
    val id: String,
    val title: String,
    val type: L3SourceType,
    val createdAt: Long,
    val rawText: String,
    val status: String,
)

data class TranscriptSegment(
    val segmentId: String,
    val sourceId: String,
    val startMs: Long?,
    val endMs: Long?,
    val text: String,
    val sourceType: L3SourceType,
    val confidence: Double? = null,
    val fallbackGenerated: Boolean = false,
)

data class Evidence(
    val id: String,
    val sourceId: String,
    val sourceType: L3SourceType,
    val text: String,
    val segmentStartMs: Long? = null,
    val segmentEndMs: Long? = null,
    val page: Int? = null,
    val blockIndex: Int? = null,
)

data class L3KnowledgePoint(
    val id: String,
    val title: String,
    val explanation: String,
    val sourceEvidenceIds: List<String>,
    val masteryState: L3MasteryState,
)

data class L3GeneratedQuestion(
    val id: String,
    val lessonId: String,
    val knowledgePointId: String,
    val stem: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val evidenceIds: List<String>,
    val difficulty: Difficulty,
)

data class L3PracticeAttempt(
    val id: String,
    val questionId: String,
    val userAnswer: String,
    val correct: Boolean,
    val createdAt: Long,
    val selectedAnswers: List<String> = if (userAnswer.isBlank()) emptyList() else listOf(userAnswer),
    val textAnswer: String? = null,
    val elapsedMs: Long? = null,
    val mode: PracticeQuestionMode = PracticeQuestionMode.REAL_QUIZ,
)

data class PracticeAnswerSubmission(
    val itemId: String,
    val questionId: String,
    val selectedAnswers: List<String>,
    val textAnswer: String? = null,
    val correct: Boolean,
    val submittedAt: Long,
    val elapsedMs: Long? = null,
    val mode: PracticeQuestionMode,
    val state: PracticeAnswerState,
)

data class ExamSession(
    val id: String,
    val sourceLessonId: String? = null,
    val questionBankId: String? = null,
    val questionIds: List<String>,
    val startedAt: Long,
    val submittedAt: Long? = null,
    val status: ExamStatus = ExamStatus.IN_PROGRESS,
    val score: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
)

data class WrongQuestionRecord(
    val id: String,
    val questionId: String,
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val knowledgePointId: String,
    val evidenceIds: List<String>,
    val createdAt: Long,
    val retryCount: Int,
)

data class ReviewQueueItem(
    val id: String,
    val knowledgePointId: String,
    val dueAt: Long,
    val masteryState: L3MasteryState,
    val sourceLessonId: String,
    val priority: Int = 1,
    val source: String = "L3_PIPELINE",
)

data class MasteryStat(
    val knowledgePointId: String,
    val state: L3MasteryState,
    val correctCount: Int,
    val wrongCount: Int,
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long? = null,
    val sourceLessonId: String = "",
)

data class PipelineStepLog(
    val id: String,
    val lessonId: String,
    val step: String,
    val provider: String,
    val status: String,
    val message: String,
    val createdAt: Long,
)

data class EmbeddingRecord(
    val id: String,
    val ownerType: String,
    val ownerId: String,
    val providerStatus: String,
)

data class TextSimilarityMatch(
    val id: String,
    val leftId: String,
    val rightId: String,
    val score: Double,
    val providerStatus: String,
)

data class SemanticIndexChunk(
    val id: String,
    val sourceId: String,
    val ownerType: String,
    val ownerId: String,
    val text: String,
    val vector: List<Double>,
    val status: String,
)

data class KnowledgeGraphEdge(
    val id: String,
    val fromKnowledgePointId: String,
    val toKnowledgePointId: String,
    val relation: KnowledgeGraphRelation,
    val evidenceIds: List<String>,
)

data class SimilarQuestionRecommendation(
    val id: String,
    val sourceQuestionId: String,
    val recommendedQuestionId: String,
    val score: Double,
    val status: String,
)

data class AsrLongJob(
    val id: String,
    val audioArtifactId: String,
    val status: L3AsrStatus,
    val transcriptText: String = "",
    val errorMessage: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class L3CapabilityStatus(
    val capability: String,
    val status: String,
    val message: String,
)

data class PdfPageArtifact(
    val id: String,
    val artifactId: String,
    val pageNumber: Int,
    val status: PdfPageStatus,
    val manualText: String = "",
    val ocrStatus: String = "PAGE_OCR_SEAM_READY",
    val evidenceId: String? = null,
)

data class ImportReport(
    val id: String,
    val sourceType: InputFileKind,
    val successCount: Int,
    val warningCount: Int,
    val failedItems: List<String>,
    val fallbackUsed: Boolean,
    val nextAction: String,
    val createdAt: Long,
)

data class PracticeGrade(
    val status: PracticeGradingStatus,
    val correct: Boolean,
    val partial: Boolean,
    val selectedAnswers: List<String>,
    val correctAnswers: List<String>,
    val message: String,
)

data class DistractorExplanation(
    val questionId: String,
    val optionId: String,
    val status: String,
    val explanation: String,
)

data class ExamResultReport(
    val id: String,
    val examSessionId: String,
    val score: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val elapsedMs: Long,
    val weakKnowledgePointIds: List<String>,
    val wrongQuestionIds: List<String>,
    val evidenceIds: List<String>,
)

data class ReviewDailyStats(
    val dueToday: Int,
    val weakCount: Int,
    val wrongQuestionCount: Int,
    val masteredCount: Int,
    val overdueCount: Int,
    val totalKnowledgePoints: Int,
    val distribution: Map<L3MasteryState, Int>,
)

data class ClassroomRecordingRecord(
    val id: String,
    val title: String,
    val createdAt: Long,
    val endedAt: Long? = null,
    val durationMs: Long = 0L,
    val status: L3RecordingStatus = L3RecordingStatus.IDLE,
    val artifactFileName: String? = null,
    val artifactPath: String? = artifactFileName,
    val asrStatus: L3AsrStatus = L3AsrStatus.PENDING_ASR_CONFIG,
    val message: String = "",
)

data class L3QuestionBank(
    val id: String,
    val title: String,
    val questions: List<L3GeneratedQuestion>,
    val sourceText: String,
    val importedAt: Long,
)

data class QuestionBankParseResult(
    val accepted: Boolean,
    val bank: L3QuestionBank? = null,
    val errors: List<String> = emptyList(),
) {
    val message: String
        get() = if (accepted) {
            "已解析 ${bank?.questions?.size ?: 0} 道题。"
        } else {
            errors.joinToString("；").ifBlank { "题库格式错误。" }
        }
}

data class L3PipelineSnapshot(
    val lessonSource: LessonSource? = null,
    val transcriptSegments: List<TranscriptSegment> = emptyList(),
    val summary: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val reviewFocus: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val evidence: List<Evidence> = emptyList(),
    val knowledgePoints: List<L3KnowledgePoint> = emptyList(),
    val questions: List<L3GeneratedQuestion> = emptyList(),
    val attempts: List<L3PracticeAttempt> = emptyList(),
    val wrongBook: List<WrongQuestionRecord> = emptyList(),
    val reviewQueue: List<ReviewQueueItem> = emptyList(),
    val masteryStats: List<MasteryStat> = emptyList(),
    val stepLogs: List<PipelineStepLog> = emptyList(),
    val embeddingRecords: List<EmbeddingRecord> = emptyList(),
    val semanticIndexChunks: List<SemanticIndexChunk> = emptyList(),
    val similarityMatches: List<TextSimilarityMatch> = emptyList(),
    val knowledgeGraphEdges: List<KnowledgeGraphEdge> = emptyList(),
    val similarQuestionRecommendations: List<SimilarQuestionRecommendation> = emptyList(),
    val inputArtifacts: List<InputArtifact> = emptyList(),
    val inputReports: List<ImportReport> = emptyList(),
    val pdfPages: List<PdfPageArtifact> = emptyList(),
    val asrJobs: List<AsrLongJob> = emptyList(),
    val officialToolSeams: List<OfficialToolSeam> = emptyList(),
    val toolOrchestrationPlan: ToolOrchestrationPlan? = null,
    val reviewDailyStats: ReviewDailyStats = ReviewDailyStats(
        dueToday = 0,
        weakCount = 0,
        wrongQuestionCount = 0,
        masteredCount = 0,
        overdueCount = 0,
        totalKnowledgePoints = 0,
        distribution = emptyMap(),
    ),
    val examReports: List<ExamResultReport> = emptyList(),
    val distractorExplanations: List<DistractorExplanation> = emptyList(),
    val diagnostics: List<L3CapabilityStatus> = emptyList(),
    val questionBank: L3QuestionBank? = null,
    val supportSeams: List<PipelineStepLog> = emptyList(),
) {
    companion object {
        val Empty = L3PipelineSnapshot()
    }
}
