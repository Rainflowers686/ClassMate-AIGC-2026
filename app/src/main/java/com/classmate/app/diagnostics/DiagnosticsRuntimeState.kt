package com.classmate.app.diagnostics

/**
 * Tiny process-local breadcrumb holder for the uncaught-exception handler.
 * Values are deliberately coarse and contain no lesson text or provider payload.
 */
object DiagnosticsRuntimeState {
    @Volatile
    var currentScreen: String = "unknown"

    @Volatile
    var stateSummary: String = "not_ready"

    fun update(screen: String, summary: String) {
        currentScreen = screen.take(48)
        stateSummary = summary.take(240)
    }
}
