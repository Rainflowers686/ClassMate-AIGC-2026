package com.classmate.core.learning

/**
 * The on-device "local intelligence" suggestion seam used by Report/Export (Phase C) and
 * Practice/Review (Phase D). It builds the (content-free-of-secrets) prompt for the on-device BlueLM
 * 3B and decides the final label:
 *
 *  - on-device available → "由端侧 BlueLM 生成：<text>"
 *  - on-device unavailable → a fixed SAFETY PLACEHOLDER (we NEVER let the deterministic rule path
 *    fabricate study advice).
 *
 * It deliberately does not call the model itself (that is the app controller's job, off the UI
 * thread); it only shapes the prompt and the honest labelling.
 */
object OnDeviceLocalSuggestion {

    /** Shown when the on-device model is unavailable. Never fabricated, never dressed as AI. */
    const val SAFETY_PLACEHOLDER = "模型不可用，建议先复习高优先级任务并重新测试。"

    /** Prefix marking a suggestion as genuinely produced by the on-device model. */
    const val ON_DEVICE_PREFIX = "由端侧 BlueLM 生成："

    /** Build the report-polishing prompt (study advice / next steps). Carries only safe topic labels. */
    fun buildReportPrompt(courseTitle: String, reviewTopics: List<String>, dueCount: Int): String =
        buildString {
            appendLine("你是学习助手。请用 2-3 句中文给出本节课的学习建议与下一步行动，不要编造未提供的内容。")
            appendLine("课程：${courseTitle.ifBlank { "未命名课程" }}")
            if (reviewTopics.isNotEmpty()) appendLine("适合复习的主题：${reviewTopics.take(6).joinToString("、")}")
            appendLine("当前到期复习任务数：$dueCount")
        }.trim()

    /** Build the practice next-step prompt (wrong-answer explanation / what to practise next). */
    fun buildPracticePrompt(courseTitle: String, weakTopics: List<String>, wrongCount: Int): String =
        buildString {
            appendLine("你是学习助手。请用 2-3 句中文解释薄弱原因并给出下一步练习方向，不要编造未提供的内容。")
            appendLine("课程：${courseTitle.ifBlank { "未命名课程" }}")
            if (weakTopics.isNotEmpty()) appendLine("薄弱/需多练知识点：${weakTopics.take(6).joinToString("、")}")
            appendLine("本轮答错题数：$wrongCount")
        }.trim()

    /**
     * Final, honest suggestion label. [onDeviceText] is the model output (or null/blank when the
     * model was unavailable). When present it is prefixed with [ON_DEVICE_PREFIX]; otherwise the
     * fixed [SAFETY_PLACEHOLDER] is returned. The on-device text is trimmed but never modified
     * beyond labelling.
     */
    fun label(onDeviceText: String?): String =
        onDeviceText?.trim()?.takeIf { it.isNotEmpty() }?.let { ON_DEVICE_PREFIX + it } ?: SAFETY_PLACEHOLDER

    /** True when [suggestion] is the genuine on-device output (not the safety placeholder). */
    fun isOnDevice(suggestion: String?): Boolean = suggestion?.startsWith(ON_DEVICE_PREFIX) == true
}
