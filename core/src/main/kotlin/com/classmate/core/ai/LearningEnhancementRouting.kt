package com.classmate.core.ai

/**
 * Adaptive AI Learning Layer (P0-6): a *second* model pass at high-value learning nodes — not only the
 * initial course analysis. Each [AiEnhancementType] routes Cloud (云端蓝心) → On-device (端侧蓝心) → a REAL
 * local template, reusing [AiCapabilityRouter] so the app wires the actual providers as functional stages
 * and every result carries an honest [AiExecutionSource].
 *
 * The local template is a genuine, deterministic generation grounded in the supplied learning data — never
 * a fake button, and never mislabeled as 蓝心. When neither model is wired/available the enhancement still
 * produces useful Chinese output, tagged [AiExecutionSource.SAFE_PLACEHOLDER] ("本地整理").
 */
enum class AiEnhancementType(val displayZh: String, val capability: AiCapability) {
    /** Re-organize a study pack into a clean, printable/reviewable form before export (场景 A). */
    STUDY_PACK_POLISH("学习材料整理", AiCapability.EXPORT_REPORT),

    /** A printable lecture-handout form of the course (title / concepts / KPs / pitfalls / evidence). */
    EXPORT_HANDOUT("讲义版", AiCapability.EXPORT_REPORT),

    /** A pre-exam quick-reference sheet (high-frequency KPs / formulas / confusions / tips). */
    EXAM_CRAM_SHEET("考前速记版", AiCapability.EXPORT_REPORT),

    /** Turn a finished quiz attempt into wrong-reason + weak-point + next-step feedback (场景 B). */
    POST_QUIZ_FEEDBACK("做题反馈", AiCapability.REVIEW_PLAN),

    /** Generate remediation / variant questions for a repeatedly-missed knowledge point (场景 C). */
    WEAKNESS_VARIANTS("薄弱点变式训练", AiCapability.PRACTICE_GENERATION),

    /** Explain how a piece of evidence supports a knowledge point; hedged when the link is weak (场景 D). */
    EVIDENCE_EXPLANATION("证据解释", AiCapability.EVIDENCE_RETRIEVAL),
}

/** One enhancement output: the generated text plus the type it answers. Carried in an [AiCapabilityResult]. */
data class AiEnhancement(val type: AiEnhancementType, val text: String)

private fun <T, R> StageOutcome<T>.mapValue(transform: (T) -> R): StageOutcome<R> = when (this) {
    is StageOutcome.Produced -> StageOutcome.Produced(transform(value), confidence)
    is StageOutcome.Unavailable -> this
}

/**
 * Routes one enhancement through Cloud → On-device → local template. The cloud / on-device stages are
 * functional seams supplied by the app (the real BlueLM provider and on-device bridge); when they return
 * [StageOutcome.Unavailable] the router falls to the always-producing local template terminal.
 */
class RoutedEnhancementUseCase(private val router: AiCapabilityRouter = AiCapabilityRouter()) {

    fun enhance(
        type: AiEnhancementType,
        cloud: () -> StageOutcome<String> = { StageOutcome.Unavailable(AiExecutionStatus.CONFIG_MISSING) },
        onDevice: () -> StageOutcome<String> = { StageOutcome.Unavailable(AiExecutionStatus.UNSUPPORTED_MODALITY) },
        localTemplate: () -> String,
        mode: AiExecutionMode = AiExecutionMode.CLOUD_FIRST,
    ): AiCapabilityResult<AiEnhancement> = router.route(
        capability = type.capability,
        stages = listOf(
            AiStage(AiExecutionSource.CLOUD) { cloud().mapValue { AiEnhancement(type, it) } },
            AiStage(AiExecutionSource.ON_DEVICE) { onDevice().mapValue { AiEnhancement(type, it) } },
        ),
        mode = mode,
        terminal = AiStage(AiExecutionSource.SAFE_PLACEHOLDER) {
            StageOutcome.Produced(AiEnhancement(type, localTemplate()))
        },
    )
}

/**
 * Real, deterministic local generations used as the terminal stage of each enhancement. They are grounded in
 * the supplied data (no fabrication, no fixed marketing text) and always Chinese. These are honest "本地整理"
 * outputs — the router labels them [AiExecutionSource.SAFE_PLACEHOLDER], never 蓝心.
 */
object LocalEnhancementTemplates {

    /** 场景 D — explain how an evidence quote supports a knowledge point; hedge when the link is weak. */
    fun evidenceExplanation(knowledgePointTitle: String, evidenceQuote: String, weak: Boolean): String {
        val quote = evidenceQuote.trim().take(80)
        val core = "这段原文支持知识点「$knowledgePointTitle」：「$quote」。"
        return if (weak) {
            core + "不过这条关联较弱，建议回到原文核对后再作为结论使用。"
        } else {
            core + "它直接给出了该知识点的依据，可作为复习和答题时的回溯来源。"
        }
    }

    /** 场景 B — wrong-reason + weak-point + next-step feedback from a finished attempt. */
    fun postQuizFeedback(total: Int, correct: Int, weakTitles: List<String>): String {
        if (total <= 0) return "还没有作答记录，先完成一组微测再来看反馈。"
        val accuracy = (correct * 100) / total
        val head = "本次共 $total 题，答对 $correct 题（正确率 $accuracy%）。"
        val weak = if (weakTitles.isEmpty()) {
            "整体掌握不错，可以尝试更高难度的题目。"
        } else {
            "需要重点巩固：${weakTitles.take(3).joinToString("、")}。建议先回看证据，再做对应变式题。"
        }
        val next = if (accuracy >= 80) "下一步：进阶练习。" else "下一步：先补薄弱点，再重做错题。"
        return "$head$weak$next"
    }

    /** 场景 C — remediation lead-in for a repeatedly-missed knowledge point. */
    fun weaknessRemediation(knowledgePointTitle: String, missCount: Int): String =
        "知识点「$knowledgePointTitle」已答错 $missCount 次。先用一句话复述定义，再回看原文证据，" +
            "然后从基础变式题开始逐步加难；连续答对后再进入进阶题。"

    /** 场景 A — a short study-pack organization note (the full pack content is assembled elsewhere). */
    fun studyPackNote(courseTitle: String, knowledgePointCount: Int, form: String): String =
        "《$courseTitle》$form：已整理 $knowledgePointCount 个知识点，附答案解析与证据索引，可直接复习或打印。"

    /**
     * 场景 A (讲义版) — a real, structured handout assembled deterministically from the knowledge points and
     * their evidence. No fabrication: every line comes from the supplied course data.
     */
    fun studyPackHandout(courseTitle: String, points: List<EnhancementPoint>): String {
        if (points.isEmpty()) return "《$courseTitle》讲义版：暂无知识点，请先生成学习包。"
        val body = points.take(12).mapIndexed { i, p ->
            buildString {
                append("${i + 1}. ${p.title}")
                if (p.summary.isNotBlank()) append("\n   要点：${p.summary}")
                if (p.evidenceQuote.isNotBlank()) append("\n   证据：「${p.evidenceQuote.trim().take(60)}」")
            }
        }.joinToString("\n")
        return "《$courseTitle》· 讲义版\n共 ${points.size} 个知识点：\n$body\n复习建议：先通读要点，再用证据回溯每个结论。"
    }

    /** 场景 A (考前速记版) — a compact pre-exam sheet of the highest-value points. */
    fun examCramSheet(courseTitle: String, points: List<EnhancementPoint>): String {
        if (points.isEmpty()) return "《$courseTitle》速记版：暂无知识点，请先生成学习包。"
        val lines = points.take(8).joinToString("\n") { "· ${it.title}：${it.summary.ifBlank { "回看证据巩固" }.take(40)}" }
        return "《$courseTitle》· 考前速记版\n$lines\n考前提示：① 先过高频点 ② 核对易混概念 ③ 重做错题 ④ 留 5 分钟检查。"
    }
}

/** A knowledge point projected into the enhancement layer (no raw ids — only learner-facing text). */
data class EnhancementPoint(val title: String, val summary: String, val evidenceQuote: String = "")
