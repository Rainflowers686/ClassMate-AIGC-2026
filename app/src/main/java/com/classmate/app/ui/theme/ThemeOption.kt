package com.classmate.app.ui.theme

/**
 * The three product themes. FOCUS is the default (academic / evidence-first), the other two
 * are switchable in Settings. The choice is intentional: ClassMate's headline value is
 * "evidence-based classroom understanding", and a calm, scholarly look reads as credible to
 * judges and screenshots cleanly — while Vitality and Flow cover long-term motivation and
 * deep-focus study sessions.
 */
enum class ThemeOption(
    val displayName: String,
    val tagline: String,
    val description: String,
) {
    FOCUS(
        displayName = "专注 Focus",
        tagline = "理性 · 知识感 · 课堂感",
        description = "深墨蓝 + 纸感底色，证据用琥珀色高亮。适合高数、计算机、大学物理等概念密集课程。",
    ),
    VITALITY(
        displayName = "活力 Vitality",
        tagline = "年轻 · 鼓励 · 成长感",
        description = "明亮活泼的配色与成长反馈，鼓励学生长期坚持，把复习变成一件想做的事。",
    ),
    FLOW(
        displayName = "心流 Flow",
        tagline = "雨声 · 安静 · 沉浸专注",
        description = "低打扰的冷色夜间基调，为白噪音 / 专注卡片 / 心流学习场景预留空间。",
    );

    companion object {
        val Default = FOCUS
    }
}
