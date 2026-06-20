package com.classmate.app.l3

object NextReviewPolicy {
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun nextReviewAt(state: L3MasteryState, now: Long): Long = when (state) {
        L3MasteryState.WEAK, L3MasteryState.UNKNOWN, L3MasteryState.LEARNING -> now
        L3MasteryState.REVIEWING -> now + DAY_MS
        L3MasteryState.MASTERED -> now + 3L * DAY_MS
    }

    fun priority(state: L3MasteryState): Int = when (state) {
        L3MasteryState.WEAK -> 3
        L3MasteryState.UNKNOWN, L3MasteryState.LEARNING -> 2
        L3MasteryState.REVIEWING -> 1
        L3MasteryState.MASTERED -> 0
    }
}
