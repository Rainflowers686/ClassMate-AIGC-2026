package com.classmate.app.ui.theme

/**
 * The three product themes (Stage 9A positioning).
 *
 * FOCUS is the default and the competition-proof baseline: a calm, warm-white study tool with a
 * restrained blue accent — every task page (Home / Import / Course / Review / Settings) uses it.
 * FLOW is an ambient companion skin scoped to Live Companion / focus-timer scenes only.
 * VITALITY is the reserved opt-in growth/encouragement skin; never the default.
 */
enum class ThemeOption(
    val displayName: String,
    val tagline: String,
    val description: String,
) {
    FOCUS(
        displayName = "专注 Focus",
        tagline = "默认主题 · 白静 · 克制 · 高级",
        description = "暖白灰底色与克制蓝主色，细线条、弱阴影、大留白。课程、证据、复习与导出全部使用 Focus。",
    ),
    VITALITY(
        displayName = "活力 Vitality",
        tagline = "预留主题 · 成长 · 鼓励",
        description = "轻快的靛蓝与成长绿，为成就与学习成长场景预留的可选增强主题，不作为默认。",
    ),
    FLOW(
        displayName = "心流 Flow",
        tagline = "陪学场景 · 夜间书桌 · 呼吸感",
        description = "深色暖琥珀的沉浸陪学氛围，仅用于课堂伴学与专注计时等局部场景，不接管任务页面。",
    );

    companion object {
        val Default = FOCUS
    }
}
