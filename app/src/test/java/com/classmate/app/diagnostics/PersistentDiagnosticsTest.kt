package com.classmate.app.diagnostics

import com.classmate.app.state.AppViewModel
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentDiagnosticsTest {

    @Test
    fun appendPersistsAfterReloadAndRedactsSensitiveLookingFields() {
        val dir = Files.createTempDirectory("classmate-diagnostics").toFile()
        val log = PersistentDebugEventLog(dir)

        log.append(
            id = 1,
            name = "practice.complete.clicked",
            details = mapOf("app_key_present" to "abc123", "count" to "3"),
        )

        val reloaded = PersistentDebugEventLog(dir).loadEvents()
        assertEquals(1, reloaded.size)
        assertEquals("practice.complete.clicked", reloaded.first().name)
        assertEquals("redacted", reloaded.first().details["app_key_present"])
        assertEquals("3", reloaded.first().details["count"])
        assertFalse(reloaded.first().format().contains("abc123"))
    }

    @Test
    fun crashRecordAppearsInDiagnosticsPackage() {
        val dir = Files.createTempDirectory("classmate-crash").toFile()
        val log = PersistentDebugEventLog(dir)

        log.recordUncaughtCrash(
            throwable = IllegalStateException("boom"),
            currentScreen = "PRACTICE",
            stateSummary = "practice=3",
            now = 123L,
        )

        val crash = log.lastCrashText()
        assertTrue(crash.contains("exception=IllegalStateException"))
        assertTrue(crash.contains("screen=PRACTICE"))
        assertTrue(crash.contains("stackTraceTop20="))
        assertTrue(log.diagnosticsPackage("PRACTICE", "practice=3").contains("lastCrash:"))
    }

    @Test
    fun crashRecordIncludesCauseStackWhenPresent() {
        val dir = Files.createTempDirectory("classmate-crash-cause").toFile()
        val log = PersistentDebugEventLog(dir)
        val error = IllegalStateException("outer", IllegalArgumentException("inner"))

        log.recordUncaughtCrash(
            throwable = error,
            currentScreen = "REVIEW",
            stateSummary = "screen=review",
            now = 456L,
        )

        val crash = log.lastCrashText()
        assertTrue(crash.contains("stackTraceTop20="))
        assertTrue(crash.contains("cause=IllegalArgumentException"))
        assertTrue(crash.contains("causeStackTop10="))
        assertFalse(crash.contains("AppKey=abc"))
    }

    @Test
    fun appViewModelLoadsPersistentEventsAndCanClearThem() {
        val dir = Files.createTempDirectory("classmate-vm-diagnostics").toFile()
        val first = AppViewModel(debugEventLog = PersistentDebugEventLog(dir))

        first.exitPractice()
        assertTrue(first.ui.debugEvents.map { it.name }.contains("practice.back.clicked"))

        val second = AppViewModel(debugEventLog = PersistentDebugEventLog(dir))
        assertTrue(second.ui.debugEvents.map { it.name }.contains("practice.back.clicked"))
        assertTrue(second.diagnosticsPackageText().contains("ClassMate diagnostics"))

        second.clearDiagnosticsLog()
        val third = AppViewModel(debugEventLog = PersistentDebugEventLog(dir))
        assertTrue(third.ui.debugEvents.isEmpty())
    }
}
