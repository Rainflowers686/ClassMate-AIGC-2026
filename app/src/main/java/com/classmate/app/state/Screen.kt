package com.classmate.app.state

/** All destinations. Navigation is a simple in-memory back stack in [AppViewModel]. */
enum class Screen {
    HOME,
    IMPORT,
    IMPORT_TRAY,
    IMPORT_SETTINGS,
    TRANSCRIPT_IMPORT,
    TRANSCRIPT_EDITOR,
    LIVE,
    ANALYZE,
    KNOWLEDGE,
    COURSE_DETAIL,
    EVIDENCE,
    QUIZ,
    REVIEW,
    PRACTICE,
    FEEDBACK,
    HISTORY,
    SETTINGS,
}

/** The five bottom-navigation tabs and their root [Screen]. Stage 9A adds the 资料 (Import Hub) tab. */
enum class Tab(val root: Screen, val label: String) {
    HOME(Screen.HOME, "首页"),
    IMPORT(Screen.IMPORT, "资料"),
    REVIEW(Screen.REVIEW, "复习"),
    HISTORY(Screen.HISTORY, "课程库"),
    SETTINGS(Screen.SETTINGS, "设置"),
}

/** Lifecycle of one analysis run, surfaced to the AnalyzeProgress screen. */
enum class AnalysisStatus { IDLE, RUNNING, SUCCESS, FAILED }
