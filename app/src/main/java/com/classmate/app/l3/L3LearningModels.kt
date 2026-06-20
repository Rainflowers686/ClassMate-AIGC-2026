package com.classmate.app.l3

import com.classmate.core.model.Difficulty

enum class L3SourceType {
    TEXT,
    OCR_IMAGE,
    DOCUMENT,
    AUDIO_TRANSCRIPT,
    MANUAL_TRANSCRIPT,
    RECORDING_ARTIFACT,
    QUESTION_BANK,
    WEB,
}

enum class EvidenceAssetType {
    TEXT,
    OCR_IMAGE,
    DOCUMENT,
    AUDIO,
    WEB,
    UNKNOWN,
}

enum class LearningLoopInputKind {
    TEXT,
    MARKDOWN,
    OCR_IMAGE,
    DOCUMENT,
    AUDIO_TRANSCRIPT,
    MANUAL_TRANSCRIPT,
    QUESTION_BANK,
    WEB,
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
    OFFICIAL_ASR_NOT_CONFIGURED,
    OFFICIAL_ASR_CONFIG_MISSING,
    OFFICIAL_ASR_ADAPTER_READY,
    OFFICIAL_ASR_APP_WIRING_PENDING,
    CORE_CONTRACT_PRESENT_APP_WIRING_PENDING,
    UPLOAD_PENDING,
    UPLOAD_FAILED,
    POLLING_PENDING,
    QUEUED,
    PROCESSING,
    ASR_FAILED,
    TRANSCRIPT_READY,
    MANUAL_TRANSCRIPT_FALLBACK,
}

enum class ExtractedTextRecommendedStatus {
    COMPLETE,
    PARTIAL,
    TEMPLATE_REQUIRED,
    PARSER_PENDING,
    EMPTY_FILE,
    FORMAT_ERROR,
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
    PAGE_READY,
    PAGE_OCR_SEAM_READY,
    OCR_PENDING,
    OCR_FAILED,
    OCR_TEXT_READY,
    MANUAL_PAGE_TEXT_READY,
}

enum class PracticeGradingStatus {
    CORRECT,
    WRONG,
    PARTIAL,
    SELF_ASSESSMENT_REQUIRED,
    AI_GRADING_SEAM_ONLY,
}

enum class PdfDocumentStatus {
    PDF_ARTIFACT_READY,
    PDF_TEXT_PARSER_PENDING,
    PAGE_OCR_SEAM_READY,
    MANUAL_PAGE_TEXT_READY,
}

enum class TtsPlaybackSourceType {
    SUMMARY,
    WRONG_QUESTION_EXPLANATION,
    REVIEW_CARD,
}

enum class TtsPlaybackProvider {
    OFFICIAL_TTS,
    ANDROID_LOCAL_TTS,
    NONE,
}

enum class TtsPlaybackStatus {
    OFFICIAL_TTS_READY,
    OFFICIAL_TTS_NOT_CONFIGURED,
    LOCAL_TTS_AVAILABLE,
    LOCAL_TTS_UNAVAILABLE,
    PLAYING,
    STOPPED,
    FAILED,
}

enum class TranslationTargetLanguage {
    ENGLISH,
    CHINESE,
    JAPANESE,
    KOREAN,
}

enum class TranslationProductStatus {
    OFFICIAL_TRANSLATION_READY,
    OFFICIAL_TRANSLATION_NOT_CONFIGURED,
    PENDING,
    FAILED,
}

enum class ToolInputType {
    TEXT,
    IMAGE,
    AUDIO,
    PDF,
    QUESTION_BANK,
}

enum class ToolProviderMode {
    OFFICIAL,
    LOCAL_FALLBACK,
    SEAM_ONLY,
    NOT_CONFIGURED,
}

enum class ToolStepStatus {
    PLANNED,
    EXECUTED,
    SKIPPED,
    FAILED,
}

enum class MasteryHistoryEventType {
    ANSWER_CORRECT,
    ANSWER_WRONG,
    REVIEW_DONE,
    SELF_ASSESSED,
    MASTERED,
    DECAYED,
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
    val providerProvenance: String = "",
    val assetId: String? = null,
    val sourceLabel: String = "",
    val fileName: String = "",
    val fileExt: String = "",
    val mimeType: String = "",
    val localUri: String = "",
    val thumbnailRef: String = "",
    val imageRef: String = "",
    val audioRef: String = "",
    val pageHint: String = "",
    val segmentHint: String = "",
    val transcriptSegment: String = "",
    val snippet: String = "",
)

data class EvidenceAsset(
    val id: String,
    val type: EvidenceAssetType,
    val sourceType: L3SourceType,
    val text: String = "",
    val sourceLabel: String = "",
    val fileName: String = "",
    val fileExt: String = "",
    val mimeType: String = "",
    val localUri: String = "",
    val thumbnailRef: String = "",
    val imageRef: String = "",
    val audioRef: String = "",
    val pageHint: String = "",
    val segmentHint: String = "",
    val startMs: Long? = null,
    val endMs: Long? = null,
    val createdAt: Long = 0L,
    val status: String = "READY",
    val transcriptSegment: String = "",
    val snippet: String = "",
)

data class LearningLoopInput(
    val id: String,
    val title: String,
    val kind: LearningLoopInputKind,
    val sourceType: L3SourceType,
    val text: String,
    val evidenceAssets: List<EvidenceAsset> = emptyList(),
    val sourceLabel: String = "",
    val providerProvenance: String = "",
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
    val scoreSource: String = providerStatus,
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

data class LocalSemanticIndexRecord(
    val id: String,
    val sourceType: String,
    val sourceId: String,
    val ownerType: String,
    val ownerId: String,
    val text: String,
    val embeddingStatus: String,
    val vector: List<Double>,
    val tokens: List<String>,
    val createdAt: Long,
    val officialVector: List<Double> = emptyList(),
    val localVector: List<Double> = vector,
    val vectorSource: String = embeddingStatus,
)

data class SemanticSearchHit(
    val recordId: String,
    val ownerType: String,
    val ownerId: String,
    val text: String,
    val score: Double,
    val providerStatus: String,
)

data class SemanticSearchResult(
    val query: String,
    val hits: List<SemanticSearchHit>,
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
    val providerStatus: String = "",
    val uploadStatus: String = "",
    val pollingStatus: String = "",
    val transcriptSegments: List<TranscriptSegment> = emptyList(),
    val errorCode: String? = null,
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

data class PdfDocumentArtifact(
    val id: String,
    val artifactId: String,
    val fileName: String,
    val pageCount: Int,
    val status: PdfDocumentStatus,
    val parserStatus: String,
    val createdAt: Long,
    val message: String,
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
    val qualityStatus: String = "",
    val qualityMessage: String = "",
)

data class ExtractedTextQuality(
    val textLength: Int,
    val nonWhitespaceCount: Int,
    val suspiciousCharRatio: Double,
    val replacementCharCount: Int,
    val lineCount: Int,
    val isLikelyReadable: Boolean,
    val recommendedStatus: ExtractedTextRecommendedStatus,
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
    val accuracy: Double = if (correctCount + wrongCount == 0) 0.0 else correctCount.toDouble() / (correctCount + wrongCount),
    val questionBreakdown: List<String> = emptyList(),
    val knowledgePointBreakdown: Map<String, String> = emptyMap(),
    val evidenceCoverage: Double = 0.0,
    val recommendedReviewItems: List<String> = emptyList(),
    val markdownReport: String = "",
    val generatedAt: Long = 0L,
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

data class MasteryHistoryEvent(
    val id: String,
    val knowledgePointId: String,
    val eventType: MasteryHistoryEventType,
    val oldState: L3MasteryState,
    val newState: L3MasteryState,
    val createdAt: Long,
    val sourceQuestionId: String? = null,
    val sourceLessonId: String? = null,
)

data class MasteryTrendStats(
    val dailyCorrectCount: Int,
    val dailyWrongCount: Int,
    val weakCountTrend: List<Int>,
    val masteredCountTrend: List<Int>,
    val reviewCompletionStreak: Int,
    val lapseCount: Int,
    val recentSevenDaySummary: String,
)

data class TtsPlaybackState(
    val id: String,
    val text: String,
    val sourceType: TtsPlaybackSourceType,
    val provider: TtsPlaybackProvider,
    val status: TtsPlaybackStatus,
    val message: String,
    val createdAt: Long,
)

data class TranslationRequestRecord(
    val id: String,
    val sourceText: String,
    val sourceLanguage: String,
    val targetLanguage: TranslationTargetLanguage,
    val keepTerms: List<String>,
    val createdAt: Long,
)

data class TranslationResultRecord(
    val request: TranslationRequestRecord,
    val status: TranslationProductStatus,
    val translatedText: String = "",
    val errorMessage: String? = null,
    val evidenceId: String? = null,
)

data class ToolStepRecord(
    val id: String,
    val toolName: String,
    val status: ToolStepStatus,
    val inputSummary: String,
    val outputSummary: String,
    val providerMode: ToolProviderMode,
    val createdAt: Long,
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
    val evidenceAssets: List<EvidenceAsset> = emptyList(),
    val knowledgePoints: List<L3KnowledgePoint> = emptyList(),
    val questions: List<L3GeneratedQuestion> = emptyList(),
    val attempts: List<L3PracticeAttempt> = emptyList(),
    val wrongBook: List<WrongQuestionRecord> = emptyList(),
    val reviewQueue: List<ReviewQueueItem> = emptyList(),
    val masteryStats: List<MasteryStat> = emptyList(),
    val stepLogs: List<PipelineStepLog> = emptyList(),
    val embeddingRecords: List<EmbeddingRecord> = emptyList(),
    val semanticIndexChunks: List<SemanticIndexChunk> = emptyList(),
    val semanticIndexRecords: List<LocalSemanticIndexRecord> = emptyList(),
    val semanticSearchResults: List<SemanticSearchResult> = emptyList(),
    val similarityMatches: List<TextSimilarityMatch> = emptyList(),
    val knowledgeGraphEdges: List<KnowledgeGraphEdge> = emptyList(),
    val similarQuestionRecommendations: List<SimilarQuestionRecommendation> = emptyList(),
    val inputArtifacts: List<InputArtifact> = emptyList(),
    val inputReports: List<ImportReport> = emptyList(),
    val pdfDocuments: List<PdfDocumentArtifact> = emptyList(),
    val pdfPages: List<PdfPageArtifact> = emptyList(),
    val asrJobs: List<AsrLongJob> = emptyList(),
    val officialToolSeams: List<OfficialToolSeam> = emptyList(),
    val toolOrchestrationPlan: ToolOrchestrationPlan? = null,
    val toolStepRecords: List<ToolStepRecord> = emptyList(),
    val ttsPlaybackStates: List<TtsPlaybackState> = emptyList(),
    val translationResults: List<TranslationResultRecord> = emptyList(),
    val masteryHistory: List<MasteryHistoryEvent> = emptyList(),
    val masteryTrendStats: MasteryTrendStats = MasteryTrendStats(
        dailyCorrectCount = 0,
        dailyWrongCount = 0,
        weakCountTrend = emptyList(),
        masteredCountTrend = emptyList(),
        reviewCompletionStreak = 0,
        lapseCount = 0,
        recentSevenDaySummary = "",
    ),
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
