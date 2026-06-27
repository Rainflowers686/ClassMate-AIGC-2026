package com.classmate.app.l3

import com.classmate.core.evidence.EvidenceOwnership
import com.classmate.core.evidence.EvidenceRelation
import com.classmate.core.evidence.EvidenceRelationLevel
import com.classmate.core.exporting.SafeExportText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the study-pack markdown that backs every export format. It is a clean *learning* document:
 * Chinese section headings, only review content (summary, knowledge points, easy mistakes, quiz with
 * answers + explanations, wrong book, review plan, diagnosis, evidence index, suggestions). It never
 * emits raw ids, enum names, pipeline-step labels, provider config or English debug headings — when an
 * excerpt can't be located it says so honestly instead of dumping an id.
 */
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

    private const val NO_EVIDENCE = "该内容暂无可回溯原文片段，请结合课堂材料人工确认。"

    fun buildStudyPackMarkdown(snapshot: L3PipelineSnapshot, generatedAt: Long = System.currentTimeMillis()): String {
        val title = snapshot.lessonSource?.title?.ifBlank { null } ?: "ClassMate 学习包"
        val evidenceById = snapshot.evidence.associateBy { it.id }
        val ownership = evidenceOwnershipSnapshot(snapshot)
        val kpById = snapshot.knowledgePoints.associateBy { it.id }
        val sourceTypes = snapshot.evidence.map { sourceTypeZh(it.sourceType) }.distinct()
            .ifEmpty { listOf(sourceTypeZh(snapshot.lessonSource?.type ?: L3SourceType.TEXT)) }
        val lowConfidence = snapshot.qualityWarnings.map { it.message } +
            snapshot.asrJobs.flatMap { job -> job.transcriptSegments.filter { it.lowConfidence }.map { "低置信转写：${it.text.take(80)}" } }

        val body = buildString {
            appendLine("# ${safe(title)} · 复习学习包")
            appendLine()
            appendLine("- 课程：${safe(title)}")
            appendLine("- 生成时间：${formatTime(generatedAt)}")
            appendLine("- 资料来源：${sourceTypes.joinToString("、")}")
            appendLine()

            appendSection("AI 整理摘要") {
                appendBullets(listOfNotBlank(snapshot.summary) + snapshot.keyTakeaways.take(8))
            }

            appendSection("知识点") {
                if (snapshot.knowledgePoints.isEmpty()) {
                    appendLine("- 暂无知识点。")
                } else {
                    snapshot.knowledgePoints.forEachIndexed { index, kp ->
                        appendLine("${index + 1}. ${safe(kp.title)}")
                        appendLine("   - 说明：${safe(kp.explanation)}")
                        appendLine("   - 证据：${evidenceLabelZh(ownership, evidenceById[kp.sourceEvidenceIds.firstOrNull()], kp.title)}")
                    }
                }
            }

            appendSection("易错点") {
                val weakTitles = snapshot.learningDiagnosis.weakKnowledgePoints.map { it.title }.ifEmpty {
                    snapshot.wrongBook.mapNotNull { kpById[it.knowledgePointId]?.title }.distinct()
                }
                appendLine("- 核心概念：${snapshot.knowledgePoints.take(6).joinToString("、") { safe(it.title) }.ifBlank { "暂无" }}")
                appendLine("- 易错点：${weakTitles.joinToString("、").ifBlank { "暂无" }}")
            }

            appendSection("微测题（含答案与解析）") {
                if (snapshot.questions.isEmpty()) {
                    appendLine("- 暂无微测题。")
                } else {
                    snapshot.questions.forEachIndexed { index, q ->
                        appendLine("${index + 1}. ${safe(q.stem)}")
                        q.options.forEachIndexed { optIndex, option ->
                            val text = safe(option)
                            // Options may already carry their own "A. " label from the pipeline — don't double it.
                            val labeled = if (text.matches(Regex("^[A-Za-z][.、).．].*"))) text else "${('A'.code + optIndex).toChar()}. $text"
                            appendLine("   - $labeled")
                        }
                        appendLine("   - 正确答案：${safe(q.correctAnswer)}")
                        appendLine("   - 解析：${safe(q.explanation.ifBlank { "请回到证据核对题干的限定条件，再确认正确选项。" })}")
                        appendLine("   - 证据：${q.evidenceIds.map { evidenceLabelZh(ownership, evidenceById[it], q.stem) }.firstOrNull() ?: NO_EVIDENCE}")
                    }
                }
            }

            appendSection("错题本") {
                if (snapshot.wrongBook.isEmpty()) {
                    appendLine("- 暂无错题记录。")
                } else {
                    snapshot.wrongBook.forEachIndexed { index, wrong ->
                        val question = snapshot.questions.firstOrNull { it.id == wrong.questionId }
                        appendLine("${index + 1}. ${safe(question?.stem ?: "（该题已不在题库）")}")
                        appendLine("   - 你的作答：${safe(wrong.userAnswer)}")
                        appendLine("   - 正确答案：${safe(wrong.correctAnswer)}")
                        appendLine("   - 错因：${safe(wrong.mistakeReason.ifBlank { wrong.explanation })}")
                        appendLine("   - 巩固建议：${safe(wrong.remediationHint.ifBlank { "回到证据重新核对，然后重做这道题。" })}")
                        appendLine("   - 证据：${wrong.evidenceIds.map { evidenceLabelZh(ownership, evidenceById[it], question?.stem.orEmpty()) }.firstOrNull() ?: NO_EVIDENCE}")
                    }
                }
            }

            appendSection("20 分钟复习计划") {
                if (snapshot.reviewQueue.isEmpty()) {
                    appendLine("- [ ] 暂无复习任务，请先生成学习闭环。")
                } else {
                    snapshot.reviewQueue.take(12).forEach { item ->
                        val kp = kpById[item.knowledgePointId]
                        appendLine("- [ ] ${safe(kp?.title ?: "复习知识点")}")
                        appendLine("      安排原因：${safe(item.arrangementReason.ifBlank { "来自学习闭环的复习安排。" })}")
                        appendLine("      建议动作：${safe(item.recommendedActions.joinToString("、").ifBlank { "看证据、重做微测、口头复述。" })}")
                        item.evidenceId?.let { appendLine("      证据：${evidenceLabelZh(ownership, evidenceById[it], kp?.title.orEmpty())}") }
                    }
                }
            }

            appendSection("学习诊断") {
                val diagnosis = snapshot.learningDiagnosis
                appendLine("- 近期复习压力：${safe(diagnosis.recentReviewPressure.ifBlank { "暂无明显压力。" })}")
                appendLine("- 下一步任务：${safe(diagnosis.nextStudyTasks.firstOrNull().orEmpty().ifBlank { "继续执行当前复习计划。" })}")
                diagnosis.weakKnowledgePoints.take(5).forEach {
                    appendLine("- 薄弱点：${safe(it.title)} —— ${safe(it.reason)}")
                }
            }

            appendSection("证据索引") {
                if (snapshot.evidence.isEmpty()) {
                    appendLine("- 暂无证据。")
                } else {
                    snapshot.evidence.take(30).forEach { ev ->
                        val label = ev.sourceLabel.ifBlank { ev.fileName }.ifBlank { sourceTypeZh(ev.sourceType) }
                        val excerpt = ev.snippet.ifBlank { ev.text }.take(120)
                        if (excerpt.isBlank()) {
                            appendLine("- ${sourceTypeZh(ev.sourceType)} · ${safe(label)}：$NO_EVIDENCE")
                        } else {
                            appendLine("- ${sourceTypeZh(ev.sourceType)} · ${safe(label)}：「${safe(excerpt)}」")
                        }
                    }
                }
            }

            if (lowConfidence.isNotEmpty()) {
                appendSection("需人工核对的低置信内容") {
                    lowConfidence.take(12).forEach { appendLine("- ${safe(it)}") }
                }
            }

            appendSection("学习建议") {
                appendLine("- 先看证据再做题：每道题都能回到原文核对题干限定。")
                appendLine("- 错题本里的题，间隔一天再重做一次，巩固记忆。")
                appendLine("- 标注「暂无可回溯原文片段」的内容，请结合课堂材料人工确认。")
            }
        }.trim()
        return SafeExportText.redact(redactForbidden(body))
    }

    private fun StringBuilder.appendSection(title: String, block: StringBuilder.() -> Unit) {
        appendLine("## $title")
        appendLine()
        block()
        appendLine()
    }

    private fun StringBuilder.appendBullets(items: List<String>) {
        if (items.isEmpty()) {
            appendLine("- 暂无内容。")
        } else {
            items.forEach { appendLine("- ${safe(it)}") }
        }
    }

    private fun listOfNotBlank(value: String): List<String> =
        if (value.isBlank()) emptyList() else listOf(value)

    /** A study-facing evidence label: source type (Chinese) + material name, never a raw id. */
    private fun evidenceLabelZh(ownership: EvidenceOwnership.Snapshot, evidence: Evidence?, context: String = ""): String =
        evidence?.let {
            val ownershipLevel = EvidenceOwnership.assess(ownership, it.id)
            if (ownershipLevel == EvidenceRelationLevel.MISSING) return@let NO_EVIDENCE
            val label = it.sourceLabel.ifBlank { it.fileName }.ifBlank { sourceTypeZh(it.sourceType) }
            val excerpt = it.text.ifBlank { it.snippet }.ifBlank { it.transcriptSegment }
            // Flag a likely mis-binding so the export never presents weak evidence as confirmed.
            val weak = ownershipLevel == EvidenceRelationLevel.WEAK ||
                context.isNotBlank() && excerpt.isNotBlank() &&
                EvidenceRelation.assess(excerpt, context) == EvidenceRelationLevel.WEAK
            "${sourceTypeZh(it.sourceType)} · ${safe(label)}${if (weak) "（证据待核对）" else ""}"
        } ?: NO_EVIDENCE

    private fun evidenceOwnershipSnapshot(snapshot: L3PipelineSnapshot): EvidenceOwnership.Snapshot =
        EvidenceOwnership.Snapshot(
            snapshotId = snapshot.lessonSource?.id.orEmpty(),
            lessonSourceId = snapshot.lessonSource?.id.orEmpty(),
            lessonTitle = snapshot.lessonSource?.title.orEmpty(),
            lessonSourceType = snapshot.lessonSource?.type?.name.orEmpty(),
            isSampleCourse = snapshot.lessonSource?.status.equals("SAMPLE", ignoreCase = true),
            evidence = snapshot.evidence.map {
                EvidenceOwnership.EvidenceRecord(
                    id = it.id,
                    sourceId = it.sourceId,
                    sourceType = it.sourceType.name,
                    assetId = it.assetId,
                    sourceLabel = it.sourceLabel,
                    fileName = it.fileName,
                    imageRef = it.imageRef,
                    audioRef = it.audioRef,
                )
            },
            assets = snapshot.evidenceAssets.map {
                EvidenceOwnership.AssetRecord(
                    id = it.id,
                    sourceType = it.sourceType.name,
                    sourceLabel = it.sourceLabel,
                    fileName = it.fileName,
                    imageRef = it.imageRef,
                    audioRef = it.audioRef,
                    createdAt = it.createdAt,
                )
            },
        )

    private fun sourceTypeZh(type: L3SourceType): String = when (type) {
        L3SourceType.TEXT -> "课堂文本"
        L3SourceType.OCR_IMAGE -> "OCR 图片"
        L3SourceType.DOCUMENT -> "文档"
        L3SourceType.AUDIO_TRANSCRIPT -> "课堂录音转写"
        L3SourceType.MANUAL_TRANSCRIPT -> "手动转写"
        L3SourceType.RECORDING_ARTIFACT -> "录音"
        L3SourceType.QUESTION_BANK -> "题库"
        L3SourceType.WEB -> "网络资料"
    }

    private fun safe(value: String): String =
        value.replace(Regex("\\s+"), " ").trim().take(600)

    private fun redactForbidden(value: String): String =
        forbiddenTerms.fold(value) { acc, term -> acc.replace(term, "[已隐藏]", ignoreCase = true) }

    private fun formatTime(value: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(value))
}
