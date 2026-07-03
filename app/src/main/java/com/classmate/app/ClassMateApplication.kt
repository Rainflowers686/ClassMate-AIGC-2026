package com.classmate.app

import android.app.Application
import com.classmate.app.diagnostics.DiagnosticsRuntimeState
import com.classmate.app.diagnostics.PersistentDebugEventLog

class ClassMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val diagnostics = PersistentDebugEventLog(filesDir)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            diagnostics.recordUncaughtCrash(
                throwable = throwable,
                currentScreen = DiagnosticsRuntimeState.currentScreen,
                stateSummary = "${DiagnosticsRuntimeState.stateSummary};thread=${thread.name}",
            )
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                throw throwable
            }
        }
    }
}
