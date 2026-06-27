package com.classmate.app.ui.flow

/** Focus-session length options for the Flow companion. Kept tiny + pure so selection/validation is testable. */
object FocusDurations {
    val presets = listOf(15, 25, 45)
    const val DEFAULT_MIN = 25
    private const val MIN_MIN = 5
    private const val MAX_MIN = 180

    /** Clamp any requested length (including custom input) into a sane range. */
    fun coerce(minutes: Int): Int = minutes.coerceIn(MIN_MIN, MAX_MIN)

    fun label(minutes: Int): String = "$minutes 分钟"
}
