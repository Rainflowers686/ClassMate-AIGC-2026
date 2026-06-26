package com.classmate.core.exporting

import com.classmate.core.ask.LessonAnswer
import com.classmate.core.learning.LearningSnapshot
import com.classmate.core.library.CourseSummary
import com.classmate.core.mindmap.MindMapModel
import com.classmate.core.model.CourseAnalysisResult
import com.classmate.core.model.CourseSession
import com.classmate.core.model.ReviewPlan
import com.classmate.core.video.VideoRecommendation
import com.classmate.core.weakness.WeaknessItem

enum class ExportFormat(val extension: String, val mimeType: String) {
    MARKDOWN("md", "text/markdown; charset=utf-8"),
    HTML("html", "text/html; charset=utf-8"),
    PLAIN_TEXT("txt", "text/plain; charset=utf-8"),
}

data class ExportDocument(
    val fileName: String,
    val format: ExportFormat,
    val mimeType: String,
    val content: String,
) {
    val length: Int get() = content.toByteArray(Charsets.UTF_8).size
}

class ContentExporter {
    fun exportTimeline(
        session: CourseSession,
        result: CourseAnalysisResult,
        format: ExportFormat = ExportFormat.MARKDOWN,
    ): ExportDocument = document(session.title, "timeline", format) {
        when (format) {
            ExportFormat.MARKDOWN -> markdownTimeline(session, result)
            ExportFormat.HTML -> htmlPage(session.title, markdownTimeline(session, result))
            ExportFormat.PLAIN_TEXT -> plainTimeline(session, result)
        }
    }

    fun exportQuiz(
        session: CourseSession,
        result: CourseAnalysisResult,
        format: ExportFormat = ExportFormat.MARKDOWN,
    ): ExportDocument = document(session.title, "quiz", format) {
        when (format) {
            ExportFormat.MARKDOWN -> markdownQuiz(result)
            ExportFormat.HTML -> htmlPage("${session.title} quiz", markdownQuiz(result))
            ExportFormat.PLAIN_TEXT -> plainQuiz(result)
        }
    }

    fun exportReviewPlan(
        session: CourseSession,
        plan: ReviewPlan,
        format: ExportFormat = ExportFormat.MARKDOWN,
    ): ExportDocument = document(session.title, "review-plan", format) {
        when (format) {
            ExportFormat.MARKDOWN -> markdownReviewPlan(plan)
            ExportFormat.HTML -> htmlPage("${session.title} review plan", markdownReviewPlan(plan))
            ExportFormat.PLAIN_TEXT -> plainReviewPlan(plan)
        }
    }

    fun exportFullReport(
        session: CourseSession,
        result: CourseAnalysisResult,
        reviewPlan: ReviewPlan?,
        mindMap: MindMapModel,
        videoRecommendations: List<VideoRecommendation>,
        format: ExportFormat = ExportFormat.MARKDOWN,
        courseSummaries: List<CourseSummary> = emptyList(),
        learningSnapshot: LearningSnapshot = LearningSnapshot(),
        weaknesses: List<WeaknessItem> = emptyList(),
        askAnswers: List<LessonAnswer> = emptyList(),
    ): ExportDocument = document(session.title, "full-report", format) {
        val markdown = buildString {
            appendLine("# ${session.title.ifBlank { "ClassMate learning report" }}")
            appendLine()
            appendLine("- Provider: ${result.provenance.provider.name}")
            appendLine("- Fallback used: ${result.provenance.fallbackUsed}")
            appendLine("- Knowledge points: ${result.knowledgePoints.size}")
            appendLine("- Quiz questions: ${result.quizQuestions.size}")
            appendLine()
            appendLine(markdownTimeline(session, result))
            appendLine()
            appendLine(markdownCourseLibrary(courseSummaries))
            appendLine()
            appendLine(markdownKnowledgePoints(session, result))
            appendLine()
            appendLine(markdownQuiz(result))
            appendLine()
            if (reviewPlan != null) {
                appendLine(markdownReviewPlan(reviewPlan))
                appendLine()
            }
            appendLine(markdownReviewTasks(learningSnapshot))
            appendLine()
            appendLine(markdownWeaknesses(weaknesses))
            appendLine()
            appendLine(markdownMindMap(mindMap))
            appendLine()
            appendLine(markdownVideos(videoRecommendations))
            appendLine()
            appendLine(markdownAskAnswers(askAnswers))
        }
        when (format) {
            ExportFormat.MARKDOWN -> markdown
            ExportFormat.HTML -> htmlPage("${session.title} learning report", markdown)
            ExportFormat.PLAIN_TEXT -> markdownToPlain(markdown)
        }
    }

    fun exportReviewQueue(
        snapshot: LearningSnapshot,
        format: ExportFormat = ExportFormat.MARKDOWN,
    ): ExportDocument = document("ClassMate review", "review-queue", format) {
        val markdown = buildString {
            appendLine("# Review queue")
            appendLine()
            if (snapshot.tasks.isEmpty()) {
                appendLine("No review tasks.")
            } else {
                snapshot.tasks.forEachIndexed { index, task ->
                    appendLine("${index + 1}. ${task.title}")
                    appendLine("   - Course: ${task.courseTitle}")
                    appendLine("   - Priority: ${task.priority}")
                    appendLine("   - Wrong answers: ${task.counters.wrongAnswer}")
                    appendLine("   - Need examples: ${task.counters.needExample}")
                }
            }
        }
        when (format) {
            ExportFormat.MARKDOWN -> markdown
            ExportFormat.HTML -> htmlPage("ClassMate review queue", markdown)
            ExportFormat.PLAIN_TEXT -> markdownToPlain(markdown)
        }
    }

    fun markdownMindMap(mindMap: MindMapModel): String = buildString {
        appendLine("## 思维导图")
        appendLine()
        appendLine("- ${mindMap.root}")
        mindMap.children.forEach { node ->
            appendLine("  - ${node.title}")
            appendLine("    - 重要性：${node.importance} · 难度：${node.difficulty}")
            if (node.weakPoint) appendLine("    - 薄弱点：建议重点复习")
        }
    }

    private fun markdownTimeline(session: CourseSession, result: CourseAnalysisResult): String = buildString {
        appendLine("## Knowledge timeline")
        appendLine()
        session.segments.forEach { segment ->
            appendLine("### Segment ${segment.index}")
            result.knowledgePoints.filter { it.sourceSegmentId == segment.id }.forEach { kp ->
                appendLine("- ${kp.title} (${kp.importance.name}, ${kp.difficulty.name})")
                kp.evidence.firstOrNull()?.let { appendLine("  - evidence: \"${it.quote}\"") }
            }
            appendLine()
        }
    }

    private fun plainTimeline(session: CourseSession, result: CourseAnalysisResult): String =
        markdownToPlain(markdownTimeline(session, result))

    private fun markdownKnowledgePoints(session: CourseSession, result: CourseAnalysisResult): String = buildString {
        appendLine("## Knowledge points and evidence")
        appendLine()
        result.knowledgePoints.forEachIndexed { index, kp ->
            val segmentIndex = session.segments.firstOrNull { it.id == kp.sourceSegmentId }?.index
            appendLine("${index + 1}. ${kp.title}")
            appendLine("   - summary: ${kp.summary}")
            appendLine("   - evidence_segment_id: ${kp.sourceSegmentId}${segmentIndex?.let { " (segment $it)" }.orEmpty()}")
            kp.evidence.forEach { appendLine("   - evidence: \"${it.quote}\"") }
        }
    }

    private fun markdownQuiz(result: CourseAnalysisResult): String = buildString {
        appendLine("## Micro quiz")
        appendLine()
        result.quizQuestions.forEachIndexed { index, q ->
            appendLine("${index + 1}. ${q.stem}")
            q.options.forEach { option ->
                val mark = if (option.isCorrect) " [answer]" else ""
                appendLine("   - ${option.id}: ${option.text}$mark")
            }
            appendLine("   - explanation: ${q.explanation}")
        }
    }

    private fun plainQuiz(result: CourseAnalysisResult): String =
        markdownToPlain(markdownQuiz(result))

    private fun markdownReviewPlan(plan: ReviewPlan): String = buildString {
        appendLine("## Review plan")
        appendLine()
        appendLine("- Total minutes: ${plan.totalEstimatedMinutes}")
        appendLine("- Wrong answer count: ${plan.basis.wrongAnswerCount}")
        appendLine("- Feedback count: ${plan.basis.feedbackCount}")
        plan.steps.forEach { step ->
            appendLine("${step.order}. ${step.title}")
            appendLine("   - activity: ${step.activity.name}")
            appendLine("   - why: ${step.rationale}")
            appendLine("   - minutes: ${step.estimatedMinutes}")
            appendLine("   - knowledge_point_ids: ${step.knowledgePointIds.joinToString(",")}")
        }
    }

    private fun plainReviewPlan(plan: ReviewPlan): String =
        markdownToPlain(markdownReviewPlan(plan))

    private fun markdownVideos(recommendations: List<VideoRecommendation>): String = buildString {
        appendLine("## Video recommendations")
        appendLine()
        appendLine("Video recommendations are supplemental whitelisted resources and do not replace classroom evidence.")
        if (recommendations.isEmpty()) {
            appendLine()
            appendLine("No video recommendation trigger is active.")
        } else {
            recommendations.forEach { rec ->
                appendLine("- ${rec.title} / ${rec.source}")
                appendLine("  - reason: ${rec.reason}")
                appendLine("  - triggered_by: ${rec.triggeredBy}")
                appendLine("  - search_url: ${rec.searchUrl}")
            }
        }
    }

    private fun markdownCourseLibrary(summaries: List<CourseSummary>): String = buildString {
        appendLine("## Course library")
        appendLine()
        if (summaries.isEmpty()) {
            appendLine("No course library summary is available.")
        } else {
            summaries.forEach { course ->
                appendLine("- ${course.courseName}")
                appendLine("  - subject: ${course.subject}")
                appendLine("  - lessons: ${course.lessonCount}")
                appendLine("  - knowledge_points: ${course.knowledgePointTotal}")
                appendLine("  - quiz_items: ${course.quizTotal}")
                appendLine("  - due_review_tasks: ${course.dueReviewTaskCount}")
                appendLine("  - recent_provider: ${course.recentProvider}")
            }
        }
    }

    private fun markdownReviewTasks(snapshot: LearningSnapshot): String = buildString {
        appendLine("## Review tasks")
        appendLine()
        if (snapshot.tasks.isEmpty()) {
            appendLine("No persisted review tasks.")
        } else {
            snapshot.tasks.take(20).forEach { task ->
                appendLine("- ${task.title}")
                appendLine("  - course: ${task.courseTitle}")
                appendLine("  - priority: ${task.priority}")
                appendLine("  - wrong_answer: ${task.counters.wrongAnswer}")
                appendLine("  - need_example: ${task.counters.needExample}")
            }
        }
    }

    private fun markdownWeaknesses(weaknesses: List<WeaknessItem>): String = buildString {
        appendLine("## Weakness summary")
        appendLine()
        if (weaknesses.isEmpty()) {
            appendLine("No active weakness item.")
        } else {
            weaknesses.forEach { item ->
                appendLine("- ${item.title} / ${item.courseTitle}")
                appendLine("  - priority: ${item.priority}")
                appendLine("  - wrong_answer: ${item.wrongAnswerCount}")
                appendLine("  - too_hard: ${item.tooHardCount}")
                appendLine("  - need_example: ${item.needExampleCount}")
                appendLine("  - needs_human_review: ${item.needsHumanReview}")
                appendLine("  - actions: ${item.suggestedActions.joinToString(",")}")
            }
        }
    }

    private fun markdownAskAnswers(answers: List<LessonAnswer>): String = buildString {
        appendLine("## Ask This Lesson")
        appendLine()
        if (answers.isEmpty()) {
            appendLine("No question has been asked for this lesson in the current session.")
        } else {
            answers.forEach { answer ->
                appendLine("- answer: ${answer.answer}")
                appendLine("  - groundedness: ${answer.groundedness}")
                appendLine("  - fallback_used: ${answer.fallbackUsed}")
                appendLine("  - related: ${answer.relatedKnowledgePointTitles.joinToString(",")}")
                answer.evidenceRefs.forEach { ref ->
                    appendLine("  - evidence_ref: ${ref.segmentId ?: "unlocated"}")
                }
            }
        }
    }

    private fun document(title: String, suffix: String, format: ExportFormat, body: () -> String): ExportDocument {
        val content = SafeExportText.redact(body())
        return ExportDocument(
            fileName = ExportFileNames.safe("${title.ifBlank { "ClassMate" }}-$suffix", format),
            format = format,
            mimeType = format.mimeType,
            content = content,
        )
    }

    private fun htmlPage(title: String, markdown: String): String =
        "<!doctype html><html><head><meta charset=\"utf-8\"><title>${escapeHtml(SafeExportText.redact(title))}</title></head>" +
            "<body><pre>${escapeHtml(markdown)}</pre></body></html>"

    private fun markdownToPlain(markdown: String): String =
        markdown
            .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
            .replace("**", "")
            .replace("`", "")

    private fun escapeHtml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}

object ExportFileNames {
    fun safe(baseName: String, format: ExportFormat): String {
        val base = SafeExportText.redact(baseName)
            .replace(Regex("[\\\\/:*?\"<>|\\x00-\\x1F\\[\\]]+"), "-")
            .replace(Regex("\\s+"), "-")
            .trim('-', '.', ' ')
            .take(MAX_BASE_LENGTH)
            .ifBlank { "classmate-report" }
        return "$base.${format.extension}"
    }

    private const val MAX_BASE_LENGTH = 80
}

object SafeExportText {
    private val forbidden = listOf(
        "app" + "Key",
        "api" + "Key",
        "Auth" + "orization",
        "Bear" + "er",
        "app" + "_id",
        "reasoning" + "_content",
        "pro" + "mpt",
        "mes" + "sages",
        "vendor" + " response body",
        "vendor" + " body",
        // Internal pipeline / debug tokens that must never leak into a study export. Only unambiguous
        // code tokens + app-specific phrases are listed — generic study words (embedding, similarity)
        // are intentionally NOT redacted so a CS/ML course export stays intact.
        "LOCAL_FALLBACK",
        "local-learning-pipeline",
        "Evidence chain",
        "mastery events",
        "topHit",
        "BuildConfig",
        "config.local.json",
        "smoke pass",
        "adapter injected",
        "QUERY_REWRITE",
        "TEXT_SIMILARITY",
        "LLM_SUMMARY",
        "QUESTION_GENERATION",
        "REVIEW_UPDATE",
        "Semantic index",
        "Tool steps",
    )

    fun redact(value: String): String =
        forbidden.fold(value) { acc, token ->
            acc.replace(Regex(Regex.escape(token), RegexOption.IGNORE_CASE), "redacted")
        }
}
