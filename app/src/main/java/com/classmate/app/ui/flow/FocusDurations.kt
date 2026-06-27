package com.classmate.app.ui.flow

/** Focus-session length options for the Flow companion. Kept tiny + pure so selection/validation is testable. */
object FocusDurations {
    val presets = listOf(15, 25, 45)
    const val DEFAULT_MIN = 25
    private const val MIN_MIN = 5

    /** Inclusive bounds for a user-typed custom length. */
    const val CUSTOM_MIN = 1
    const val CUSTOM_MAX = 180

    /** Clamp any requested length (including custom input) into a sane range. */
    fun coerce(minutes: Int): Int = minutes.coerceIn(MIN_MIN, CUSTOM_MAX)

    /**
     * Parse a user-typed custom length. Returns the whole-minute count when the text is a valid number
     * in [CUSTOM_MIN]..[CUSTOM_MAX], or null for empty / non-numeric / out-of-range input (the caller
     * then shows [customHint] instead of starting a bogus timer).
     */
    fun parseCustom(text: String): Int? {
        val minutes = text.trim().toIntOrNull() ?: return null
        return if (minutes in CUSTOM_MIN..CUSTOM_MAX) minutes else null
    }

    /** Chinese hint shown when custom input is illegal. */
    val customHint: String = "请输入 $CUSTOM_MIN–$CUSTOM_MAX 之间的分钟数"

    fun label(minutes: Int): String = "$minutes 分钟"
}
