package com.classmate.app.l3

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LearningExportEngine {
    private val forbiddenTerms = listOf(
        "AppKey",
        "Authorization",
        "config.local.json",
        "adapter injected",
        "smoke pass",
        "provider body",
        "vendor response",
        "reasoning_content",
    )

    fun buildStudyPackMarkdown(snapshot: L3PipelineSnapshot, generatedAt: Long = System.currentTimeMillis()): String {
        val title = snapshot.lessonSource?.title?.ifBlank { null } ?: "ClassMate study pack"
        val evidenceById = snapshot.evidence.associateBy { it.id }
        val kpById = snapshot.knowledgePoints.associateBy { it.id }
        val sourceTypes = snapshot.evidence.map { it.sourceType.name }.distinct().ifEmpty { listOf(snapshot.lessonSource?.type?.name ?: "TEXT") }
        val lowConfidence = snapshot.qualityWarnings.map { it.message } +
            snapshot.asrJobs.flatMap { job -> job.transcriptSegments.filter { it.lowConfidence }.map { "Low-confidence transcript: ${it.text.take(80)}" } }

        val body = buildString {
            appendLine("# ClassMate study pack")
            appendLine()
            appendLine("- Course: ${safe(title)}")
            appendLine("- Generated: ${formatTime(generatedAt)}")
            appendLine("- Source types: ${sourceTypes.joinToString(", ")}")
            appendLine()

            appendSection("AI organized summary") {
                appendBullets(listOfNotBlank(snapshot.summary) + snapshot.keyTakeaways.take(8))
            }

            appendSection("Knowledge points") {
                if (snapshot.knowledgePoints.isEmpty()) {
                    appendLine("- No knowledge points yet.")
                } else {
                    snapshot.knowledgePoints.forEachIndexed { index, kp ->
                        appendLine("${index + 1}. ${safe(kp.title)}")
                        appendLine("   - Explanation: ${safe(kp.explanation)}")
                        appendLine("   - Mastery: ${kp.masteryState.name}")
                        kp.sourceEvidenceIds.firstOrNull()?.let { appendLine("   - Evidence: ${evidenceLabel(evidenceById[it])}") }
                    }
                }
            }

            appendSection("Key concepts and easy mistakes") {
                val weakTitles = snapshot.learningDiagnosis.weakKnowledgePoints.map { it.title }.ifEmpty {
                    snapshot.wrongBook.mapNotNull { kpById[it.knowledgePointId]?.title }.distinct()
                }
                appendLine("- Key concepts: ${snapshot.knowledgePoints.take(6).joinToString(", ") { safe(it.title) }.ifBlank { "None yet" }}")
                appendLine("- Easy mistakes: ${weakTitles.joinToString(", ").ifBlank { "None yet" }}")
            }

            appendSection("Micro quiz") {
                if (snapshot.questions.isEmpty()) {
                    appendLine("- No quiz items yet.")
                } else {
                    snapshot.questions.forEachIndexed { index, q ->
                        appendLine("${index + 1}. ${safe(q.stem)}")
                        q.options.forEachIndexed { optIndex, option ->
                            appendLine("   - ${('A'.code + optIndex).toChar()}. ${safe(option)}")
                        }
                        appendLine("   - Correct answer: ${safe(q.correctAnswer)}")
                        appendLine("   - Explanation: ${safe(q.explanation)}")
                        appendLine("   - Evidence: ${q.evidenceIds.map { evidenceLabel(evidenceById[it]) }.joinToString("; ").ifBlank { "No evidence linked" }}")
                    }
                }
            }

            appendSection("Wrong book") {
                if (snapshot.wrongBook.isEmpty()) {
                    appendLine("- No wrong answers recorded yet.")
                } else {
                    snapshot.wrongBook.forEachIndexed { index, wrong ->
                        val question = snapshot.questions.firstOrNull { it.id == wrong.questionId }
                        appendLine("${index + 1}. ${safe(question?.stem ?: wrong.questionId)}")
                        appendLine("   - Your answer: ${safe(wrong.userAnswer)}")
                        appendLine("   - Correct answer: ${safe(wrong.correctAnswer)}")
                        appendLine("   - Mistake reason: ${safe(wrong.mistakeReason.ifBlank { wrong.explanation })}")
                        appendLine("   - Remediation: ${safe(wrong.remediationHint.ifBlank { "Review the linked evidence, then retry this question." })}")
                        appendLine("   - Evidence: ${wrong.evidenceIds.map { evidenceLabel(evidenceById[it]) }.joinToString("; ").ifBlank { "No evidence linked" }}")
                    }
                }
            }

            appendSection("20-minute review plan") {
                if (snapshot.reviewQueue.isEmpty()) {
                    appendLine("- [ ] No review task yet. Generate a learning loop first.")
                } else {
                    snapshot.reviewQueue.take(12).forEach { item ->
                        val kp = kpById[item.knowledgePointId]
                        appendLine("- [ ] ${safe(kp?.title ?: item.knowledgePointId)}")
                        appendLine("      Reason: ${safe(item.arrangementReason.ifBlank { "Scheduled from the learning loop." })}")
                        appendLine("      Action: ${safe(item.recommendedActions.joinToString(", ").ifBlank { "Read evidence, retry quiz, explain aloud." })}")
                        item.evidenceId?.let { appendLine("      Evidence: ${evidenceLabel(evidenceById[it])}") }
                    }
                }
            }

            appendSection("Learning diagnosis") {
                val diagnosis = snapshot.learningDiagnosis
                appendLine("- Review pressure: ${safe(diagnosis.recentReviewPressure.ifBlank { "No strong pressure yet." })}")
                appendLine("- Next task: ${safe(diagnosis.nextStudyTasks.firstOrNull().orEmpty().ifBlank { "Continue the current review plan." })}")
                diagnosis.weakKnowledgePoints.take(5).forEach {
                    appendLine("- Weak point: ${safe(it.title)} - ${safe(it.reason)}")
                }
            }

            appendSection("Evidence source index") {
                if (snapshot.evidence.isEmpty()) {
                    appendLine("- No evidence yet.")
                } else {
                    snapshot.evidence.take(30).forEach { ev ->
                        appendLine("- ${ev.id}: ${ev.sourceType.name} / ${safe(ev.sourceLabel.ifBlank { ev.fileName }.ifBlank { ev.sourceId })} / ${safe(ev.snippet.ifBlank { ev.text.take(120) })}")
                    }
                }
            }

            appendSection("Low-confidence notes") {
                if (lowConfidence.isEmpty()) appendLine("- No low-confidence OCR/ASR warning recorded.")
                lowConfidence.take(12).forEach { appendLine("- ${safe(it)}") }
            }

            appendSection("Capability usage note") {
                appendLine("- Cloud model: organizes class content when configured.")
                appendLine("- Edge model: can provide offline or privacy-sensitive fallback when device resources are ready.")
                appendLine("- Local rules: keep summary, quiz, review, and export usable when cloud or edge routes are unavailable.")
            }
        }.trim()
        return redactForbidden(body)
    }

    private fun StringBuilder.appendSection(title: String, block: StringBuilder.() -> Unit) {
        appendLine("## $title")
        appendLine()
        block()
        appendLine()
    }

    private fun StringBuilder.appendBullets(items: List<String>) {
        if (items.isEmpty()) {
            appendLine("- No content yet.")
        } else {
            items.forEach { appendLine("- ${safe(it)}") }
        }
    }

    private fun listOfNotBlank(value: String): List<String> =
        if (value.isBlank()) emptyList() else listOf(value)

    private fun evidenceLabel(evidence: Evidence?): String =
        evidence?.let {
            val label = it.sourceLabel.ifBlank { it.fileName }.ifBlank { it.sourceType.name }
            "${it.id} (${safe(label)})"
        } ?: "Evidence missing"

    private fun safe(value: String): String =
        value.replace(Regex("\\s+"), " ").trim().take(600)

    private fun redactForbidden(value: String): String =
        forbiddenTerms.fold(value) { acc, term -> acc.replace(term, "[redacted]", ignoreCase = true) }

    private fun formatTime(value: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(value))
}
