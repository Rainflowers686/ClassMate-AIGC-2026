package com.classmate.core.exporting

/**
 * The single, render-agnostic source for a user-initiated "AI 精修学习包 / 导出资料升级". Unlike the fast
 * default export (rendered from the already-computed analysis), this pack holds ONE block of structured
 * markdown-like text produced by a deliberate, long-running deep model pass (or an honest local
 * organize). PDF / HTML / Word and the in-app study-pack ALL render from [markdown], so the formats can
 * never drift apart.
 *
 * [sourceLabel] is always honest — "蓝心精修版" only when the cloud model actually produced it, "端侧精修草稿"
 * for an on-device pass, "本地整理版" for the deterministic local organize. The local path is NEVER labelled
 * as 蓝心. [markdown] is already redacted via [SafeExportText] at build time: no ids, tokens, or provider trace.
 */
data class PolishedStudyPack(
    val courseTitle: String,
    val sourceLabel: String,
    val generatedAtLabel: String,
    val markdown: String,
) {
    val isBlank: Boolean get() = markdown.isBlank()

    /** A clean document header prepended to every rendered format (course / source / time). */
    fun headedMarkdown(): String = buildString {
        append("# ").append(courseTitle.ifBlank { "ClassMate 精修学习包" }).append(" · 精修学习包\n\n")
        append("- 课程：").append(courseTitle.ifBlank { "未命名课程" }).append('\n')
        append("- 整理来源：").append(sourceLabel.ifBlank { "本地整理版" }).append('\n')
        if (generatedAtLabel.isNotBlank()) append("- 生成时间：").append(generatedAtLabel).append('\n')
        append("- 说明：本学习包由 AI/本地整理，仅供复习参考，请结合课程材料核对。\n\n")
        append(markdown.trim())
    }
}
