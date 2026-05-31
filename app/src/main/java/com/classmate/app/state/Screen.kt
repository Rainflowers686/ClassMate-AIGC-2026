package com.classmate.app.state

/** The nine destinations. Navigation is a simple in-memory back stack in [AppViewModel]. */
enum class Screen {
    HOME,
    IMPORT,
    ANALYZE,
    KNOWLEDGE,
    EVIDENCE,
    QUIZ,
    REVIEW,
    FEEDBACK,
    SETTINGS,
}

/** Lifecycle of one analysis run, surfaced to the AnalyzeProgress screen. */
enum class AnalysisStatus { IDLE, RUNNING, SUCCESS, FAILED }
