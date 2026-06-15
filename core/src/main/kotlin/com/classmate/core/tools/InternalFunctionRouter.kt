package com.classmate.core.tools

import com.classmate.core.ask.EvidenceRetriever
import com.classmate.core.audio.CourseEssenceAudioExporter
import com.classmate.core.exporting.StudyReport
import com.classmate.core.exporting.StudyReportRenderer
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.learning.ReviewEngine
import com.classmate.core.learning.ReviewEventType
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.practice.PracticeGenerationRequest
import com.classmate.core.practice.PracticeMode
import com.classmate.core.practice.RoutedPracticeGenerationUseCase

enum class InternalToolName {
    SEARCH_EVIDENCE,
    CREATE_PRACTICE,
    UPDATE_MASTERY,
    CREATE_REVIEW_TASK,
    EXPORT_STUDY_REPORT,
    CREATE_ESSENCE_AUDIO_SCRIPT,
}

data class InternalToolCall(
    val name: InternalToolName,
    val courseTitle: String = "",
    val query: String = "",
    val knowledgePointId: String = "",
    val eventType: ReviewEventType? = null,
    val now: Long = 0L,
)

data class InternalToolResult(
    val name: InternalToolName,
    val success: Boolean,
    val summary: String,
    val requiresUserConfirmation: Boolean,
    val payloadCount: Int = 0,
)

class InternalFunctionRouter(
    private val practiceGeneration: RoutedPracticeGenerationUseCase = RoutedPracticeGenerationUseCase(),
) {
    fun execute(
        call: InternalToolCall,
        result: CourseAnalysisResult,
        snapshot: LearningSnapshot,
        report: StudyReport? = null,
    ): InternalToolResult = when (call.name) {
        InternalToolName.SEARCH_EVIDENCE -> searchEvidence(call, result)
        InternalToolName.CREATE_PRACTICE -> createPractice(call, result, snapshot)
        InternalToolName.UPDATE_MASTERY -> updateMastery(call, result, snapshot)
        InternalToolName.CREATE_REVIEW_TASK -> createReviewTask(call, result, snapshot)
        InternalToolName.EXPORT_STUDY_REPORT -> exportStudyReport(report)
        InternalToolName.CREATE_ESSENCE_AUDIO_SCRIPT -> createEssenceAudioScript(report)
    }

    private fun searchEvidence(call: InternalToolCall, result: CourseAnalysisResult): InternalToolResult {
        val hits = EvidenceRetriever.retrieve(call.query, result)
        return InternalToolResult(
            name = call.name,
            success = hits.isNotEmpty(),
            summary = "evidence_hits=${hits.size}",
            requiresUserConfirmation = false,
            payloadCount = hits.size,
        )
    }

    private fun createPractice(call: InternalToolCall, result: CourseAnalysisResult, snapshot: LearningSnapshot): InternalToolResult {
        val generated = practiceGeneration.generate(
            PracticeGenerationRequest(
                result = result,
                snapshot = snapshot,
                mode = PracticeMode.QUICK_REVIEW,
                now = call.now.takeIf { it > 0 } ?: System.currentTimeMillis(),
                courseTitle = call.courseTitle,
            ),
        )
        val count = generated.value?.session?.items?.size ?: 0
        return InternalToolResult(
            name = call.name,
            success = count > 0,
            summary = "practice_items=$count source=${generated.source.name}",
            requiresUserConfirmation = true,
            payloadCount = count,
        )
    }

    private fun updateMastery(call: InternalToolCall, result: CourseAnalysisResult, snapshot: LearningSnapshot): InternalToolResult {
        val kpId = call.knowledgePointId.ifBlank { result.knowledgePoints.firstOrNull()?.id.orEmpty() }
        val type = call.eventType ?: ReviewEventType.CORRECT_ANSWER
        val next = ReviewEngine.recordFeedback(snapshot, result.sessionId, kpId, type, call.now.takeIf { it > 0 } ?: System.currentTimeMillis())
        val changed = next.events.size > snapshot.events.size
        return InternalToolResult(call.name, changed, "mastery_event=$type", requiresUserConfirmation = true, payloadCount = if (changed) 1 else 0)
    }

    private fun createReviewTask(call: InternalToolCall, result: CourseAnalysisResult, snapshot: LearningSnapshot): InternalToolResult {
        val next = ReviewEngine.generateTasks(
            s = snapshot,
            result = result,
            courseTitle = call.courseTitle,
            sourceProvider = "INTERNAL_TOOL",
            sourceProfile = "internal",
            sourceModel = "",
            now = call.now.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
        val added = next.tasks.size - snapshot.tasks.size
        return InternalToolResult(call.name, added > 0, "review_tasks_added=$added", requiresUserConfirmation = true, payloadCount = added)
    }

    private fun exportStudyReport(report: StudyReport?): InternalToolResult {
        val markdown = report?.let { StudyReportRenderer.renderMarkdown(it) }.orEmpty()
        return InternalToolResult(
            name = InternalToolName.EXPORT_STUDY_REPORT,
            success = markdown.isNotBlank(),
            summary = "markdown_chars=${markdown.length}",
            requiresUserConfirmation = true,
            payloadCount = if (markdown.isBlank()) 0 else 1,
        )
    }

    private fun createEssenceAudioScript(report: StudyReport?): InternalToolResult {
        val script = report?.let { CourseEssenceAudioExporter.buildScript(it) }
        val lines = script?.toPlainText()?.lineSequence()?.count() ?: 0
        return InternalToolResult(
            name = InternalToolName.CREATE_ESSENCE_AUDIO_SCRIPT,
            success = lines > 0,
            summary = "script_lines=$lines",
            requiresUserConfirmation = true,
            payloadCount = lines,
        )
    }
}
