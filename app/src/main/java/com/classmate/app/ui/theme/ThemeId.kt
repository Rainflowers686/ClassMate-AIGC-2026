package com.classmate.app.ui.theme

/**
 * Three v0.4 themes — see ClassMate UI / UX Design Spec §2.
 *
 *  - [FocusGlass]: default. Quiet, credible, light tech. Designed for the
 *    competition panel and for long-form study.
 *  - [VividStudy]: energetic but not noisy. Warm orange + indigo accents.
 *  - [LowPower]: flat solid colors only. Disables glass, gradient, shadow,
 *    and motion. In v0.4 it is selected manually from Settings.
 */
enum class ThemeId {
    FocusGlass,
    VividStudy,
    LowPower;

    val displayLabel: String
        get() = when (this) {
            FocusGlass -> "Focus Glass · 静研"
            VividStudy -> "Vivid Study · 活力"
            LowPower -> "Low Power · 省电"
        }
}
