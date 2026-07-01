package com.classmate.app.ui.theme

/**
 * ClassMate Theme Engine v1 presets extracted from the three Stitch references.
 *
 * Fixed mapping:
 * - Dashboard -> STANDARD_STUDY / 默认学习
 * - Main Dashboard -> ACTIVE_STUDY / 活力学习
 * - Vertical Feed -> FOCUS_IMMERSION / 沉浸学习
 */
enum class ThemePreset(
    val displayName: String,
    val shortName: String,
    val sourceLabel: String,
    val tagline: String,
    val description: String,
) {
    STANDARD_STUDY(
        displayName = "默认学习",
        shortName = "Standard Study",
        sourceLabel = "Dashboard",
        tagline = "日常学习 · 柔和留白 · 稳定阅读",
        description = "来自 Dashboard 的 off-white / sage 语言，适合主页、课程、问答、复习、导出和设置。",
    ),
    ACTIVE_STUDY(
        displayName = "活力学习",
        shortName = "Active Study",
        sourceLabel = "Main Dashboard",
        tagline = "练习推进 · 进度反馈 · 清爽行动",
        description = "来自 Main Dashboard 的 green-blue 进度语言，适合练习、反馈、薄弱点和今日复习。",
    ),
    FOCUS_IMMERSION(
        displayName = "沉浸学习",
        shortName = "Focus Immersion",
        sourceLabel = "Vertical Feed",
        tagline = "深度专注 · 暗色层次 · 减少干扰",
        description = "来自 Vertical Feed 的暗色层次，但不带入媒体内容；强调色由 ClassMate 色卡控制。",
    );

    companion object {
        val Default = FOCUS_IMMERSION
    }
}

enum class AccentColorPreset(
    val displayName: String,
    val englishName: String,
    val tokenHex: String,
) {
    BLUE("蓝色", "Blue", "#2563eb"),
    CYAN("青色", "Cyan", "#00a0aa"),
    GREEN("绿色", "Green", "#006d32"),
    PURPLE("紫色", "Purple", "#7c3aed"),
    AMBER("琥珀", "Amber", "#b26a00"),
    ROSE("玫瑰", "Rose", "#ff4b89"),
    GRAPHITE("石墨", "Graphite", "#353535"),
    OCEAN("海蓝", "Ocean", "#0059bb");

    companion object {
        val Default = GREEN
    }
}

enum class TypographyPreset(
    val displayName: String,
    val description: String,
) {
    SYSTEM_DEFAULT("系统默认", "跟随设备默认字形，正文优先稳定可读"),
    ACADEMIC("端正阅读", "标题更稳重，适合长文复习与导出前预览"),
    MODERN_ROUNDED("现代圆润", "标题更柔和，适合日常学习驾驶舱"),
    CLEAN_SANS("清爽无衬线", "更紧凑清晰，适合高密度设置和资料列表"),
    TITLE_PERSONALITY("个性标题", "仅强化大标题气质，正文保持清晰无衬线");

    companion object {
        val Default = ACADEMIC
    }
}

@Deprecated("Use ThemePreset. Kept temporarily so older tests and call sites fail less noisily.")
typealias ThemeOption = ThemePreset
